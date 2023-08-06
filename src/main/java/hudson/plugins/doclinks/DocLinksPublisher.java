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
import hudson.model.Descriptor.FormException;
import hudson.model.Job;
import hudson.model.Result;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.util.FormValidation;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletException;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

/**
 * Saves documents which are created in build step.
 *
 * @author Seiji Sogabe
 */
public class DocLinksPublisher extends Recorder {

    @Extension
    public static final BuildStepDescriptor<Publisher> DESCRIPTOR = new DocLinksDescriptor();

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
    public BuildStepDescriptor<Publisher> getDescriptor() {
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
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    @Override
    public boolean perform(final AbstractBuild<?, ?> build, final Launcher launcher, final BuildListener listener)
            throws InterruptedException, IOException {

        final PrintStream logger = listener.getLogger();

        if (build.getResult().isWorseOrEqualTo(Result.FAILURE)) {
            return true;
        }

        final FilePath ws = build.getWorkspace();
        final FilePath docLinksDir = new FilePath(getDocLinksDir(build.getProject()));

        try {
            synchronized (this) {
                docLinksDir.deleteRecursive();
                for (final Document doc : documents) {
                    doc.publish(ws, docLinksDir, logger);
                }
            }
        } catch (final IOException e) {
            Util.displayIOException(e, listener);
            build.setResult(Result.UNSTABLE);
            return true;
        }

        return true;
    }

    public static class DocLinksDescriptor extends BuildStepDescriptor<Publisher> {

        public DocLinksDescriptor() {
            super(DocLinksPublisher.class);
        }

        @Override
        public Publisher newInstance(final StaplerRequest req, final JSONObject formData) throws FormException {
            final List<Document> docs = req.bindJSONToList(Document.class, formData.get("docs"));
            // assign id for new documents;
            for (final Document doc : docs) {
                if (doc.getId() == null) {
                    doc.setId(DocLinksUtils.getNextId(docs));
                }
            }
            return new DocLinksPublisher(docs);
        }

        @Override
        public boolean isApplicable(final Class<? extends AbstractProject> jobType) {
            if (Jenkins.getInstance().getPlugin("maven-plugin") == null) {
                return true;
            }
            return !AbstractMavenProject.class.isAssignableFrom(jobType);
        }

        /**
         * check to see if title is not null.
         */
        public FormValidation doCheckTitle(
                @AncestorInPath final AbstractProject<?, ?> project, @QueryParameter final String title)
                throws IOException, ServletException {
            project.checkPermission(Job.CONFIGURE);
            return DocLinksUtils.validateTitle(title);
        }

        /**
         * check to see if directory is valid and exists.
         */
        public FormValidation doCheckDirectory(
                @AncestorInPath final AbstractProject<?, ?> project, @QueryParameter final String dir)
                throws IOException, ServletException {
            project.checkPermission(Job.CONFIGURE);
            return DocLinksUtils.validateDirectory(project, dir);
        }

        /**
         * check to see if file exists.
         */
        public FormValidation doCheckFile(
                @AncestorInPath final AbstractProject<?, ?> project,
                @QueryParameter final String dir,
                @QueryParameter final String file)
                throws IOException, ServletException {
            project.checkPermission(Job.CONFIGURE);
            return DocLinksUtils.validateFile(project, dir, file);
        }

        @Override
        public String getDisplayName() {
            return Messages.DocLinksPublisher_DisplayName();
        }
    }
}
