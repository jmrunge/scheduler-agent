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

import ar.com.zir.utils.LogService;
import ar.com.zir.utils.PropertiesReader;
import java.util.Timer;
import org.apache.log4j.Level;

/**
 *
 * @author jmrunge
 */
public class SchedulerService {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        SchedulerService service = new SchedulerService();
        service.startService();
    }
    
    public void startService() {
        try {
            PropertiesReader.getInstance().init("./../conf/scheduler.properties");
            LogService.getInstance().configureDailyLog("./../log", "SchedulerService", PropertiesReader.getInstance().getProperties());
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(0);
        }
        LogService.getInstance().log(Level.INFO, "Servicio iniciado, comenzando a leer jobs", true);            
        Timer timer = new Timer();
        int reloadTime = Integer.parseInt(PropertiesReader.getInstance().getProperty("jobs.reload")) * 1000;
        timer.scheduleAtFixedRate(new JobsReaderTask(), 0, reloadTime);
        timer.scheduleAtFixedRate(new JobsExecuterTask(), 30000, 60000);
    }
    
}
