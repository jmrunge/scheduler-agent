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
import ar.com.zir.utils.LogService;
import java.io.File;
import org.apache.log4j.Level;
import org.w3c.dom.Element;

/**
 *
 * @author jmrunge
 */
public class DeleteFileTask extends JobTask {
    private String fileName;

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
    
    @Override
    public void run() throws Exception {
        File file = new File(fileName);
        if (!file.delete()) {
            file.deleteOnExit();
            LogService.getInstance().log(Level.INFO, fileName + " could not be deleted and was marked for future deletion");
        } else {
            LogService.getInstance().log(Level.INFO, fileName + " deleted succesfully");
        }
    }

    @Override
    public String getTaskType() {
        return "deleteFile";
    }

    @Override
    public JobTask getTask(Job job, Element task, boolean abortOnError) throws Exception {
        DeleteFileTask dft = new DeleteFileTask();
        dft.setAbortOnError(abortOnError);
        dft.setFileName(job.replaceVars(task.getElementsByTagName("file").item(0).getFirstChild().getNodeValue()));
        return dft;
    }
    
}
