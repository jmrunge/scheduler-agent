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
import ar.com.zir.utils.StringUtils;
import ar.com.zir.utils.XmlUtils;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Level;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 *
 * @author jmrunge
 */
public class ExecuteCmdTask extends JobTask {
    private String[] commands;
    private String responseOk;

    public String[] getCommands() {
        return commands;
    }

    public void setCommands(String[] commands) {
        this.commands = commands;
    }

    public String getResponseOk() {
        return responseOk;
    }

    public void setResponseOk(String responseOk) {
        this.responseOk = responseOk;
    }
    
    @Override
    public void run() throws Exception {
        LogService.getInstance().log(Level.INFO, "Executing command [" + StringUtils.arrayToString(commands) + "]");

        Process child = Runtime.getRuntime().exec(commands);
        
        String response = "";
        LogService.getInstance().log(Level.DEBUG, "Reading response [" + StringUtils.arrayToString(commands) + "]");
        try (InputStream in = child.getInputStream()) {
            int c;
            while ((c = in.read()) != -1) {
                response = response + (char)c;
                LogService.getInstance().log(Level.DEBUG, "Reading response [" + StringUtils.arrayToString(commands) + "] " + response);
                if (checkResponse(responseOk, response))
                    break;
            }
        }
        LogService.getInstance().log(Level.DEBUG, "Checking response [" + StringUtils.arrayToString(commands) + "]");
        if (responseOk != null && responseOk.trim().length() > 0) {
//            if (!response.trim().toLowerCase().contains(responseOk.trim().toLowerCase())) 
            if (!checkResponse(responseOk, response)) 
                throw new Exception("Command [" + StringUtils.arrayToString(commands) + "] not executed succesfully");
        }
        LogService.getInstance().log(Level.INFO, "Command [" + StringUtils.arrayToString(commands) + "] executed succesfully");
    }

    @Override
    public String getTaskType() {
        return "executeCmd";
    }

    @Override
    public JobTask getTask(Job job, Element task, boolean abortOnError) throws Exception {
        List<String> cmd = new ArrayList<>();
        for (Element line : XmlUtils.getNodeList(task.getElementsByTagName("command"))) {
            cmd.add(line.getFirstChild().getNodeValue());
        }
        String[] cmds = new String[cmd.size()];
        int i = 0;
        for (String c : cmd) {
            cmds[i] = job.replaceVars(c);
            i++;
        }
        String response = null;
        NodeList responseNL = task.getElementsByTagName("response");
        if (responseNL != null && responseNL.getLength() > 0) {
            response = job.replaceVars(responseNL.item(0).getFirstChild().getNodeValue());
        }
        
        ExecuteCmdTask ect = new ExecuteCmdTask();
        ect.setAbortOnError(abortOnError);
        ect.setCommands(cmds);
        ect.setResponseOk(response);
        return ect;
    }

    private boolean checkResponse(String responseOk, String response) {
        if (responseOk != null && responseOk.trim().length() > 0) {
            if (response.trim().toLowerCase().contains(responseOk.trim().toLowerCase())) 
                return true;
        }
        return false;
    }
    
}
