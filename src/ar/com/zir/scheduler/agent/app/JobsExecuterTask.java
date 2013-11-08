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

import ar.com.zir.utils.DateUtils;
import ar.com.zir.utils.LogService;
import java.util.Calendar;
import java.util.List;
import java.util.TimerTask;
import org.apache.log4j.Level;

/**
 *
 * @author jmrunge
 */
public class JobsExecuterTask extends TimerTask {

    @Override
    public void run() {
        String time = DateUtils.format(Calendar.getInstance().getTime(), "HH:mm");
        LogService.getInstance().log(Level.INFO, "Iniciando ejecución de las " + time);
        List<Job> jobs = Jobs.getInstance().getJobs(time);
        for (Job job : jobs) {
            Thread t = new Thread(job);
            t.start();
        }
    }
    
}
