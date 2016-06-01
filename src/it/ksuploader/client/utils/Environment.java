package it.ksuploader.client.utils;

import it.ksuploader.client.KSUploader;

import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.security.CodeSource;
import java.util.Arrays;
import java.util.Locale;
import java.util.logging.Level;

/**
 * Represents the currently used Operating System. Provides OS-specific behavior
 * and file system access functionality.
 */
public class Environment {

    /**
     * The file name of the application's icon.
     */
    static final String ICON_NAME = "icon.png";

    /**
     * Gets an Environment object appropriate to the used Operating System.
     *
     * @return an Environment based on the OS name
     */
    public static final Environment getEnvironment() {
        String osName = System.getProperty("os.name").toLowerCase(Locale.ENGLISH);

        if (osName.contains("linux")) {
            return new Linux();
        } else if (osName.contains("mac")) {
            return new OSX();
        } else if (osName.contains("windows")) {
            return new Windows();
        } else {
            return new Environment();
        }
    }

    /**
     * Gets the path to the application's icon outside of the JAR-file. If it
     * isn't there yet, it will be copied from the JAR to the correct location.
     *
     * @return the path to the application icon
     */
    String getIconPath() {
        String iconPath = null;
        File configDir = getConfigurationDirectory();
        if (configDir != null) {
            File targetIcon = new File(configDir, ICON_NAME);
            iconPath = targetIcon.getPath();
            if (!targetIcon.exists()) {
                Environment.copyIconTo(targetIcon);
            } else if (!targetIcon.isFile()) {
                iconPath = null;
                KSUploader.logger.log(Level.INFO, "Failed to copy icon for shortcut to {0}: target already exists but isn''t a file.", targetIcon.getPath());
            }
        }
        return iconPath;
    }

    /**
     * Gets the file system path to the location of the JAR-file in which this
     * program resides.
     *
     * @return the path to this application's executable JAR-file
     */
    static String getPathToJar() {
        CodeSource source = KSUploader.class.getProtectionDomain().getCodeSource();
        if (source == null) {
            return null;
        }
        String pathToJar = source.getLocation().getPath();
        if (pathToJar.equals("")) {
            return null;
        }
        return pathToJar;
    }

    /**
     * Copies this application's icon to the specified location.
     *
     * @param destination The target location to copy the icon to.
     * @return a boolean indicating the success of the operation
     */
    public static boolean copyIconTo(File destination) {
        final String path = "/" + ICON_NAME;
        URL iconInJar = KSUploader.class.getResource(path);
        if (iconInJar == null) {
            KSUploader.logger.log(Level.WARNING, "Couldn't find resource in the JAR-file: " + path);
            return false;
        }
        return copyFile(iconInJar, destination);
    }

    /**
     * Copies a file to another location. The source file may be a file inside
     * the JAR-file.
     *
     * @param source An URL to the source file.
     * @param destination The target location to copy the file to.
     * @return a boolean indicating the success of the operation
     */
    public static boolean copyFile(URL source, File destination) {
        InputStream is = null;
        OutputStream os = null;
        try {
            if (!destination.isFile()) {
                KSUploader.logger.log(Level.FINE, "Tried to copy file {0} to {1}, but target location is a directory.", new String[]{source.getPath(), destination.getPath()});
                return false;
            }
            is = source.openStream();
            os = new FileOutputStream(destination, false);
            byte[] buffer = new byte[4096];
            int i;
            while ((i = is.read(buffer)) != -1) {
                os.write(buffer, 0, i);
            }
        } catch (FileNotFoundException ex) {
            KSUploader.logger.log(Level.WARNING, "Error while copying file " + source.getPath() + " to " + destination.getPath() + ": couldn't open target file.", ex);
            return false;
        } catch (IOException ex) {
            KSUploader.logger.log(Level.WARNING, "I/O error while copying file " + source.getPath() + " to " + destination.getPath(), ex);
            return false;
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
                if (os != null) {
                    os.close();
                }
            } catch (IOException ex) {
                KSUploader.logger.log(Level.WARNING, "I/O error while copying file " + source.getPath() + " to " + destination.getPath() + ": couldn't close streams.", ex);
            }
        }
        return true;
    }

    /**
     * Gets the directory containing files for this program such as
     * configuration and log files.
     *
     * @return the configuration directory
     */
    public File getConfigurationDirectory() {
        return new File(System.getProperty("user.home"), String.format(".config$1ksuploader", File.separator));
    }

    /**
     * Gets the resolution restrictions for the used screen.
     *
     * @return the screen bounds
     */
    public Rectangle getScreenBounds() {
        Rectangle screen = new Rectangle();
        for (GraphicsDevice gd : GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()) {
            screen = screen.union(gd.getDefaultConfiguration().getBounds());
        }
        return screen;
    }

    /**
     * Gets whether the program is configured in the Operating System to start
     * automatically on system boot.
     *
     * @return true if the program is configured to auto-start; false otherwise
     */
    public boolean autoStartIsEnabled() {
        return false;
    }

    /**
     * Configures this program in the Operating System to start automatically on
     * system boot.
     *
     * @return a boolean indicating the success of this operation
     */
    public boolean enableAutoStart() {
        return false;
    }

    /**
     * Removes this program from the list of programs that start automatically
     * on system boot.
     *
     * @return a boolean indicating the success of this operation
     */
    public boolean disableAutoStart() {
        return false;
    }

    @Override
    public String toString() {
        return "Unknown: " + System.getProperty("os.name");
    }

}

