@echo off
setlocal EnableExtensions
cd /d "%~dp0"

set "LIBS=%~dp0libs"
if not exist "%LIBS%\firma-desktop-client-1.0-SNAPSHOT.jar" set "LIBS=%~dp0..\libs"

if not exist "%LIBS%\firma-desktop-client-1.0-SNAPSHOT.jar" (
  echo [ERROR] No se encontro el JAR de la aplicacion en "%LIBS%"
  echo Ejecute antes: mvn -Pinstaller package
  exit /b 1
)

where java >nul 2>&1
if errorlevel 1 (
  echo [ERROR] Java no esta en el PATH. Instale JDK 21 o superior.
  exit /b 1
)

java ^
  --module-path "%LIBS%" ^
  --add-modules com.osinergmin.firma.desktop,javafx.controls,javafx.fxml,javafx.swing,ALL-MODULE-PATH ^
  -Dfirma.parametria.enabled=false ^
  -Dfirma.signing.image.path=%~dp0assets\osinergmin-sello.png ^
  -m com.osinergmin.firma.desktop/com.osinergmin.firma.desktop.App %*

endlocal
