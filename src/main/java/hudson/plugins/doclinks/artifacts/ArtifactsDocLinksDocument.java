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

package hudson.plugins.doclinks.artifacts;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.plexus.util.StringUtils;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import hudson.Util;
import hudson.model.ModelObject;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;

/**
 * Holds a link to an artifact published as a document.
 * 
 * The build that holds the artifact is resolved by {@link StaplerRequest#findAncestorObject(Class)}
 * at runtime.
 */
public class ArtifactsDocLinksDocument implements ModelObject {
    private static Logger LOGGER = Logger.getLogger(ArtifactsDocLinksDocument.class.getName());
    private String artifactName;
    /**
     * @return the artifactName
     */
    public String getArtifactName() {
        return artifactName;
    }
    
    private String title;
    /**
     * @return the title
     */
    public String getTitle() {
        return title;
    }
    
    private String initialPath;
    /**
     * @return the initialPath
     */
    public String getInitialPath() {
        return initialPath;
    }
    
    private String indexFile;
    /**
     * @return the indexFile
     */
    public String getIndexFile() {
        return indexFile;
    }
    
    private String id;
    /**
     * @return the id used in URL.
     */
    public String getId() {
        return id;
    }
    
    /**
     * @return the URL for the initial path.
     */
    public String getUrl() {
        return (getInitialPath() != null)
                ?String.format("%s/%s", Util.rawEncode(getId()), getInitialPath())
                :Util.rawEncode(getId());
    }
    
    /**
     * @param id
     * @param artifactName
     * @param title
     * @param initialPath
     * @param indexFile
     */
    public ArtifactsDocLinksDocument(String id, String artifactName, String title, String initialPath, String indexFile) {
        this.id = id;
        this.artifactName = artifactName;
        this.title = title;
        this.initialPath = initialPath;
        this.indexFile = indexFile;
    }
    
    
    /**
     * @return
     * @see hudson.model.ModelObject#getDisplayName()
     */
    @Override
    public String getDisplayName() {
        return getTitle();
    }
    
    /**
     * Resolves the build containing the artifact by {@link StaplerRequest#findAncestorObject(Class)}
     * 
     * @param req
     * @return
     */
    protected AbstractBuild<?, ?> getBuild(StaplerRequest req) {
        AbstractBuild<?,?> build = req.findAncestorObject(AbstractBuild.class);
        if (build != null) {
            return build;
        }
        
        AbstractProject<?,?> project = req.findAncestorObject(AbstractProject.class);
        if (project != null) {
            return project.getLastSuccessfulBuild();
        }
        
        return null;
    }
    
    /**
     * Send a contents of the artifact that is requested via HTTP.
     * 
     * @param req
     * @param resp
     * @throws IOException
     * @throws ServletException 
     */
    public void doDynamic(StaplerRequest req, StaplerResponse resp) throws IOException, ServletException {
        AbstractBuild<?,?> build = getBuild(req);
        if (build == null) {
            LOGGER.warning(String.format("No build found for url %s", req.getRequestURI()));
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        
        File artifact = new File(build.getArtifactsDir(), getArtifactName());
        if (!artifact.exists()) {
            LOGGER.warning(String.format("Artifact does not exists: %s for %s", getArtifactName(), build.getFullDisplayName()));
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        if (!artifact.isFile()) {
            LOGGER.warning(String.format("Artifact is not a file: %s for %s", getArtifactName(), build.getFullDisplayName()));
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        
        if (req.getDateHeader("If-Modified-Since") >= 0) {
            if (req.getDateHeader("If-Modified-Since") >= artifact.lastModified()) {
                resp.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
                return;
            }
        }
        
        String path = req.getRestOfPath();
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        ZipFile zip = null;
        try {
            try {
                zip = new ZipFile(artifact);
            } catch (ZipException e) {
                LOGGER.warning(String.format("Artifact is not a zip file: %s for %s", getArtifactName(), build.getFullDisplayName()));
                resp.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            
            if(path.length() > 0 && !req.getRequestURI().endsWith("/") && isDirectory(zip, path)) {
                // It seems that getRestOfPath() never contains trailing slash.
                // So we should see getRequestURI().
                resp.sendRedirect(String.format("%s/", req.getRequestURI()));
                return;
            }
            
            ZipEntry entry = getFileEntry(zip, path);
            if (entry == null) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            
            InputStream is = null;
            try {
                is = zip.getInputStream(entry);
                resp.serveFile(req, is, artifact.lastModified(), entry.getSize(), entry.getName());
            } finally {
                if (is != null) {
                    is.close();
                }
            }
            return;
        } finally {
            if (zip != null) {
                zip.close();
            }
        }
    }
    
    /**
     * @param zip
     * @param path
     * @return
     * @throws IOException
     */
    private ZipEntry getFileEntry(ZipFile zip, String path) throws IOException {
        if (!isDirectory(zip, path)) {
            ZipEntry entry = zip.getEntry(path);
            if (entry != null) {
                return entry;
            }
        }
        
        String indexFile = getIndexFile();
        if (StringUtils.isEmpty(indexFile)) {
            indexFile = "index.html,index.htm";
        }
        
        for (String file: StringUtils.split(indexFile, ",")) {
            file = StringUtils.trim(file);
            String filePath = StringUtils.isEmpty(path)?file:String.format("%s/%s", path, file);
            ZipEntry entry = zip.getEntry(filePath);
            if (entry != null && !isDirectory(zip, entry)) {
                return entry;
            }
        }
        
        return null;
    }
    
    /**
     * @param zip
     * @param entry
     * @return
     * @throws IOException
     */
    private static boolean isDirectory(ZipFile zip, ZipEntry entry) throws IOException {
        if (entry.isDirectory()) {
            return true;
        }
        InputStream is = zip.getInputStream(entry);
        if (is == null) {
            return true;
        }
        is.close();
        return false;
    }
    
    /**
     * @param zip
     * @param path
     * @return
     * @throws IOException
     */
    public static boolean isDirectory(ZipFile zip, String path) throws IOException {
        if (StringUtils.isEmpty(path) || "/".equals(path)) {
            return true;
        }
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        
        {
            ZipEntry entry = zip.getEntry(path);
            if (entry != null) {
                return isDirectory(zip, entry);
            }
        }
        
        String dirPrefix = String.format("%s/", path);
        
        Enumeration<? extends ZipEntry> entryEnum = zip.entries();
        while (entryEnum.hasMoreElements()) {
            ZipEntry entry = entryEnum.nextElement();
            if (entry.getName().startsWith(dirPrefix)) {
                return true;
            }
        }
        
        return false;
    }
}