/**
 * Represents a Linux Operating System. Provides OS-specific behavior and file
 * system access functionality.
 */
class Linux extends Environment {

    /**
     * The name of the .desktop file for this program. If this file is placed in
     * the correct autostart directory, the application will be started
     * automatically on system boot.
     */
    private static final String AUTOSTART_NAME = "it.ksuploader.client.desktop";

    private File configHomeDirectory;
    private File configurationDirectory;

    /**
     * Creates a .desktop file for this application at the specified location.
     *
     * @param targetLocation The location where the file should be created.
     * @param iconLocation An absolute path to an icon to use for the file. May
     * be null.
     * @return a boolean indicating the success of this operation
     */
    private static boolean createDesktopFile(File targetLocation, String iconLocation) {
        String pathToJar = getPathToJar();
        if (pathToJar == null) {
            KSUploader.logger.log(Level.WARNING, "Could not find the JAR-file containing this program while creating a shortcut.");
            return false;
        }

        File parent = targetLocation.getParentFile();
        if (!parent.exists()) {
            if (!parent.mkdirs()) {
                KSUploader.logger.log(Level.WARNING, "Could not create an application shortcut: could not make directories to: {0}", parent.getPath());
                return false;
            }
        }
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(targetLocation);
            OutputStreamWriter osw;
            try {
                osw = new OutputStreamWriter(fos, "UTF-8");
            } catch (UnsupportedEncodingException ex) {
                KSUploader.logger.log(Level.INFO, "Encoding UTF-8 is not supported. Using the default encoding to create a .desktop file.", ex);
                osw = new OutputStreamWriter(fos);
            }
            osw.write("[Desktop Entry]\n");
            osw.write("Type=Application\n");
            osw.write("Version=1.1\n");
            osw.write("Name=KSUploader\n");
            osw.write("GenericName=KSUploader\n");
            osw.write("Comment=Share screenshots and files\n");
            if (iconLocation != null) {
                osw.write("Icon=" + iconLocation + "\n");
            }
            osw.write("Exec=java -jar " + pathToJar + "\n");
            osw.write("Terminal=false\n");
            return true;
        } catch (FileNotFoundException ex) {
            KSUploader.logger.log(Level.WARNING, "Error while creating a .desktop file: can''t open file: " + targetLocation.getPath(), ex);
            return false;
        } catch (IOException ex) {
            KSUploader.logger.log(Level.WARNING, "I/O error while creating .desktop file: " + targetLocation.getPath(), ex);
            return false;
        } finally {
            try {
                if (fos != null) {
                    fos.close();
                }
            } catch (IOException ex) {
                KSUploader.logger.log(Level.WARNING, "I/O error while creating .desktop file " + targetLocation.getPath() + ": couldn't close streams.", ex);
            }
        }
    }

    /**
     * Gets the directory that contains .desktop files for programs that start
     * automatically on system boot.
     *
     * @see <a href="https://developer.gnome.org/autostart-spec/">Desktop
     * Application Autostart Specification</a>
     * @return the autostart directory
     */
    private File getAutostartDirectory() {
        File configHomeDir = getConfigHomeDirectory();
        if (configHomeDir == null) {
            KSUploader.logger.log(Level.INFO, "Couldn't find the autostart directory; using ~/.config/autostart");
            String userHome = System.getProperty("user.home");
            String autostart = String.format("$1$2.config$2autostart", userHome, File.separator);
            return new File(autostart);
        }
        return new File(configHomeDir, "autostart");
    }

    /**
     * Gets the directory with application configurations, as defined by the
     * environment variable $XDG_CONFIG_HOME.
     *
     * @return the system's directory for application preference files
     */
    private File getConfigHomeDirectory() {
        if (configHomeDirectory == null) {
            String envVar = System.getenv("$XDG_CONFIG_HOME");
            if (envVar != null) {
                configHomeDirectory = new File(envVar);
            }
        }
        return configHomeDirectory;
    }

    @Override
    public File getConfigurationDirectory() {
        if (configurationDirectory == null) {
            File configHome = getConfigHomeDirectory();
            if (configHome != null) {
                KSUploader.logger.log(Level.INFO, "Using {0}/ksuploader to save files.", configHome.getPath());
                configurationDirectory = new File(configHome, "ksuploader");
            } else {
                KSUploader.logger.log(Level.INFO, "System config home is not set; using ~/.config/ksuploader to save files.");
                configurationDirectory = new File(System.getProperty("user.home"), String.format(".config$1ksuploader", File.separator));
            }
        }
        return configHomeDirectory;
    }

    @Override
    public boolean autoStartIsEnabled() {
        File autostartDir = getAutostartDirectory();
        if (!autostartDir.isDirectory()) {
            KSUploader.logger.log(Level.INFO, "The path to the autostart directory should point to a directory, but doesn't.");
            return false;
        }
        String[] autoStartFiles = autostartDir.list();
        if (autoStartFiles == null) {
            KSUploader.logger.log(Level.INFO, "I/O error while trying to access the autostart directory at {0}", autostartDir.getPath());
            return false;
        }
        return Arrays.asList(autoStartFiles).contains(AUTOSTART_NAME);
    }

    @Override
    public boolean enableAutoStart() {
        File autostart = getAutostartDirectory();
        if (!autostart.exists()) {
            KSUploader.logger.log(Level.INFO, "Directory {0} does not exist; creating directories.", autostart.getPath());
            if (!autostart.mkdirs()) {
                KSUploader.logger.log(Level.WARNING, "Couldn''t enable the autostart function: failed to create directories: {0}", autostart.getPath());
                return false;
            }
            /* Chmod it to 0700, see
             https://standards.freedesktop.org/basedir-spec/basedir-spec-latest.html#referencing */
            autostart.setExecutable(true, true);
            autostart.setReadable(true, true);
            autostart.setWritable(true, true);
        }

        File targetLocation = new File(autostart, AUTOSTART_NAME);
        boolean success = createDesktopFile(targetLocation, getIconPath());
        if (success) {
            File parent = autostart.getParentFile();
            parent.setExecutable(true, true);
            parent.setReadable(true, true);
            parent.setWritable(true, true);
        }
        return success;
    }

    @Override
    public boolean disableAutoStart() {
        File autostart = getAutostartDirectory();
        if (!autostart.exists()) {
            KSUploader.logger.log(Level.INFO, "Couldn't disable autostart: startup directory doesn't exist.");
            return false;
        }
        File targetLocation = new File(autostart, AUTOSTART_NAME);
        return targetLocation.delete();
    }
    
    @Override
    public String toString() {
        return "Linux: " + System.getProperty("os.name");
    }

}

