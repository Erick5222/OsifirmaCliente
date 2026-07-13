# Osinergmin Firmador — Cliente JavaFX

Cliente de escritorio para firma digital (PAdES, XAdES, CAdES) del ecosistema Osifirma.

Repositorio: https://github.com/Erick5222/OsifirmaCliente

## Dependencias

- JDK 21, Maven 3.6+
- `firmaOsinergmin` en `libs/` — instalar con `mvn install:install-file` (ver `libs/README.md`)

## Ejecutar en desarrollo

```bash
mvn clean javafx:run
```

## Instalador Windows

```powershell
.\installer\build-installer.ps1 -Msi
```

Ver `installer/INSTALADOR.md` y `docs/protocolo-build.txt`.

## Instalador macOS (fase 1)

```bash
chmod +x installer/build-installer-macos.sh
./installer/build-installer-macos.sh
```

Ver `docs/INSTALADOR-MACOS.md` para limitaciones y errores esperados.

## CI/CD (GitHub Actions)

- **Windows MSI:** `.github/workflows/msi-build.yml`
- **macOS:** `.github/workflows/macos-build.yml`

## Version del instalador

Propiedad `installer.app.version` en `pom.xml` (perfiles `installer` / `installer-mac`).
Valor actual: **1.0.3**

## Estructura

```
src/                 Codigo JavaFX
installer/           Scripts de empaquetado (Windows + macOS)
parametria-casos/    Casos de prueba JSON
.github/workflows/   Pipelines CI/CD
docs/                Documentacion de build
```
