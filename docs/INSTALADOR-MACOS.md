# Instalador macOS — Osinergmin Firmador (fase 1)

Documentacion inicial para generar un empaquetado macOS del cliente JavaFX.
**No se garantiza un instalador funcional en esta etapa.** El objetivo es dejar
la estructura, scripts y CI reproducibles, y documentar errores pendientes.

## Estrategia

Herramienta principal: **jpackage** (JDK 21).

Tipos de salida soportados por el script:

- `app-image` — genera `Osinergmin Firmador.app` (recomendado para primera prueba)
- `dmg` — disco instalable (requiere Xcode Command Line Tools)
- `pkg` — instalador nativo macOS (fase posterior)

## Requisitos locales (Mac)

- macOS 12 o superior
- JDK 21 completo con `jpackage` (Temurin, Oracle o Microsoft Build)
- Maven 3.6+
- `firmaOsinergmin:1.0` instalado en `~/.m2/repository/pe/gob/osinergmin/firmaOsinergmin/1.0/`
- Para DMG: Xcode Command Line Tools (`xcode-select --install`)

## Construccion local

Desde la raiz del proyecto:

```bash
chmod +x installer/build-installer-macos.sh
./installer/build-installer-macos.sh
```

Para intentar DMG:

```bash
./installer/build-installer-macos.sh dmg
```

Salida esperada: `target/installer/dist/`

## Construccion en GitHub Actions

Workflow: `.github/workflows/macos-build.yml`

- Runner: `macos-latest` (Apple Silicon → `javafx.platform=mac-aarch64`)
- Se dispara en push a `main`/`master` o manualmente (`workflow_dispatch`)
- Artifact: `Osinergmin-Firmador-macOS-{version}`

Pasos:

1. Push a `main` en https://github.com/Erick5222/OsifirmaCliente
2. Ir a **Actions** → **Build Osinergmin Firmador macOS**
3. Revisar logs y descargar artifact si se genero

## Diferencias respecto al instalador Windows

| Componente | Windows | macOS |
|----------|---------|-------|
| Script | `build-installer.ps1` | `build-installer-macos.sh` |
| Perfil Maven | `installer` | `installer-mac` |
| JavaFX classifier | `win` | `mac` / `mac-aarch64` |
| Empaquetado | MSI + WiX | app-image / dmg / pkg |
| Certificados | Windows-MY (MSCAPI) | Keychain (no implementado) |
| Protocolo URL | WiX + registro | Info.plist (pendiente) |

## Versionamiento

Version publicada: propiedad `installer.app.version` en `pom.xml` (perfiles `installer` e `installer-mac`).

Valor actual: **1.0.3**

Incrementar solo esa propiedad antes de cada release del instalador.

## Dependencias en CI

La libreria `firmaOsinergmin-1.0.jar` se incluye en `libs/` e instala automaticamente
en GitHub Actions antes del build (`mvn install:install-file`).

**Advertencia:** el repositorio es publico. Si la libreria es confidencial, hacer el
repo privado o migrar a GitHub Packages.

## Dependencias pendientes (fase 2)

Para distribucion fuera del equipo de desarrollo se requiere:

- Certificado Developer ID Application
- Notarizacion con `notarytool`
- Configurar `jpackage` con `--mac-sign` y credenciales

No implementado en fase 1.

## Errores esperados (fase 1)

| Error | Causa probable | Accion |
|-------|----------------|--------|
| `Could not find artifact firmaOsinergmin` | JAR no en CI | Publicar dependencia |
| JavaFX classifier vacio | Arquitectura incorrecta | Usar `mac-aarch64` en Apple Silicon |
| DMG falla | Sin Xcode CLI o sin signing | Probar `app-image` primero |
| Certificado no encontrado | Windows-MY no existe en Mac | Adaptacion Keychain (futuro) |
| Protocolo URL no abre app | Sin Info.plist custom | Agregar en `jpackage-resources` |

## Proximos pasos

1. Ejecutar workflow y revisar logs del primer build con firmaOsinergmin
2. Validar `.app` en Mac real o runner
3. Agregar Info.plist para protocolo URL en macOS
4. Implementar acceso a certificados via Keychain
5. Habilitar DMG firmado y notarizado para distribucion
