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

import ar.com.zir.mail.api.Mail;
import ar.com.zir.mail.client.MailClient;
import ar.com.zir.scheduler.agent.app.Job;
import ar.com.zir.scheduler.agent.tasks.mail.MailAttachment;
import ar.com.zir.utils.XmlUtils;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 *
 * @author jmrunge
 */
public class SendMailTask extends JobTask {
    private String title;
    private List<String> bodyLines;
    private String sender;
    private List<String> recipients;
    private String host;
    private String port;
    private List<MailAttachment> attachments;

    public List<String> getBodyLines() {
        return bodyLines;
    }

    public void setBodyLines(List<String> bodyLines) {
        this.bodyLines = bodyLines;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public List<String> getRecipients() {
        return recipients;
    }

    public void setRecipients(List<String> recipients) {
        this.recipients = recipients;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<MailAttachment> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<MailAttachment> attachments) {
        this.attachments = attachments;
    }
    
    public void addBodyLine(String line) {
        if (bodyLines == null)
            bodyLines = new ArrayList<>();
        bodyLines.add(line);
    }
    
    public void addRecipient(String recipient){
        if (recipients == null)
            recipients = new ArrayList<>();
        recipients.add(recipient);
    }
    
    public void addAttachment(MailAttachment attach){
        if (attachments == null)
            attachments = new ArrayList<>();
        attachments.add(attach);
    }
    
    @Override
    public void run() throws Exception {
        MailClient client = new MailClient(host, Integer.parseInt(port));
        Mail mail = new Mail();
        mail.setSender(sender);
        mail.setSubject(title);
        String message = "";
        if (bodyLines != null) {
            for (String line : bodyLines) {
                message = message + line + "<br>";
            }
        }
        mail.setMessage(message);
        for (String recipient : recipients) {
            mail.addRecipient(recipient);
        }
        if (attachments != null) {
            for (MailAttachment attach : attachments) {
                String[] paths = attach.getFileName().split("/");
                Path path = FileSystems.getDefault().getPath(attach.getFileName());
                byte[] b = Files.readAllBytes(path);
                mail.addAttachment(b, paths[paths.length - 1], attach.getMimeType());
            }
        }
        client.sendMail(mail);
    }

    @Override
    public String getTaskType() {
        return "sendMail";
    }

    @Override
    public JobTask getTask(Job job, Element task, boolean abortOnError) throws Exception {
        List<String> bl = new ArrayList<>();
        for (Element line : XmlUtils.getNodeList(task.getElementsByTagName("body"))) {
            bl.add(line.getFirstChild().getNodeValue());
        }
        List<String> rcs = new ArrayList<>();
        for (Element recipient : XmlUtils.getNodeList(task.getElementsByTagName("recipient"))) {
            rcs.add(recipient.getFirstChild().getNodeValue());
        }
        Element server = (Element) task.getElementsByTagName("server").item(0);
        String h = server.getElementsByTagName("host").item(0).getFirstChild().getNodeValue();
        String p = server.getElementsByTagName("port").item(0).getFirstChild().getNodeValue();
        Element attachs = (Element) task.getElementsByTagName("attach").item(0);
        List<MailAttachment> attach = new ArrayList<>();
        if (attachs != null) {
            NodeList at = attachs.getElementsByTagName("file");
            for (Element file : XmlUtils.getNodeList(at)) {
                String mime = file.getAttribute("mime");
                MailAttachment ma = new MailAttachment();
                ma.setFileName(file.getFirstChild().getNodeValue());
                ma.setMimeType(mime);
                attach.add(ma);
            }
        }
        
        SendMailTask smt = new SendMailTask();
        smt.setAbortOnError(abortOnError);
        smt.setTitle(job.replaceVars(task.getElementsByTagName("title").item(0).getFirstChild().getNodeValue()));
        for (String line : bl) {
            smt.addBodyLine(job.replaceVars(line));
        }
        smt.setSender(job.replaceVars(task.getElementsByTagName("sender").item(0).getFirstChild().getNodeValue()));
        for (String recipient : rcs) {
            smt.addRecipient(job.replaceVars(recipient));
        }
        smt.setHost(job.replaceVars(h));
        smt.setPort(job.replaceVars(p));
        for (MailAttachment file : attach) {
            MailAttachment ma = new MailAttachment();
            ma.setFileName(job.replaceVars(file.getFileName()));
            ma.setMimeType(job.replaceVars(file.getMimeType()));
            smt.addAttachment(ma);
        }
        return smt;
    }
    
}
