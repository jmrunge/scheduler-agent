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
import java.util.ServiceLoader;
import org.w3c.dom.Element;

/**
 *
 * @author jmrunge
 */
public abstract class JobTask {
    private boolean abortOnError;
    private static ServiceLoader<JobTask> tasksLoader = ServiceLoader.load(JobTask.class);

    public JobTask() {
    }

    public boolean isAbortOnError() {
        return abortOnError;
    }

    public void setAbortOnError(boolean abortOnError) {
        this.abortOnError = abortOnError;
    }
    
    public abstract void run() throws Exception;
    
    public abstract String getTaskType();
    
    public static JobTask findTask(String type) {
        for (JobTask jt : tasksLoader) {
            if (jt.getTaskType().trim().equals(type.trim()))
                return jt;
        }
        return null;
    }
    
    public abstract JobTask getTask(Job job, Element task, boolean abortOnError) throws Exception;
}
