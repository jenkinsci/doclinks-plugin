package hudson.plugins.doclinks;

import hudson.Util;
import hudson.maven.MavenModule;
import hudson.model.AbstractItem;
import hudson.plugins.doclinks.m2.DocLinksMavenReporter;
import java.io.File;
import java.io.Serializable;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 *
 * @author Seiji Sogabe
 */
public class Document implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String title;
    private final String description;
    private final String directory;
    private final String file;
    private String id;

    @DataBoundConstructor
    public Document(final String title, final String description, final String directory, final String file, final String id) {
        this.title = Util.fixEmptyAndTrim(title);
        this.description = Util.fixEmptyAndTrim(description);
        this.directory = Util.fixEmptyAndTrim(directory);
        this.file = Util.fixEmptyAndTrim(file);
        this.id = Util.fixEmptyAndTrim(id);
    }

    public String getTitle() {
        return title;
    }

    public String getDirectory() {
        return directory;
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

    private boolean isDocumentExits(final File docLinksDir) {
        final File docDir = new File(docLinksDir, String.valueOf(getId()));
        if (file == null) {
            return docDir.exists();
        }
        final File indexFile = new File(docDir, file);
        return indexFile.exists();
    }
}
