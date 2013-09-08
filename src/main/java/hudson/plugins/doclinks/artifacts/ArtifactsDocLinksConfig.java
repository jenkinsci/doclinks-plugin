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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipFile;

import org.apache.tools.ant.DirectoryScanner;
import org.codehaus.plexus.util.StringUtils;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import hudson.Extension;
import hudson.matrix.MatrixProject;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Hudson;
import hudson.model.Result;
import hudson.util.FormValidation;

/**
 * An entry for artifact documents configured by a user.
 */
public class ArtifactsDocLinksConfig implements Describable<ArtifactsDocLinksConfig> {
    private String title;
    /**
     * @return the title
     */
    public String getTitle() {
        return title;
    }
    
    private String artifactsPattern;
    /**
     * Returns ant file name pattern to specify artifacts.
     * 
     * @return the artifactsPattern
     */
    public String getArtifactsPattern() {
        return artifactsPattern;
    }
    
    private String initialPath;
    /**
     * The initial path when accessed to this artifact.
     * 
     * @return the initialPath
     */
    public String getInitialPath() {
        return initialPath;
    }
    
    private String indexFile;
    /**
     * The file used for access to a directory.
     * 
     * @return the indexFile
     */
    public String getIndexFile() {
        return indexFile;
    }
    
    /**
     * @param title
     * @param artifactsPattern
     * @param initialPath
     * @param indexFile
     */
    @DataBoundConstructor
    public ArtifactsDocLinksConfig(String title, String artifactsPattern, String initialPath, String indexFile) {
        this.title = StringUtils.trim(title);
        this.artifactsPattern = StringUtils.trim(artifactsPattern);
        this.initialPath = StringUtils.trim(initialPath);
        this.indexFile = StringUtils.trim(indexFile);
    }
    
