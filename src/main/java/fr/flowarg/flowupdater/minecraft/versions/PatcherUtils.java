package fr.flowarg.flowupdater.minecraft.versions;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class PatcherUtils
{
    public static final PatcherUtils INSTANCE = new PatcherUtils();

    public void compressFiles(File[] listFiles, File destZipFile) throws IOException
    {
        final ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(destZipFile));

        for (File file : listFiles)
        {
            if (file.isDirectory()) this.addFolderToZip(file, file.getName(), zos);
            else this.addFileToZip(file, zos);
        }

        zos.flush();
        zos.close();
    }

    private void addFolderToZip(File folder, String parentFolder, ZipOutputStream zos) throws IOException
    {
        for (File file : folder.listFiles())
        {
            if (file.isDirectory())
            {
                this.addFolderToZip(file, parentFolder + "/" + file.getName(), zos);
                continue;
            }
            zos.putNextEntry(new ZipEntry(parentFolder + "/" + file.getName()));

            final BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
            final byte[] buffer   = new byte[4096];
            int    read;

            while ((read = bis.read(buffer)) != -1)
                zos.write(buffer, 0, read);

            zos.closeEntry();
            bis.close();
        }
    }

    private void addFileToZip(File file, ZipOutputStream zos) throws IOException
    {
        zos.putNextEntry(new ZipEntry(file.getName()));

        final BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));

        final byte[] buffer   = new byte[1024];
        int    read;
        while ((read = bis.read(buffer)) != -1)
            zos.write(buffer, 0, read);

        zos.closeEntry();
        bis.close();
    }
}
