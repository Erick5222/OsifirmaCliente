# Instalador — Osinergmin Firmador (firma-desktop-client)

Guía para generar e instalar el cliente de escritorio **sin cambiar** el flujo de desarrollo habitual (`mvn javafx:run`).

## Requisitos

| Componente | Detalle |
|------------|---------|
| Maven | 3.6+ (`C:\maven\bin\mvn.cmd` o en PATH) |
| JDK **completo** 21+ | Debe incluir `jpackage.exe` (no basta con JRE) |
| `firmaOsinergmin` | En repositorio Maven local (`~/.m2` o carpeta `Avance_ENTREGABLES_LIBRERIA`) |
| Windows 64 bits | Perfil actual orientado a `javafx.platform=win` |

Comprobar JDK:

```powershell
java -version
# Debe existir:
& "$env:JAVA_HOME\bin\jpackage.exe" --version
```

Si `jpackage` no está en PATH, instale [JDK 21](https://adoptium.net/) y defina `JAVA_HOME`.

## Generar instalador (recomendado)

Desde la raíz del proyecto:

```powershell
.\installer\build-installer.ps1
```

Para intentar MSI (requiere [WiX Toolset](https://wixtoolset.org/) en PATH):

```powershell
.\installer\build-installer.ps1 -Msi
```

Equivalente manual:

```powershell
# Solo portable (sin jpackage):
mvn -Pinstaller package -Dskip.jpackage=true

# Con instalador nativo:
mvn -Pinstaller package -Dskip.jpackage=false
```

## Salidas

| Ruta | Descripción |
|------|-------------|
| `target/installer/libs/` | Todos los JAR (app, JavaFX win, firmaOsinergmin y dependencias) |
| `target/installer/OsinergminFirmador.bat` | Lanzador portable (usa Java del sistema) |
| `target/installer/portable/OsinergminFirmador.bat` | Lanzador portable (apunta a `../libs`) |
| `target/installer/dist/` | App-image o MSI generado por `jpackage` |

### Portable (sin jpackage)

1. `mvn -Pinstaller package -Dskip.jpackage=true`
2. Ejecutar `target\installer\OsinergminFirmador.bat`
3. Requiere **JDK/JRE 21+** instalado en la máquina.

### Instalador nativo (con jpackage)

1. `.\installer\build-installer.ps1`
2. Abrir `target\installer\dist\` y ejecutar el `.exe` del instalador o la carpeta `Osinergmin Firmador`.

La instalación empaquetada arranca con:

- `-Dfirma.parametria.enabled=false` (invocación y **param** desde la URL del protocolo / simulador, no desde `parametria.json` en disco).
- El bloque `param` del JSON pegado en el navegador viaja como `paramB64` en la URL (`osinergmin-firmador://firmar?...&paramB64=...`).

## Instalar en su máquina

1. Ejecute el MSI o el setup dentro de `target/installer/dist/`.
2. Siga el asistente (acceso directo en menú Inicio).
3. La primera ejecución puede pedir certificados Windows y red corporativa según su caso.

## Actualizaciones entre versiones (MSI)

El instalador MSI está preparado para **major upgrade**: el usuario ejecuta el nuevo `.msi` y Windows reemplaza la versión anterior sin desinstalar manualmente.

### Qué ya hace el build

| Concepto MSI | Comportamiento |
|--------------|----------------|
| **UpgradeCode** | Fijo en `pom.xml` (`installer.upgrade.uuid`). Identifica la familia «Osinergmin Firmador». |
| **ProductCode** | Lo genera jpackage automáticamente a partir de vendor + nombre + **versión** (cambia en cada release). |
| **ProductVersion** | Viene de `installer.app.version` (`--app-version`). |
| **WiX** | `main.wxs` incluye tabla `Upgrade`, `RemoveExistingProducts` y permite upgrades/downgrades (plantilla jpackage). |

### Publicar una nueva versión

1. Edite **solo** `installer.app.version` en `pom.xml` (perfil `installer`), por ejemplo `2.4.0` → `2.4.1`.
2. **No modifique** `installer.upgrade.uuid` ni `installer.app.name` ni `installer.vendor`.
3. Genere el MSI:

```powershell
.\installer\build-installer.ps1 -Msi
```

4. Distribuya `target\installer\dist\Osinergmin Firmador-<versión>.msi`.

**Importante:** en cada release el número de versión **debe ser mayor** que el instalado. Si genera otro MSI con la misma versión (p. ej. otro build de `2.4.0` cuando ya hay `2.4.0` instalado), Windows mostrará el error 1638 («Ya está instalada otra versión…»). Eso es comportamiento normal de Windows Installer, no un fallo del empaquetado.

### Convención de numeración (semver)

Use `MAJOR.MINOR.PATCH` (máx. 255.255.65535 en MSI):

| Cambio | Ejemplo | Cuándo |
|--------|---------|--------|
| PATCH | 2.4.0 → 2.4.1 | Correcciones, sin cambios funcionales relevantes |
| MINOR | 2.4.1 → 2.5.0 | Nuevas funciones compatibles |
| MAJOR | 2.5.0 → 3.0.0 | Cambios incompatibles o rediseño importante |

Windows Installer compara solo los **tres primeros segmentos** de la versión.

### Verificar que la actualización funciona

1. Instale el MSI de la versión anterior (p. ej. 2.4.0) en una VM o PC de prueba.
2. Compruebe en «Programas y características» que aparece **una** entrada «Osinergmin Firmador».
3. Anote el ProductCode instalado (opcional):

```powershell
Get-ItemProperty HKLM:\Software\Microsoft\Windows\CurrentVersion\Uninstall\* |
  Where-Object { $_.DisplayName -eq 'Osinergmin Firmador' } |
  Select-Object DisplayName, DisplayVersion, PSChildName
```

4. Incremente `installer.app.version`, genere el nuevo MSI e instálelo sobre la versión existente.
5. Confirme:
   - Sigue habiendo **una sola** entrada en «Programas y características».
   - `DisplayVersion` refleja la nueva versión.
   - La aplicación y el protocolo `osinergmin-firmador://` siguen funcionando.

Log detallado de instalación (opcional):

```powershell
msiexec /i "target\installer\dist\Osinergmin Firmador-2.5.0.msi" /L*v install-upgrade.log
```

Busque en el log `UpgradeCode`, `RemoveExistingProducts` y `ProductVersion`.

## Invocar desde una web

### Protocolo `osinergmin-firmador://`

El **MSI** generado con `-Msi` registra automáticamente el protocolo en el Registro de Windows (HKCR), apuntando a `[INSTALLDIR]Osinergmin Firmador.exe`. No hace falta ejecutar `.reg` manualmente tras instalar el MSI.

La personalización WiX está en `installer/wix/main.wxs` (copiada a `target/installer/jpackage-resources/` durante el build).

Para **app-image** o **portable** (sin MSI), siga usando `installer/register-protocol.reg.template`.

### Enlace en la página

```html
<a href="osinergmin-firmador://firmar?idSolicitud=SOL-001&token=JWT&origenDocumento=alfresco&idDocumentoAlfresco=NODE_ID&sinVisor=true&modo=soloFirma">
  Firmar
</a>
```

El cliente interpreta la URL en `ExternalInvokeParams` (protocolos `osinergmin-firmador:` y `firmador:`).

### 3. Parametría completa (XAdES, `param.*`, etc.)

El protocolo cubre sobre todo el bloque **invocacion**. Para el bloque **param** use una de estas opciones:

- Pasar ruta JSON con variables de entorno en el registro (avanzado), o
- Extender el portal para escribir un JSON temporal y lanzar con script que defina `FIRMA_PARAMETRIA_PATH`.

En desarrollo: `parametria.enabled=true` en `client.properties`. En instalador: desactivado por defecto vía `-Dfirma.parametria.enabled=false`.

## Integridad del proyecto

- El perfil Maven **`installer`** es opcional (`-Pinstaller`); no altera `mvn compile`, tests ni `javafx:run`.
- No se usa `jlink` directo (incompatible con dependencias automáticas de `firmaOsinergmin`).
- `jpackage` usa modo **classpath** (`--input` + `--main-class`) con JavaFX en `--module-path=$APPDIR`.
- El portable usa `ALL-MODULE-PATH` sobre la carpeta `libs/` (excluye `jakarta.activation` duplicado).

## Solución de problemas

| Problema | Acción |
|----------|--------|
| `jpackage` no reconocido | Instalar JDK completo y `JAVA_HOME` |
| Falla `firmaOsinergmin` | `mvn install` en la librería o copiar a `~/.m2/repository/pe/gob/osinergmin/firmaOsinergmin/1.0/` |
| MSI no se crea | Use `app-image` (por defecto) o instale WiX |
| Web abre app pero ignora parámetros | Verificar `parametria.enabled` y que no exista `parametria.json` junto al `.exe` |
| Enlace no abre nada (MSI) | Reinstale el MSI; el protocolo se registra en la instalación |
| Enlace no abre nada (portable) | Use `register-protocol.reg.template` con la ruta del `.exe` |
| Error 1638 / «Ya está instalada otra versión» | Incremente `installer.app.version` (p. ej. 2.4.0 → 2.4.1) antes de generar el MSI, o desinstale la versión actual desde «Programas y características». Reinstalar el **mismo** número de versión no está soportado. |

## Simulador de URLs

Proyecto Angular: `ProyectoFirmasDigitalesCLiente/firma-desktop-simulador` — genera URLs `osinergmin-firmador://firmar?...` para pruebas.
