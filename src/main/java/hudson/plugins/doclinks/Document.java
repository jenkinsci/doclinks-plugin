package hudson.plugins.doclinks;

import hudson.Util;
import hudson.model.AbstractItem;
import java.io.File;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 *
 * @author Seiji Sogabe
 */
public class Document {

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
        final File docDir = new File(docLinksDir, String.valueOf(getId()));
        if (file == null) {
            return docDir.exists();
        }
        final File indexFile = new File(docDir, file);
        return indexFile.exists();
    }
}
