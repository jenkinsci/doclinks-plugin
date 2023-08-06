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

import hudson.model.AbstractBuild;

import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest;

/**
 * Action to show a link to artifact documents in side menu of projects.
 */
public class ArtifactsDocLinksProjectAction extends ArtifactsDocsLinksActionBase {
    /**
     * Returns the {@link ArtifactsDocLinksAction} in the last build that have artifact documents.
     * 
     * Resolves the owner by {@link StaplerRequest#findAncestorObject(Class)}.
     * 
     * @param req
     * @return the last {@link ArtifactsDocLinksAction}. null if no proper build exists.
     */
    public ArtifactsDocLinksAction getLastBuildAction(StaplerRequest req) {
        AbstractBuild<?,?> build = getBuild(req);
        return (build != null)?build.getAction(ArtifactsDocLinksAction.class):null;
    }
    
    /**
     * An icon file used in the side menu of a project.
     * 
     * @return an icon path. null if no artifact documents are available.
     * @see hudson.plugins.doclinks.artifacts.ArtifactsDocsLinksActionBase#getIconFileName()
     */
    @Override
    public String getIconFileName() {
        if (getBuild(Stapler.getCurrentRequest()) == null) {
            return null;
        }
        return super.getIconFileName();
    }
    
    /**
     * @return
     * @see hudson.model.Action#getDisplayName()
     */
    @Override
    public String getDisplayName() {
        return Messages.ArtifactsDocLinksProjectAction_DisplayName();
    }
    
    /**
     * Returns {@link ArtifactsDocLinksDocument} specified by the URL.
     * 
     * Delegates to the last {@link ArtifactsDocLinksAction}.
     * 
     * @param token
     * @param req
     * @return
     */
    public ArtifactsDocLinksDocument getDynamic(String token, StaplerRequest req) {
        return getLastBuildAction(req).getDynamic(token);
    }
}
