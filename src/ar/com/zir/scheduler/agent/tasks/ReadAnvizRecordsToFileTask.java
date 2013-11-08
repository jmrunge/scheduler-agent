/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ar.com.zir.scheduler.agent.tasks;

import ar.com.zir.scheduler.agent.app.Job;
import ar.com.zir.scheduler.agent.tasks.common.FileField;
import ar.com.zir.scheduler.agent.tasks.mdb.FieldType;
import static ar.com.zir.scheduler.agent.tasks.mdb.FieldType.DATE;
import static ar.com.zir.scheduler.agent.tasks.mdb.FieldType.NUMBER;
import static ar.com.zir.scheduler.agent.tasks.mdb.FieldType.STRING;
import ar.com.zir.utils.DateUtils;
import ar.com.zir.utils.NumberUtils;
import ar.com.zir.utils.StringUtils;
import ar.com.zir.utils.XmlUtils;
import com.monits.anviz.TimeKeeper;
import com.monits.anviz.model.Action;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import org.joda.time.DateTime;
import org.w3c.dom.Element;

/**
 *
 * @author jmrunge
 */
public class ReadAnvizRecordsToFileTask extends JobTask {
    private String anvizIP;
    private Integer anvizID;
    private List<FileField> fileFields;
    private String fileName;
    private Date from;
    private Date to;

    public String getAnvizIP() {
        return anvizIP;
    }

    public void setAnvizIP(String anvizIP) {
        this.anvizIP = anvizIP;
    }

    public Integer getAnvizID() {
        return anvizID;
    }

    public void setAnvizID(Integer anvizID) {
        this.anvizID = anvizID;
    }

    public List<FileField> getFileFields() {
        return fileFields;
    }

    public void setFileFields(List<FileField> fileFields) {
        this.fileFields = fileFields;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Date getFrom() {
        return from;
    }

    public void setFrom(Date from) {
        this.from = from;
    }

    public Date getTo() {
        return to;
    }

    public void setTo(Date to) {
        this.to = to;
    }

    @Override
    public void run() throws Exception {
        List<Action> actions = new ArrayList<Action>();
        TimeKeeper anviz = new TimeKeeper(anvizIP, anvizID);
        for (Action action : anviz.getActions()) {
            DateTime fromDt = new DateTime(from);
            DateTime toDt = new DateTime(to);
            if (action.getTime().isAfter(fromDt) && action.getTime().isBefore(toDt)) {
                actions.add(action);
            }
        }

        Collections.sort(fileFields, new Comparator() {
            @Override
            public int compare(Object o1, Object o2) {
                return NumberUtils.compareInt(((FileField)o1).getStart(), ((FileField)o2).getStart());
            }
        });
        File f = new File(fileName);
        try (FileOutputStream fos = new FileOutputStream(f)) {
            try (PrintStream ps = new PrintStream(fos)) {
                for (Action action : actions) {
                    String line = "";
                    for (FileField field : fileFields) {
                        String aux = null;
                        Object value;
                        if (field.getFieldAlias() != null && field.getFieldAlias().trim().length() > 0) {
                            switch (field.getFieldAlias()) {
                                case "codigo":
                                    value = "AVC" + NumberUtils.format(action.getId(), "0000");
                                    break;
                                case "fecha":
                                case "hora":
                                    value = new Date(action.getTime().getMillis());
                                    break;
                                default:
                                    value = field.getName();
                            }
                        } else {
                            value = field.getName();
                        }
                        switch (field.getType()) {
                            case DATE:
                                String dateFormat = field.getFormat();
                                if (dateFormat == null || dateFormat.trim().length() == 0)
                                    throw new Exception("Format of a Date field can not be null [" + field.getName() + "]");
                                aux = DateUtils.format((Date)value, dateFormat);
                                break;
                            case NUMBER:
                                String numberFormat = field.getFormat();
                                if (numberFormat == null || numberFormat.trim().length() == 0)
                                    aux = new BigDecimal(value.toString()).toPlainString();
                                else
                                    aux = NumberUtils.format(new BigDecimal(value.toString()), numberFormat);
                                break;
                            case STRING:
                                String stringFormat = field.getFormat();
                                if (stringFormat != null && stringFormat.trim().length() > 0)
                                    throw new Exception("Format of a String field must be null [" + field.getName() + "]");
                                aux = value.toString();
                                break;
                            default:
                                throw new Exception("Format of " + field.getName() + " field not supported [" + field.getType() + "]");
                        }
                        int spaces = field.getLength() - aux.length();
                        if (spaces > 0)
                            aux = aux + StringUtils.repeat(" ", spaces);
                        spaces = field.getStart() - line.length();
                        if (spaces > 0)
                            aux = StringUtils.repeat(" ", spaces) + aux;
                        line = line + aux;
                    }
                    ps.println(line);
                }
            }
            fos.close();
        } catch (Exception ex) {
            throw new Exception("Error creating file " + fileName, ex);
        }            
    }

    @Override
    public String getTaskType() {
        return "readAnvizRecordsToFile";
    }

    @Override
    public JobTask getTask(Job job, Element task, boolean abortOnError) throws Exception {
        Element file = (Element) task.getElementsByTagName("file").item(0);
        List<FileField> ffs = new ArrayList<>();
        Element fields = (Element) file.getElementsByTagName("fields").item(0);
        for (Element field : XmlUtils.getNodeList(fields.getElementsByTagName("field"))) {
            Element f = (Element) field;
            FileField ff = new FileField();
            ff.setFieldAlias(f.getAttribute("alias"));
            ff.setFormat(f.getAttribute("format"));
            ff.setLength(Integer.parseInt(f.getAttribute("length")));
            ff.setName(f.getFirstChild().getNodeValue());
            ff.setStart(Integer.parseInt(f.getAttribute("start")));
            switch (f.getAttribute("type").toLowerCase().trim()) {
                case "date":
                    ff.setType(FieldType.DATE);
                    break;
                case "number":
                    ff.setType(FieldType.NUMBER);
                    break;
                case "string":
                    ff.setType(FieldType.STRING);
                    break;
                default:
                    throw new Exception("Invalid type [" + f.getAttribute("type") + "] for field [" + ff.getName() + "]");
            }
            ffs.add(ff);
        }
        Element anviz = (Element) task.getElementsByTagName("anviz").item(0);
        String ip = anviz.getElementsByTagName("IP").item(0).getFirstChild().getNodeValue();
        String id = anviz.getElementsByTagName("ID").item(0).getFirstChild().getNodeValue();
        
        Element range = (Element) task.getElementsByTagName("range").item(0);
        Element fromElement = (Element) range.getElementsByTagName("from").item(0);
        Date fromDate = DateUtils.parse(job.replaceVars(fromElement.getFirstChild().getNodeValue()), fromElement.getAttribute("format"));
        Element toElement = (Element) anviz.getElementsByTagName("to").item(0);
        Date toDate = DateUtils.parse(job.replaceVars(toElement.getFirstChild().getNodeValue()), toElement.getAttribute("format"));
        
        ReadAnvizRecordsToFileTask rartft = new ReadAnvizRecordsToFileTask();
        rartft.setAbortOnError(abortOnError);
        rartft.setFileFields(ffs);
        rartft.setFileName(job.replaceVars(file.getAttribute("path")));
        rartft.setAnvizID(Integer.valueOf(id));
        rartft.setAnvizIP(ip);
        rartft.setFrom(fromDate);
        rartft.setTo(toDate);
        return rartft;
    }
    
}
