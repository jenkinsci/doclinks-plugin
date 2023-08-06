/*
 * The MIT License
 *
 * Copyright (c) 2013 IKEDA Yasuyuki
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package hudson.plugins.doclinks.artifacts;

import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.plugins.doclinks.artifacts.testtools.ArtifactDocLinksHudsonTestCase;
import hudson.plugins.doclinks.artifacts.testtools.TestFileBuilder;
import hudson.plugins.doclinks.artifacts.testtools.TestZipBuilder;
import hudson.tasks.ArtifactArchiver;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang.StringUtils;
import org.htmlunit.html.HtmlAnchor;
import org.htmlunit.html.HtmlPage;
import org.xml.sax.SAXException;

/**
 *
 */
public class ArtifactsDocLinksPublisherHudsonTest extends ArtifactDocLinksHudsonTestCase {
    public static final int BUILD_TIMEOUT = 10;

    protected void assertNoDocumentLink(AbstractProject<?, ?> p) throws IOException, SAXException {
        // There is no link in project.
        String url = p.getAction(ArtifactsDocLinksProjectAction.class).getUrlName();
        WebClient wc = getWebClient();
        HtmlPage page = wc.getPage(p);
        for (HtmlAnchor a : page.getAnchors()) {
            assertFalse(a.getHrefAttribute().contains(url));
        }
    }

    protected void assertDocumentLink(AbstractProject<?, ?> p) throws IOException, SAXException {
        // There is a link in project.
        String url = p.getAction(ArtifactsDocLinksProjectAction.class).getUrlName();
        boolean found = false;
        WebClient wc = getWebClient();
        HtmlPage page = wc.getPage(p);
        for (HtmlAnchor a : page.getAnchors()) {
            if (a.getHrefAttribute().contains(url)) {
                found = true;
                break;
            }
        }
        assertTrue(found);
    }

    protected void assertDocumentContains(AbstractBuild<?, ?> build, int index, String path, String content)
            throws IOException, SAXException {
        WebClient wc = getWebClient();
        ArtifactsDocLinksAction action = build.getAction(ArtifactsDocLinksAction.class);
        String url = null;

        if (StringUtils.isEmpty(path)) {
            url = String.format(
                    "%s/%s",
                    action.getUrlName(),
                    Util.rawEncode(
                            action.getArtifactsDocLinksDocumentList().get(index).getId()));
        } else if (path.startsWith("/")) {
            url = String.format(
                    "%s/%s%s",
                    action.getUrlName(),
                    Util.rawEncode(
                            action.getArtifactsDocLinksDocumentList().get(index).getId()),
                    path);
        } else {
            url = String.format(
                    "%s/%s/%s",
                    action.getUrlName(),
                    Util.rawEncode(
                            action.getArtifactsDocLinksDocumentList().get(index).getId()),
                    path);
        }

        HtmlPage page = wc.getPage(build, url);
        assertTrue(page.asNormalizedText(), page.asNormalizedText().contains(content));
    }

    protected void assertLatestDocumentContains(AbstractBuild<?, ?> build, int index, String path, String content)
            throws IOException, SAXException {
        AbstractProject<?, ?> p = build.getParent();

        WebClient wc = getWebClient();
        ArtifactsDocLinksProjectAction action = p.getAction(ArtifactsDocLinksProjectAction.class);
        ArtifactsDocLinksAction buildAction = build.getAction(ArtifactsDocLinksAction.class);

        String url = null;
        if (StringUtils.isEmpty(path)) {
            url = String.format(
                    "%s/%s",
                    action.getUrlName(),
                    Util.rawEncode(buildAction
                            .getArtifactsDocLinksDocumentList()
                            .get(index)
                            .getId()));
        } else if (path.startsWith("/")) {
            url = String.format(
                    "%s/%s%s",
                    action.getUrlName(),
                    Util.rawEncode(buildAction
                            .getArtifactsDocLinksDocumentList()
                            .get(index)
                            .getId()),
                    path);
        } else {
            url = String.format(
                    "%s/%s/%s",
                    action.getUrlName(),
                    Util.rawEncode(buildAction
                            .getArtifactsDocLinksDocumentList()
                            .get(index)
                            .getId()),
                    path);
        }
        HtmlPage page = wc.getPage(p, url);
        assertTrue(page.asNormalizedText(), page.asNormalizedText().contains(content));
    }

