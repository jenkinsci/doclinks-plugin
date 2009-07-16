package hudson.plugins.doclinks;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.maven.AbstractMavenProject;
import hudson.model.AbstractBuild;
import hudson.model.AbstractItem;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.Descriptor.FormException;
import hudson.model.Hudson;
import hudson.model.Result;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Publisher;
import hudson.util.FormValidation;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletException;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

/**
 * Saves documents which are created in build step.
 * 
 * @author Seiji Sogabe
 */
public class DocLinksPublisher extends Publisher {

    @Extension
    public static final Descriptor<Publisher> DESCRIPTOR = new DocLinksDescriptor();

    private final List<Document> documents;

    public static File getDocLinksDir(final AbstractItem project) {
        return new File(project.getRootDir(), Constants.PLUGIN_URL);
    }

    public DocLinksPublisher(final List<Document> docs) {
        this.documents = docs;
    }

    /**
     * for config.jelly
     */
    public List<Document> getDocuments() {
        return Collections.unmodifiableList(documents);
    }

    @Override
    public Descriptor<Publisher> getDescriptor() {
        return DESCRIPTOR;
    }

    @Override
    public Action getProjectAction(final AbstractProject<?, ?> project) {
        final Map<String, Document> map = new LinkedHashMap<String, Document>();
        for (final Document doc : documents) {
            map.put(doc.getId(), doc);
        }
        return new DocLinksAction(project, map);
    }

    @Override
    public boolean perform(final AbstractBuild<?, ?> build, final Launcher launcher, final BuildListener listener) throws InterruptedException, IOException {

        final PrintStream logger = listener.getLogger();

        if (build.getResult().isWorseOrEqualTo(Result.FAILURE)) {
            return true;
        }

        // remove the old documents.
        final FilePath docLinksDir = new FilePath(getDocLinksDir(build.getProject()));
        try {
            docLinksDir.deleteRecursive();
        } catch (final IOException e) {
            Util.displayIOException(e, listener);
            build.setResult(Result.UNSTABLE);
            return true;
        }

        for (final Document doc : documents) {
            final String directory = doc.getDirectory();
            if (!DocLinksUtils.isValidDirectory(directory)) {
                final String cause = Messages.DocLinksUtils_DirectoryInvalid();
                DocLinksUtils.log(logger,
                        Messages.DocLinksPublisher_SkipDocument(doc.getTitle(), cause));
                continue;
            }

            final FilePath ws = build.getParent().getWorkspace();
            final FilePath docDir = (directory != null) ? ws.child(directory) : ws;
            if (!docDir.exists()) {
                final String cause = Messages.DocLinksPublisher_DirectoryNotExist(docDir.getName());
                DocLinksUtils.log(logger,
                        Messages.DocLinksPublisher_SkipDocument(doc.getTitle(), cause));
                continue;
            }

            final FilePath target = new FilePath(docLinksDir, String.valueOf(doc.getId()));
            try {
                DocLinksUtils.log(logger, Messages.DocLinksPublisher_CopyDocument(doc.getTitle(), target.getName()));
                docDir.copyRecursiveTo("**/*", target);
            } catch (final IOException e) {
                Util.displayIOException(e, listener);
                build.setResult(Result.UNSTABLE);
            }
        }

        return true;
    }
  
    public static class DocLinksDescriptor extends BuildStepDescriptor<Publisher> {

        public DocLinksDescriptor() {
            super(DocLinksPublisher.class);
        }

        @Override
        public Publisher newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            final List<Document> docs = req.bindParametersToList(Document.class, "doc.");
            // assign id for new documents;
            for (final Document doc : docs) {
                if (doc.getId() == null) {
                    doc.setId(DocLinksUtils.getNextId(docs));
                }
            }
            return new DocLinksPublisher(docs);
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return !AbstractMavenProject.class.isAssignableFrom(jobType);
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
        public FormValidation doCheckFile(@AncestorInPath AbstractProject project, @QueryParameter String dir,
                @QueryParameter String file) throws IOException, ServletException {
            Hudson.getInstance().checkPermission(Hudson.ADMINISTER);
            return DocLinksUtils.validateFile(project, dir, file);
        }

        @Override
        public String getDisplayName() {
            return Messages.DocLinksPublisher_DisplayName();
        }
    }
}