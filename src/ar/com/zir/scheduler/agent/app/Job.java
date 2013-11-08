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

import ar.com.zir.scheduler.agent.tasks.JobTask;
import ar.com.zir.utils.DateUtils;
import ar.com.zir.utils.LogService;
import java.util.*;
import org.apache.log4j.Level;

/**
 *
 * @author jmrunge
 */
public class Job implements Runnable {
    private String name;
    private String time;
    private Map<String, String> vars;
    private List<JobTask> tasks;
    
    public Job(String name){
        this.name = name;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }
    
    public void addVar(String name, String value, String type, String format, String function) throws Exception {
        if (vars == null)
            vars = new HashMap<>();
        value = replaceVars(value);
        if (type.trim().equals("date")) {
            if (format == null || format.trim().length() == 0)
                throw new Exception("Missing attribute [format]");
            Date date;
            switch (value.trim()) {
                case "CURRENT_DATE":
                    date = Calendar.getInstance().getTime();
                    break;
                case "CURRENT_FIRST_DATE":
                    date = DateUtils.firstHourOfDay(Calendar.getInstance().getTime());
                    break;
                case "CURRENT_LAST_DATE":
                    date = DateUtils.lastHourOfDay(Calendar.getInstance().getTime());
                    break;
                default:
                    date = DateUtils.parse(value, format);
                    break;
            }
            if (function != null && function.trim().length() > 0) {
                if (function.trim().startsWith("dayAdd")) {
                    int pos1 = function.indexOf("(") + 1;
                    int pos2 = function.indexOf(")");
                    int num = Integer.parseInt(function.substring(pos1, pos2));
                    date = DateUtils.dateAdd(DateUtils.DATE_PART_DAY, num, date);
                }
            }
            value = DateUtils.format(date, format);
        } else if (!type.trim().equals("string"))
            throw new Exception("Variable Type not supported!");
        vars.put(name, value);
    }
    
    public String replaceVars(String value) throws Exception {
        int pos1 = value.indexOf("{");
        while (pos1 >= 0) {
            int pos2 = value.indexOf("}");
            String varName = value.substring(pos1 + 1, pos2);
            String varValue = vars.get(varName);
            if (varValue == null)
                throw new Exception("Variable [" + varName + "] does not exists");
            value = value.replaceAll("\\{" + varName + "\\}", varValue);
            pos1 = value.indexOf("{");
        }
        return value;
    }
    
    public void addTask(JobTask task) {
        if (tasks == null)
            tasks = new ArrayList<>();
        tasks.add(task);
    }

    @Override
    public void run() {
        for (JobTask task : tasks) {
            try {
                task.run();
            } catch (Exception ex) {
                if (task.isAbortOnError()) {
                    LogService.getInstance().log(Level.FATAL, "[JOB ABORTED] Error executing job " + name, ex, true);            
                    return;
                } else {
                    LogService.getInstance().log(Level.ERROR, "Error executing job " + name, ex, true);            
                }
            }
        }
    }
    
}
