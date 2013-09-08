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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


/**
 * An action to hold the link to artifact documents.
 */
public class ArtifactsDocLinksAction extends ArtifactsDocsLinksActionBase {
    private List<ArtifactsDocLinksDocument> artifactsDocLinksDocumentList
        = new ArrayList<ArtifactsDocLinksDocument>();
    /**
     * @return the list of artifact documents.
     */
    public List<ArtifactsDocLinksDocument> getArtifactsDocLinksDocumentList() {
        return artifactsDocLinksDocumentList;
    }
    
    /**
     * @param doc
     * @return
     */
    public boolean add(ArtifactsDocLinksDocument doc) {
        return getArtifactsDocLinksDocumentList().add(doc);
    }
    
    /**
     * @param docs
     * @return
     */
    public boolean addAll(Collection<ArtifactsDocLinksDocument> docs) {
        return getArtifactsDocLinksDocumentList().addAll(docs);
    }
    
    /**
     * @return
     * @see hudson.model.Action#getDisplayName()
     */
    @Override
    public String getDisplayName() {
        return Messages.ArtifactsDocLinksAction_DisplayName();
    }
    
    /**
     * @param token
     * @return
     */
    public ArtifactsDocLinksDocument getDynamic(String token) {
        for (ArtifactsDocLinksDocument doc: getArtifactsDocLinksDocumentList()) {
            if (token.equals(doc.getId())) {
                return doc;
            }
        }
        
        return null;
    }
}
