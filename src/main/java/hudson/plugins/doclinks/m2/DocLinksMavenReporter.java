package hudson.plugins.doclinks.m2;

import hudson.plugins.doclinks.*;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.maven.MavenBuild;
import hudson.maven.MavenModule;
import hudson.maven.MavenReporter;
import hudson.maven.MavenReporterDescriptor;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.BuildListener;
import hudson.model.Descriptor.FormException;
import hudson.model.Hudson;
import hudson.model.Result;
import hudson.util.FormValidation;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

/**
 *
 * @author sogabe
 */
public class DocLinksMavenReporter extends MavenReporter {

    private static final long serialVersionUID = 1L;

    @Extension
    public static final MavenReporterDescriptor DESCRIPTOR = new DocLinksMavenReporterDescriptor();

    private final List<Document> documents;

    public DocLinksMavenReporter(final List<Document> docs) {
        this.documents = docs;
    }

    public static File getDocLinksDir(final MavenModule module) {
        return new File(module.getRootDir(), Constants.PLUGIN_URL);
    }

    @Override
    public MavenReporterDescriptor getDescriptor() {
        return DESCRIPTOR;
    }

    /**
     * for config.jelly
     */
    public List<Document> getDocuments() {
        return Collections.unmodifiableList(documents);
    }

    @Override
    public Action getProjectAction(final MavenModule module) {
        final Map<String, Document> map = new LinkedHashMap<String, Document>();
        for (final Document doc : documents) {
            map.put(doc.getId(), doc);
        }
        return new DocLinksMavenAction(module, map);
    }

    @Override
    public boolean end(final MavenBuild build, final Launcher launcher, final BuildListener listener) throws InterruptedException, IOException {

        final PrintStream logger = listener.getLogger();

        if (build.getResult().isWorseOrEqualTo(Result.FAILURE)) {
            return true;
        }

        final FilePath ws = build.getParent().getWorkspace();
        final FilePath docLinksDir = new FilePath(getDocLinksDir(build.getParent()));

        try {
            docLinksDir.deleteRecursive();
            for (final Document doc : documents) {
                DocLinksUtils.publishDocument(doc, ws, docLinksDir, logger);
            }
        } catch (final IOException e) {
            Util.displayIOException(e, listener);
            build.setResult(Result.UNSTABLE);
            return true;
        }
 
        build.registerAsProjectAction(this);

        return true;
    }

    public static class DocLinksMavenReporterDescriptor extends MavenReporterDescriptor {

        public DocLinksMavenReporterDescriptor() {
            super(DocLinksMavenReporter.class);
        }

        @Override
        public DocLinksMavenReporter newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            final List<Document> docs = req.bindParametersToList(Document.class, "doc.");
            // assign id for new documents;
            for (final Document doc : docs) {
                if (doc.getId() == null) {
                    doc.setId(DocLinksUtils.getNextId(docs));
                }
            }
            return new DocLinksMavenReporter(docs);
        }

        /**
         * check to see if title is not null.
         */
        public FormValidation doCheckTitle(@QueryParameter String title) throws IOException, ServletException {
            Hudson.getInstance().checkPermission(Hudson.ADMINISTER);
            return DocLinksUtils.validateTitle(title);
        }

        /**
         * check to see if directory is valid and exists.
         */
        public FormValidation doCheckDirectory(@AncestorInPath AbstractProject project, @QueryParameter String dir)
                throws IOException, ServletException {
            Hudson.getInstance().checkPermission(Hudson.ADMINISTER);
            return DocLinksUtils.validateDirectory(project, dir);
        }

        /**
         * check to see if file exists.
         */
        public FormValidation doCheckFile(@AncestorInPath AbstractProject project, @QueryParameter String dir, @QueryParameter String file)
                throws IOException, ServletException {
            Hudson.getInstance().checkPermission(Hudson.ADMINISTER);
            return DocLinksUtils.validateFile(project, dir, file);
        }

        @Override
        public String getDisplayName() {
            return Messages.DocLinksMavenReporter_DisplayName();
        }
    }

    private static final Logger LOGGER = Logger.getLogger(DocLinksMavenReporter.class.getName());
}