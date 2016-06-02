package it.ksuploader.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;

/**
 * Represents the user-defined settings for the program.
 */
public class Configuration extends Properties {

    private static final long serialVersionUID = 1L;

    /**
     * The name of the file with user preferences.
     */
    private static final String CONFIG_NAME = "client.properties";

    /**
     * A description for this configuration.
     */
    private static final String DESCRIPTION = "Configuration settings for the KSUploader client.";

    /**
     * The file containing the configuration for the application.
     */
    private File config;

    /**
     * Constructs a new Configuration object.
     *
     * @param configDir The directory in which the file with the user's
     * preferences is, or should be, stored.
     * @throws IOException If there is a problem with the configuration file.
     * @throws NullPointerException If {@code config} is null.
     */
    public Configuration(File configDir) throws IOException {
        if (configDir == null) {
            throw new NullPointerException("Cannot instantiate Configuration without a config file.");
        }

        config = new File(configDir, CONFIG_NAME);
        if (!config.exists()) {
            KSUploader.logger.log(Level.INFO, "The configuration file does not exist yet; creating it at {0}", configDir.getPath());
            loadDefaults();
            try (FileOutputStream fos = new FileOutputStream(config)) {
                store(fos, DESCRIPTION);
            }
        } else if (!config.isFile()) {
            throw new IOException("Configuration location already exists but is not a file: " + config.getPath());
        } else {
            loadFromFile();
            absentsToDefaults();
        }
    }

    /**
     * Uses default values for all properties. Will replace any values that are
     * already stored in this object.
     */
    private void loadDefaults() {
        putAll(Setting.defaults());
    }

    /**
     * Uses default values for all properties that have not been set yet.
     */
    private void absentsToDefaults() {
        Setting.defaults().entrySet().stream().forEach((def) -> {
            putIfAbsent(def.getKey(), def.getValue());
        });
    }

    /**
     * Loads a configuration file into the Setting enum.
     *
     * @throws IOException If an I/O error occurs while reading the
     * configuration file.
     */
    private void loadFromFile() throws IOException {
        try (FileInputStream is = new FileInputStream(config)) {
            load(is);
        }
        entrySet().stream().forEach((entry) -> {
            Setting.getSetting((String) entry.getKey()).setValue(entry.getValue());
        });
    }

    /**
     * Saves this Configuration object to the configuration file.
     *
     * @throws IOException If there is a problem with the configuration file.
     */
    public void save() throws IOException {
        /* Use default values for any absent keys. */
        absentsToDefaults();
        try (FileOutputStream fos = new FileOutputStream(config)) {
            store(fos, DESCRIPTION);
        }
    }

    /**
     * Represents a configuration setting for this application.
     */
    public enum Setting {

        /**
         * The IP-address of a host running KSUploader-server.
         */
        KSUPLOADER_SERVER_ADDRESS("ksuploader_server_address", "localhost"),
        /**
         * The port on which KSUploader-server is running on the host.
         */
        KSUPLOADER_PORT("ksuploader_port", 4030),
        /**
         * The password for the KSUploader-server.
         */
        KSUPLOADER_PASSWORD("ksuploader_password", "pass"),
        /**
         * Whether FTP(S) should be used to upload files, instead of
         * KSUploader-server.
         */
        FTP_ENABLED("ftp_enabled", false),
        /**
         * The IP-address of the FTP(s) host to which files should be uploaded.
         */
        FTP_ADDRESS("ftp_address", "ftp.mydomain.name"),
        /**
         * The port on which FTP(S) is running on the host.
         */
        FTP_PORT("ftp_port", 21),
        /**
         * The username of a user who has access to the FTP(S) server.
         */
        FTP_USER("ftp_user", "user"),
        /**
         * The password of the {@code FTP_USER}.
         */
        FTP_PASSWORD("ftp_password", "pass"),
        /**
         * The directory to which files should be uploaded on the FTP(S) server.
         */
        FTP_DIRECTORY("ftp_directory", "subFolder/anotherFolder"),
        /**
         * The URL to a website on which files appear after uploading them using
         * FTP(S).
         */
        FTP_WEB_URL("ftp_weburl", "http://mydomain.name"),
        /**
         * Whether FTPS (Secure FTP) instead of regular FTP is used on the FTP
         * host.
         */
        FTPS_ENABLED("ftps_enabled", false),
        /**
         * Whether all certificates should be accepted. (?)
         */
        ACCEPT_ALL_CERTIFICATES("accept_all_certificates", false),
        /**
         * Whether captured files should be saved to the local disk.
         */
        SAVE_ENABLED("save_enabled", false),
        /**
         * The directory to which files should be locally saved, if
         * {@code SAVE_ENABLED} is true.
         */
        SAVE_DIRECTORY("save_dir", "."),
        /**
         * Whether the application should automatically start when the Operating
         * System boots.
         */
        OPEN_AT_STARTUP_ENABLED("open_at_startup_enabled", false),
        /**
         * The keyboard shortcut to capture an area of the screen.
         */
        SHORTCUT_SCREEN("shortcut_screen", "56+2"),
        /**
         * The keyboard shortcut to capture the entire screen.
         */
        SHORTCUT_COMPLETE_SCREEN("shortcut_complete_screen", "56+3"),
        /**
         * The keyboard shortcut to upload a file.
         */
        SHORTCUT_FILE("shortcut_file", "56+4"),
        /**
         * The keyboard shortcut to upload the contents of the system clipboard.
         */
        SHORTCUT_CLIPBOARD("key_clipboard", "56+5");

        private final String configKey;
        private final Object defaultValue;
        private Object value;

        /**
         * Constructs a new Setting.
         *
         * @param configKey The key for in the configuration file.
         * @param defaultValue The default value for this setting.
         */
        private Setting(String configKey, Object defaultValue) {
            this.configKey = configKey;
            this.defaultValue = defaultValue;
        }

        /**
         * Gets the configuration key for this setting.
         *
         * @return the configuration key
         */
        public String getConfigKey() {
            return configKey;
        }

        /**
         * Gets the default value for this setting.
         *
         * @return the default value
         */
        public Object getDefaultValue() {
            return defaultValue;
        }

        /**
         * Gets the value for this setting.
         *
         * @return the setting's value
         */
        public Object getValue() {
            return (value == null) ? getDefaultValue() : value;
        }

        /**
         * Sets the value of this setting.
         *
         * @param newValue The value to set this setting to.
         * @return the previous value of this setting
         */
        public Object setValue(Object newValue) {
            Object previousValue = value;
            value = newValue;
            return (previousValue == null) ? defaultValue : previousValue;
        }

        /**
         * Gets the Setting with the specified configuration key.
         *
         * @param key The configuration key to search for.
         * @return the setting with the specified key, or null if there is no
         * such setting
         */
        public static Setting getSetting(String key) {
            for (Setting setting : values()) {
                if (setting.getConfigKey().equals(key)) {
                    return setting;
                }
            }
            return null;
        }

        /**
         * Gets an unmodifiable map containing all settings mapped to their
         * default value.
         *
         * @return an unmodifiable map with default settings
         */
        public static Map<String, Object> defaults() {
            Map<String, Object> defaults = new HashMap<>();
            for (Setting setting : values()) {
                defaults.put(setting.getConfigKey(), setting.getDefaultValue());
            }
            return Collections.unmodifiableMap(defaults);
        }

    }

}
