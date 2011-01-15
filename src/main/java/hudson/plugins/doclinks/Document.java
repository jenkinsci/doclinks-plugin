package hudson.plugins.doclinks;

import hudson.FilePath;
import hudson.Util;
import hudson.maven.MavenModule;
import hudson.model.AbstractItem;
import hudson.plugins.doclinks.m2.DocLinksMavenReporter;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Serializable;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Document Model.
 * @author Seiji Sogabe
 */
public class Document implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String title;
    private final String description;
    private final String directory;
    /**
     * @since 0.4
     */
    private final boolean recursive;
    private final String file;
    private String id;

    @DataBoundConstructor
    public Document(String title, String description, String directory, boolean recursive, String file,
            String id) {
        this.title = Util.fixEmptyAndTrim(title);
        this.description = Util.fixEmptyAndTrim(description);
        this.directory = Util.fixEmptyAndTrim(directory);
        this.recursive = recursive;
        this.file = Util.fixEmptyAndTrim(file);
        this.id = Util.fixEmptyAndTrim(id);
    }

    public String getTitle() {
        return title;
    }

    public String getDirectory() {
        return directory;
    }

    /**
     * @since 1.4
     */
    public boolean isRecursive(){
        return recursive;
    }
    
    public String getFile() {
        return file;
    }

    public String getDescription() {
        return description;
    }

    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public boolean hasResources(final AbstractItem project) {
        final File docLinksDir = DocLinksPublisher.getDocLinksDir(project);
        return isDocumentExits(docLinksDir);
    }

    public boolean hasResources(final MavenModule module) {
        final File docLinksDir = DocLinksMavenReporter.getDocLinksDir(module);
        return isDocumentExits(docLinksDir);
    }

    public void publish(FilePath origin, FilePath dest, PrintStream logger)
            throws IOException, InterruptedException {

        String dir = getDirectory();
        if (!DocLinksUtils.isValidDirectory(dir)) {
            String cause = Messages.DocLinksUtils_DirectoryInvalid();
            DocLinksUtils.log(logger, Messages.Document_SkipDocument(getTitle(), cause));
            throw new IOException("directory is invalid.");
        }

        FilePath docDir = (dir != null) ? origin.child(dir) : origin;
        if (!docDir.exists()) {
            String cause = Messages.DocLinksUtils_DirectoryNotExist(docDir.getName());
            DocLinksUtils.log(logger, Messages.Document_SkipDocument(getTitle(), cause));
            throw new IOException("docDir does not exist.");
        }

        FilePath targetDir = new FilePath(dest, String.valueOf(getId()));
        DocLinksUtils.log(logger, Messages.Document_CopyDocument(getTitle(), targetDir.getName()));

        if (isRecursive()) {
            docDir.copyRecursiveTo("**/*", targetDir);
        } else {
            docDir.copyRecursiveTo("*", targetDir);
        }
    }

    private boolean isDocumentExits(final File docLinksDir) {
        final File docDir = new File(docLinksDir, String.valueOf(getId()));
        if (file == null) {
            return docDir.exists();
        }
        final File indexFile = new File(docDir, file);
        return indexFile.exists();
    }
}
