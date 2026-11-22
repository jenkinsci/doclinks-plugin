package hudson.plugins.doclinks;

import hudson.FilePath;
import hudson.model.AbstractItem;
import hudson.model.Action;
import hudson.model.DirectoryBrowserSupport;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.logging.Logger;
import org.kohsuke.stapler.StaplerRequest2;
import org.kohsuke.stapler.StaplerResponse2;

/**
 * Action which publishes ducuments.
 *
 * @author Seiji Sogabe
 */
public class DocLinksAction implements Action {

    private final AbstractItem project;
    private final Map<String, Document> documents;

    public DocLinksAction(final AbstractItem project, final Map<String, Document> document) {
        this.project = project;
        this.documents = document;
    }

    /**
     * for jobMain.jelly
     */
    public Map<String, Document> getDocumentsMap() {
        return Collections.unmodifiableMap(documents);
    }

    @Override
    public String getDisplayName() {
        return "";
    }

    @Override
    public String getIconFileName() {
        return null;
    }

    @Override
    public String getUrlName() {
        return Constants.PLUGIN_URL;
    }

    public boolean hasDocument(final Document doc) {
        return doc.hasResources(project);
    }

    public DirectoryBrowserSupport doDynamic(final StaplerRequest2 req, final StaplerResponse2 rsp)
            throws IOException, ServletException {

        // get document id from request
        final String id = DocLinksUtils.getDocumentId(req.getRestOfPath());
        if (id == null) {
            LOGGER.warning(Messages.DocLinksAction_IllegalURI(req.getRestOfPath()));
            rsp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return null;
        }

        // check document
        final Document doc = documents.get(id);
        if (doc == null) {
            LOGGER.warning(Messages.DocLinksAction_DocumentNotFound());
            rsp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return null;
        }

        final FilePath basePath = new FilePath(DocLinksPublisher.getDocLinksDir(project));
        final DirectoryBrowserSupport dbs =
                new DirectoryBrowserSupport(this, basePath, Constants.PLUGIN_NAME, null, false);
        // set indexfile
        if (doc.getFile() != null) {
            dbs.setIndexFileName(doc.getFile());
        }

        return dbs;
    }

    private static final Logger LOGGER = Logger.getLogger(DocLinksAction.class.getName());
}
