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
package ar.com.zir.scheduler.agent.app;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author jmrunge
 */
public class Jobs {
    private List<Job> jobs;
    
    private Jobs() {
    }
    
    public static Jobs getInstance() {
        return JobsHolder.INSTANCE;
    }
    
    private static class JobsHolder {
        private static final Jobs INSTANCE = new Jobs();
    }

    public synchronized void addJob(Job job) {
        if (jobs == null)
            jobs = new ArrayList<>();
        jobs.add(job);
    }
    
    public synchronized List<Job> getJobs(String time) {
        List<Job> j = new ArrayList<>();
        for (Job job: jobs) {
            if (job.getTime().toLowerCase().trim().equals(time.toLowerCase().trim()))
                j.add(job);
        }
        return j;
    }
    
    public synchronized void reset() {
        jobs = new ArrayList<>();
    }
}
