import java.util.Properties
import org.gradle.testing.jacoco.tasks.JacocoCoverageVerification
import org.gradle.testing.jacoco.tasks.JacocoReport
import org.gradle.testing.jacoco.plugins.JacocoTaskExtension

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    id("jacoco")
}

// EFT (Fase A): la firma se lee de keystore.properties (versionado junto al .jks,
// como exige la rúbrica). Las variables CASAJOJO_* del entorno tienen prioridad.
val firmas = Properties().apply {
    val archivo = rootProject.file("keystore.properties")
    if (archivo.exists()) archivo.inputStream().use { load(it) }
}
fun clave(nombre: String, variable: String): String? =
    (System.getenv(variable) ?: firmas.getProperty(nombre))?.takeIf { it.isNotBlank() }

val firmaArchivo = clave("storeFile", "CASAJOJO_KEYSTORE_FILE")
val firmaStorePassword = clave("storePassword", "CASAJOJO_KEYSTORE_PASSWORD")
val firmaAlias = clave("keyAlias", "CASAJOJO_KEY_ALIAS")
val firmaKeyPassword = clave("keyPassword", "CASAJOJO_KEY_PASSWORD")
val firmaCompleta = listOf(firmaArchivo, firmaStorePassword, firmaAlias, firmaKeyPassword).all { it != null }
if (!firmaCompleta) {
    logger.warn("Casa JoJo: falta keystore.properties; el APK release quedará SIN firmar.")
}

// EFT (Fase A): host de los microservicios. Prioridad: -PapiHost=... > casajojo.apiHost
// en local.properties > 10.0.2.2 (la PC vista desde el emulador). Aplica a debug y release.
val localProps = Properties().apply {
    val archivo = rootProject.file("local.properties")
    if (archivo.exists()) archivo.inputStream().use { load(it) }
}
val apiHost: String = (findProperty("apiHost") as String?)?.takeIf { it.isNotBlank() }
    ?: localProps.getProperty("casajojo.apiHost")?.takeIf { it.isNotBlank() }
    ?: "10.0.2.2"

