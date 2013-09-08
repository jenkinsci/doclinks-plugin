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

import java.net.URL;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.apache.commons.httpclient.util.DateUtil;

import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.plugins.doclinks.artifacts.testtools.ArtifactDocLinksHudsonTestCase;
import hudson.plugins.doclinks.artifacts.testtools.CleanupBuilder;
import hudson.plugins.doclinks.artifacts.testtools.TestZipBuilder;
import hudson.tasks.ArtifactArchiver;

/**
 *
 */
public class ArtifactsDocLinksDocumentHudsonTest extends ArtifactDocLinksHudsonTestCase {
    private static final int BUILD_TIMEOUT = 10;
    
    public void testInitialPath() throws Exception {
        WebClient wc = getWebClient();
        
        FreeStyleProject p = createFreeStyleProject();
        {
            p.getBuildersList().clear();
            p.getBuildersList().add(new TestZipBuilder("artifact1.zip"));
            p.getPublishersList().clear();
            p.getPublishersList().add(new ArtifactArchiver("artifact1.zip", "", false));
            p.getPublishersList().add(new ArtifactsDocLinksPublisher(Arrays.asList(
                    new ArtifactsDocLinksConfig("Test", "artifact1.zip", null, null)
            )));
            p.save();
            
            FreeStyleBuild build = p.scheduleBuild2(0).get(BUILD_TIMEOUT, TimeUnit.SECONDS);
            ArtifactsDocLinksAction action = build.getAction(ArtifactsDocLinksAction.class);
            ArtifactsDocLinksDocument doc = action.getArtifactsDocLinksDocumentList().get(0);
            
            HtmlPage page = wc.getPage(build, String.format("%s/%s", action.getUrlName(), doc.getUrl()));
            assertTrue(page.asText(), page.asText().contains("Default top page."));
        }
        
        {
            p.getBuildersList().clear();
            p.getBuildersList().add(new TestZipBuilder("artifact1.zip"));
            p.getPublishersList().clear();
            p.getPublishersList().add(new ArtifactArchiver("artifact1.zip", "", false));
            p.getPublishersList().add(new ArtifactsDocLinksPublisher(Arrays.asList(
                    new ArtifactsDocLinksConfig("Test", "artifact1.zip", "subdir", null)
            )));
            p.save();
            
            FreeStyleBuild build = p.scheduleBuild2(0).get(BUILD_TIMEOUT, TimeUnit.SECONDS);
            ArtifactsDocLinksAction action = build.getAction(ArtifactsDocLinksAction.class);
            ArtifactsDocLinksDocument doc = action.getArtifactsDocLinksDocumentList().get(0);
            
            HtmlPage page = wc.getPage(build, String.format("%s/%s", action.getUrlName(), doc.getUrl()));
            assertTrue(page.asText(), page.asText().contains("Page in a sub directory."));
        }
    }
    
    public void testIndexFile() throws Exception {
        WebClient wc = getWebClient();
        
        FreeStyleProject p = createFreeStyleProject();
        {
            p.getBuildersList().clear();
            p.getBuildersList().add(new TestZipBuilder("artifact1.zip"));
            p.getPublishersList().clear();
            p.getPublishersList().add(new ArtifactArchiver("artifact1.zip", "", false));
            p.getPublishersList().add(new ArtifactsDocLinksPublisher(Arrays.asList(
                    new ArtifactsDocLinksConfig("Test", "artifact1.zip", null, null)
            )));
            p.save();
            
            FreeStyleBuild build = p.scheduleBuild2(0).get(BUILD_TIMEOUT, TimeUnit.SECONDS);
            ArtifactsDocLinksAction action = build.getAction(ArtifactsDocLinksAction.class);
            ArtifactsDocLinksDocument doc = action.getArtifactsDocLinksDocumentList().get(0);
            
            {
                // index.html
                HtmlPage page = wc.getPage(build, String.format("%s/%s/subdir", action.getUrlName(), doc.getUrl()));
                assertTrue(page.asText(), page.asText().contains("Page in a sub directory."));
            }
            
            {
                // index.htm
                HtmlPage page = wc.getPage(build, String.format("%s/%s/subdir2/", action.getUrlName(), doc.getUrl()));
                assertTrue(page.asText(), page.asText().contains("Page in a sub directory 2."));
            }
        }
        
        {
            p.getBuildersList().clear();
            p.getBuildersList().add(new TestZipBuilder("artifact1.zip"));
            p.getPublishersList().clear();
            p.getPublishersList().add(new ArtifactArchiver("artifact1.zip", "", false));
            p.getPublishersList().add(new ArtifactsDocLinksPublisher(Arrays.asList(
                    new ArtifactsDocLinksConfig("Test", "artifact1.zip", null, "   ")
            )));
            p.save();
            
            FreeStyleBuild build = p.scheduleBuild2(0).get(BUILD_TIMEOUT, TimeUnit.SECONDS);
            ArtifactsDocLinksAction action = build.getAction(ArtifactsDocLinksAction.class);
            ArtifactsDocLinksDocument doc = action.getArtifactsDocLinksDocumentList().get(0);
            
            {
                // index.html
                HtmlPage page = wc.getPage(build, String.format("%s/%s/subdir", action.getUrlName(), doc.getUrl()));
                assertTrue(page.asText(), page.asText().contains("Page in a sub directory."));
            }
            
            {
                // index.htm
                HtmlPage page = wc.getPage(build, String.format("%s/%s/subdir2/", action.getUrlName(), doc.getUrl()));
                assertTrue(page.asText(), page.asText().contains("Page in a sub directory 2."));
            }
        }
        
        
        {
            p.getBuildersList().clear();
            p.getBuildersList().add(new TestZipBuilder("artifact1.zip"));
            p.getPublishersList().clear();
            p.getPublishersList().add(new ArtifactArchiver("artifact1.zip", "", false));
            p.getPublishersList().add(new ArtifactsDocLinksPublisher(Arrays.asList(
                    new ArtifactsDocLinksConfig("Test", "artifact1.zip", null, "   default.html   ,   default.htm")
            )));
            p.save();
            
            FreeStyleBuild build = p.scheduleBuild2(0).get(BUILD_TIMEOUT, TimeUnit.SECONDS);
            ArtifactsDocLinksAction action = build.getAction(ArtifactsDocLinksAction.class);
            ArtifactsDocLinksDocument doc = action.getArtifactsDocLinksDocumentList().get(0);
            
            {
                // default.html
                HtmlPage page = wc.getPage(build, String.format("%s/%s/subdir", action.getUrlName(), doc.getUrl()));
                assertTrue(page.asText(), page.asText().contains("Alternate page in a sub directory."));
            }
            
            {
                // default.htm
                HtmlPage page = wc.getPage(build, String.format("%s/%s/subdir2/", action.getUrlName(), doc.getUrl()));
                assertTrue(page.asText(), page.asText().contains("Alternate page in a sub directory 2."));
            }
        }
    }
    
