package it.ksuploader.client;

import it.ksuploader.client.ui.MyScreen;
import it.ksuploader.client.utils.Zipper;

import java.awt.AWTException;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.JFileChooser;

/**
 * Represents a file that can be captured on the client and sent to a server.
 */
public class Capture {

    private static final String IMAGE_FORMAT = "png";
    private static final String TEMP_FILE_PREFIX = "KSU_";
    private static final String TEMP_FILE_SUFFIX = ".tmp";
    private static final String TEMP_ZIP_FILE_SUFFIX = ".zip" + TEMP_FILE_SUFFIX;
    private static final String TEMP_IMAGE_FILE_SUFFIX = "." + IMAGE_FORMAT + TEMP_FILE_SUFFIX;
    private static final String TEMP_TEXT_FILE_SUFFIX = ".txt" + TEMP_FILE_SUFFIX;

    private File file;
    private boolean fileIsTemporary = false;

    /**
     * Lets the user choose a file from their file system for this Capture.
     *
     * @throws CaptureException If the file chooser didn't succeed in getting a
     * file or directory from the local file system.
     */
    public void captureFile() throws CaptureException {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setMultiSelectionEnabled(true);
        fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        switch (fileChooser.showOpenDialog(null)) {
            case JFileChooser.ERROR_OPTION:
                throw new CaptureException("File capturing failed: there was an error when choosing the file.");
            case JFileChooser.CANCEL_OPTION:
                throw new CaptureException("File capturing failed: choosing the file was cancelled by the user.");
            case JFileChooser.APPROVE_OPTION:
                File[] selectedFiles = fileChooser.getSelectedFiles();
                if (selectedFiles.length == 1) {
                    file = selectedFiles[0];
                } else if (selectedFiles.length > 1) {
                    file = createTempFile(TEMP_FILE_PREFIX, TEMP_ZIP_FILE_SUFFIX);
                    try {
                        Zipper.toZip(selectedFiles, file);
                    } catch (IOException ex) {
                        throw new CaptureException("File capturing failed: couldn't store the files in a ZIP archive.", ex);
                    }
                }
                break;
            default:
                throw new CaptureException("File capturing failed: got an unrecognized response from the file chooser.");
        }
    }

    /**
     * Brings up a UI with which a part of the user's screen can be selected as
     * an image for this Capture.
     *
     * @throws CaptureException If the user selected an invalid screenshot, or
     * if the screenshot could not be saved.
     */
    public void capturePartialScreenshot() throws CaptureException {
        MyScreen partialScreen = new MyScreen();
        if (!partialScreen.isValidScreen()) {
            throw new CaptureException("Partial screenshot capturing failed: invalid screenshot selected.");
        }
        saveScreenshot(partialScreen.getImage());
    }

    /**
     * Gets the user's current entire screen and uses it as an image for this
     * Capture.
     *
     * @throws CaptureException If the user's Operating System doesn't allow
     * making this screenshot.
     */
    public void captureEntireScreenshot() throws CaptureException {
        Robot robot;
        try {
            robot = new Robot();
        } catch (AWTException ex) {
            throw new CaptureException("Entire screenshot capturing failed: not supported by Operating System.", ex);
        }
        Rectangle bounds = KSUploader.inst.getEnvironment().getScreenBounds();
        BufferedImage image = robot.createScreenCapture(bounds);
        saveScreenshot(image);
    }

    /**
     * Saves the given image to a temporary file using the {@code IMAGE_FORMAT}
     * format.
     *
     * @param image The image to save.
     * @throws CaptureException If the temporary file could not be created, or
     * if the used image format could not be used.
     */
    private void saveScreenshot(BufferedImage image) throws CaptureException {
        fileIsTemporary = true;
        file = createTempFile(TEMP_FILE_PREFIX, TEMP_IMAGE_FILE_SUFFIX);
        try {
            writeImageToFile(image, IMAGE_FORMAT, file);
        } catch (IOException ex) {
            throw new CaptureException("Entire screenshot capturing failed: couldn't save the screenshot using format: " + IMAGE_FORMAT, ex);
        }
    }

