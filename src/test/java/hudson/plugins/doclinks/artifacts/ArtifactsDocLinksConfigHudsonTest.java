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

import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.plugins.doclinks.artifacts.ArtifactsDocLinksConfig.DescriptorImpl;
import hudson.plugins.doclinks.artifacts.testtools.ArtifactDocLinksHudsonTestCase;
import hudson.plugins.doclinks.artifacts.testtools.CleanupBuilder;
import hudson.plugins.doclinks.artifacts.testtools.TestFileBuilder;
import hudson.plugins.doclinks.artifacts.testtools.TestZipBuilder;
import hudson.tasks.ArtifactArchiver;
import hudson.util.FormValidation;
import org.jvnet.hudson.test.UnstableBuilder;

/**
 *
 */
public class ArtifactsDocLinksConfigHudsonTest extends ArtifactDocLinksHudsonTestCase {
    private DescriptorImpl getDescriptor() {
        return (DescriptorImpl) hudson.getDescriptor(ArtifactsDocLinksConfig.class);
    }

    public void testDescriptor_doCheckArtifactsPattern() throws Exception {
        DescriptorImpl descriptor = getDescriptor();
        FreeStyleProject p = createFreeStyleProject();

        // no builds.
        assertEquals(FormValidation.Kind.ERROR, descriptor.doCheckArtifactsPattern(null, p).kind);
        assertEquals(FormValidation.Kind.ERROR, descriptor.doCheckArtifactsPattern("", p).kind);
        assertEquals(FormValidation.Kind.ERROR, descriptor.doCheckArtifactsPattern("  ", p).kind);
        assertEquals(FormValidation.Kind.OK, descriptor.doCheckArtifactsPattern("*.zip", p).kind);

        // build with no artifact.
        assertBuildStatusSuccess(p.scheduleBuild2(0).get());
        assertEquals(FormValidation.Kind.WARNING, descriptor.doCheckArtifactsPattern("*.zip", p).kind);

        // build with an artifact.
        p.getBuildersList().clear();
        p.getBuildersList().add(new CleanupBuilder());
        p.getBuildersList().add(new TestZipBuilder("artifact1.zip"));
        p.getPublishersList().clear();
        p.getPublishersList().add(new ArtifactArchiver("artifact1.zip", "", false));
        assertBuildStatusSuccess(p.scheduleBuild2(0).get());
        assertEquals(FormValidation.Kind.OK, descriptor.doCheckArtifactsPattern("*.zip", p).kind);
        assertEquals(FormValidation.Kind.OK, descriptor.doCheckArtifactsPattern("    *.zip    ", p).kind);
        assertEquals(
                FormValidation.Kind.WARNING, descriptor.doCheckArtifactsPattern("artifact1.zip,artifact2.zip", p).kind);

        // build with artifacts.
        p.getBuildersList().clear();
        p.getBuildersList().add(new CleanupBuilder());
        p.getBuildersList().add(new TestZipBuilder("path/to/artifact1.zip"));
        p.getBuildersList().add(new TestZipBuilder("path/to/artifact2.zip"));
        p.getPublishersList().clear();
        p.getPublishersList().add(new ArtifactArchiver("**/*.zip", "", false));
        assertBuildStatusSuccess(p.scheduleBuild2(0).get());
        assertEquals(FormValidation.Kind.OK, descriptor.doCheckArtifactsPattern("**/*.zip", p).kind);
        assertEquals(
                FormValidation.Kind.OK,
                descriptor.doCheckArtifactsPattern("**/artifact1.zip,**/artifact2.zip", p).kind);

        // build without artifacts, but not succeeded.
        p.getBuildersList().clear();
        p.getBuildersList().add(new CleanupBuilder());
        p.getBuildersList().add(new UnstableBuilder());
        p.getPublishersList().clear();
        assertBuildStatus(Result.UNSTABLE, p.scheduleBuild2(0).get());
        assertEquals(
                FormValidation.Kind.OK,
                descriptor.doCheckArtifactsPattern("**/artifact1.zip,**/artifact2.zip", p).kind);

        // build not succeeded, but with artifacts.
        p.getBuildersList().clear();
        p.getBuildersList().add(new CleanupBuilder());
        p.getBuildersList().add(new TestZipBuilder("path/to/artifact3.zip"));
        p.getBuildersList().add(new UnstableBuilder());
        p.getPublishersList().clear();
        p.getPublishersList().add(new ArtifactArchiver("**/*.zip", "", false));
        assertBuildStatus(Result.UNSTABLE, p.scheduleBuild2(0).get());
        assertEquals(
                FormValidation.Kind.OK,
                descriptor.doCheckArtifactsPattern("**/artifact1.zip,**/artifact2.zip,**/artifact3.zip", p).kind);

        // build with artifacts again.
        p.getBuildersList().clear();
        p.getBuildersList().add(new CleanupBuilder());
        p.getBuildersList().add(new TestZipBuilder("path/to/artifact4.zip"));
        p.getPublishersList().clear();
        p.getPublishersList().add(new ArtifactArchiver("**/*.zip", "", false));
        assertBuildStatusSuccess(p.scheduleBuild2(0).get());
        assertEquals(FormValidation.Kind.OK, descriptor.doCheckArtifactsPattern("**/artifact4.zip", p).kind);
        assertEquals(
                FormValidation.Kind.WARNING,
                descriptor.doCheckArtifactsPattern("**/artifact3.zip,**/artifact4.zip", p).kind);

        // not a zip file.
        p.getBuildersList().clear();
        p.getBuildersList().add(new CleanupBuilder());
        p.getBuildersList().add(new TestFileBuilder("artifact1.zip", "Not a zip file."));
        p.getPublishersList().clear();
        p.getPublishersList().add(new ArtifactArchiver("*.zip", "", false));
        assertBuildStatusSuccess(p.scheduleBuild2(0).get());
        assertEquals(FormValidation.Kind.WARNING, descriptor.doCheckArtifactsPattern("artifact1.zip", p).kind);
    }

