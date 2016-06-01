package it.ksuploader.client;

import it.ksuploader.client.ui.SystemTrayMenu;
import it.ksuploader.client.ui.PopupDialog;
import it.ksuploader.client.utils.Environment;
import it.ksuploader.client.utils.MyKeyListener;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
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

    private final Environment environment;
    private LoadConfig config;
    private SystemTrayMenu tray;
    private MyKeyListener keyListener;
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

        this.config = new LoadConfig();
        autoStartCheck();
        SwingUtilities.invokeLater(() -> this.tray = new SystemTrayMenu());
        this.keyListener = new MyKeyListener();
        this.popup = new PopupDialog();
    }
    
    /**
     * Gets the Environment in which this application resides.
     * 
     * @return the current Environment
     */
    public Environment getEnvironment() {
        return this.environment;
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
        if (config.isStartUpEnabled()) {
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
        config = new LoadConfig();
    }

}