/**
 * Represents a Windows Operating System. Provides OS-specific behavior and file
 * system access functionality.
 */
class Windows extends Environment {

    /**
     * The name of the shortcut file to this program. If this shortcut is placed
     * in the correct location, the program will start automatically on system
     * boot.
     */
    private static final String SHORTCUT_NAME = "KSUploader.url";

    private File startupDirectory;
    private File appdataDirectory;
    private File configurationDirectory;

    /**
     * Creates a shortcut to the application JAR at the specified location.
     *
     * @param targetLocation The location where the shortcut should be created.
     * @param iconLocation The absolute path to an icon which should be used for
     * the shortcut. May be null.
     * @return a boolean indicating the success of the operation
     */
    private static boolean createApplicationShortcut(File targetLocation, String iconLocation) {
        String pathToJar = getPathToJar();
        if (pathToJar == null) {
            KSUploader.logger.log(Level.WARNING, "Could not determine the path to the JAR-file containing this program while creating a shortcut.");
            return false;
        }

        File parent = targetLocation.getParentFile();
        if (!parent.exists()) {
            if (!parent.mkdirs()) {
                KSUploader.logger.log(Level.WARNING, "Could not create an application shortcut: could not make directories to: {0}", parent.getPath());
                return false;
            }
        }
        try (FileWriter fw = new FileWriter(targetLocation)) {
            fw.write("[InternetShortcut]\n");
            fw.write("URL=file://" + pathToJar + "\n");
            fw.write("IDList=\n");
            fw.write("HotKey=0\n");
            if (iconLocation != null) {
                fw.write("IconFile=" + iconLocation + "\n");
            }
        } catch (IOException ex) {
            KSUploader.logger.log(Level.WARNING, "I/O error thrown while creating an application shortcut at location: " + targetLocation, ex);
            return false;
        }
        return true;
    }