    public void testPublishSingleArtifact() throws Exception {
        FreeStyleProject p = createFreeStyleProject();
        p.getPublishersList().add(new ArtifactArchiver("artifact1.zip", "", false));
        p.getPublishersList()
                .add(new ArtifactsDocLinksPublisher(
                        Arrays.asList(new ArtifactsDocLinksConfig("Test", "artifact1.zip", null, null))));
        p.save();
        updateTransientActions(p);
        // Opening configure with TestZipBuilder causes 500.
        p.getBuildersList().add(new TestZipBuilder("artifact1.zip"));

        assertNotNull(p.getAction(ArtifactsDocLinksProjectAction.class));
        assertNoDocumentLink(p);

        FreeStyleBuild build = p.scheduleBuild2(0).get(BUILD_TIMEOUT, TimeUnit.SECONDS);
        assertBuildStatusSuccess(build);

        assertNotNull(build.getAction(ArtifactsDocLinksAction.class));
        assertDocumentContains(build, 0, null, "Default top page.");

        assertDocumentLink(p);
        assertLatestDocumentContains(build, 0, null, "Default top page.");

        // Try again!
        p.getBuildersList().clear();
        p.getPublishersList().clear();
        p.getPublishersList().add(new ArtifactArchiver("artifact1.zip", "", false));
        p.getPublishersList()
                .add(new ArtifactsDocLinksPublisher(
                        Arrays.asList(new ArtifactsDocLinksConfig("Test", "artifact1.zip", null, "default.html"))));
        p.save();
        updateTransientActions(p);
        // Opening configure with TestZipBuilder causes 500.
        p.getBuildersList().add(new TestZipBuilder("artifact1.zip"));

        assertNotNull(p.getAction(ArtifactsDocLinksProjectAction.class));
        assertDocumentLink(p);

        build = p.scheduleBuild2(0).get(BUILD_TIMEOUT, TimeUnit.SECONDS);
        assertBuildStatusSuccess(build);

        assertNotNull(build.getAction(ArtifactsDocLinksAction.class));
        assertDocumentContains(build, 0, null, "Alternate top page.");

        assertDocumentLink(p);
        assertLatestDocumentContains(build, 0, null, "Alternate top page.");

        // Failure
        p.getBuildersList().clear();
        p.getPublishersList().clear();
        p.getPublishersList().add(new ArtifactArchiver("artifact1.zip", "", false));
        p.getPublishersList()
                .add(new ArtifactsDocLinksPublisher(
                        Arrays.asList(new ArtifactsDocLinksConfig("Test", "artifact1.zip", null, "default.html"))));
        p.save();
        updateTransientActions(p);

        assertNotNull(p.getAction(ArtifactsDocLinksProjectAction.class));
        assertDocumentLink(p);

        build = p.scheduleBuild2(0).get(BUILD_TIMEOUT, TimeUnit.SECONDS);
        assertBuildStatusSuccess(build);

        assertNotNull(build.getAction(ArtifactsDocLinksAction.class));
        assertDocumentContains(build, 0, null, "Alternate top page.");

        assertDocumentLink(p);
        assertLatestDocumentContains(build, 0, null, "Alternate top page.");
    }

    public void testPublishNestedArtifact() throws Exception {
        FreeStyleProject p = createFreeStyleProject();
        p.getPublishersList().add(new ArtifactArchiver("sub1/sub2/artifact1.zip", "", false));
        p.getPublishersList()
                .add(new ArtifactsDocLinksPublisher(
                        Arrays.asList(new ArtifactsDocLinksConfig("Test", "sub1/sub2/artifact1.zip", null, null))));
        p.save();
        updateTransientActions(p);
        // Opening configure with TestZipBuilder causes 500.
        p.getBuildersList().add(new TestZipBuilder("sub1/sub2/artifact1.zip"));

        assertNotNull(p.getAction(ArtifactsDocLinksProjectAction.class));
        assertNoDocumentLink(p);

        FreeStyleBuild build = p.scheduleBuild2(0).get(BUILD_TIMEOUT, TimeUnit.SECONDS);
        assertBuildStatusSuccess(build);

        assertNotNull(build.getAction(ArtifactsDocLinksAction.class));
        assertDocumentContains(build, 0, null, "Default top page.");

        assertDocumentLink(p);
        assertLatestDocumentContains(build, 0, null, "Default top page.");
    }

    public void testPublishMultipleArtifact1() throws Exception {
        FreeStyleProject p = createFreeStyleProject();
        p.getPublishersList().add(new ArtifactArchiver("artifact1.zip,artifact2.zip", "", false));
        p.getPublishersList()
                .add(new ArtifactsDocLinksPublisher(Arrays.asList(
                        new ArtifactsDocLinksConfig("Test", "artifact1.zip", null, null),
                        new ArtifactsDocLinksConfig("Test", "artifact2.zip", null, "default.html"))));
        p.save();
        updateTransientActions(p);
        // Opening configure with TestZipBuilder causes 500.
        p.getBuildersList().add(new TestZipBuilder("artifact1.zip"));
        p.getBuildersList().add(new TestZipBuilder("artifact2.zip"));

        assertNotNull(p.getAction(ArtifactsDocLinksProjectAction.class));
        assertNoDocumentLink(p);

        FreeStyleBuild build = p.scheduleBuild2(0).get(BUILD_TIMEOUT, TimeUnit.SECONDS);
        assertBuildStatusSuccess(build);

        assertNotNull(build.getAction(ArtifactsDocLinksAction.class));
        assertDocumentContains(build, 0, null, "Default top page.");
        assertDocumentContains(build, 1, null, "Alternate top page.");

        assertDocumentLink(p);
        assertLatestDocumentContains(build, 0, null, "Default top page.");
        assertLatestDocumentContains(build, 1, null, "Alternate top page.");
    }

