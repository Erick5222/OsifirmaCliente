package com.osinergmin.firma.desktop.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Persistencia local de configuracion TSL/TSA del panel de administracion. */
public final class SigningAdminConfigStore {

    private static final String KEY_TSL_URL = "signing.tsl.url";
    private static final String KEY_VERIFY_TSL = "signing.verify.tsl";
    private static final String KEY_TSA_URL = "signing.tsa.url";
    private static final String KEY_TSA_USER = "signing.tsa.user";
    private static final String KEY_TSA_PASSWORD = "signing.tsa.password";
    private static final String KEY_SIGNATURE_LEVEL = "signing.signature.level";
    private static final String KEY_SIGNATURE_ALGORITHM = "signing.signature.algorithm.note";

    private static volatile SigningAdminConfig cached;

    private SigningAdminConfigStore() {}

    public static Path getConfigFilePath() {
        String override = System.getProperty("firma.signing.admin.config");
        if (override != null && !override.isBlank()) {
            return Path.of(override.trim()).toAbsolutePath().normalize();
        }
        String env = System.getenv("FIRMA_SIGNING_ADMIN_CONFIG");
        if (env != null && !env.isBlank()) {
            return Path.of(env.trim()).toAbsolutePath().normalize();
        }
        return Path.of(System.getProperty("user.home", "."))
                .resolve(".osinergmin-firma")
                .resolve("signing-admin.properties")
                .toAbsolutePath()
                .normalize();
    }

    public static SigningAdminConfig load() {
        if (cached != null) {
            return cached;
        }
        synchronized (SigningAdminConfigStore.class) {
            if (cached != null) {
                return cached;
            }
            cached = readFromDisk();
            return cached;
        }
    }

    public static void save(SigningAdminConfig config) throws IOException {
        SigningAdminConfig toSave = config == null ? SigningAdminConfig.defaults() : config;
        Path file = getConfigFilePath();
        Files.createDirectories(file.getParent());
        Properties properties = new Properties();
        properties.setProperty(KEY_TSL_URL, nullToEmpty(toSave.tslUrl()));
        properties.setProperty(KEY_VERIFY_TSL, Boolean.toString(toSave.verifyTsl()));
        properties.setProperty(KEY_TSA_URL, nullToEmpty(toSave.tsaUrl()));
        properties.setProperty(KEY_TSA_USER, nullToEmpty(toSave.tsaUser()));
        properties.setProperty(KEY_TSA_PASSWORD, nullToEmpty(toSave.tsaPassword()));
        properties.setProperty(KEY_SIGNATURE_LEVEL, normalizeLevel(toSave.signatureLevel()));
        properties.setProperty(KEY_SIGNATURE_ALGORITHM, nullToEmpty(toSave.signatureAlgorithmNote()));
        try (OutputStream out = Files.newOutputStream(file)) {
            properties.store(out, "Configuracion administracion firma OSINERGMIN");
        }
        cached = toSave;
    }

    public static void reload() {
        synchronized (SigningAdminConfigStore.class) {
            cached = null;
            load();
        }
    }

    private static SigningAdminConfig readFromDisk() {
        SigningAdminConfig defaults = SigningAdminConfig.defaults();
        Path file = getConfigFilePath();
        if (!Files.isRegularFile(file)) {
            return defaults;
        }
        Properties properties = new Properties();
        try (InputStream in = Files.newInputStream(file)) {
            properties.load(in);
        } catch (IOException ex) {
            return defaults;
        }
        return new SigningAdminConfig(
                firstNonBlank(properties.getProperty(KEY_TSL_URL), defaults.tslUrl()),
                Boolean.parseBoolean(
                        firstNonBlank(
                                properties.getProperty(KEY_VERIFY_TSL),
                                Boolean.toString(defaults.verifyTsl()))),
                nullToEmpty(properties.getProperty(KEY_TSA_URL)),
                nullToEmpty(properties.getProperty(KEY_TSA_USER)),
                nullToEmpty(properties.getProperty(KEY_TSA_PASSWORD)),
                normalizeLevel(
                        firstNonBlank(properties.getProperty(KEY_SIGNATURE_LEVEL), defaults.signatureLevel())),
                firstNonBlank(
                        properties.getProperty(KEY_SIGNATURE_ALGORITHM), defaults.signatureAlgorithmNote()));
    }

    private static String normalizeLevel(String level) {
        if (level == null || level.isBlank()) {
            return "B";
        }
        return level.trim().toUpperCase();
    }

    private static String firstNonBlank(String preferred, String fallback) {
        if (preferred != null && !preferred.isBlank()) {
            return preferred.trim();
        }
        return fallback == null ? "" : fallback.trim();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
