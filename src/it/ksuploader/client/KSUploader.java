package it.ksuploader.client;

import it.ksuploader.client.ui.SystemTrayMenu;
import it.ksuploader.client.ui.PopupDialog;
import it.ksuploader.client.utils.Environment;
import it.ksuploader.client.utils.ShortcutListener;
import it.ksuploader.client.utils.Sound;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.SwingUtilities;

/**
 * Main class for KSUploader. Contains logging and startup functionality.
 */
public class KSUploader {

    /**
     * The main logger for this application.
     */
    public static final Logger logger = Logger.getLogger("KSULog");

    /**
     * The name of the file containing the log messages for the application.
     */
    private static final String LOG_NAME = "KSULog.txt";

    /**
     * Static accessible instance of KSUploader.
     */
    public static KSUploader inst;

    private Sound sound;
    private final Environment environment;
    private Configuration config;
    private SystemTrayMenu tray;
    private ShortcutListener shortcutListener;
    private PopupDialog popup;

    private KSUploader() {
        environment = Environment.getEnvironment();

        String logPath = environment.getConfigurationDirectory().getPath() + File.separator + LOG_NAME;
        try {
            FileHandler fh = new FileHandler(logPath, true);
            fh.setFormatter(new SimpleFormatter());
            logger.addHandler(fh);
            logger.setUseParentHandlers(false);
        } catch (IOException ex) {
            System.out.println("Can't open the log file: " + logPath);
            System.out.println(ex);
            System.out.println("Starting without log file.");
        }
        logger.log(Level.FINE, "Starting KSUploader client. {0}", new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date()));
        logger.log(Level.FINE, "Operating system is: {0}", environment.toString());

        File configDir = environment.getConfigurationDirectory();
        if (configDir == null) {
            logger.log(Level.SEVERE, "Cannot start the program without a configuration file!");
            return;
        }
        try {
            config = new Configuration(configDir);
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "I/O error while trying to read the configuration file.", ex);
            return;
        }

        try {
            sound = new Sound(Sound.URL_TO_SUCCESS_SOUND);
        } catch (UnsupportedAudioFileException ex) {
            logger.log(Level.WARNING, "Couldn't load the application's success sound: invalid file.", ex);
        } catch (IOException ex) {
            logger.log(Level.WARNING, "I/O error while trying to load the application's success sound.", ex);
        } catch (LineUnavailableException ex) {
            logger.log(Level.WARNING, "Couldn't load the application's success sound due to system resource restrictions.", ex);
        }

        autoStartCheck();
        SwingUtilities.invokeLater(() -> this.tray = new SystemTrayMenu());
        this.shortcutListener = new ShortcutListener();
        this.popup = new PopupDialog();
    }

    /**
     * Gets the Environment in which this application resides.
     *
     * @return the current Environment
     */
    public Environment getEnvironment() {
        return environment;
    }

    /**
     * Gets the application's Configuration object.
     *
     * @return Configuration instance
     */
    public Configuration getConfig() {
        return config;
    }

    /**
     * Gets the SystemTrayMenu object.
     *
     * @return SystemTrayMenu instance
     */
    public SystemTrayMenu getSystemTrayMenu() {
        return tray;
    }

    /**
     * Starts the application.
     *
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        inst = new KSUploader();
    }

    /**
     * Checks if the application should start automatically at system boot and
     * if it's configured to do so.
     */
    private void autoStartCheck() {
        if ((Boolean) Configuration.Setting.OPEN_AT_STARTUP_ENABLED.getValue()) {
            if (!environment.autoStartIsEnabled()) {
                environment.enableAutoStart();
            }
        } else {
            if (environment.autoStartIsEnabled()) {
                environment.disableAutoStart();
            }
        }
    }

    /**
     * Reloads the application's configuration file.
     */
    public void reloadConfiguration() {
        try {
            config = new Configuration(environment.getConfigurationDirectory());
        } catch (IOException ex) {
            logger.log(Level.WARNING, "I/O error while trying to read the configuration file.", ex);
        }
    }

    /**
     * Runs the application's success sound.
     *
     * @return a boolean indicating the success of this operation
     */
    public boolean runSound() {
        if (sound == null) {
            logger.log(Level.FINE, "Tried to run the success sound, but it's not available.");
            return false;
        }
        sound.start();
        return true;
    }

}
