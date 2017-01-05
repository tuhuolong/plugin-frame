
package app.lib.plugin.frame.util;

import android.text.TextUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by chenhao on 16/12/29.
 */

public class FileUtil {
    public static boolean copyFileToFile(String filePath, String targetFilePath) {
        InputStream is = null;
        try {
            is = new FileInputStream(new File(filePath));
            File f = createFileWhetherExists(targetFilePath);
            FileOutputStream fo = new FileOutputStream(f);
            int len = -1;
            byte[] bt = new byte[2048];
            while ((len = is.read(bt)) != -1) {
                fo.write(bt, 0, len);
            }
            fo.flush();
            is.close();
            fo.close();
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    public static File createFileWhetherExists(String filePath) {
        File file = new File(filePath);

        File parent = file.getParentFile();
        if (!parent.exists()) {
            parent.mkdirs();
        }

        try {
            file.createNewFile();
        } catch (IOException e) {
        }

        return file;
    }

    public static boolean deleteFile(String filePath) {
        if (TextUtils.isEmpty(filePath)) {
            return false;
        }
        File file = new File(filePath);
        // return FileUtil.safeDelSingleFile(file);
        return file.exists() && !file.isDirectory() && file.delete();
    }

    public static void deleteDirectory(String filePath) {
        if (TextUtils.isEmpty(filePath)) {
            return;
        }
        File file = new File(filePath);
        if (!file.exists()) {
            return;
        }
        if (file.isDirectory()) {
            for (File subFile : file.listFiles()) {
                if (subFile != null && subFile.exists()) {
                    deleteDirectory(subFile.getAbsolutePath());
                }
            }
        }
        // FileUtil.safeDelSingleFile(file);
        file.delete();
    }

    public static boolean fileExists(String path) {
        return !TextUtils.isEmpty(path) && new File(path).exists();
    }

    public static void makeFileDirectories(String updatePath) {
        if (TextUtils.isEmpty(updatePath)) {
            return;
        }

        File out = new File(updatePath);
        File dir = out.getParentFile();
        if (dir != null && !dir.exists()) {
            dir.mkdirs();
        }

    }

    public static File createFileIfNotExists(String filePath) {
        if (TextUtils.isEmpty(filePath)) {
            return null;
        }

        File file = new File(filePath);

        File parent = file.getParentFile();
        if (!parent.exists()) {
            parent.mkdirs();
        }

        try {
            if (!file.exists()) {
                file.createNewFile();
            }
        } catch (IOException e) {
            //
        }

        return file;
    }

    public static void createDirIfNotExists(String dirPath) {
        final File dirFile = new File(dirPath);
        if (!dirFile.exists()) {
            dirFile.mkdirs();
        }
    }
}