    /**
     * Gets the current user's Appdata directory, which contains data and
     * configuration files for programs.
     *
     * @return the Appdata directory
     */
    private File getAppdataDirectory() {
        if (appdataDirectory == null) {
            appdataDirectory = new File(System.getenv("Appdata"));
        }
        return appdataDirectory;
    }

    /**
     * Gets the directory that contains shortcuts to programs that should start
     * automatically on system boot.
     *
     * @return the startup directory
     */
    private File getStartupDirectory() {
        if (startupDirectory == null) {
            String pathFromAppdata = String.format("Microsoft$1Windows$1Start Menu$1Programs$1Startup", File.separator);
            File appdata = getAppdataDirectory();
            if (appdata == null) {
                KSUploader.logger.log(Level.INFO, "Couldn't find the startup directory: Appdata not found.");
                return null;
            }
            startupDirectory = new File(appdata, pathFromAppdata);
        }
        return startupDirectory;
    }

    @Override
    public File getConfigurationDirectory() {
        if (configurationDirectory == null) {
            File appdata = getAppdataDirectory();
            if (appdata == null) {
                KSUploader.logger.log(Level.INFO, "Couldn't find the configuration directory: Appdata not found.");
                return null;
            }
            configurationDirectory = new File(appdata, String.format("Local$1KSUploader", File.separator));
        }
        return configurationDirectory;
    }

    @Override
    public boolean autoStartIsEnabled() {
        String[] autoStartFiles = getStartupDirectory().list();
        if (autoStartFiles == null) {
            return false;
        }
        return Arrays.asList(autoStartFiles).contains(SHORTCUT_NAME);
    }

    @Override
    public boolean enableAutoStart() {
        File startup = getStartupDirectory();
        if (startup == null) {
            KSUploader.logger.log(Level.WARNING, "Couldn't enable the autostart function: couldn't find the startup directory.");
            return false;
        }
        if (!startup.exists()) {
            KSUploader.logger.log(Level.INFO, "Directory {0} does not exist; creating directories.", startup.getPath());
            if (!startup.mkdirs()) {
                KSUploader.logger.log(Level.WARNING, "Couldn''t enable the autostart function: failed to create directories: {0}", startup.getPath());
                return false;
            }
        }
        return createApplicationShortcut(new File(startup, SHORTCUT_NAME), getIconPath());
    }

    @Override
    public boolean disableAutoStart() {
        File startup = getStartupDirectory();
        if (startup == null) {
            KSUploader.logger.log(Level.WARNING, "Couldn't disable the autostart function: couldn't find the startup directory.");
            return false;
        }
        if (!startup.isDirectory()) {
            KSUploader.logger.log(Level.WARNING, "Couldn''t disable the autostart function: path to startup directory doesn''t point to a directory: {0}", startup.getPath());
            return false;
        }
        File shortcut = new File(startup, SHORTCUT_NAME);
        if (!shortcut.exists()) {
            KSUploader.logger.log(Level.WARNING, "Couldn''t disable the autostart function: couldn''t find the autostart file at {0}", shortcut.getPath());
            return false;
        }
        return shortcut.delete();
    }
    
    @Override
    public String toString() {
        return "Windows: " + System.getProperty("os.name");
    }

}

