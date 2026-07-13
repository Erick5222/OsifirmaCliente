#!/usr/bin/env bash
# Genera el firmador empaquetado para macOS.
# Uso:
#   ./installer/build-installer-macos.sh              # app-image (.app)
#   ./installer/build-installer-macos.sh dmg          # DMG (requiere Xcode CLI tools)
#   ./installer/build-installer-macos.sh pkg          # PKG

set -euo pipefail

PACKAGE_TYPE="${1:-app-image}"
PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$PROJECT_ROOT"

echo "== Osinergmin Firmador - build instalador macOS =="
echo "Proyecto: $PROJECT_ROOT"
echo "Tipo: $PACKAGE_TYPE"

if ! command -v java &>/dev/null; then
  echo "ERROR: java no encontrado. Instale JDK 21."
  exit 1
fi

if ! command -v jpackage &>/dev/null; then
  echo "ERROR: jpackage no encontrado. Use un JDK 21 completo (no solo JRE)."
  exit 1
fi

# GitHub Actions macos-latest y Mac Apple Silicon usan aarch64
ARCH="$(uname -m)"
if [[ "$ARCH" == "arm64" ]]; then
  JAVAFX_PLATFORM="mac-aarch64"
else
  JAVAFX_PLATFORM="mac"
fi
echo "Arquitectura: $ARCH -> javafx.platform=$JAVAFX_PLATFORM"

if [[ "$PACKAGE_TYPE" == "dmg" ]]; then
  if ! xcode-select -p &>/dev/null; then
    echo "AVISO: Xcode Command Line Tools no detectadas. DMG puede fallar."
    echo "       Ejecute: xcode-select --install"
  fi
fi

MVN_ARGS=(
  clean
  -Pinstaller-mac
  package
  -DskipTests
  -Dskip.jpackage=false
  "-Dinstaller.type=$PACKAGE_TYPE"
  "-Djavafx.platform=$JAVAFX_PLATFORM"
)

echo "Ejecutando: mvn ${MVN_ARGS[*]}"
mvn "${MVN_ARGS[@]}"
EXIT_CODE=$?

DIST="$PROJECT_ROOT/target/installer/dist"
echo ""
if [[ -d "$DIST" ]]; then
  echo "Salida generada en: $DIST"
  ls -la "$DIST" || true
  find "$DIST" -maxdepth 2 \( -name "*.app" -o -name "*.dmg" -o -name "*.pkg" \) -print 2>/dev/null || true
else
  echo "AVISO: No se creo target/installer/dist"
fi

echo ""
echo "Ver docs/INSTALADOR-MACOS.md para errores conocidos y proximos pasos."
exit $EXIT_CODE