    public void testPublishMultipleArtifact2() throws Exception {
        FreeStyleProject p = createFreeStyleProject();
        p.getPublishersList().add(new ArtifactArchiver("artifact1.zip,artifact2.zip", "", false));
        p.getPublishersList()
                .add(new ArtifactsDocLinksPublisher(
                        Arrays.asList(new ArtifactsDocLinksConfig("Test", "artifact1.zip,artifact2.zip", null, null))));
        p.save();
        updateTransientActions(p);
        // Opening configure with TestZipBuilder causes 500.
        p.getBuildersList().add(new TestZipBuilder("artifact1.zip"));
        p.getBuildersList().add(new TestZipBuilder("artifact2.zip"));

        assertNotNull(p.getAction(ArtifactsDocLinksProjectAction.class));
        assertNoDocumentLink(p);

        FreeStyleBuild build = p.scheduleBuild2(0).get(BUILD_TIMEOUT, TimeUnit.SECONDS);
        assertBuildStatusSuccess(build);

        assertNotNull(build.getAction(ArtifactsDocLinksAction.class));
        assertDocumentContains(build, 0, null, "Default top page.");
        assertDocumentContains(build, 1, null, "Default top page.");

        assertDocumentLink(p);
        assertLatestDocumentContains(build, 0, null, "Default top page.");
        assertLatestDocumentContains(build, 1, null, "Default top page.");
    }

    public void testPublishMultipleArtifact3() throws Exception {
        FreeStyleProject p = createFreeStyleProject();
        p.getPublishersList().add(new ArtifactArchiver("artifact1.zip,artifact2.zip", "", false));
        p.getPublishersList()
                .add(new ArtifactsDocLinksPublisher(
                        Arrays.asList(new ArtifactsDocLinksConfig("Test", "*.zip", null, null))));
        p.save();
        updateTransientActions(p);
        // Opening configure with TestZipBuilder causes 500.
        p.getBuildersList().add(new TestZipBuilder("artifact1.zip"));
        p.getBuildersList().add(new TestZipBuilder("artifact2.zip"));

        assertNotNull(p.getAction(ArtifactsDocLinksProjectAction.class));
        assertNoDocumentLink(p);

        FreeStyleBuild build = p.scheduleBuild2(0).get(BUILD_TIMEOUT, TimeUnit.SECONDS);
        assertBuildStatusSuccess(build);

        assertNotNull(build.getAction(ArtifactsDocLinksAction.class));
        assertDocumentContains(build, 0, null, "Default top page.");
        assertDocumentContains(build, 1, null, "Default top page.");

        assertDocumentLink(p);
        assertLatestDocumentContains(build, 0, null, "Default top page.");
        assertLatestDocumentContains(build, 1, null, "Default top page.");
    }

    public void testScanArtifacts() throws Exception {
        {
            FreeStyleProject p = createFreeStyleProject();
            p.getBuildersList().add(new TestZipBuilder("artifact1.zip"));
            p.getBuildersList().add(new TestZipBuilder("artifact2.zip"));
            p.getPublishersList().add(new ArtifactArchiver("artifact1.zip,artifact2.zip", "", false));
            p.getPublishersList()
                    .add(new ArtifactsDocLinksPublisher(Arrays.asList(
                            new ArtifactsDocLinksConfig("Test", "  *1.zip  ,  *2.zip   , *3.zip   ", null, null))));
            p.save();

            FreeStyleBuild build = p.scheduleBuild2(0).get(BUILD_TIMEOUT, TimeUnit.SECONDS);
            assertBuildStatusSuccess(build);

            assertNotNull(build.getAction(ArtifactsDocLinksAction.class));
            assertEquals(
                    2,
                    build.getAction(ArtifactsDocLinksAction.class)
                            .getArtifactsDocLinksDocumentList()
                            .size());
            assertDocumentContains(build, 0, null, "Default top page.");
            assertDocumentContains(build, 1, null, "Default top page.");
        }

        {
            FreeStyleProject p = createFreeStyleProject();
            p.getBuildersList().add(new TestZipBuilder("artifact1.zip"));
            p.getPublishersList().add(new ArtifactArchiver("artifact1.zip,artifact2.zip", "", false));
            p.getPublishersList()
                    .add(new ArtifactsDocLinksPublisher(Arrays.asList(new ArtifactsDocLinksConfig(
                            "Test", "  *1.zip  ,  *.zip   , artifact*.zip   ", null, null))));
            p.save();

            FreeStyleBuild build = p.scheduleBuild2(0).get(BUILD_TIMEOUT, TimeUnit.SECONDS);
            assertBuildStatusSuccess(build);

            assertNotNull(build.getAction(ArtifactsDocLinksAction.class));
            assertEquals(
                    1,
                    build.getAction(ArtifactsDocLinksAction.class)
                            .getArtifactsDocLinksDocumentList()
                            .size());
            assertDocumentContains(build, 0, null, "Default top page.");
        }
    }

