# Genera el firmador empaquetado.
# Por defecto: carpeta con .exe (app-image), NO genera .msi.
# Para MSI: instale WiX Toolset y ejecute: .\installer\build-installer.ps1 -Msi

param(
    [switch]$Msi
)

$ErrorActionPreference = "Stop"
$ProjectRoot = Split-Path -Parent $PSScriptRoot
$Mvn = if (Test-Path "C:\maven\bin\mvn.cmd") { "C:\maven\bin\mvn.cmd" } else { "mvn" }

function Find-Jpackage {
    if ($env:JAVA_HOME) {
        $candidate = Join-Path $env:JAVA_HOME "bin\jpackage.exe"
        if (Test-Path $candidate) { return $candidate }
    }
    $javaCmd = Get-Command java -ErrorAction SilentlyContinue
    if ($javaCmd) {
        $bin = Split-Path $javaCmd.Source -Parent
        $candidate = Join-Path $bin "jpackage.exe"
        if (Test-Path $candidate) { return $candidate }
        $jdkRoot = Split-Path $bin -Parent
        $candidate = Join-Path $jdkRoot "bin\jpackage.exe"
        if (Test-Path $candidate) { return $candidate }
    }
    $searchRoots = @(
        "C:\Program Files\Java",
        "C:\Program Files\Eclipse Adoptium",
        "C:\Program Files\Microsoft",
        "C:\Program Files\BellSoft",
        "C:\Program Files\Amazon Corretto"
    )
    foreach ($root in $searchRoots) {
        if (-not (Test-Path $root)) { continue }
        $found = Get-ChildItem -Path $root -Recurse -Filter "jpackage.exe" -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($found) { return $found.FullName }
    }
    return $null
}

function Find-WixBin {
    $candle = Get-Command candle.exe -ErrorAction SilentlyContinue
    if ($candle) { return Split-Path $candle.Source -Parent }
    $searchRoots = @(
        "${env:ProgramFiles(x86)}\WiX Toolset v3.14\bin",
        "${env:ProgramFiles(x86)}\WiX Toolset v3.11\bin",
        "$env:ProgramFiles\WiX Toolset v3.14\bin",
        "$env:ProgramFiles\WiX Toolset v4.0\bin"
    )
    foreach ($dir in $searchRoots) {
        if (Test-Path (Join-Path $dir "candle.exe")) { return $dir }
    }
    return $null
}

Write-Host "== Osinergmin Firmador - build instalador ==" -ForegroundColor Cyan
Write-Host "Proyecto: $ProjectRoot"

$jpackage = Find-Jpackage
$extraArgs = @("-Pinstaller", "package", "-DskipTests")
if ($jpackage) {
    $jdkBin = Split-Path $jpackage -Parent
    $env:JAVA_HOME = Split-Path $jdkBin -Parent
    $env:Path = "$jdkBin;" + $env:Path
    $extraArgs += "-Dskip.jpackage=false"
    if ($Msi) {
        $extraArgs += "-Dinstaller.type=msi"
        $wixBin = Find-WixBin
        if ($wixBin) {
            $env:Path = "$wixBin;" + $env:Path
            Write-Host "WiX Toolset: $wixBin" -ForegroundColor Green
        } else {
            Write-Host "AVISO: WiX Toolset no detectado (candle.exe / light.exe)." -ForegroundColor Yellow
            Write-Host "      Instale WiX 3+ desde https://wixtoolset.org/ antes de generar el .msi" -ForegroundColor Yellow
        }
    }
    Write-Host "JDK con jpackage: $env:JAVA_HOME" -ForegroundColor Green
} else {
    $extraArgs += "-Dskip.jpackage=true"
    Write-Host "AVISO: No se encontro jpackage.exe (se requiere JDK completo, no solo JRE)." -ForegroundColor Yellow
    Write-Host "       Se generara solo el empaquetado portable en target\installer\" -ForegroundColor Yellow
}

Push-Location $ProjectRoot
try {
    $distDir = Join-Path $ProjectRoot "target\installer\dist"
    if (Test-Path $distDir) {
        Write-Host "Limpiando instalador anterior: $distDir" -ForegroundColor DarkGray
        Remove-Item -Recurse -Force $distDir
    }
    if ($extraArgs -notcontains "clean") {
        $extraArgs = @("clean") + $extraArgs
    }
    & $Mvn @extraArgs
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

    Write-Host ""
    if (-not $Msi) {
        Write-Host "Nota: se genero app-image (carpeta con .exe), no un .msi." -ForegroundColor Yellow
        Write-Host "      Para MSI: instale WiX (https://wixtoolset.org/) y ejecute: .\installer\build-installer.ps1 -Msi" -ForegroundColor Yellow
    }
    if ($jpackage -and (Test-Path "target\installer\dist")) {
        Get-ChildItem "target\installer\dist" | ForEach-Object {
            Write-Host "Instalador / app-image: $($_.FullName)" -ForegroundColor Green
            $exe = Get-ChildItem $_.FullName -Filter "*.exe" -Recurse -ErrorAction SilentlyContinue | Select-Object -First 1
            if ($exe) {
                Write-Host "Ejecutable: $($exe.FullName)" -ForegroundColor Green
            }
        }
        if ($Msi) {
            $builtMsi = Get-ChildItem "target\installer\dist\*.msi" -ErrorAction SilentlyContinue | Select-Object -First 1
            if ($builtMsi -and $builtMsi.Name -match '-(\d+\.\d+\.\d+)\.msi$') {
                $builtVer = $Matches[1]
                $installed = Get-ItemProperty "HKLM:\Software\Microsoft\Windows\CurrentVersion\Uninstall\*" -ErrorAction SilentlyContinue |
                    Where-Object { $_.DisplayName -eq 'Osinergmin Firmador' } | Select-Object -First 1
                if ($installed) {
                    Write-Host ""
                    Write-Host "Instalado en este PC: $($installed.DisplayVersion)  ->  MSI generado: $builtVer" -ForegroundColor Cyan
                    if ($installed.DisplayVersion -eq $builtVer) {
                        Write-Host "AVISO: Misma version que la instalada. Windows dara error 1638 al ejecutar el MSI." -ForegroundColor Yellow
                        Write-Host "      Incremente installer.app.version en pom.xml (ej. $builtVer -> siguiente patch) o desinstale antes." -ForegroundColor Yellow
                    } elseif ($installed.DisplayVersion -gt $builtVer) {
                        Write-Host "AVISO: La version instalada ($($installed.DisplayVersion)) es mayor que el MSI generado." -ForegroundColor Yellow
                    } else {
                        Write-Host "OK: El MSI deberia actualizar $($installed.DisplayVersion) -> $builtVer automaticamente." -ForegroundColor Green
                    }
                }
            }
        }
    }
    $zipPath = "target\installer\OsinergminFirmador-portable.zip"
    if (Test-Path "target\installer\libs") {
        Compress-Archive -Path "target\installer\libs", "target\installer\assets", "target\installer\OsinergminFirmador.bat" -DestinationPath $zipPath -Force
        Write-Host "ZIP portable: $((Resolve-Path $zipPath).Path)" -ForegroundColor Green
    }
    if (Test-Path "target\installer\OsinergminFirmador.bat") {
        Write-Host "Portable (requiere Java 21+ en PATH): target\installer\OsinergminFirmador.bat" -ForegroundColor Green
    }
    Write-Host ""
    Write-Host "Siguiente paso: ver installer\INSTALADOR.md" -ForegroundColor Cyan
} finally {
    Pop-Location
}
