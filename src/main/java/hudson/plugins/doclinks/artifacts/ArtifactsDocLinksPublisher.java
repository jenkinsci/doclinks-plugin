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
import java.util.Collection;
import java.util.List;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.kohsuke.stapler.DataBoundConstructor;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;

/**
 *
 */
public class ArtifactsDocLinksPublisher extends Recorder {
    private List<ArtifactsDocLinksConfig> artifactsDocLinksConfigList;
    
    /**
     * @return the artifactsDocLinksConfigList
     */
    public List<ArtifactsDocLinksConfig> getArtifactsDocLinksConfigList() {
        return artifactsDocLinksConfigList;
    }
    
    /**
     * @param artifactsDocLinksConfigList
     */
    @DataBoundConstructor
    public ArtifactsDocLinksPublisher(List<ArtifactsDocLinksConfig> artifactsDocLinksConfigList) {
        this.artifactsDocLinksConfigList = artifactsDocLinksConfigList;
    }
    
    /**
     * @param build
     * @param launcher
     * @param listener
     * @return
     * @throws InterruptedException
     * @throws IOException
     * @see hudson.tasks.BuildStepCompatibilityLayer#perform(hudson.model.AbstractBuild, hudson.Launcher, hudson.model.BuildListener)
     */
    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher,
            BuildListener listener) throws InterruptedException, IOException {
        
        listener.getLogger().println("Publishing artifacts as documents.");
        List<ArtifactsDocLinksDocument> docList
            = new ArrayList<ArtifactsDocLinksDocument>();
        if (getArtifactsDocLinksConfigList() != null) {
            for (ArtifactsDocLinksConfig config: getArtifactsDocLinksConfigList()) {
                Collection<String> files = config.scanArtifacts(build);
                
                if (files.size() <= 0) {
                    listener.getLogger().println(String.format("ERROR: No artifacts found for %s", config.getArtifactsPattern()));
                    build.setResult(Result.FAILURE);
                    return true;
                }
                
                for (String file: files) {
                    ZipFile zip = null;
                    try {
                        zip = new ZipFile(new File(build.getArtifactsDir(), file));
                    } catch(ZipException e) {
                        listener.getLogger().println(String.format("ERROR: %s seems not a zip file", file));
                        build.setResult(Result.FAILURE);
                        return true;
                    } finally {
                        if (zip != null) {
                            zip.close();
                        }
                    }
                    
                    listener.getLogger().println(file);
                    docList.add(new ArtifactsDocLinksDocument(
                            String.format("%d", docList.size() + 1),
                            file, 
                            (files.size() <= 1)
                                ?config.getTitle()
                                :String.format("%s(%s)", config.getTitle(), file),
                            config.getInitialPath(),
                            config.getIndexFile()
                    ));
                }
            }
        }
        
        if (docList.isEmpty()) {
            listener.getLogger().println("ERROR: No artifacts to publish as documents");
            build.setResult(Result.FAILURE);
            return true;
        }
        
        // For the case launched multiple times
        // (it can be a case with Flexible Publish plugin).
        ArtifactsDocLinksAction action = build.getAction(ArtifactsDocLinksAction.class);
        if (action == null) {
            action = new ArtifactsDocLinksAction();
            build.addAction(action);
        }
        action.addAll(docList);
        
        return true;
    }
    
    /**
     * @param project
     * @return
     */
    @Override
    public Action getProjectAction(AbstractProject<?, ?> project) {
        return new ArtifactsDocLinksProjectAction();
    }
    
    /**
     * @return
     * @see hudson.tasks.BuildStep#getRequiredMonitorService()
     */
    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }
    
    /**
     * @return
     */
    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl)super.getDescriptor();
    }
    
    /**
     *
     */
    @Extension(ordinal=-1) // This would be better to be displayed later than hudson.tasks.ArtifactArchiver
    public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {
        /**
         * @param jobType
         * @return
         * @see hudson.tasks.BuildStepDescriptor#isApplicable(java.lang.Class)
         */
        @Override
        public boolean isApplicable(@SuppressWarnings("rawtypes") Class<? extends AbstractProject> jobType) {
            return true;
        }
        
        /**
         * @return
         * @see hudson.model.Descriptor#getDisplayName()
         */
        @Override
        public String getDisplayName() {
            return Messages.ArtifactsDocLinksPublisher_DisplayName();
        }
    }
}