    public void testPerformSuccess() throws Exception {
        FreeStyleProject p = createFreeStyleProject();
        p.getBuildersList().add(new TestZipBuilder("artifact1.zip"));
        p.getPublishersList().add(new ArtifactArchiver("artifact1.zip", "", false));
        p.getPublishersList()
                .add(new ArtifactsDocLinksPublisher(
                        Arrays.asList(new ArtifactsDocLinksConfig("Test", "artifact1.zip", null, null))));
        p.save();

        FreeStyleBuild build = p.scheduleBuild2(0).get(BUILD_TIMEOUT, TimeUnit.SECONDS);
        assertBuildStatusSuccess(build);
    }

    public void testPerformFailureForNull() throws Exception {
        FreeStyleProject p = createFreeStyleProject();
        p.getBuildersList().add(new TestZipBuilder("artifact1.zip"));
        p.getPublishersList().add(new ArtifactArchiver("artifact1.zip", "", false));
        p.getPublishersList().add(new ArtifactsDocLinksPublisher(null));
        p.save();

        FreeStyleBuild build = p.scheduleBuild2(0).get(BUILD_TIMEOUT, TimeUnit.SECONDS);
        assertBuildStatus(Result.FAILURE, build);
    }

    public void testPerformFailureForEmpty() throws Exception {
        FreeStyleProject p = createFreeStyleProject();
        p.getBuildersList().add(new TestZipBuilder("artifact1.zip"));
        p.getPublishersList().add(new ArtifactArchiver("artifact1.zip", "", false));
        p.getPublishersList().add(new ArtifactsDocLinksPublisher(Collections.<ArtifactsDocLinksConfig>emptyList()));
        p.save();

        FreeStyleBuild build = p.scheduleBuild2(0).get(BUILD_TIMEOUT, TimeUnit.SECONDS);
        assertBuildStatus(Result.FAILURE, build);
    }

    public void testPerformFailureForNoMatch() throws Exception {
        FreeStyleProject p = createFreeStyleProject();
        p.getBuildersList().add(new TestZipBuilder("artifact1.zip"));
        p.getPublishersList().add(new ArtifactArchiver("artifact1.zip", "", false));
        p.getPublishersList()
                .add(new ArtifactsDocLinksPublisher(
                        Arrays.asList(new ArtifactsDocLinksConfig("Test", "artifactNone.zip", null, null))));
        p.save();

        FreeStyleBuild build = p.scheduleBuild2(0).get(BUILD_TIMEOUT, TimeUnit.SECONDS);
        assertBuildStatus(Result.FAILURE, build);
    }

    public void testPerformFailureForNoArtifacts() throws Exception {
        FreeStyleProject p = createFreeStyleProject();
        p.getBuildersList().add(new TestZipBuilder("artifact1.zip"));
        p.getPublishersList()
                .add(new ArtifactsDocLinksPublisher(
                        Arrays.asList(new ArtifactsDocLinksConfig("Test", "*", null, null))));
        p.save();

        FreeStyleBuild build = p.scheduleBuild2(0).get(BUILD_TIMEOUT, TimeUnit.SECONDS);
        assertBuildStatus(Result.FAILURE, build);
    }

    public void testPerformFailureForNotZip() throws Exception {
        FreeStyleProject p = createFreeStyleProject();
        p.getBuildersList().add(new TestFileBuilder("artifact1.zip", "Some proper content"));
        p.getPublishersList().add(new ArtifactArchiver("artifact1.zip", "", false));
        p.getPublishersList()
                .add(new ArtifactsDocLinksPublisher(
                        Arrays.asList(new ArtifactsDocLinksConfig("Test", "artifact1.zip", null, null))));
        p.save();

        FreeStyleBuild build = p.scheduleBuild2(0).get(BUILD_TIMEOUT, TimeUnit.SECONDS);
        assertBuildStatus(Result.FAILURE, build);
    }
}
