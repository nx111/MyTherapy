package medichine.mediacationalert.mytherapy.utils;

import android.content.Context;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class MyTherapyBackupManager {
    private static final String MANIFEST_ENTRY = "mytherapy-backup.json";
    private static final String MANIFEST_CONTENT = "{\"format\":\"mytherapy-full-backup\",\"version\":1}";
    private static final String[] BACKUP_DIRS = new String[]{"databases", "shared_prefs", "files", "no_backup"};

    public int exportFullBackup(Context context, OutputStream outputStream) throws IOException {
        ZipOutputStream zip = new ZipOutputStream(outputStream);
        int files = 0;
        try {
            addTextEntry(zip, MANIFEST_ENTRY, MANIFEST_CONTENT);
            File dataDir = new File(context.getApplicationInfo().dataDir);
            for (String dirName : BACKUP_DIRS) {
                File dir = new File(dataDir, dirName);
                if (dir.exists()) {
                    files += addFile(zip, dir, dirName);
                }
            }
        } finally {
            zip.finish();
        }
        return files;
    }

    public int restoreFullBackup(Context context, InputStream inputStream) throws IOException {
        File tempDir = new File(context.getCacheDir(), "mytherapy_restore");
        deleteRecursively(tempDir);
        if (!tempDir.mkdirs() && !tempDir.exists()) {
            throw new IOException("Cannot create restore temp directory");
        }

        boolean hasManifest = false;
        int files = 0;
        ZipInputStream zip = new ZipInputStream(inputStream);
        try {
            ZipEntry entry;
            byte[] buffer = new byte[8192];
            while ((entry = zip.getNextEntry()) != null) {
                File target = safeTarget(tempDir, entry.getName());
                if (entry.isDirectory()) {
                    if (!target.mkdirs() && !target.exists()) {
                        throw new IOException("Cannot create directory " + target);
                    }
                    continue;
                }
                File parent = target.getParentFile();
                if (parent != null && !parent.exists() && !parent.mkdirs()) {
                    throw new IOException("Cannot create directory " + parent);
                }
                try (FileOutputStream output = new FileOutputStream(target)) {
                    int read;
                    while ((read = zip.read(buffer)) != -1) {
                        output.write(buffer, 0, read);
                    }
                }
                if (MANIFEST_ENTRY.equals(entry.getName())) {
                    hasManifest = true;
                } else {
                    files++;
                }
            }
        } finally {
            zip.close();
        }

        if (!hasManifest) {
            deleteRecursively(tempDir);
            throw new IOException("Invalid MyTherapy backup");
        }

        File dataDir = new File(context.getApplicationInfo().dataDir);
        for (String dirName : BACKUP_DIRS) {
            File restoredDir = new File(tempDir, dirName);
            File targetDir = new File(dataDir, dirName);
            if (restoredDir.exists()) {
                deleteRecursively(targetDir);
                copyRecursively(restoredDir, targetDir);
            }
        }
        deleteRecursively(tempDir);
        return files;
    }

    private void addTextEntry(ZipOutputStream zip, String name, String text) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(text.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private int addFile(ZipOutputStream zip, File file, String entryName) throws IOException {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children == null || children.length == 0) {
                zip.putNextEntry(new ZipEntry(entryName + "/"));
                zip.closeEntry();
                return 0;
            }
            int count = 0;
            for (File child : children) {
                count += addFile(zip, child, entryName + "/" + child.getName());
            }
            return count;
        }

        zip.putNextEntry(new ZipEntry(entryName));
        try (FileInputStream input = new FileInputStream(file)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) != -1) {
                zip.write(buffer, 0, read);
            }
        }
        zip.closeEntry();
        return 1;
    }

    private File safeTarget(File root, String entryName) throws IOException {
        File target = new File(root, entryName);
        String rootPath = root.getCanonicalPath();
        String targetPath = target.getCanonicalPath();
        if (!targetPath.equals(rootPath) && !targetPath.startsWith(rootPath + File.separator)) {
            throw new IOException("Unsafe backup entry");
        }
        return target;
    }

    private void copyRecursively(File source, File target) throws IOException {
        if (source.isDirectory()) {
            if (!target.mkdirs() && !target.exists()) {
                throw new IOException("Cannot create directory " + target);
            }
            File[] children = source.listFiles();
            if (children == null) {
                return;
            }
            for (File child : children) {
                copyRecursively(child, new File(target, child.getName()));
            }
            return;
        }

        File parent = target.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("Cannot create directory " + parent);
        }
        try (FileInputStream input = new FileInputStream(source);
             FileOutputStream output = new FileOutputStream(target)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
        }
    }

    private void deleteRecursively(File file) {
        if (file == null || !file.exists()) {
            return;
        }
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursively(child);
                }
            }
        }
        file.delete();
    }
}
