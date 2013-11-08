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
import ar.com.zir.scheduler.agent.tasks.common.FileField;
import ar.com.zir.scheduler.agent.tasks.common.Operator;
import ar.com.zir.scheduler.agent.tasks.mdb.*;
import ar.com.zir.utils.DateUtils;
import ar.com.zir.utils.NumberUtils;
import ar.com.zir.utils.StringUtils;
import ar.com.zir.utils.XmlUtils;
import com.healthmarketscience.jackcess.DataType;
import com.healthmarketscience.jackcess.Database;
import com.healthmarketscience.jackcess.Table;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.util.*;
import org.w3c.dom.Element;

/**
 *
 * @author jmrunge
 */
public class ReadMdbToFileTask extends JobTask {
    private String dbPath;
    private List<FileField> fileFields;
    private List<DbTable> tables;
    private String fileName;

    public String getDbPath() {
        return dbPath;
    }

    public void setDbPath(String dbPath) {
        this.dbPath = dbPath;
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

    public List<DbTable> getTables() {
        return tables;
    }

    public void setTables(List<DbTable> tables) {
        this.tables = tables;
    }
    
    @Override
    public void run() throws Exception {
        Database db = Database.open(new File(dbPath));
        List<Map<String, Object>> rows = new ArrayList<>();
        for (DbTable t : tables) {
            Table table = db.getTable(t.getName());
            List<Map<String, Object>> auxRows = new ArrayList<>();
            for(Map<String, Object> row : table) {
                boolean includeRow = true;
                boolean rowIncluded = false;
                for (DbTableCondition cond : t.getConditions()) {
                    Object rowValue = convertDataType(row.get(cond.getField()), table.getColumn(cond.getField()).getType());
                    Object condValue = null;
                    if (cond instanceof VarDbTableCondition) {
                        condValue = getObjectFromVar(((VarDbTableCondition)cond).getVarValue(), ((VarDbTableCondition)cond).getFormat(), rowValue);
                    } 
                    if (condValue != null) {
                        if (!meetsCondition(rowValue, condValue, cond.getOperator())) {
                            includeRow = false;
                            break;
                        }
                    }
                }
                if (includeRow) {
                    if (hasFieldDbTableConditions(t) && rows.isEmpty()) {
                        includeRow = false;
                    } else {
                        for (Map<String, Object> fileRow : rows) {
                            boolean includeRow2 = true;
                            for (DbTableCondition cond : t.getConditions()) {
                                Object rowValue = row.get(cond.getField());
                                Object condValue = null;
                                if (cond instanceof FieldDbTableCondition) {
                                    condValue = fileRow.get(((FieldDbTableCondition)cond).getFieldAlias());
                                } 
                                if (condValue != null) {
                                    if (!meetsCondition(rowValue, condValue, cond.getOperator())) {
                                        includeRow2 = false;
                                        break;
                                    }
                                }
                            }
                            if (includeRow2) {
                                Map<String, Object> newRow = new HashMap<>();
                                for (String key : fileRow.keySet()) {
                                    newRow.put(key, fileRow.get(key));
                                }
                                for (DbTableField field : t.getFields()) {
                                    newRow.put(field.getAlias(), row.get(field.getName()));
                                }
                                auxRows.add(newRow);
                                rowIncluded = true;
                            }
                            includeRow = includeRow2;
                        }
                    }
                }
                if (includeRow && !rowIncluded) {
                    Map<String, Object> newRow = new HashMap<>();
                    for (DbTableField field : t.getFields()) {
                        newRow.put(field.getAlias(), row.get(field.getName()));
                    }
                    auxRows.add(newRow);
                    rowIncluded = true;
                }
            }
            rows = auxRows;
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
                for (Map<String, Object> row : rows) {
                    String line = "";
                    for (FileField field : fileFields) {
                        String aux = null;
                        Object value;
                        if (field.getFieldAlias() != null && field.getFieldAlias().trim().length() > 0)
                            value = row.get(field.getFieldAlias());
                        else
                            value = field.getName();
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

    private boolean meetsCondition(Object rowValue, Object condValue, Operator operator) throws Exception {
        switch (operator) {
            case EQUAL:
                if (rowValue.equals(condValue))
                    return true;
                break;
            case LESS:
                if (rowValue instanceof Date) {
                    if (((Date)rowValue).before((Date)condValue))
                        return true;
                } else if (rowValue instanceof Integer) {
                    if ((((Integer)rowValue).intValue() < ((Integer)condValue).intValue()))
                        return true;
                } else {
                    throw new Exception("DataType [" + rowValue.getClass().getCanonicalName() + "] not still supported for operator LESS");
                }
                break;
            case LESS_OR_EQUAL:
                if (rowValue.equals(condValue))
                    return true;
                else if (rowValue instanceof Date) {
                    if (((Date)rowValue).before((Date)condValue))
                        return true;
                } else if (rowValue instanceof Integer) {
                    if ((((Integer)rowValue).intValue() < ((Integer)condValue).intValue()))
                        return true;
                } else {
                    throw new Exception("DataType [" + rowValue.getClass().getCanonicalName() + "] not still supported for operator LESS_OR_EQUAL");
                }
                break;
            case GREATER:
                if (rowValue instanceof Date) {
                    if (((Date)rowValue).after((Date)condValue))
                        return true;
                } else if (rowValue instanceof Integer) {
                    if ((((Integer)rowValue).intValue() > ((Integer)condValue).intValue()))
                        return true;
                } else {
                    throw new Exception("DataType [" + rowValue.getClass().getCanonicalName() + "] not still supported for operator GREATER");
                }
                break;
            case GREATER_OR_EQUAL:
                if (rowValue.equals(condValue))
                    return true;
                else if (rowValue instanceof Date) {
                    if (((Date)rowValue).after((Date)condValue))
                        return true;
                } else if (rowValue instanceof Integer) {
                    if ((((Integer)rowValue).intValue() > ((Integer)condValue).intValue()))
                        return true;
                } else {
                    throw new Exception("DataType [" + rowValue.getClass().getCanonicalName() + "] not still supported for operator GREATER_OR_EQUAL");
                }
                break;
        }
        return false;
    }

    private Object convertDataType(Object obj, DataType type) throws Exception {
        if (type.equals(DataType.SHORT_DATE_TIME))
            return (Date)obj;
        else if (type.equals(DataType.INT))
            return (Integer)obj;
        else if (type.equals(DataType.TEXT))
            return (String)obj;
        else if (type.equals(DataType.LONG))
            return (Integer)obj;
        else
            throw new Exception("DataType [" + type + "] not still supported");
    }

    private Object getObjectFromVar(Object varValue, String format, Object rowValue) throws Exception {
        if (rowValue instanceof Date)
            return DateUtils.parse(varValue.toString(), format);
        else if (rowValue instanceof Integer)
            return new Integer(varValue.toString());
        else if (rowValue instanceof String)
            return varValue.toString();
        else
            throw new Exception("DataType [" + rowValue.getClass().getCanonicalName() + "] not still supported");
    }

    @Override
    public String getTaskType() {
        return "readMDBToFile";
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
        List<DbTable> dbTables = new ArrayList<>();
        Element ts = (Element) task.getElementsByTagName("tables").item(0);
        for (Element table : XmlUtils.getNodeList(ts.getElementsByTagName("table"))) {
            Element t = (Element) table;
            DbTable dbTable = new DbTable();
            dbTable.setName(t.getAttribute("name"));
            List<DbTableField> dbTableFields = new ArrayList<>();
            Element dbFields = (Element) t.getElementsByTagName("tableFields").item(0);
            for (Element field : XmlUtils.getNodeList(dbFields.getElementsByTagName("tableField"))) {
                Element f = (Element) field;
                DbTableField dbField = new DbTableField();
                dbField.setTable(dbTable);
                dbField.setAlias(f.getAttribute("alias"));
                dbField.setName(f.getFirstChild().getNodeValue());
                dbTableFields.add(dbField);
            }
            dbTable.setFields(dbTableFields);
            List<DbTableCondition> dbTableConditions = new ArrayList<>();
            Element dbConditions = (Element) t.getElementsByTagName("conditions").item(0);
            for (Element condition : XmlUtils.getNodeList(dbConditions.getElementsByTagName("varCondition"))) {
                Element c = (Element) condition;
                VarDbTableCondition cond = new VarDbTableCondition();
                cond.setField(c.getAttribute("field"));
                cond.setOperator(Operator.getOperator(c.getAttribute("operator").trim()));
                cond.setFormat(c.getAttribute("format").trim());
                cond.setTable(dbTable);
                cond.setVarName(c.getFirstChild().getNodeValue());
                dbTableConditions.add(cond);
            }
            for (Element condition : XmlUtils.getNodeList(dbConditions.getElementsByTagName("fieldCondition"))) {
                Element c = (Element) condition;
                FieldDbTableCondition cond = new FieldDbTableCondition();
                cond.setField(c.getAttribute("field"));
                cond.setOperator(Operator.getOperator(c.getAttribute("operator").trim()));
                cond.setTable(dbTable);
                cond.setFieldAlias(c.getFirstChild().getNodeValue());
                dbTableConditions.add(cond);
            }
            dbTable.setConditions(dbTableConditions);
            dbTables.add(dbTable);
        }
        
        ReadMdbToFileTask rmtft = new ReadMdbToFileTask();
        rmtft.setAbortOnError(abortOnError);
        rmtft.setDbPath(job.replaceVars(task.getElementsByTagName("mdb").item(0).getFirstChild().getNodeValue()));
        rmtft.setFileFields(ffs);
        rmtft.setFileName(job.replaceVars(file.getAttribute("path")));
        for (DbTable table : dbTables) {
            for (DbTableCondition cond : table.getConditions()) {
                if (cond instanceof VarDbTableCondition) {
                    ((VarDbTableCondition)cond).setVarValue(job.replaceVars(((VarDbTableCondition)cond).getVarName()));
                }
            }
        }
        rmtft.setTables(dbTables);
        return rmtft;
    }

    private boolean hasFieldDbTableConditions(DbTable t) {
        if (t.getConditions() != null) {
            for (DbTableCondition tc : t.getConditions()) {
                if (tc instanceof FieldDbTableCondition)
                    return true;
            }
        }
        return false;
    }
}
