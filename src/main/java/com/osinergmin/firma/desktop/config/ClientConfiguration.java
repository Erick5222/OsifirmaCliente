package com.osinergmin.firma.desktop.config;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

public final class ClientConfiguration {

    private static final String RESOURCE = "/com/osinergmin/firma/desktop/config/client.properties";
    private static final String BUNDLED_STAMP_RESOURCE = "/com/osinergmin/firma/desktop/assets/osinergmin-sello.png";
    private static final String INSTALL_STAMP_FILE = "assets/osinergmin-sello.png";

    private static volatile Path cachedBundledStampPath;

    private static volatile Properties loaded;

    private ClientConfiguration() {
    }

    private static Properties properties() {
        if (loaded != null) {
            return loaded;
        }
        synchronized (ClientConfiguration.class) {
            if (loaded != null) {
                return loaded;
            }
            Properties p = new Properties();
            try (InputStream in = ClientConfiguration.class.getResourceAsStream(RESOURCE)) {
                if (in != null) {
                    p.load(in);
                }
            } catch (IOException ignored) {
                // valores por defecto en codigo
            }
            loaded = p;
            return loaded;
        }
    }

    public static Optional<Path> getDocumentsDirectory() {
        String sys = System.getProperty("firma.documents.dir");
        if (sys != null && !sys.isBlank()) {
            return Optional.of(Path.of(sys.trim()));
        }
        String env = System.getenv("FIRMA_DOCUMENTS_DIR");
        if (env != null && !env.isBlank()) {
            return Optional.of(Path.of(env.trim()));
        }
        String v = properties().getProperty("documents.directory", "").trim();
        if (v.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(Path.of(v));
    }

    public static String getAuthApiBaseUrl() {
        String sys = System.getProperty("firma.auth.api.baseUrl");
        if (sys != null && !sys.isBlank()) {
            return trimTrailingSlash(sys.trim());
        }
        String env = System.getenv("FIRMA_AUTH_API_BASE_URL");
        if (env != null && !env.isBlank()) {
            return trimTrailingSlash(env.trim());
        }
        String v = properties().getProperty("auth.api.baseUrl", "").trim();
        if (v.isEmpty()) {
            throw new IllegalStateException(
                    "auth.api.baseUrl no configurada. Defina auth.api.baseUrl en client.properties, "
                            + "FIRMA_AUTH_API_BASE_URL o -Dfirma.auth.api.baseUrl");
        }
        return trimTrailingSlash(v);
    }

    public static String getAuthLoginUrl() {
        return getAuthApiBaseUrl() + "/api/auth/login";
    }

    public static String getAuthValidateTokenUrl() {
        return getAuthApiBaseUrl() + "/api/auth/validate-token";
    }

    public static String getAlfrescoContentUrl(String nodeId) {
        return getAlfrescoContentUrl(nodeId, null);
    }

    public static String getAlfrescoContentUrl(String nodeId, Integer version) {
        String id = nodeId == null ? "" : nodeId.trim();
        String url = getAuthApiBaseUrl() + "/api/alfresco/content/" + id;
        if (version != null && version > 0) {
            return url + "?version=" + version;
        }
        return url;
    }

    public static String getAlfrescoUploadUrl(String nodeId) {
        String id = nodeId == null ? "" : nodeId.trim();
        return getAuthApiBaseUrl() + "/api/alfresco/documents/" + id + "/upload";
    }

    public static String getBitacoraEventoUrl() {
        return getAuthApiBaseUrl() + "/api/bitacora/evento";
    }

    public static boolean isBitacoraEnabled() {
        String sys = System.getProperty("firma.bitacora.enabled");
        if (sys != null && !sys.isBlank()) {
            return Boolean.parseBoolean(sys.trim());
        }
        String env = System.getenv("FIRMA_BITACORA_ENABLED");
        if (env != null && !env.isBlank()) {
            return Boolean.parseBoolean(env.trim());
        }
        return Boolean.parseBoolean(properties().getProperty("bitacora.enabled", "true"));
    }

    public static boolean isAuditLogEnabled() {
        String sys = System.getProperty("firma.audit.log.enabled");
        if (sys != null && !sys.isBlank()) {
            return Boolean.parseBoolean(sys.trim());
        }
        String env = System.getenv("FIRMA_AUDIT_LOG_ENABLED");
        if (env != null && !env.isBlank()) {
            return Boolean.parseBoolean(env.trim());
        }
        return Boolean.parseBoolean(properties().getProperty("audit.log.enabled", "true"));
    }

    public static Path getAuditLogFilePath() {
        String sys = System.getProperty("firma.audit.log.path");
        if (sys != null && !sys.isBlank()) {
            return Path.of(sys.trim()).toAbsolutePath().normalize();
        }
        String env = System.getenv("FIRMA_AUDIT_LOG_PATH");
        if (env != null && !env.isBlank()) {
            return Path.of(env.trim()).toAbsolutePath().normalize();
        }
        String configured = properties().getProperty("audit.log.path", "logs/firma-desktop-audit.log").trim();
        Path path = Path.of(configured.isEmpty() ? "logs/firma-desktop-audit.log" : configured);
        if (path.isAbsolute()) {
            return path.normalize();
        }
        return Path.of(System.getProperty("user.dir", ".")).resolve(path).toAbsolutePath().normalize();
    }

    public static boolean isParametriaEnabled() {
        String sys = System.getProperty("firma.parametria.enabled");
        if (sys != null && !sys.isBlank()) {
            return Boolean.parseBoolean(sys.trim());
        }
        String env = System.getenv("FIRMA_PARAMETRIA_ENABLED");
        if (env != null && !env.isBlank()) {
            return Boolean.parseBoolean(env.trim());
        }
        return Boolean.parseBoolean(properties().getProperty("parametria.enabled", "false"));
    }

    /** Busca parametria.json en varias ubicaciones habituales (IDE, Maven, JAR). */
    public static Optional<Path> findParametriaFile() {
        String configured = configuredParametriaRelativePath();
        Path configuredPath = Path.of(configured);
        if (configuredPath.isAbsolute()) {
            return Files.isRegularFile(configuredPath)
                    ? Optional.of(configuredPath.toAbsolutePath().normalize())
                    : Optional.empty();
        }
        for (Path candidate : parametriaCandidates(configuredPath)) {
            if (Files.isRegularFile(candidate)) {
                return Optional.of(candidate.toAbsolutePath().normalize());
            }
        }
        return Optional.empty();
    }

    public static Path getParametriaFilePath() {
        return findParametriaFile()
                .orElseGet(
                        () ->
                                Path.of(System.getProperty("user.dir", "."))
                                        .resolve(configuredParametriaRelativePath())
                                        .toAbsolutePath()
                                        .normalize());
    }

    private static String configuredParametriaRelativePath() {
        String sys = System.getProperty("firma.parametria.path");
        if (sys != null && !sys.isBlank()) {
            return sys.trim();
        }
        String env = System.getenv("FIRMA_PARAMETRIA_PATH");
        if (env != null && !env.isBlank()) {
            return env.trim();
        }
        return properties().getProperty("parametria.path", "parametria.json").trim();
    }

    private static List<Path> parametriaCandidates(Path relative) {
        Set<Path> candidates = new LinkedHashSet<>();
        Path cwd = Path.of(System.getProperty("user.dir", ".")).toAbsolutePath().normalize();
        candidates.add(cwd.resolve(relative));
        candidates.add(cwd.resolve("firma-desktop-client").resolve(relative));
        if (cwd.getParent() != null) {
            candidates.add(cwd.getParent().resolve(relative));
        }
        candidates.add(cwd.resolve("..").normalize().resolve(relative));

        try {
            URL codeLocation =
                    ClientConfiguration.class.getProtectionDomain().getCodeSource().getLocation();
            if (codeLocation != null) {
                Path codePath = Path.of(codeLocation.toURI()).toAbsolutePath().normalize();
                Path start = Files.isDirectory(codePath) ? codePath : codePath.getParent();
                Path walk = start;
                for (int depth = 0; depth < 6 && walk != null; depth++) {
                    candidates.add(walk.resolve(relative));
                    if (Files.isRegularFile(walk.resolve("pom.xml"))) {
                        break;
                    }
                    walk = walk.getParent();
                }
            }
        } catch (URISyntaxException | NullPointerException ignored) {
            // best effort
        }

        return new ArrayList<>(candidates);
    }

    private static String trimTrailingSlash(String url) {
        while (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        return url;
    }

    /** Valores empaquetados en client.properties (sin panel admin). */
    public static String getBundledSigningTslUrl() {
        return properties()
                .getProperty("signing.tsl.url", "https://iofe.indecopi.gob.pe/TSL/tsl-pe.xml")
                .trim();
    }

    public static boolean getBundledSigningVerifyTsl() {
        return Boolean.parseBoolean(properties().getProperty("signing.verify.tsl", "false"));
    }

    /** TSL efectiva: panel de administracion o empaquetado. */
    public static String getSigningTslUrl() {
        return SigningAdminSettings.effectiveTslUrl();
    }

    public static boolean isSigningVerifyTsl() {
        return SigningAdminSettings.effectiveVerifyTsl();
    }

    /**
     * Roles JWT con acceso al panel de administracion (provisional Keycloak realm-management).
     * Actualizar cuando Seguridad defina el rol oficial.
     */
    public static Set<String> getAdminJwtRoles() {
        String raw = properties().getProperty("admin.jwt.roles", "view-clients,query-clients,query-users");
        if (raw == null || raw.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> s.toLowerCase(Locale.ROOT))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public static String getSigningImagePath() {
        String sys = System.getProperty("firma.signing.image.path");
        if (sys != null && !sys.isBlank()) {
            return sys.trim();
        }
        String env = System.getenv("FIRMA_SIGNING_IMAGE_PATH");
        if (env != null && !env.isBlank()) {
            return env.trim();
        }
        return properties().getProperty("signing.image.path", "").trim();
    }

    /** Ruta del PNG de sello grafico: config explicita, carpeta de instalacion o recurso empaquetado. */
    public static Optional<Path> resolveSigningImageFile() {
        Optional<Path> configured = resolveConfiguredSigningImageFile();
        if (configured.isPresent()) {
            return configured;
        }
        Optional<Path> installAssets = resolveInstallAssetsStamp();
        if (installAssets.isPresent()) {
            return installAssets;
        }
        return resolveBundledStampImageFile();
    }

    private static Optional<Path> resolveConfiguredSigningImageFile() {
        String configured = getSigningImagePath();
        if (configured.isEmpty()) {
            return Optional.empty();
        }
        Path candidate = Path.of(configured);
        if (Files.isRegularFile(candidate)) {
            return Optional.of(candidate.toAbsolutePath().normalize());
        }
        return Optional.empty();
    }

    /** Sello copiado junto al instalador portable o app-image ({@code assets/osinergmin-sello.png}). */
    private static Optional<Path> resolveInstallAssetsStamp() {
        try {
            var codeSource = ClientConfiguration.class.getProtectionDomain().getCodeSource();
            if (codeSource == null || codeSource.getLocation() == null) {
                return Optional.empty();
            }
            Path base = Path.of(codeSource.getLocation().toURI()).getParent();
            if (base == null) {
                return Optional.empty();
            }
            List<Path> candidates = new ArrayList<>();
            candidates.add(base.resolve(INSTALL_STAMP_FILE));
            Path parent = base.getParent();
            if (parent != null) {
                candidates.add(parent.resolve(INSTALL_STAMP_FILE));
            }
            for (Path stamp : candidates) {
                if (Files.isRegularFile(stamp)) {
                    return Optional.of(stamp.toAbsolutePath().normalize());
                }
            }
        } catch (Exception ignored) {
            // sin code source (tests) o URI no file:
        }
        return Optional.empty();
    }

    private static Optional<Path> resolveBundledStampImageFile() {
        if (cachedBundledStampPath != null && Files.isRegularFile(cachedBundledStampPath)) {
            return Optional.of(cachedBundledStampPath);
        }
        try (InputStream in = ClientConfiguration.class.getResourceAsStream(BUNDLED_STAMP_RESOURCE)) {
            if (in == null) {
                return Optional.empty();
            }
            Path workDir =
                    Path.of(System.getProperty("java.io.tmpdir", ".")).resolve("osinergmin-firmador");
            Files.createDirectories(workDir);
            Path target = workDir.resolve("osinergmin-sello.png");
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            cachedBundledStampPath = target.toAbsolutePath().normalize();
            return Optional.of(cachedBundledStampPath);
        } catch (IOException ignored) {
            return Optional.empty();
        }
    }

    public static int getSigningDefaultPosX() {
        return parseIntProperty("signing.default.posx", 10);
    }

    public static int getSigningDefaultPosY() {
        return parseIntProperty("signing.default.posy", 400);
    }

    public static int getSigningTextSize() {
        return parseIntProperty("signing.text.size", 13);
    }

    public static String getSigningReason() {
        return properties().getProperty("signing.reason", "Firma digital OSINERGMIN").trim();
    }

    private static int parseIntProperty(String key, int defaultValue) {
        String raw = properties().getProperty(key, String.valueOf(defaultValue)).trim();
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }
}