    /**
     * Gets the user's system clipboard and uses the data inside for this
     * capture.
     *
     * @throws CaptureException If the clipboard is empty, or if the kind of the
     * data inside the clipboard could not be determined, or if the data
     * couldn't be temporarily saved, or if the data is not serializable.
     */
    public void captureClipboardContents() throws CaptureException {
        Transferable clipboardContents = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(this);
        if (clipboardContents == null) {
            throw new CaptureException("Clipboard capturing failed: clipboard is empty.");
        }
        for (DataFlavor flavor : clipboardContents.getTransferDataFlavors()) {
            try {
                Object data = clipboardContents.getTransferData(flavor);/* May go into catch */

                fileIsTemporary = true;
                if (flavor.isFlavorJavaFileListType()) {
                    File[] files = (File[]) ((List) data).toArray();
                    file = createTempFile(TEMP_FILE_PREFIX, TEMP_ZIP_FILE_SUFFIX);
                    Zipper.toZip(files, file);
                } else if (flavor.isFlavorTextType()) {
                    file = createTempFile(TEMP_FILE_PREFIX, TEMP_TEXT_FILE_SUFFIX);
                    try (PrintWriter pw = new PrintWriter(file)) {
                        pw.println(data);
                    }
                } else if (data instanceof Serializable) {
                    file = createTempFile(TEMP_FILE_PREFIX, TEMP_FILE_SUFFIX);
                    try (FileOutputStream fos = new FileOutputStream(file);
                            ObjectOutputStream oos = new ObjectOutputStream(fos)) {
                        oos.writeObject(data);
                    }
                } else {
                    throw new CaptureException("Clipboard capturing failed: failed to write the data type to a file: " + flavor.getDefaultRepresentationClassAsString());
                }
                return;
            } catch (UnsupportedFlavorException | IOException ex) {
                KSUploader.logger.log(Level.INFO, "Failed to get the clipboard data in the flavor: " + flavor.getDefaultRepresentationClassAsString(), ex);
            }
        }
        throw new CaptureException("Clipboard capturing failed: failed to find the correct data flavor.");
    }

    /**
     * Uploads the captured file and puts the URL to where it can be found in
     * the history.
     */
    public void upload() {
        String url = KSUploader.inst.getUploader().upload(file);
        KSUploader.inst.getSystemTrayMenu().history(url);
    }

    /**
     * Deletes temporary files if there are any.
     *
     * @return false if the file couldn't be deleted, or if there was no file to
     * delete
     */
    public boolean clean() {
        if (fileIsTemporary) {
            return file.delete();
        }
        return false;
    }

    /**
     * Writes image data to a file.
     *
     * @param image The image to save to a file.
     * @param format The image format to save this image as.
     * @param targetLocation The file to save the image to.
     * @throws IOException If an error occurs during writing, or if the image
     * could not be saved using the specified format.
     */
    public static void writeImageToFile(BufferedImage image, String format, File targetLocation) throws IOException {
        if (!ImageIO.write(image, format, targetLocation)) {
            throw new IOException("Couldn't save the screenshot in format: " + format);
        }
    }

    /**
     * Creates a temporary file.
     *
     * @param prefix A prefix for the file name. Must be 3 characters or more.
     * @param suffix A suffix for the file name. May be null.
     * @return the newly created temporary file
     * @throws CaptureException If an I/O error occurred.
     */
    public static File createTempFile(String prefix, String suffix) throws CaptureException {
        try {
            return File.createTempFile(prefix, suffix);
        } catch (IOException ex) {
            throw new CaptureException("Couldn't temporarily save the screenshot.", ex);
        }
    }

}

/**
 * Thrown if there is an error while capturing a file to use for a Capture.
 */
class CaptureException extends Exception {

    private static final long serialVersionUID = 1L;

    public CaptureException(String message) {
        super(message);
    }

    public CaptureException(Throwable cause) {
        super(cause);
    }

    public CaptureException(String message, Throwable cause) {
        super(message, cause);
    }

}
