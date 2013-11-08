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
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import org.apache.log4j.Level;
import org.w3c.dom.Element;

/**
 *
 * @author jmrunge
 */
public class BackupSqlServerTask extends JobTask {
    private String connectUrl;
    private String dbUser;
    private String dbPassword;
    private String storedProcedureName;
    private String successMessage;

    public String getConnectUrl() {
        return connectUrl;
    }

    public void setConnectUrl(String connectUrl) {
        this.connectUrl = connectUrl;
    }

    public String getDbPassword() {
        return dbPassword;
    }

    public void setDbPassword(String dbPassword) {
        this.dbPassword = dbPassword;
    }

    public String getDbUser() {
        return dbUser;
    }

    public void setDbUser(String dbUser) {
        this.dbUser = dbUser;
    }

    public String getStoredProcedureName() {
        return storedProcedureName;
    }

    public void setStoredProcedureName(String storedProcedureName) {
        this.storedProcedureName = storedProcedureName;
    }

    public String getSuccessMessage() {
        return successMessage;
    }

    public void setSuccessMessage(String successMessage) {
        this.successMessage = successMessage;
    }
    
    @Override
    public void run() throws Exception {
        DriverManager.registerDriver(new com.microsoft.sqlserver.jdbc.SQLServerDriver());
        String result;
        try (Connection conn = DriverManager.getConnection(connectUrl, dbUser, dbPassword); 
                CallableStatement cs = conn.prepareCall("{call " + storedProcedureName + "}"); 
                ResultSet rs = cs.executeQuery()) {
            rs.next();
            result = rs.getString(1);
        }
        if (!result.trim().equals(successMessage.trim()))
            throw new Exception("Error generating SQL Backup");
        LogService.getInstance().log(Level.INFO, "DB Backups executed successfully");
    }

    @Override
    public String getTaskType() {
        return "backupSQLServer";
    }

    @Override
    public JobTask getTask(Job job, Element task, boolean abortOnError) throws Exception {
        BackupSqlServerTask bst = new BackupSqlServerTask();
        bst.setAbortOnError(abortOnError);
        bst.setConnectUrl(job.replaceVars(task.getElementsByTagName("connectUrl").item(0).getFirstChild().getNodeValue()));
        bst.setDbPassword(job.replaceVars(task.getElementsByTagName("dbPassword").item(0).getFirstChild().getNodeValue()));
        bst.setDbUser(job.replaceVars(task.getElementsByTagName("dbUser").item(0).getFirstChild().getNodeValue()));
        bst.setStoredProcedureName(job.replaceVars(task.getElementsByTagName("storedProcedureName").item(0).getFirstChild().getNodeValue()));
        bst.setSuccessMessage(job.replaceVars(task.getElementsByTagName("successMessage").item(0).getFirstChild().getNodeValue()));
        return bst;
    }
    
}
