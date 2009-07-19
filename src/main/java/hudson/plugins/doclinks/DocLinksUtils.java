package hudson.plugins.doclinks;

import hudson.FilePath;
import hudson.Util;
import hudson.model.AbstractProject;
import hudson.util.FormValidation;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Utilities.
 *
 * @author Seiji Sogabe
 */
public final class DocLinksUtils {

    private DocLinksUtils() {
        // 
    }

    public static void log(final PrintStream logger, final String message) {
        final StringBuilder builder = new StringBuilder();
        builder.append('[').append(Constants.PLUGIN_NAME).append("] ").append(message);
        logger.println(builder.toString());
    }

    /**
     * get the id, which has not been used.
     */
    public static synchronized String getNextId(final List<Document> documents) {
        int max = 0;
        for (final Document doc : documents) {
            if (doc.getId() == null) {
                continue;
            }
            final int id = Integer.parseInt(doc.getId());
            max = Math.max(max, id);
        }
        return String.valueOf(max + 1);
    }

    public static boolean isValidDirectory(final String dir) {
        if (dir == null) {
            return true;
        }
        String d = dir.replace("\\", "/");
        if (!d.endsWith("/")) {
            d = d + "/";
        }
        return d.indexOf("../") == -1;
    }

    public static String getDocumentId(final String path) {
        final StringTokenizer tokenizer = new StringTokenizer(path, "/");
        if (tokenizer.hasMoreElements()) {
            return tokenizer.nextToken();
        }
        return null;
    }

    public static FormValidation validateTitle(final String title) {
        final String t = Util.fixEmptyAndTrim(title);
        if (t == null) {
            return FormValidation.error(Messages.DocLinksUtils_Required());
        }
        return FormValidation.ok();
    }

    public static FormValidation validateDirectory(final AbstractProject<?, ?> project, final String directory)
            throws IOException {
        final String dir = Util.fixEmptyAndTrim(directory);
        if (!DocLinksUtils.isValidDirectory(dir)) {
            return FormValidation.error(Messages.DocLinksUtils_DirectoryInvalid());
        }
        final FilePath ws = project.getWorkspace();
        return (ws != null) ? ws.validateRelativeDirectory(dir) : FormValidation.ok();
    }

    public static FormValidation validateFile(final AbstractProject<?, ?> project, final String directory, final String file)
            throws IOException {
        final String f = Util.fixEmptyAndTrim(file);
        if (f == null) {
            FormValidation.ok();
        }
        // job has not built yet
        final FilePath ws = project.getWorkspace();
        if (ws == null) {
            return FormValidation.ok();
        }
        final String dir = Util.fixEmptyAndTrim(directory);
        final FilePath targetDir = (dir != null) ? new FilePath(ws, dir) : ws;
        return targetDir.validateRelativePath(file, true, true);
    }

    public static void publishDocument(final Document doc, final FilePath ws, final FilePath docLinksDir, 
            final PrintStream logger) throws IOException, InterruptedException {

        final String directory = doc.getDirectory();
        if (!DocLinksUtils.isValidDirectory(directory)) {
            final String cause = Messages.DocLinksUtils_DirectoryInvalid();
            DocLinksUtils.log(logger, Messages.DocLinksUtils_SkipDocument(doc.getTitle(), cause));
            throw new IOException("directory is invalid.");
        }

        final FilePath docDir = (directory != null) ? ws.child(directory) : ws;
        if (!docDir.exists()) {
            final String cause = Messages.DocLinksUtils_DirectoryNotExist(docDir.getName());
            DocLinksUtils.log(logger, Messages.DocLinksUtils_SkipDocument(doc.getTitle(), cause));
            throw new IOException("docDir does not exist.");
        }

        final FilePath target = new FilePath(docLinksDir, String.valueOf(doc.getId()));
        DocLinksUtils.log(logger, Messages.DocLinksUtils_CopyDocument(doc.getTitle(), target.getName()));
        docDir.copyRecursiveTo("**/*", target);
    }

}
