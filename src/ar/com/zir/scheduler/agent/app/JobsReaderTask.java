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
import ar.com.zir.utils.LogService;
import ar.com.zir.utils.XmlUtils;
import java.io.File;
import java.util.TimerTask;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.log4j.Level;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 *
 * @author jmrunge
 */
public class JobsReaderTask extends TimerTask {

    @Override
    public void run() {
        LogService.getInstance().log(Level.INFO, "Leyendo jobs");
        File jobsDir = new File("./../jobs");
        Jobs.getInstance().reset();
        for (File jobFile : jobsDir.listFiles()) {
            try {
                Job job = readJob(jobFile);
                Jobs.getInstance().addJob(job);
            } catch (Exception ex) {
                LogService.getInstance().log(Level.ERROR, "No se pudo leer el job " + jobFile.getPath(), ex, true);
            }
        }
    }
    
    private Job readJob(File jobFile) throws Exception {
        LogService.getInstance().log(Level.INFO, "Leyendo job " + jobFile.getPath());
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document doc = builder.parse(jobFile);
        Job job = new Job(jobFile.getName());
        Element root = doc.getDocumentElement();
        job.setTime(root.getElementsByTagName("time").item(0).getFirstChild().getNodeValue());
        Node vars = root.getElementsByTagName("vars").item(0);
        for (Element var : XmlUtils.getChildNodes(vars)) {
            job.addVar(var.getAttribute("name"), var.getFirstChild().getNodeValue(), var.getAttribute("type"), 
                    var.getAttribute("format"), var.getAttribute("function"));
        }
        Node tasks = root.getElementsByTagName("tasks").item(0);
        for (Element task : XmlUtils.getChildNodes(tasks)) {
            String type = task.getAttribute("type");
            boolean abortOnError = task.getAttribute("abortOnError").trim().toLowerCase().equals("true");
            JobTask jt = JobTask.findTask(type);
            job.addTask(jt.getTask(job, task, abortOnError));
        }
        LogService.getInstance().log(Level.INFO, "Job " + jobFile.getPath() + " leido exitosamente");
        return job;
    }
    
}
