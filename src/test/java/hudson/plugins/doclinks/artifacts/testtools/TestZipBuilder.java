/*
 * The MIT License
 *
 * Copyright (c) 2013 IKEDA Yasuyuki
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package hudson.plugins.doclinks.artifacts.testtools;

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.tasks.Builder;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

/**
 * Creates a zip file with specified name.
 */
public class TestZipBuilder extends Builder {
    private String filename;
    private boolean noEntryForDirectories;

    public TestZipBuilder(String filename) {
        this(filename, false);
    }

    public TestZipBuilder(String filename, boolean noEntryForDirectories) {
        this.filename = filename;
        this.noEntryForDirectories = noEntryForDirectories;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
            throws InterruptedException, IOException {
        FilePath file = build.getWorkspace().child(filename);
        FilePath dir = file.getParent();

        if (dir != null && !dir.exists()) {
            dir.mkdirs();
        }

        String seperator = System.getProperty("file.separator");
        String resourceDirName = StringUtils.join(getClass().getName().split("\\."), seperator);
        File resourceDir = null;
        try {
            resourceDir =
                    new File(ClassLoader.getSystemResource(resourceDirName).toURI());
        } catch (URISyntaxException e) {
            listener.getLogger().println("Failed to retrieve contents to zip");
            e.printStackTrace(listener.getLogger());
        }

        OutputStream os = null;
        ZipOutputStream zos = null;

        try {
            os = file.write();
            zos = new ZipOutputStream(os);

            compress(zos, resourceDir, null);
        } finally {
            if (zos != null) {
                zos.close();
            }

            if (os != null) {
                os.close();
            }
        }

        return true;
    }

    private void compress(ZipOutputStream zos, File dir, String relative) throws IOException {
        for (String filename : dir.list()) {
            File file = new File(dir, filename);
            String path = StringUtils.isEmpty(relative) ? filename : String.format("%s/%s", relative, filename);
            if (file.isDirectory()) {
                if (!noEntryForDirectories) {
                    ZipEntry entry = new ZipEntry(String.format("%s/", path));
                    zos.putNextEntry(entry);
                }
                compress(zos, file, path);
            } else {
                ZipEntry entry = new ZipEntry(path);
                zos.putNextEntry(entry);
                FileInputStream is = null;
                try {
                    is = new FileInputStream(file);
                    IOUtils.copy(is, zos);
                } finally {
                    if (is != null) {
                        is.close();
                    }
                }
            }
        }
    }
}
