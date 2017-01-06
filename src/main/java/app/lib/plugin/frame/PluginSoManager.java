
package app.lib.plugin.frame;

import android.content.Context;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Created by chenhao on 17/1/6.
 */

public class PluginSoManager {

    public static final String CPU_ARMEABI = "armeabi";
    public static final String CPU_ARMEABIV7 = "armeabi-v7a";
    public static final String CPU_X86 = "x86";
    public static final String CPU_MIPS = "mips";

    private static final Object sLock = new Object();
    private static PluginSoManager sInstance;

    private PluginSoManager() {
    }

    public static PluginSoManager getInstance() {
        if (sInstance == null) {
            synchronized (sLock) {
                // 有可能在其他线程已创建
                if (sInstance == null) {
                    sInstance = new PluginSoManager();
                }
            }
        }
        return sInstance;
    }

    public static void deleteDir(File dir) {
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files == null)
                return;
            for (File f : files) {
                deleteDir(f);
            }
            dir.delete();
        } else
            dir.delete();
    }

    /**
     * copy so lib to installDir directory installdir/model/version/armeabi/so
     */
    public void install(Context context, String pluginId, int versionCode, String apkPath) {
        String soInstallDir = PluginSetting.getSoDir(context, pluginId, versionCode);

        new CopySoTask(apkPath, soInstallDir).run();
    }

    private class CopySoTask implements Runnable {

        String mApkFile;
        String mInstallDir;

        CopySoTask(String apkFile, String installDir) {
            mApkFile = apkFile;
            mInstallDir = installDir;
        }

        private String parseSoFileName(String zipEntryName) {
            return zipEntryName.substring(zipEntryName.lastIndexOf("/") + 1);
        }

        private void writeSoFile2LibDir(ZipFile zipFile, ZipEntry zipEntry, String sofile) {
            InputStream is = null;
            FileOutputStream fos = null;
            try {
                is = zipFile.getInputStream(zipEntry);
                fos = new FileOutputStream(new File(mInstallDir, sofile));
            } catch (IOException e) {

            }
            copy(is, fos);
        }

        /**
         * 输入输出流拷贝
         *
         * @param is
         * @param os
         */
        public void copy(InputStream is, OutputStream os) {
            if (is == null || os == null)
                return;
            BufferedInputStream bis = new BufferedInputStream(is);
            BufferedOutputStream bos = new BufferedOutputStream(os);
            byte[] buf = null;
            try {
                buf = new byte[getAvailableSize(bis)];
            } catch (IOException e) {

            }
            int i = 0;
            try {
                while ((i = bis.read(buf)) != -1) {
                    bos.write(buf, 0, i);
                }
            } catch (IOException e) {

            }
            try {
                bos.flush();
                bos.close();
                bis.close();
            } catch (IOException e) {

            }
        }

        private int getAvailableSize(InputStream is) throws IOException {
            if (is == null)
                return 0;
            int available = is.available();
            return available <= 0 ? 1024 : available;
        }

        @Override
        public void run() {
            File installDirFile = new File(mInstallDir);
            installDirFile.mkdirs();
            String cpuArchitect = CPU_ARMEABI;

            ZipFile zipFile = null;
            try {
                zipFile = new ZipFile(mApkFile);
                Enumeration<? extends ZipEntry> entries = zipFile.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry zipEntry = entries.nextElement();
                    if (zipEntry.isDirectory()) {
                        continue;
                    }
                    String zipEntryName = zipEntry.getName();
                    if (zipEntryName.endsWith(".so") && zipEntryName.contains(cpuArchitect)) {
                        writeSoFile2LibDir(zipFile, zipEntry, parseSoFileName(zipEntryName));
                    }
                }
            } catch (IOException e) {

            } finally {
                if (zipFile != null) {
                    try {
                        zipFile.close();
                    } catch (IOException e) {

                    }
                }
            }

        }

    }

}
