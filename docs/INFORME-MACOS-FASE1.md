# Informe fase 1 — Cliente JavaFX en GitHub + instalador macOS

**Repositorio:** https://github.com/Erick5222/OsifirmaCliente  
**Proyecto local:** `osifirma_client_repo - copia`  
**Fecha:** 2026-07-13

## Que se logro

- Fuentes preparadas para publicacion en GitHub (sin remote Bitbucket)
- JWT real eliminado de `CP-16` (reemplazado por placeholder y rutas de ejemplo)
- `.gitignore` actualizado (target, logs, instaladores, .gk)
- Perfil Maven `installer-mac` para empaquetado macOS con jpackage
- Script `installer/build-installer-macos.sh`
- Workflow GitHub Actions `macos-build.yml` (runner macos-latest)
- Documentacion `docs/INSTALADOR-MACOS.md` y `README.md` actualizado
- Pipeline Windows MSI existente conservado sin cambios funcionales

## Archivos modificados

- `.gitignore`
- `pom.xml` — perfil `installer-mac`
- `parametria-casos/CP-16-local-ruta-windows.json` — sanitizado
- `README.md`

## Archivos agregados

- `installer/build-installer-macos.sh`
- `.github/workflows/macos-build.yml`
- `docs/INSTALADOR-MACOS.md`
- `docs/INFORME-MACOS-FASE1.md`

## Configuracion Git

- Remote anterior (Bitbucket) eliminado
- Remote `origin` → `https://github.com/Erick5222/OsifirmaCliente.git`
- Rama `main` con commit inicial limpio

## GitHub Actions

| Workflow | Runner | Salida |
|----------|--------|--------|
| `msi-build.yml` | windows-latest | MSI (Windows) |
| `macos-build.yml` | macos-latest | .app / dmg / pkg (macOS) |

El workflow macOS usa `continue-on-error: true` en el build para no bloquear el pipeline
mientras falta `firmaOsinergmin` en CI.

## Errores esperados en CI macOS

1. **firmaOsinergmin no resuelto** — dependencia privada, no en Maven Central
2. **DMG sin code signing** — requiere certificado Apple Developer
3. **Certificados de firma digital** — el cliente usa Windows-MY; en Mac no aplica aun

## Construccion local macOS

```bash
chmod +x installer/build-installer-macos.sh
./installer/build-installer-macos.sh
```

## Construccion desde GitHub Actions

Push a `main` → Actions → **Build Osinergmin Firmador macOS** → descargar artifact.

## Proximos pasos

1. Publicar `firmaOsinergmin` en GitHub Packages o agregar paso install-file en CI
2. Ejecutar workflow y revisar logs del primer build
3. Validar `.app` en Mac con dependencia instalada localmente
4. Agregar Info.plist para protocolo URL en macOS
5. Code signing y notarizacion para distribucion externa
