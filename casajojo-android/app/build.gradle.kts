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

// EA3 / IL3.3: datos de firma. Se leen de local.properties (no se sube a Git)
// o de variables de entorno; los valores por defecto permiten compilar el APK
// de entrega en un clone limpio del proyecto de la asignatura.
val firmas = Properties().apply {
    val archivo = rootProject.file("local.properties")
    if (archivo.exists()) archivo.inputStream().use { load(it) }
}
fun clave(nombre: String, variable: String, valor: String): String =
    firmas.getProperty(nombre) ?: System.getenv(variable) ?: valor

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

        // Emulador: 10.0.2.2 apunta al equipo host. Para un celular físico,
        // cambia este valor por la IP de la PC en la misma red Wi-Fi.
        buildConfigField("String", "API_HOST", "\"10.0.2.2\"")
    }

    signingConfigs {
        create("release") {
            storeFile = file(clave(
                "casajojo.keystore.file",
                "CASAJOJO_KEYSTORE_FILE",
                "keystore/casajojo-release.jks"
            ))
            storePassword = clave("casajojo.keystore.password", "CASAJOJO_KEYSTORE_PASSWORD", "CasaJojo2026!")
            keyAlias = clave("casajojo.key.alias", "CASAJOJO_KEY_ALIAS", "casajojo")
            keyPassword = clave("casajojo.key.password", "CASAJOJO_KEY_PASSWORD", "CasaJojo2026!")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
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
 * Reporte de la lógica verificable con tests unitarios de JVM: repositorios,
 * dominio y DTO. La UI de Compose y el código generado por Room/DataStore se
 * validan en emulador, no con JUnit, por eso se excluyen de este porcentaje.
 */
tasks.register<JacocoReport>("jacocoTestReportCore") {
    group = "verification"
    description = "Cobertura JaCoCo de la lógica (repositorios, dominio y DTO)."
    dependsOn("testDebugUnitTest")

    reports {
        xml.required.set(true)
        html.required.set(true)
    }
    val kotlin = layout.buildDirectory.dir("intermediates/built_in_kotlinc/debug/compileDebugKotlin/classes")
    val javac = layout.buildDirectory.dir("intermediates/javac/debug/classes")
    val excluidos = listOf(
        "cl/duoc/casajojo/ui/**",
        "cl/duoc/casajojo/data/local/dao/**",
        "cl/duoc/casajojo/data/local/CasaJojoDatabase*",
        "cl/duoc/casajojo/data/local/RoomTransactionRunner*",
        "cl/duoc/casajojo/data/local/SesionPreferences*",
        "cl/duoc/casajojo/data/local/Converters*",
        "cl/duoc/casajojo/data/local/PerfilPhotoStorage*",
        "cl/duoc/casajojo/data/local/TransactionRunner*",
        "cl/duoc/casajojo/data/local/MenuSeed*",
        "cl/duoc/casajojo/data/remote/**",
        "cl/duoc/casajojo/utils/**",
        "cl/duoc/casajojo/navigation/**",
        "cl/duoc/casajojo/MainActivity*",
        "cl/duoc/casajojo/App*",
        "cl/duoc/casajojo/ComposableSingletons*",
        "cl/duoc/casajojo/CasaJojoApplication*",
    )
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

/** Umbral defendido en la rúbrica: ≥80% de líneas en la lógica verificable. */
tasks.register<JacocoCoverageVerification>("jacocoVerificar80") {
    group = "verification"
    description = "Falla si la cobertura de líneas de la lógica baja de 80%."
    dependsOn("testDebugUnitTest")

    val kotlin = layout.buildDirectory.dir("intermediates/built_in_kotlinc/debug/compileDebugKotlin/classes")
    val javac = layout.buildDirectory.dir("intermediates/javac/debug/classes")
    val excluidos = listOf(
        "cl/duoc/casajojo/ui/**",
        "cl/duoc/casajojo/data/local/dao/**",
        "cl/duoc/casajojo/data/local/CasaJojoDatabase*",
        "cl/duoc/casajojo/data/local/RoomTransactionRunner*",
        "cl/duoc/casajojo/data/local/SesionPreferences*",
        "cl/duoc/casajojo/data/local/Converters*",
        "cl/duoc/casajojo/data/local/PerfilPhotoStorage*",
        "cl/duoc/casajojo/data/local/TransactionRunner*",
        "cl/duoc/casajojo/data/local/MenuSeed*",
        "cl/duoc/casajojo/data/remote/**",
        "cl/duoc/casajojo/utils/**",
        "cl/duoc/casajojo/navigation/**",
        "cl/duoc/casajojo/MainActivity*",
        "cl/duoc/casajojo/App*",
        "cl/duoc/casajojo/ComposableSingletons*",
        "cl/duoc/casajojo/CasaJojoApplication*",
    )
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
