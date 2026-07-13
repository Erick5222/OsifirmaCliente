# Dependencias Maven no publicadas en repositorios remotos

Este directorio contiene artefactos instalados localmente en CI/CD mediante
`mvn install:install-file` antes del build.

## firmaOsinergmin-1.0.jar

- **groupId:** `pe.gob.osinergmin`
- **artifactId:** `firmaOsinergmin`
- **version:** `1.0`
- **Uso:** motor criptografico PAdES/XAdES/CAdES del cliente desktop

**Nota de seguridad:** libreria interna OSINERGMIN. Si el repositorio es publico,
evaluar hacerlo privado o migrar a GitHub Packages con acceso restringido.

## Instalacion local

```bash
mvn install:install-file \
  -Dfile=libs/firmaOsinergmin-1.0.jar \
  -DgroupId=pe.gob.osinergmin \
  -DartifactId=firmaOsinergmin \
  -Dversion=1.0 \
  -Dpackaging=jar \
  -DpomFile=libs/firmaOsinergmin-1.0.pom
```

PowerShell:

```powershell
mvn install:install-file `
  -Dfile=libs/firmaOsinergmin-1.0.jar `
  -DgroupId=pe.gob.osinergmin `
  -DartifactId=firmaOsinergmin `
  -Dversion=1.0 `
  -Dpackaging=jar `
  -DpomFile=libs/firmaOsinergmin-1.0.pom
```
