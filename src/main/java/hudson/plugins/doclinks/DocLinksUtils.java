package hudson.plugins.doclinks;

import java.io.PrintStream;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Utilities.
 *
 * @author Seiji Sogabe
 */
public class DocLinksUtils {

    private DocLinksUtils() {
        // 
    }

    public static void log(final PrintStream logger, final String message) {
        final StringBuilder builder = new StringBuilder();
        builder.append("[").append(Constants.PLUGIN_NAME).append("] ").append(message);
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
}
