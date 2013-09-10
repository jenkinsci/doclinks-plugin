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

import hudson.plugins.doclinks.artifacts.ArtifactsDocLinksConfig.DescriptorImpl;
import hudson.util.FormValidation;
import junit.framework.TestCase;

/**
 *
 */
public class ArtifactsDocLinksConfigNoHudsonTest extends TestCase {
    private DescriptorImpl getDescriptor() {
        return new DescriptorImpl();
    }
    
    public void testArtifactsDocLinksConfig() throws Exception {
        {
            ArtifactsDocLinksConfig target = new ArtifactsDocLinksConfig(
                    "Some Document Title",
                    "**/*.zip",
                    "path/to/initial/",
                    "defaults.html,defaults.shtml"
            );
            assertEquals("Some Document Title", target.getTitle());
            assertEquals("**/*.zip", target.getArtifactsPattern());
            assertEquals("path/to/initial/", target.getInitialPath());
            assertEquals("defaults.html,defaults.shtml", target.getIndexFile());
        }
        {
            ArtifactsDocLinksConfig target = new ArtifactsDocLinksConfig(
                    "    Some Document Title     ",
                    "  **/*.zip  ,  **/*.jar  ",
                    "   path/to/initial/   ",
                    " defaults.html , defaults.shtml "
            );
            assertEquals("Some Document Title", target.getTitle());
            assertEquals("**/*.zip  ,  **/*.jar", target.getArtifactsPattern());
            assertEquals("path/to/initial/", target.getInitialPath());
            assertEquals("defaults.html , defaults.shtml", target.getIndexFile());
        }
        {
            ArtifactsDocLinksConfig target = new ArtifactsDocLinksConfig(
                    null,
                    null,
                    null,
                    null
            );
            assertNull(target.getTitle());
            assertNull(target.getArtifactsPattern());
            assertNull(target.getInitialPath());
            assertNull(target.getIndexFile());
        }
        {
            ArtifactsDocLinksConfig target = new ArtifactsDocLinksConfig(
                    "",
                    "",
                    "",
                    ""
            );
            assertEquals("", target.getTitle());
            assertEquals("", target.getArtifactsPattern());
            assertEquals("", target.getInitialPath());
            assertEquals("", target.getIndexFile());
        }
        {
            ArtifactsDocLinksConfig target = new ArtifactsDocLinksConfig(
                    "    ",
                    "  ",
                    "   ",
                    " "
            );
            assertEquals("", target.getTitle());
            assertEquals("", target.getArtifactsPattern());
            assertEquals("", target.getInitialPath());
            assertEquals("", target.getIndexFile());
        }
    }
    
    public void testDescriptor_doCheckTitleOk() {
        DescriptorImpl descriptor = getDescriptor();
        {
            assertEquals(FormValidation.Kind.OK, descriptor.doCheckTitle("Some Title").kind);
            assertEquals(FormValidation.Kind.OK, descriptor.doCheckTitle("  Some Title  ").kind);
        }
    }
    
    public void testDescriptor_doCheckTitleError() {
        DescriptorImpl descriptor = getDescriptor();
        {
            assertEquals(FormValidation.Kind.ERROR, descriptor.doCheckTitle(null).kind);
            assertEquals(FormValidation.Kind.ERROR, descriptor.doCheckTitle("").kind);
            assertEquals(FormValidation.Kind.ERROR, descriptor.doCheckTitle("  ").kind);
        }
    }
}
