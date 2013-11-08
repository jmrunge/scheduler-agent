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
import java.util.Calendar;
import org.apache.log4j.Level;
import org.w3c.dom.Element;

/**
 *
 * @author jmrunge
 */
public class CleanDirTask extends JobTask {
    private String dir;
    private String daysToKeep;

    public String getDaysToKeep() {
        return daysToKeep;
    }

    public void setDaysToKeep(String daysToKeep) {
        this.daysToKeep = daysToKeep;
    }

    public String getDir() {
        return dir;
    }

    public void setDir(String dir) {
        this.dir = dir;
    }

    @Override
    public void run() throws Exception {
        File d = new File(dir);
        if (!d.exists())
            throw new Exception(dir + " does not exists!");
        if (!d.isDirectory())
            throw new Exception(dir + " is not a directory!");
        
        for (File f : d.listFiles()) {
            Calendar fileDate = Calendar.getInstance();
            fileDate.setTimeInMillis(f.lastModified());
            Calendar today = Calendar.getInstance();
            fileDate.add(Calendar.DAY_OF_YEAR, Integer.parseInt(daysToKeep));
            if (fileDate.before(today))
                f.delete();
        }
        LogService.getInstance().log(Level.INFO, dir + " succesfully cleaned");
    }

    @Override
    public String getTaskType() {
        return "cleanDir";
    }

    @Override
    public JobTask getTask(Job job, Element task, boolean abortOnError) throws Exception {
        CleanDirTask cdt = new CleanDirTask();
        cdt.setAbortOnError(abortOnError);
        cdt.setDaysToKeep(job.replaceVars(task.getElementsByTagName("daysToKeep").item(0).getFirstChild().getNodeValue()));
        cdt.setDir(job.replaceVars(task.getElementsByTagName("dir").item(0).getFirstChild().getNodeValue()));
        return cdt;
    }
    
}