android {
    namespace = "cl.duoc.casajojo"
    compileSdk = 37
    buildToolsVersion = "36.0.0"

    defaultConfig {
        applicationId = "cl.duoc.casajojo"
        minSdk = 26
        targetSdk = 37
        versionCode = 3
        versionName = "0.3.0" // EA3: microservicios + Retrofit + APK firmado

        // Emulador: 10.0.2.2 apunta al equipo host. Para un celular físico se compila
        // con -PapiHost=<IP de la PC> (ver docs/DEMO.md).
        buildConfigField("String", "API_HOST", "\"$apiHost\"")
    }

    signingConfigs {
        // Solo se crea si están los 4 datos; así un clone limpio sigue compilando en debug.
        if (firmaCompleta) {
            create("release") {
                storeFile = file(firmaArchivo!!)
                storePassword = firmaStorePassword
                keyAlias = firmaAlias
                keyPassword = firmaKeyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (firmaCompleta) {
                signingConfig = signingConfigs.getByName("release")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // NUEVO (fase 1): DataStore Preferences, guarda la sesión del usuario.
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.play.services.code.scanner)

    // NUEVO (EA3 / IL3.1): Retrofit + Gson + logging de OkHttp para consumir
    // los microservicios de La Casa JoJo siguiendo el patrón de la guía.
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp.logging)

    testImplementation(libs.junit)
    // NUEVO (EA3 / IL3.2): Kotest para aserciones, MockK para mocks y
    // coroutines-test para corrutinas. Se mantiene JUnit4 como runner.
    testImplementation(libs.mockk)
    testImplementation(libs.kotest.assertions)
    testImplementation(libs.kotlinx.coroutines.test)

    debugImplementation(libs.androidx.compose.ui.tooling)
}

// EA3 / IL3.2: cobertura de las pruebas unitarias. Genera HTML y XML para
// acreditar el porcentaje en la defensa. No modifica el APK de entrega.
jacoco {
    toolVersion = "0.8.13"
}

tasks.withType<Test>().configureEach {
    extensions.configure<JacocoTaskExtension> {
        isIncludeNoLocationClasses = true
        excludes = listOf("jdk.internal.*")
    }
}

tasks.register<JacocoReport>("jacocoTestReportDebug") {
    group = "verification"
    description = "Genera el reporte JaCoCo de las pruebas unitarias debug."
    dependsOn("testDebugUnitTest")

    reports {
        xml.required.set(true)
        html.required.set(true)
    }
    // AGP 9 compila Kotlin en built_in_kotlinc; el javac queda vacío en este proyecto.
    val kotlin = layout.buildDirectory.dir("intermediates/built_in_kotlinc/debug/compileDebugKotlin/classes")
    val javac = layout.buildDirectory.dir("intermediates/javac/debug/classes")
    sourceDirectories.setFrom(files("src/main/java"))
    classDirectories.setFrom(files(kotlin, javac))
    executionData.setFrom(
        fileTree(layout.buildDirectory) {
            include("jacoco/testDebugUnitTest.exec", "jacoco/testDebugUnitTest.ec")
        }
    )
}

/**
 * EFT (Fase F): lo que NO entra al porcentaje. Se mide ViewModel + Repository + domain;
 * la UI de Compose (pantallas, componentes, tema), Room/DataStore generados, Retrofit y
 * utilidades de Android se validan en el emulador, no con JUnit.
 */
val excluidosCobertura = listOf(
    "cl/duoc/casajojo/ui/screens/**",
    "cl/duoc/casajojo/ui/components/**",
    "cl/duoc/casajojo/ui/layouts/**",
    "cl/duoc/casajojo/ui/theme/**",
    "cl/duoc/casajojo/ui/App*",
    "cl/duoc/casajojo/ui/ComposableSingletons*",
    "cl/duoc/casajojo/ui/NavegacionApp*",
    "cl/duoc/casajojo/ui/viewmodels/AvisosPedido\$DelSistema*",
    "cl/duoc/casajojo/data/local/dao/**",
    "cl/duoc/casajojo/data/local/CasaJojoDatabase*",
    "cl/duoc/casajojo/data/local/RoomTransactionRunner*",
    "cl/duoc/casajojo/data/local/SesionPreferences*",
    "cl/duoc/casajojo/data/local/Converters*",
    "cl/duoc/casajojo/data/local/PerfilPhotoStorage*",
    "cl/duoc/casajojo/data/local/TransactionRunner*",
    "cl/duoc/casajojo/data/local/MenuSeed*",
    "cl/duoc/casajojo/data/local/entity/**",
    "cl/duoc/casajojo/data/remote/**",
    "cl/duoc/casajojo/utils/**",
    "cl/duoc/casajojo/navigation/**",
    "cl/duoc/casajojo/MainActivity*",
    "cl/duoc/casajojo/ComposableSingletons*",
    "cl/duoc/casajojo/CasaJojoApplication*",
)

/**
 * Reporte de la lógica verificable con tests unitarios de JVM: ViewModel,
 * Repository y domain. La UI de Compose y el código generado por Room/DataStore se
 * validan en emulador, no con JUnit, por eso se excluyen de este porcentaje.
 */
tasks.register<JacocoReport>("jacocoTestReportCore") {
    group = "verification"
    description = "Cobertura JaCoCo de ViewModel, Repository y domain."
    dependsOn("testDebugUnitTest")

    reports {
        xml.required.set(true)
        html.required.set(true)
    }
    val kotlin = layout.buildDirectory.dir("intermediates/built_in_kotlinc/debug/compileDebugKotlin/classes")
    val javac = layout.buildDirectory.dir("intermediates/javac/debug/classes")
    val excluidos = excluidosCobertura
    sourceDirectories.setFrom(files("src/main/java"))
    classDirectories.setFrom(
        fileTree(kotlin.get().asFile) {
            include("**/*.class")
            exclude(excluidos)
        },
        fileTree(javac.get().asFile) {
            include("**/*.class")
            exclude(excluidos)
        },
    )
    executionData.setFrom(
        fileTree(layout.buildDirectory) {
            include("jacoco/testDebugUnitTest.exec", "jacoco/testDebugUnitTest.ec")
        }
    )
}

/** Umbral defendido en la rúbrica: ≥80% de líneas en ViewModel, Repository y domain. */
tasks.register<JacocoCoverageVerification>("jacocoVerificar80") {
    group = "verification"
    description = "Falla si la cobertura de líneas de la lógica baja de 80%."
    dependsOn("testDebugUnitTest")

    val kotlin = layout.buildDirectory.dir("intermediates/built_in_kotlinc/debug/compileDebugKotlin/classes")
    val javac = layout.buildDirectory.dir("intermediates/javac/debug/classes")
    val excluidos = excluidosCobertura
    classDirectories.setFrom(
        fileTree(kotlin.get().asFile) { include("**/*.class"); exclude(excluidos) },
        fileTree(javac.get().asFile) { include("**/*.class"); exclude(excluidos) },
    )
    executionData.setFrom(
        fileTree(layout.buildDirectory) {
            include("jacoco/testDebugUnitTest.exec", "jacoco/testDebugUnitTest.ec")
        }
    )
    violationRules {
        rule {
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "0.80".toBigDecimal()
            }
        }
    }
}