    public void testDescriptor_doCheckInitialPath() throws Exception {
        DescriptorImpl descriptor = getDescriptor();
        FreeStyleProject p = createFreeStyleProject();

        // no builds.
        assertEquals(FormValidation.Kind.OK, descriptor.doCheckInitialPath("**/*.zip", null, p).kind);
        assertEquals(FormValidation.Kind.OK, descriptor.doCheckInitialPath("**/*.zip", "", p).kind);
        assertEquals(FormValidation.Kind.OK, descriptor.doCheckInitialPath("**/*.zip", "  ", p).kind);
        assertEquals(FormValidation.Kind.OK, descriptor.doCheckInitialPath("**/*.zip", "path/to/anywhere", p).kind);

        // build with no artifact.
        assertBuildStatusSuccess(p.scheduleBuild2(0).get());
        assertEquals(FormValidation.Kind.OK, descriptor.doCheckInitialPath("**/*.zip", "path/to/anywhere", p).kind);

        // build with an artifact.
        p.getBuildersList().clear();
        p.getBuildersList().add(new CleanupBuilder());
        p.getBuildersList().add(new TestZipBuilder("artifact1.zip"));
        p.getPublishersList().clear();
        p.getPublishersList().add(new ArtifactArchiver("artifact1.zip", "", false));
        assertBuildStatusSuccess(p.scheduleBuild2(0).get());
        assertEquals(FormValidation.Kind.OK, descriptor.doCheckInitialPath("**/*.zip", "subdir", p).kind);
        assertEquals(FormValidation.Kind.OK, descriptor.doCheckInitialPath("**/*.zip", "  subdir  ", p).kind);
        assertEquals(FormValidation.Kind.OK, descriptor.doCheckInitialPath("**/*.zip", "subdir/default.html", p).kind);
        assertEquals(FormValidation.Kind.WARNING, descriptor.doCheckInitialPath("**/*.zip", "nosuchdir", p).kind);
        assertEquals(
                FormValidation.Kind.WARNING,
                descriptor.doCheckInitialPath("**/*.zip", "subdir/nosuchfile.html", p).kind);

        // build with artifacts.
        p.getBuildersList().clear();
        p.getBuildersList().add(new CleanupBuilder());
        p.getBuildersList().add(new TestZipBuilder("path/to/artifact1.zip"));
        p.getBuildersList().add(new TestZipBuilder("path/to/artifact2.zip"));
        p.getPublishersList().clear();
        p.getPublishersList().add(new ArtifactArchiver("**/*.zip", "", false));
        assertBuildStatusSuccess(p.scheduleBuild2(0).get());
        assertEquals(FormValidation.Kind.OK, descriptor.doCheckInitialPath("**/*.zip", "subdir", p).kind);

        // build without artifacts, but not succeeded.
        p.getBuildersList().clear();
        p.getBuildersList().add(new CleanupBuilder());
        p.getBuildersList().add(new UnstableBuilder());
        p.getPublishersList().clear();
        assertBuildStatus(Result.UNSTABLE, p.scheduleBuild2(0).get());
        assertEquals(FormValidation.Kind.OK, descriptor.doCheckInitialPath("**/*.zip", "subdir", p).kind);
        assertEquals(FormValidation.Kind.WARNING, descriptor.doCheckInitialPath("**/*.zip", "nosuchdir", p).kind);

        // not a zip file.
        p.getBuildersList().clear();
        p.getBuildersList().add(new CleanupBuilder());
        p.getBuildersList().add(new TestFileBuilder("artifact1.zip", "Not a zip file."));
        p.getPublishersList().clear();
        p.getPublishersList().add(new ArtifactArchiver("*.zip", "", false));
        assertBuildStatusSuccess(p.scheduleBuild2(0).get());
        assertEquals(FormValidation.Kind.OK, descriptor.doCheckInitialPath("**/*.zip", "subdir", p).kind);
        assertEquals(FormValidation.Kind.OK, descriptor.doCheckInitialPath("**/*.zip", "nosuchdir", p).kind);
    }

    public void testDescriptor_doCheckInitialPathWithoutDirectoryEntry() throws Exception {
        DescriptorImpl descriptor = getDescriptor();
        FreeStyleProject p = createFreeStyleProject();

        // build with an artifact.
        p.getBuildersList().clear();
        p.getBuildersList().add(new CleanupBuilder());
        p.getBuildersList().add(new TestZipBuilder("artifact1.zip", true));
        p.getPublishersList().clear();
        p.getPublishersList().add(new ArtifactArchiver("artifact1.zip", "", false));
        assertBuildStatusSuccess(p.scheduleBuild2(0).get());
        assertEquals(FormValidation.Kind.OK, descriptor.doCheckInitialPath("**/*.zip", "subdir", p).kind);
    }
}
