package it.ksuploader.client.utils;

import it.ksuploader.client.KSUploader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Can put files into new ZIP archives.
 */
public class Zipper {

    /**
     * A comment that will be placed in newly created ZIP files.
     */
    private static final String ZIP_COMMENT = "ZIP file created by KSUploader Client.";

    /**
     * Copies the specified files into a new ZIP archive.
     *
     * @param files The files that should be in the new archive file.
     * @param targetZIP A File indicating the location of the newly created ZIP.
     * @throws IOException If an I/O error occurs.
     */
    public static void toZip(File[] files, File targetZIP) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(targetZIP))) {
            zos.setComment(ZIP_COMMENT);
            add(zos, files);
            zos.flush();
        }
    }

    /**
     * Adds Files to a ZipOutputStream.
     *
     * @param out The ZipOutputStream to which the Files should be added.
     * @param file The Files that should be added to the ZipOutputStream.
     * @throws FileNotFoundException If one of the files does not exist.
     * @throws IOException If an I/O exception occurs.
     */
    private static void add(ZipOutputStream out, File[] files) throws FileNotFoundException, IOException {
        for (File file : files) {
            if (file.isDirectory()) {
                KSUploader.logger.log(Level.INFO, "Adding the files inside {0} to the archive.", file);
                add(out, file.listFiles());
            } else {
                KSUploader.logger.log(Level.INFO, "Adding the file {0} to the archive.", file);
                try (FileInputStream fis = new FileInputStream(file)) {
                    out.putNextEntry(new ZipEntry(file.getName()));

                    byte[] buffer = new byte[4096];
                    int length;

                    while ((length = fis.read(buffer)) > 0) {
                        out.write(buffer, 0, length);
                    }
                    out.closeEntry();
                }
            }
        }

    }
}
