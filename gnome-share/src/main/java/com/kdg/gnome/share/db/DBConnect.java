package com.kdg.gnome.share.db;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.kdg.gnome.share.Constants;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.io.Serializable;
import java.sql.*;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DBConnect {
  public static String DB_URL_FORMAT = "jdbc:%s://%s:%d/?characterEncoding=UTF-8&zeroDateTimeBehavior=convertToNull&rewriteBatchedStatements=true&noAccessToProcedureBodies=true&Pooling=false&autoReconnect=true&maxReconnects=3&initialTimeout=6";

  private static final Logger log = LogManager.getLogger("ES_OUT_INFO");

  private static final int QUERY_TIMEOUT = 600 * 1000;

  private String driver = "";
  private String ip = "";
  private Long port = 0L;
  private String userName = "";
  private String passwd = "";
  private Connection conn = null;
  private List<PreparedStatement> preStatList;

  public Connection getConn() {
    return conn;
  }

  public DBConnect(String driver, String ip, Long port, String userName, String passwd) {
    this.driver = driver;
    this.ip = ip;
    this.port = port;
    this.userName = userName;
    this.passwd = passwd;
    preStatList = Lists.newArrayList();
  }

  public class SelectWalker {
    private ResultSet rltSet = null;
    ResultSetMetaData rsmd = null;
    private int columnCount = -1;

    public SelectWalker(ResultSet rltSet) throws SQLException {
      this.rltSet = rltSet;

      rsmd = rltSet.getMetaData();
      columnCount = rsmd.getColumnCount();
    }

    public boolean next() throws SQLException {
      return rltSet.next();
    }

    public void close() throws SQLException {
      rltSet.close();
    }

    /**
     * @return never return null
     * @throws SQLException
     */
    public Map<String, Object> getRecord() throws SQLException {
      Map<String, Object> mapFields = Maps.newHashMap();
      for (int idx = 1; idx <= columnCount; idx++) {
        String columnName = rsmd.getColumnName(idx);
        Object columnValue = rltSet.getObject(idx);
        mapFields.put(columnName, columnValue);
      }

      return mapFields;
    }
  }

  public static class SelectResult implements Serializable {
    /**
     *
     */
    private static final long serialVersionUID = -3408941646036785571L;

    private Map<Map<String, String>, Map<String, Object>> mapRecord;

    public SelectResult() {
      mapRecord = Maps.newHashMap();
    }

    /**
     * @param order linkedHashMap OR HashMap
     */
    public SelectResult(boolean order) {
      if (order) {
        mapRecord = Maps.newLinkedHashMap();
      } else {
        mapRecord = Maps.newHashMap();
      }
    }

    public Iterator getIterator() {
      return mapRecord.entrySet().iterator();
    }

    public int size() {
      return mapRecord.size();
    }

    protected void addRecord(Map<String, String> dimFields, Map<String, Object> tarFields) {
      mapRecord.put(dimFields, tarFields);
    }

    public boolean isKeyExist(String... keyInfo) {
      if (null == keyInfo || keyInfo.length <= 0 || (0 != keyInfo.length % 2)) {
        return false;
      }

      Map<String, String> dimFields = Maps.newTreeMap();
      for (int idx = 1; idx <= keyInfo.length; idx += 2) {
        dimFields.put(keyInfo[idx - 1], keyInfo[idx]);
      }

      return mapRecord.containsKey(dimFields);
    }

    public Map<String, Object> getRecordByKey(String... keyInfo) {
      if (null == keyInfo || keyInfo.length <= 0 || (0 != keyInfo.length % 2)) {
        return null;
      }

      Map<String, String> dimFields = Maps.newTreeMap();
      for (int idx = 1; idx <= keyInfo.length; idx += 2) {
        dimFields.put(keyInfo[idx - 1], keyInfo[idx]);
      }

      return mapRecord.get(dimFields);
    }

    public Map<String, Object> getRecordByKey(Map<String, String> keyInfo) {
      if (null == keyInfo || keyInfo.size() <= 0) {
        return null;
      }
      return mapRecord.get(keyInfo);
    }

    public Object getFieldByKey(String fieldName, String... keyInfo) {
      Map<String, Object> mapValue = Maps.newHashMap();
      if (null == fieldName || null == keyInfo || keyInfo.length <= 0 || (0 != keyInfo.length % 2)) {
        return null;
      }

      Map<String, String> dimFields = Maps.newTreeMap();
      for (int idx = 1; idx <= keyInfo.length; idx += 2) {
        dimFields.put(keyInfo[idx - 1], keyInfo[idx]);
      }

      mapValue = mapRecord.get(dimFields);
      if (null == mapValue) {
        return null;
      }

      return mapValue.get(fieldName);
    }

    public Integer getIntegerByKey(String fieldName, int defaultValue, String... keyInfo) {
      Map<String, Object> mapValue = Maps.newHashMap();
      if (null == fieldName || null == keyInfo || keyInfo.length <= 0 || (0 != keyInfo.length % 2)) {
        return defaultValue;
      }

      Map<String, String> dimFields = Maps.newTreeMap();
      for (int idx = 1; idx <= keyInfo.length; idx += 2) {
        dimFields.put(keyInfo[idx - 1], keyInfo[idx]);
      }

      mapValue = mapRecord.get(dimFields);
      if (null == mapValue) {
        return defaultValue;
      }

      return (Integer) mapValue.get(fieldName);
    }

    public Object getFieldByKey(String fieldName, Map<String, String> dimFields) {
      Map<String, Object> mapValue = Maps.newHashMap();
      mapValue = mapRecord.get(dimFields);
      if (null == mapValue) {
        return null;
      }

      return mapValue.get(fieldName);
    }
  }

  public boolean getConnection() {
    String connUrl = String.format(DB_URL_FORMAT, this.driver, this.ip, this.port);
    try {
      Class.forName("com.mysql.jdbc.Driver");
      conn = DriverManager.getConnection(connUrl, this.userName, this.passwd);
      conn.setAutoCommit(false);
    } catch (Exception e) {
      log.error("db connect failed...: ", e);
      return false;
    }
    return true;
  }

  private boolean isConnected = false;

  public DBConnect setConnected(boolean connected) {
    isConnected = connected;
    return this;
  }

  public boolean isConnOk() {
    if (null == conn) {
      isConnected = false;
    }

    // 下面这个isClosed方法不靠谱，不能依赖它判断连接是否正常
//        try
//        {
//            if (conn.isClosed())
//            {
//                return false;
//            }
//        }
//        catch (SQLException e)
//        {
//            e.printStackTrace();
//            return false;
//        }

    return isConnected;
  }

  public int[] executeBatch(List<String> sqlList) throws SQLException {
    if (null == sqlList || sqlList.size() <= 0) {
      return null;
    }
    Statement stmt = conn.createStatement();
    for (String tmpSql : sqlList) {
      stmt.addBatch(tmpSql);
    }
    int[] ret = stmt.executeBatch();
    stmt.close();
    return ret;
  }

  public ResultSet executeQuery(String sql) throws SQLException {
    Statement execSql = conn.createStatement();
    execSql.setQueryTimeout(QUERY_TIMEOUT);
    return execSql.executeQuery(sql);
  }

  public boolean execute(String sql) throws SQLException {
    Statement stat = conn.createStatement();
    boolean ret = stat.execute(sql);
    stat.close();
    return ret;
  }

  public int executeUpdate(String sql) throws SQLException {
    Statement stat = conn.createStatement();
    int count = stat.executeUpdate(sql);
    stat.close();
    return count;
  }

  public void addPreparedStatement(String sql) throws SQLException {
    preStatList.add(conn.prepareStatement(sql));
  }

  public int[] execPreparedStatement(int index) throws SQLException {
    if (index >= preStatList.size()) {
      return null;
    }
    return preStatList.get(index).executeBatch();
  }

  public void execPreparedStatement() throws SQLException {
    for (PreparedStatement pre : preStatList) {
      pre.executeBatch();
    }
  }

  public void closePreparedStatement(int index) throws SQLException {
    if (index >= preStatList.size()) {
      return;
    }
    preStatList.get(index).close();
  }

  public void closePreparedStatement() throws SQLException {
    for (PreparedStatement pre : preStatList) {
      pre.close();
    }
  }

  public void clearPreparedStatementBatch(int index) throws SQLException {
    if (index >= preStatList.size()) {
      return;
    }
    preStatList.get(index).clearBatch();
  }

  public void clearPreparedStatementBatch() throws SQLException {
    for (PreparedStatement pre : preStatList) {
      pre.clearBatch();
    }
  }

  public void clearPreparedStatement() {
    preStatList.clear();
  }

  public void setPreparedStatement(int index, String... fields) throws SQLException {
    if (index >= preStatList.size()) {
      return;
    }
    PreparedStatement pre = preStatList.get(index);
    for (int i = 1; i <= fields.length; ++i) {
      pre.setString(i, fields[i - 1]);
    }
    pre.addBatch();
  }

  public void setPreparedStatement(String... fields) throws SQLException {
    for (PreparedStatement pre : preStatList) {
      for (int i = 1; i <= fields.length; ++i) {
        pre.setString(i, fields[i - 1]);
      }
      pre.addBatch();
    }
  }

  public void setAutoCommit(boolean auto) throws SQLException {
    conn.setAutoCommit(auto);
  }

  public int commit() {
    try {
      conn.commit();
    } catch (SQLException e) {
      e.printStackTrace();
      return Constants.RET_ERROR;
    }

    return Constants.RET_OK;
  }

  public int rollback() {
    try {
      conn.rollback();
    } catch (SQLException e) {
      e.printStackTrace();
      return Constants.RET_ERROR;
    }

    return Constants.RET_OK;
  }

  public void closeConn() {
    try {
      conn.close();
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  /**
   * @param sql
   * @return never return null
   * @throws Exception
   */
  public SelectWalker selectRowWalker(String sql) throws Exception {
    ResultSet rltSet = executeQuery(sql);
    return new SelectWalker(rltSet);
  }

  public SelectResult selectRowSet(String sql, Set<String> setDimFields) throws Exception {
    ResultSet rltSet = executeQuery(sql);
    if (null == rltSet) {
      return null;
    }

    SelectResult selectResult = new SelectResult(false);
    SelectWalker walker = new SelectWalker(rltSet);
    while (walker.next()) {
      Map<String, Object> mapFields = walker.getRecord();

      Map<String, String> dimFields = Maps.newHashMap();
      Iterator<String> itSet = setDimFields.iterator();
      while (itSet.hasNext()) {
        String dimFieldName = itSet.next();
        Object o = mapFields.remove(dimFieldName);
        if (o == null) {
          continue;
        }
        dimFields.put(dimFieldName, o.toString());
      }

      selectResult.addRecord(dimFields, mapFields);
    }
    walker.close();

    return selectResult;
  }

  public SelectResult selectOrderRowSet(String sql, Set<String> setDimFields) throws Exception {
    ResultSet rltSet = executeQuery(sql);
    if (null == rltSet) {
      return null;
    }

    SelectResult selectResult = new SelectResult(true);
    SelectWalker walker = new SelectWalker(rltSet);
    while (walker.next()) {
      Map<String, Object> mapFields = walker.getRecord();

      Map<String, String> dimFields = Maps.newHashMap();
      Iterator<String> itSet = setDimFields.iterator();
      while (itSet.hasNext()) {
        String dimFieldName = itSet.next();
        dimFields.put(dimFieldName, mapFields.remove(dimFieldName).toString());
      }

      selectResult.addRecord(dimFields, mapFields);
    }
    walker.close();

    return selectResult;
  }

  /**
   * @param tableName
   * @param mapFields
   * @return either (1) the row count for SQL Data Manipulation Language (DML)
   * statements or (2) 0 for SQL statements that return nothing or (3)
   * -1 if execution throws Exception
   * @throws Exception
   */
  public int InsertRow(String tableName, Map<String, Object> mapFields) throws Exception {
    if (null == tableName || tableName.length() <= 0) {
      log.error("UpdateRow: tableName param error");
      return Constants.RET_ERROR;
    }

    if (null == mapFields || mapFields.size() <= 0) {
      log.error("UpdateRow: mapFields param error");
      return Constants.RET_ERROR;
    }

    boolean firstFlag = true;
    String strFields = "";
    String strValues = "";
    for (Map.Entry<String, Object> entry : mapFields.entrySet()) {
      if (firstFlag) {
        firstFlag = false;
      } else {
        strFields += ", ";
        strValues += ", ";
      }

      strFields += entry.getKey();
      strValues += "'" + entry.getValue() + "'";
    }

    String insertSql = "insert into " + tableName + " (" + strFields + ") values (" + strValues + ")";

    int count = 0;
    try {
      count = executeUpdate(insertSql);
    } catch (Exception e) {
      e.printStackTrace();
      return -1;
    }
    return count;
  }

  public int UpdateRow(String tableName, Map<String, Object> dimMap, Map<String, Object> tarMap) throws Exception {
    if (null == tableName || tableName.length() <= 0) {
      log.error("UpdateRow: tableName param error");
      return Constants.RET_ERROR;
    }

    if (null == dimMap || dimMap.size() <= 0) {
      log.error("UpdateRow: dimMap param error");
      return Constants.RET_ERROR;
    }

    if (null == tarMap || tarMap.size() <= 0) {
      log.error("UpdateRow: tarMap param error");
      return Constants.RET_ERROR;
    }

    boolean firstFlag = true;
    String updateSql = "update " + tableName + " set ";
    for (Map.Entry<String, Object> entry : tarMap.entrySet()) {
      if (firstFlag) {
        firstFlag = false;
      } else {
        updateSql += ", ";
      }

      updateSql += entry.getKey() + "='" + entry.getValue() + "'";
    }

    updateSql += " where ";

    firstFlag = true;
    for (Map.Entry<String, Object> entry : dimMap.entrySet()) {
      if (firstFlag) {
        firstFlag = false;
      } else {
        updateSql += " and ";
      }

      updateSql += entry.getKey() + "='" + entry.getValue() + "'";
    }

    int count = 0;
    try {
      count = executeUpdate(updateSql);
    } catch (Exception e) {
      e.printStackTrace();
      return -1;
    }
    return count;
  }

  public int DeleteRow(String tableName, Map<String, Object> mapFields) throws Exception {
    if (null == tableName || tableName.length() <= 0) {
      log.error("UpdateRow: tableName param error");
      return Constants.RET_ERROR;
    }

    if (null == mapFields || mapFields.size() <= 0) {
      log.error("UpdateRow: mapFields param error");
      return Constants.RET_ERROR;
    }

    boolean firstFlag = true;
    String conditions = "";
    for (Map.Entry<String, Object> entry : mapFields.entrySet()) {
      if (firstFlag) {
        firstFlag = false;
      } else {
        conditions += " and ";
      }

      conditions += entry.getKey() + "=" + "'" + entry.getValue() + "'";
    }

    String delSql = "delete from " + tableName + " where " + conditions;

    int count = 0;
    try {
      count = executeUpdate(delSql);
    } catch (Exception e) {
      e.printStackTrace();
      return -1;
    }
    return count;
  }

  public int UpdateRow(String tableName, Map<String, Object> dimMap, Map<String, Object> tarMap, Map<String, Object> deltaMap) throws Exception {
    if (null == tableName || tableName.length() <= 0) {
      log.error("UpdateRow: tableName param error");
      return Constants.RET_ERROR;
    }

    if (null == dimMap || dimMap.size() <= 0) {
      log.error("UpdateRow: dimMap param error");
      return Constants.RET_ERROR;
    }

/*        if (null == tarMap || tarMap.size() <= 0)
        {
            log.error("UpdateRow: tarMap param error");
            return Constants.RET_ERROR;
        }*/

    boolean firstFlag = true;
    String updateSql = "update " + tableName + " set ";
    for (Map.Entry<String, Object> entry : tarMap.entrySet()) {
      if (firstFlag) {
        firstFlag = false;
      } else {
        updateSql += ", ";
      }

      updateSql += entry.getKey() + "='" + entry.getValue() + "'";
    }

    for (Map.Entry<String, Object> entry : deltaMap.entrySet()) {
      if (firstFlag) {
        firstFlag = false;
      } else {
        updateSql += ", ";
      }

      updateSql += entry.getKey() + "=" + entry.getKey() + " + " + entry.getValue();
    }

    updateSql += " where ";

    firstFlag = true;
    for (Map.Entry<String, Object> entry : dimMap.entrySet()) {
      if (firstFlag) {
        firstFlag = false;
      } else {
        updateSql += " and ";
      }

      updateSql += entry.getKey() + "='" + entry.getValue() + "'";
    }

    int count = 0;
    try {
      count = executeUpdate(updateSql);
    } catch (Exception e) {
      e.printStackTrace();
      return -1;
    }
    return count;
  }
}
