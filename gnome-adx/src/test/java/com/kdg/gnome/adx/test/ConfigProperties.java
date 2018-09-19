package com.kdg.gnome.adx.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Blob;
import java.util.Properties;

public class ConfigProperties {
	private static final String filename = "Config.properties";

	private String port;
	private String isFixResponse;
	private String threadPoolNum;
	private String waitTimeMin;
	private String waitTimeMax;
	private String statusCodeList;
	private String responseDataPath200List;
	private String responseDataPath204List;
	private String responseDataPath504List;
	private String timeOut504;
	private String fixResponseDataPath;

	


	public String getFixResponseDataPath() {
		return fixResponseDataPath;
	}

	public void setFixResponseDataPath(String fixResponseDataPath) {
		this.fixResponseDataPath = fixResponseDataPath;
	}

	public String getIsFixResponse() {
		return isFixResponse;
	}

	public void setIsFixResponse(String isFixResponse) {
		this.isFixResponse = isFixResponse;
	}

	public String getResponseDataPath200List() {
		return responseDataPath200List;
	}

	public void setResponseDataPath200List(String responseDataPath200List) {
		this.responseDataPath200List = responseDataPath200List;
	}

	public String getResponseDataPath204List() {
		return responseDataPath204List;
	}

	public void setResponseDataPath204List(String responseDataPath204List) {
		this.responseDataPath204List = responseDataPath204List;
	}


	public String getResponseDataPath504List() {
		return responseDataPath504List;
	}

	public void setResponseDataPath504List(String responseDataPath504List) {
		this.responseDataPath504List = responseDataPath504List;
	}

	public String getTimeOut504() {
		return timeOut504;
	}

	public void setTimeOut504(String timeOut504) {
		this.timeOut504 = timeOut504;
	}

	public String getStatusCodeList() {
		return statusCodeList;
	}

	public void setStatusCodeList(String statusCodeList) {
		this.statusCodeList = statusCodeList;
	}

	public String getThreadPoolNum() {
		return threadPoolNum;
	}

	public void setThreadPoolNum(String threadPoolNum) {
		this.threadPoolNum = threadPoolNum;
	}

	public String getWaitTimeMin() {
		return waitTimeMin;
	}

	public void setWaitTimeMin(String waitTimeMin) {
		this.waitTimeMin = waitTimeMin;
	}

	public String getWaitTimeMax() {
		return waitTimeMax;
	}

	public void setWaitTimeMax(String waitTimeMax) {
		this.waitTimeMax = waitTimeMax;
	}

	private Properties p;

	public String getPort() {
		return port;
	}

	public void setPort(String port) {
		this.port = port;
	}

	/**
	 * @throws FileNotFoundException
	 * 
	 */
	public ConfigProperties() throws FileNotFoundException {
		addProperties();
		this.port = getMesg("port");
		this.isFixResponse = getMesg("isFixResponse");
		this.fixResponseDataPath = getMesg("fixResponseDataPath");
		this.threadPoolNum = getMesg("threadPoolNum");
		this.waitTimeMin = getMesg("waitTimeMin");
		this.waitTimeMax = getMesg("waitTimeMax");
		this.statusCodeList = getMesg("statusCodeList");
		this.responseDataPath200List = getMesg("responseDataPath200List");
		this.responseDataPath204List = getMesg("responseDataPath204List");
		this.responseDataPath504List = getMesg("responseDataPath504List");
		this.timeOut504 = getMesg("timeOut504");

	}

	/*
	 * 缺省值ֵ
	 */
	public void addProperties() throws FileNotFoundException {
		if (p == null) {
			p = new Properties();
			File f = new File(filename);
			InputStream in = new FileInputStream(f);
			try {
				p.load(in);
				in.close();
			} catch (IOException e) {
				System.out.println("加载缺省值");
				p.put("port", "80");
				p.put("isFixResponse", "true");
				p.put("fixResponseDataPath", "80");
				p.put("threadPoolNum", "128");
				p.put("waitTimeMin", "100");
				p.put("waitTimeMax", "1000");
				p.put("statusCodeList", "200,1");
				p.put("responseDataPath200List", "responseData200,1");
				p.put("responseDataPath204List", "responseData204,1");
				p.put("responseDataPath504List", "responseData504,1");
				p.put("timeOut504", "3000");

			}
		}
	}

	// 取配置文件中对应值
	public String getMesg(String key) {
		return p.getProperty(key) == null ? key : p.getProperty(key);

	}

	public Properties getP() {
		return p;
	}

	public void setP(Properties p) {
		this.p = p;
	}

	public static String getFilename() {
		return filename;
	}

}