/**
 * Represents an OS X Operating System. Provides OS-specific behavior and file
 * system access functionality.
 */
class OSX extends Environment {

    /**
     * The name of the .plist file for this program. If this file is placed
     * in the correct location, the program will start automatically on system
     * boot.
     */
    private static final String PLIST_NAME = "it.ksuploader.client.plist";

    private File launchAgentsDirectory;
    private File configurationDirectory;

    /**
     * Gets the directory in which .plist files are stored for launchd.
     *
     * @return the user's launch agents directory
     */
    private File getLaunchAgentsDirectory() {
        if (launchAgentsDirectory == null) {
            launchAgentsDirectory = new File(System.getProperty("user.home"), String.format("Library$1LaunchAgents", File.separator));
        }
        return launchAgentsDirectory;
    }

    /**
     * Creates a .plist file for the launchd at the specified location.
     *
     * @param targetLocation The location to create the file at.
     * @return a boolean indicating the success of this operation
     */
    private boolean createAutostartPlist(File targetLocation) {
        String pathToJar = getPathToJar();
        if (pathToJar == null) {
            KSUploader.logger.log(Level.WARNING, "Could not find the JAR-file containing this program while creating a .plist file.");
            return false;
        }

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(targetLocation);
            OutputStreamWriter osw;
            try {
                osw = new OutputStreamWriter(fos, "UTF-8");
            } catch (UnsupportedEncodingException ex) {
                KSUploader.logger.log(Level.INFO, "Encoding UTF-8 is not supported. Using the default encoding to create a .desktop file.", ex);
                osw = new OutputStreamWriter(fos);
            }
            osw.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            osw.write("<!DOCTYPE plist PUBLIC \"-//Apple//DTD PLIST 1.0//EN\" \"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">\n");
            osw.write("<plist version=\"1.0\">\n");
            osw.write("<dict>\n");
            osw.write(" <key>Label</key>\n");
            osw.write(" <string>it.ksuploader.client</string>\n");
            osw.write(" <key>ProgramArguments</key>\n");
            osw.write(" <array>\n");
            osw.write("  <string>java</string>\n");
            osw.write("  <string>-jar</string>\n");
            osw.write("  <string>" + pathToJar + "</string>\n");
            osw.write(" </array>\n");
            osw.write("</dict>\n");
            osw.write("</plist>\n");
            return true;
        } catch (FileNotFoundException ex) {
            KSUploader.logger.log(Level.WARNING, "Error while creating a .plist file: can''t open file: " + targetLocation.getPath(), ex);
            return false;
        } catch (IOException ex) {
            KSUploader.logger.log(Level.WARNING, "I/O error while creating .plist file: " + targetLocation.getPath(), ex);
            return false;
        } finally {
            try {
                if (fos != null) {
                    fos.close();
                }
            } catch (IOException ex) {
                KSUploader.logger.log(Level.WARNING, "I/O error while creating .plist file " + targetLocation.getPath() + ": couldn't close streams.", ex);
            }
        }
    }

    @Override
    public File getConfigurationDirectory() {
        if (configurationDirectory == null) {
            configurationDirectory = new File(System.getProperty("user.home"), String.format(".config$1ksuploader", File.separator));
        }
        return configurationDirectory;
    }

    @Override
    public boolean autoStartIsEnabled() {
        return Arrays.asList(getLaunchAgentsDirectory().list()).contains(PLIST_NAME);
    }

    @Override
    public boolean enableAutoStart() {
        File launchAgents = getLaunchAgentsDirectory();
        if (!launchAgents.exists()) {
            KSUploader.logger.log(Level.INFO, "Error while enabling autostart: directory {0} does not exist.", launchAgents.getPath());
            return false;
        }
        return createAutostartPlist(new File(launchAgents, PLIST_NAME));
    }
    
    @Override
    public boolean disableAutoStart() {
        File launchAgents = getLaunchAgentsDirectory();
        if (!launchAgents.exists()) {
            KSUploader.logger.log(Level.INFO, "Error while disabling autostart: directory {0} does not exist.", launchAgents.getPath());
            return false;
        }
        return new File(launchAgents, PLIST_NAME).delete();
    }
    
    @Override
    public String toString() {
        return "OS X: " + System.getProperty("os.name");
    }

}