    public void testLinksInDocuments() throws Exception {
        WebClient wc = getWebClient();
        
        FreeStyleProject p = createFreeStyleProject();
        p.getBuildersList().clear();
        p.getBuildersList().add(new TestZipBuilder("artifact1.zip"));
        p.getPublishersList().clear();
        p.getPublishersList().add(new ArtifactArchiver("artifact1.zip", null, false));
        p.getPublishersList().add(new ArtifactsDocLinksPublisher(Arrays.asList(
                new ArtifactsDocLinksConfig("Test", "artifact1.zip", null, null)
        )));
        p.save();
        
        FreeStyleBuild build = p.scheduleBuild2(0).get(BUILD_TIMEOUT, TimeUnit.SECONDS);
        ArtifactsDocLinksAction action = build.getAction(ArtifactsDocLinksAction.class);
        ArtifactsDocLinksDocument doc = action.getArtifactsDocLinksDocumentList().get(0);
        
        HtmlPage page = wc.getPage(build, String.format("%s/%s/contents", action.getUrlName(), doc.getUrl()));
        assertTrue(page.asText(), page.asText().contains("This is a updated text."));
    }
    
    public void testLastModifiedSince() throws Exception {
        WebClient wc = getWebClient();
        
        FreeStyleProject p = createFreeStyleProject();
        p.getBuildersList().clear();
        p.getPublishersList().clear();
        p.getPublishersList().add(new ArtifactArchiver("artifact1.zip", null, false));
        p.getPublishersList().add(new ArtifactsDocLinksPublisher(Arrays.asList(
                new ArtifactsDocLinksConfig("Test", "artifact1.zip", null, null)
        )));
        p.save();
        updateTransientActions(p);
        // Opening configure with TestZipBuilder causes 500.
        p.getBuildersList().add(new CleanupBuilder());
        p.getBuildersList().add(new TestZipBuilder("artifact1.zip"));
        
        ArtifactsDocLinksProjectAction projectAction = p.getAction(ArtifactsDocLinksProjectAction.class);
        
        FreeStyleBuild build = p.scheduleBuild2(0).get(BUILD_TIMEOUT, TimeUnit.SECONDS);
        ArtifactsDocLinksAction action = build.getAction(ArtifactsDocLinksAction.class);
        ArtifactsDocLinksDocument doc = action.getArtifactsDocLinksDocumentList().get(0);
        
        Page page = wc.getPage(new URL(wc.getContextPath() + p.getUrl() + String.format("%s/%s", projectAction.getUrlName(), doc.getUrl())));
        assertEquals(200, page.getWebResponse().getStatusCode());
        Date lastModified = DateUtil.parseDate(page.getWebResponse().getResponseHeaderValue("Last-Modified"));
        assertNotNull(lastModified);
        
        wc.addRequestHeader("If-Modified-Since", DateUtil.formatDate(lastModified));
        page = wc.getPage(new URL(wc.getContextPath() + p.getUrl() + String.format("%s/%s", projectAction.getUrlName(), doc.getUrl())));
        assertEquals(304, page.getWebResponse().getStatusCode());
        
        page = wc.getPage(new URL(wc.getContextPath() + p.getUrl() + String.format("%s/%s", projectAction.getUrlName(), doc.getUrl())));
        assertEquals(304, page.getWebResponse().getStatusCode());
        
        Thread.sleep(1000);
        
        build = p.scheduleBuild2(0).get(BUILD_TIMEOUT, TimeUnit.SECONDS);
        action = build.getAction(ArtifactsDocLinksAction.class);
        doc = action.getArtifactsDocLinksDocumentList().get(0);
        
        page = wc.getPage(new URL(wc.getContextPath() + p.getUrl() + String.format("%s/%s", projectAction.getUrlName(), doc.getUrl())));
        assertEquals(200, page.getWebResponse().getStatusCode());
    }
}