    /**
     * @return
     * @see hudson.model.Describable#getDescriptor()
     */
    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl)Hudson.getInstance().getDescriptor(getClass());
    }
    
    public Collection<String> scanArtifacts(AbstractBuild<?,?> build) {
        return getDescriptor().scanArtifacts(build, getArtifactsPattern());
    }
    
    /**
     *
     */
    @Extension
    public static class DescriptorImpl extends Descriptor<ArtifactsDocLinksConfig> {
        /**
         * @return
         * @see hudson.model.Descriptor#getDisplayName()
         */
        @Override
        public String getDisplayName() {
            return "Configuration Entry for ArtifactsDocLinksPublisher";
        }
        
        public Collection<String> scanArtifacts(AbstractBuild<?,?> build, String artifactsPattern) {
            File dir = build.getArtifactsDir();
            if (!dir.exists() || !dir.isDirectory()) {
                return Collections.emptyList();
            }
            DirectoryScanner ds = new DirectoryScanner();
            ds.setBasedir(dir);
            ds.setIncludes(artifactsPattern.split("\\s*,\\s*"));
            ds.scan();
            
            return Arrays.asList(ds.getIncludedFiles());
        }
        
        public FormValidation doCheckTitle(@QueryParameter String value) {
            value = StringUtils.trim(value);
            if (StringUtils.isEmpty(value)) {
                return FormValidation.error(Messages.ArtifactsDocLinksConfig_title_required());
            }
            
            return FormValidation.ok();
        }
        
        private List<AbstractBuild<?,?>> getBuildsToCheckArtifacts(AbstractProject<?,?> project) {
            List<AbstractProject<?,?>> projectList = new ArrayList<AbstractProject<?,?>>();
            if (project instanceof MatrixProject) {
                projectList.addAll(((MatrixProject)project).getActiveConfigurations());
            } else {
                projectList.add(project);
            }
            
            // Retrieve builds containing artifacts
            List<AbstractBuild<?,?>> buildList = new ArrayList<AbstractBuild<?,?>>();
            for (AbstractProject<?,?> p: projectList) {
                for (
                        AbstractBuild<?,?> b = p.getLastBuild();
                        b != null;
                        b = b.getPreviousBuild()
                ) {
                    if (b.getResult().isBetterOrEqualTo(Result.SUCCESS)) {
                        buildList.add(b);
                        break;
                    }
                    File dir = b.getArtifactsDir();
                    if (!dir.exists() || !dir.isDirectory() || dir.listFiles().length <= 0) {
                        continue;
                    }
                    buildList.add(b);
                }
            }
            
            return buildList;
        }
        
        public FormValidation doCheckArtifactsPattern(@QueryParameter String value, @AncestorInPath AbstractProject<?,?> project) {
            value = StringUtils.trim(value);
            if (StringUtils.isEmpty(value)) {
                return FormValidation.error(Messages.ArtifactsDocLinksConfig_artifactsPattern_required());
            }
            
            List<AbstractBuild<?,?>> buildList = getBuildsToCheckArtifacts(project);
            if (buildList.isEmpty()) {
                // There is no available builds.
                return FormValidation.ok();
            }
            
            OUTER:
            for (String pattern: value.split("\\s*,\\s*")) {
                for (AbstractBuild<?,?> build: buildList) {
                    Collection<String> artifacts = scanArtifacts(build, pattern);
                    if (artifacts.size() > 0) {
                        for (String artifact: artifacts) {
                            ZipFile file = null;
                            try {
                                file = new ZipFile(new File(build.getArtifactsDir(), artifact));
                            } catch(IOException e) {
                                return FormValidation.warning(
                                        Messages.ArtifactsDocLinksConfig_artifactsPattern_invalid(artifact, build.getFullDisplayName())
                                );
                            } finally {
                                if (file != null) {
                                    try {
                                        file.close();
                                    } catch(IOException e) {
                                        // ignore
                                    }
                                }
                            }
                        }
                        continue OUTER;
                    }
                }
                return FormValidation.warning(Messages.ArtifactsDocLinksConfig_artifactsPattern_notfound(pattern));
            }
            
            return FormValidation.ok();
        }
        
        public FormValidation doCheckInitialPath(@QueryParameter String artifactsPattern, @QueryParameter String value, @AncestorInPath AbstractProject<?,?> project) {
            artifactsPattern = StringUtils.trim(artifactsPattern);
            value = StringUtils.trim(value);
            if (StringUtils.isEmpty(artifactsPattern) || StringUtils.isEmpty(value)) {
                return FormValidation.ok();
            }
            
            List<AbstractBuild<?,?>> buildList = getBuildsToCheckArtifacts(project);
            if (buildList.isEmpty()) {
                // There is no available builds.
                return FormValidation.ok();
            }
            
            for (AbstractBuild<?,?> build: buildList) {
                Collection<String> artifactNames = scanArtifacts(build, artifactsPattern);
                for (String artifactName: artifactNames) {
                    File artifact = new File(build.getArtifactsDir(), artifactName);
                    ZipFile file = null;
                    try {
                        file = new ZipFile(artifact);
                        if (file.getEntry(value) == null) {
                            return FormValidation.warning(
                                    Messages.ArtifactsDocLinksConfig_initialPath_notfound(artifactName, build.getFullDisplayName())
                            );
                        }
                    } catch(IOException e) {
                        // ignore if file is not zip.
                    } finally {
                        if (file != null) {
                            try {
                                file.close();
                            } catch(IOException e) {
                                // ignore
                            }
                        }
                    }
                }
                if (artifactNames.size() > 0) {
                    break;
                }
            }
            
            return FormValidation.ok();
        }
        
        @Override
        public String getCheckUrl(String fieldName)
        {
            if ("initialPath".equals(fieldName)) {
                String url = super.getCheckUrl(fieldName);
                if (!url.contains("artifactsPattern")) {
                    url
                        +="+ '&artifactsPattern='"
                        + "+ toValue(findPreviousFormItem(this, 'artifactsPattern'))";
                }
                return url;
            }
            return super.getCheckUrl(fieldName);
        }
    }
}
