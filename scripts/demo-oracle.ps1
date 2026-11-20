<#
.SYNOPSIS
Arranca los microservicios que consume la app (auth, catálogo y ventas) con
las credenciales Oracle del entorno local.

REQUISITO: crear .env en la raíz del repo con:
  ORACLE_USERNAME=...
  ORACLE_PASSWORD=...
  ORACLE_SERVICE=clases_high
  TNS_ADMIN=C:/Users/ben95/.elvolcan/OracleWallet   (usar /, no \)

El .env está ignorado por Git. Este script no imprime contraseñas.
#>
param(
    [string]$EnvFile = (Join-Path $PSScriptRoot '..\.env'),
)

$ErrorActionPreference = 'Stop'

if (Test-Path $EnvFile) {
    foreach ($linea in Get-Content $EnvFile) {
        if ($linea -match '^\s*([A-Z0-9_]+)=(.*)$') {
            $valor = $Matches[2].Trim().Trim("'", '"')
            [Environment]::SetEnvironmentVariable($Matches[1], $valor, 'Process')
        }
    }
} else {
    throw "No encontré $EnvFile. Crea el .env local con las variables Oracle."
}

# En la URL JDBC de Oracle las barras deben ser /, nunca \.
$env:TNS_ADMIN = ($env:TNS_ADMIN -replace '\\', '/')
if (-not $env:ORACLE_SERVICE) { $env:ORACLE_SERVICE = 'clases_high' }

# La app Android no usa Eureka; lo desactivamos para una demo limpia.
$env:EUREKA_CLIENT_REGISTER_WITH_EUREKA = 'false'
$env:EUREKA_CLIENT_FETCH_REGISTRY = 'false'

$base = Join-Path $PSScriptRoot '..\microservicios mobil'
$modulos = @(
    @{ Nombre = 'auth     '; Ruta = 'MiniMarket-Proyecto-ms-auth'; Puerto = 8081 },
    @{ Nombre = 'catalogo '; Ruta = 'MiniMarket-Proyecto-ms-catalogoProductos'; Puerto = 8082 },
    @{ Nombre = 'ventas   '; Ruta = 'MiniMarket-Proyecto-ms-ventas'; Puerto = 8086 },
)

foreach ($modulo in $modulos) {
    $directorio = Join-Path $base $modulo.Ruta
    $mvnw = Join-Path $directorio 'mvnw.cmd'
    if (-not (Test-Path $mvnw)) { throw "No encontré $mvnw" }

    Start-Process -FilePath $mvnw `
        -ArgumentList 'spring-boot:run' `
        -WorkingDirectory $directorio `
        -PassThru | Out-Null

    Write-Host ("Arrancado ms-{0} en http://localhost:{1}" -f $modulo.Nombre.Trim(), $modulo.Puerto)
}

Write-Host ''
Write-Host 'Protegido: credenciales solo en variables de entorno del proceso.'
Write-Host 'Para el emulador, la app usa 10.0.2.2:<puerto> (ver ApiConfig.kt).'
