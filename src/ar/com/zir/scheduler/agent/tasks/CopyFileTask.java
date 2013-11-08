/*
    Copyright 2012 Juan Martín Runge
    
    jmrunge@gmail.com
    http://www.zirsi.com.ar
    
    This file is part of SchedulerAgent.

    SchedulerAgent is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    SchedulerAgent is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with SchedulerAgent.  If not, see <http://www.gnu.org/licenses/>.
*/
package ar.com.zir.scheduler.agent.tasks;

import ar.com.zir.scheduler.agent.app.Job;
import ar.com.zir.utils.FileUtils;
import ar.com.zir.utils.LogService;
import java.io.File;
import org.apache.log4j.Level;
import org.w3c.dom.Element;

/**
 *
 * @author jmrunge
 */
public class CopyFileTask extends JobTask {
    private String origFile;
    private String destFile;
    private boolean createDir;
    private boolean overwriteFile;

    public boolean isCreateDir() {
        return createDir;
    }

    public void setCreateDir(boolean createDir) {
        this.createDir = createDir;
    }

    public String getDestFile() {
        return destFile;
    }

    public void setDestFile(String destFile) {
        this.destFile = destFile;
    }

    public String getOrigFile() {
        return origFile;
    }

    public void setOrigFile(String origFile) {
        this.origFile = origFile;
    }

    public boolean isOverwriteFile() {
        return overwriteFile;
    }

    public void setOverwriteFile(boolean overwriteFile) {
        this.overwriteFile = overwriteFile;
    }
    
    @Override
    public void run() throws Exception {
        File orig = new File(origFile);
        File dest = new File(destFile);
        
        if (!orig.exists())
            throw new Exception(origFile + " does not exists!");
        if (dest.exists() && !overwriteFile)
            throw new Exception(destFile + " already exists!");
        if (!dest.getParentFile().exists()) {
            if (createDir)
                dest.getParentFile().mkdirs();
            else
                throw new Exception(dest.getParent() + " does not exists!");
        }
        FileUtils.copyFile(orig, dest);
        LogService.getInstance().log(Level.INFO, origFile + " succesfully copied to " + destFile);
    }

    @Override
    public String getTaskType() {
        return "copyFile";
    }

    @Override
    public JobTask getTask(Job job, Element task, boolean abortOnError) throws Exception {
        CopyFileTask cft = new CopyFileTask();
        cft.setAbortOnError(abortOnError);
        cft.setCreateDir(task.getAttribute("createDirIfNotExists").trim().toLowerCase().equals("true"));
        cft.setOverwriteFile(task.getAttribute("overwriteFileIfExists").trim().toLowerCase().equals("true"));
        cft.setOrigFile(job.replaceVars(task.getElementsByTagName("from").item(0).getFirstChild().getNodeValue()));
        cft.setDestFile(job.replaceVars(task.getElementsByTagName("to").item(0).getFirstChild().getNodeValue()));
        return cft;
    }
    
}
