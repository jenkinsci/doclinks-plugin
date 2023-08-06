package hudson.plugins.doclinks.artifacts;

import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import org.kohsuke.stapler.StaplerRequest;

/**
 * Common base class for {@link ArtifactsDocLinksAction} and {@link ArtifactsDocLinksProjectAction}.
 */
public abstract class ArtifactsDocsLinksActionBase implements Action {
    /**
     * URL for this action.
     */
    public static final String URLNAME = "ArtifactsDocLinks";

    /**
     * Returns the container of this action.
     *
     * @param req {@link StaplerRequest} used for access this action.
     * @return {@link AbstractProject} or {@link AbstractBuild}.
     */
    public Object getOwner(StaplerRequest req) {
        AbstractBuild<?, ?> build = req.findAncestorObject(AbstractBuild.class);
        if (build != null) {
            return build;
        }

        AbstractProject<?, ?> project = req.findAncestorObject(AbstractProject.class);
        if (project != null) {
            return project;
        }

        return null;
    }

    /**
     * @param req
     * @return the project containing this action
     */
    public AbstractProject<?, ?> getProject(StaplerRequest req) {
        AbstractProject<?, ?> project = req.findAncestorObject(AbstractProject.class);
        if (project != null) {
            return project;
        }

        return null;
    }

    /**
     * Returns the build that have artifacts this action handles.
     *
     * If called in a project context, returns the last build
     * that contains artifact documents.
     *
     * @param req
     * @return the build with document artifacts to handle.
     */
    public AbstractBuild<?, ?> getBuild(StaplerRequest req) {
        AbstractBuild<?, ?> build = req.findAncestorObject(AbstractBuild.class);
        if (build != null) {
            return build;
        }

        AbstractProject<?, ?> project = req.findAncestorObject(AbstractProject.class);
        if (project != null) {
            return getLastDocumentedBuild(project);
        }

        return null;
    }

    /**
     * Returns the last build with artifact documents in a project.
     *
     * @param project
     * @return a build with artifact documents.
     */
    public AbstractBuild<?, ?> getLastDocumentedBuild(AbstractProject<?, ?> project) {
        for (AbstractBuild<?, ?> build = project.getLastBuild(); build != null; build = build.getPreviousBuild()) {
            if (build.getAction(ArtifactsDocsLinksActionBase.class) != null) {
                return build;
            }
        }
        return null;
    }

    /**
     * Returns the name of an icon used in the side menu.
     *
     * @return
     * @see hudson.model.Action#getIconFileName()
     */
    @Override
    public String getIconFileName() {
        return "document.gif";
    }

    /**
     * Returns an URL to access this action.
     *
     * This URL is relative from the owner object.
     *
     * @return
     * @see hudson.model.Action#getUrlName()
     */
    @Override
    public String getUrlName() {
        return URLNAME;
    }
}
