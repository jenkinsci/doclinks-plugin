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

package hudson.plugins.doclinks.artifacts.testtools;

import org.jvnet.hudson.test.HudsonTestCase;

import hudson.model.AbstractProject;

/**
 *
 */
public abstract class ArtifactDocLinksHudsonTestCase extends HudsonTestCase {
    private WebClient aWebClient = null;
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        aWebClient = null;
    }
    
    /**
     * Singleton for {@link WebClient}.
     * 
     * There seems no way to have {@link WebClient} close the connection.
     * This causes Jenkins exhaust its connection pools
     * when creating {@link WebClient} for each assertion step.
     * 
     * @return
     */
    protected synchronized WebClient getWebClient() {
        if (aWebClient == null) {
            aWebClient = createWebClient();
        }
        return aWebClient;
    }
    
    public WebClient createWebClient() {
        WebClient wc = new WebClient();
        return wc;
    }
    
    protected void updateTransientActions(AbstractProject<?,?> p) throws Exception {
        WebClient wc = getWebClient();
        submit(wc.getPage(p, "configure").getFormByName("config"));
    }
}
