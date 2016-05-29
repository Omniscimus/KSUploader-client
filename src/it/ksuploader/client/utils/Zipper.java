package it.ksuploader.client.utils;

import it.ksuploader.client.Main;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Random;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class Zipper {

    public static String toZip(String method, File[] files, String root) {
        String fileName = null;
        ZipOutputStream zos;
        try {

            Main.myLog("[Zipper] file.length: " + files.length);

            if (method.equals("socket")) // socket or ftp
                fileName = "KStemp.zip";
            else
                fileName = System.currentTimeMillis() / 1000 + new Random().nextInt(999) + ".zip";

            zos = new ZipOutputStream(new FileOutputStream(Main.so.getTempDir() + File.separator + fileName));
            Main.dialog.setButtonClickable(false);
            Main.dialog.show("Zipping...", "", false);
            add(zos, files, root);
            Main.dialog.setButtonClickable(true);
            zos.flush();
            zos.close();
        } catch (IOException e) {
            Main.dialog.setButtonClickable(true);
            e.printStackTrace();
            Main.myErr(Arrays.toString(e.getStackTrace()).replace(",", "\n"));
        }
        Main.myLog("[Zipper] Zipping finished: " + Main.so.getTempDir() + File.separator + fileName);
        return Main.so.getTempDir() + File.separator + fileName;
    }

    private static void add(ZipOutputStream zout, File[] fileSource, String root) {
        for (File f : fileSource) {
            if (f.isDirectory()) {
                add(zout, f.listFiles(), root);
            } else {
                try (FileInputStream fin = new FileInputStream(f)) {
                    byte[] buffer = new byte[4096];
                    zout.putNextEntry(new ZipEntry(f.getPath().replace(root + File.separator, "")));

                    int length;
                    long count = 0;

                    while ((length = fin.read(buffer)) > 0) {
                        zout.write(buffer, 0, length);
                        count += length;
                        Main.dialog.set((int) (count * 100 / f.length()));
                    }
                    zout.closeEntry();

                } catch (IOException e) {
                    Main.myErr(Arrays.toString(e.getStackTrace()).replace(",", "\n"));
                }
            }
        }

    }
}
