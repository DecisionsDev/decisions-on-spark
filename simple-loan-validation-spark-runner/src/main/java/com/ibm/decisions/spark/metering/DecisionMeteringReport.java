package com.ibm.decisions.spark.metering;

import java.io.IOException;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibm.license.metric.InstanceDetails;
import com.ibm.license.metric.LicenseMetricLogger;
import com.ibm.license.metric.LoggerConfiguration;
import com.ibm.license.metric.Metric;
import com.ibm.license.metric.MetricPersistenceException;
import com.ibm.license.metric.SoftwareIdentity;

public class DecisionMeteringReport implements Serializable {

	private static final long serialVersionUID = 165369033778659904L;
	private String version = "1.0";
	private LocalDateTime startDateTime;
	private LocalDateTime stopDateTime;
	private long nbDecisions;
	private String batchId;

	public DecisionMeteringReport() {

	}

	public DecisionMeteringReport(String batchId) {

		init();

		this.batchId = batchId;
	}

	public DecisionMeteringReport(long nBDecisions, String batchId) {

		init();

		// nb of executions
		this.nbDecisions = nBDecisions;

		this.batchId = batchId;
	}

	private void init() {
		LocalDateTime timePoint = LocalDateTime.now();
		this.startDateTime = timePoint;
		this.stopDateTime = timePoint;
	}

	// Start Timestamp

	@JsonIgnore
	public LocalDateTime getStartTimeStamp() {
		return startDateTime;
	}

	@JsonIgnore
	public void setStartTimeStamp(LocalDateTime timeStamp) {
		this.startDateTime = timeStamp;
	}

	// Start Timestamp as String
	@JsonProperty("startTimeStamp")
	public String getStartTimeStampString() {
		return startDateTime.toString();
	}

	public void setStartTimeStampString(String timeStampString) {
		this.startDateTime = LocalDateTime.parse(timeStampString);
	}

	// Stop Timestamp

	@JsonIgnore
	public LocalDateTime getStopTimeStamp() {
		return stopDateTime;
	}

	@JsonIgnore
	public void setStopTimeStamp(LocalDateTime timeStamp) {
		this.stopDateTime = timeStamp;
	}

	public void setStopTimeStamp() {
		LocalDateTime timePoint = LocalDateTime.now();
		this.stopDateTime = timePoint;
	}

	// Stop Timestamp as String
	@JsonProperty("stopTimeStamp")
	public String getStopTimeStampString() {
		return stopDateTime.toString();
	}

	public void setStopTimeStampString(String timeStampString) {
		this.stopDateTime = LocalDateTime.parse(timeStampString);
	}

	// nbDecision

	public long getNbDecisions() {
		return nbDecisions;
	}

	public void setNbDecisions(long nbDecisions) {
		this.nbDecisions = nbDecisions;
	}

	// BatchId

	public String getBatchId() {
		return batchId;
	}

	public void setBatchId(String batchId) {
		this.batchId = batchId;
	}

	// Version

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	// ILMT Writing
	public void writeILMTFile() {
		SoftwareIdentity softwareIdentity = new SoftwareIdentity(DecisionMetering.productId, // Persistent ID / ICP
																								// productID
				"IBM Operational Decision Manager Server", new InstanceDetails("/usr/IBM/TAMIT"));

		LoggerConfiguration config = new LoggerConfiguration("./var/ibm/slmtags", 102400, 10);
		try {
			LicenseMetricLogger.setLoggerConfiguration(config);
		} catch (MetricPersistenceException e) {
			e.printStackTrace();
		}

		LicenseMetricLogger logger = null;
		try {
			logger = LicenseMetricLogger.getLogger(softwareIdentity);
		} catch (MetricPersistenceException e) {
			e.printStackTrace();
		}

		Date startDate = Date.from(this.getStartTimeStamp().atZone(ZoneId.systemDefault()).toInstant());
		Date stopDate = Date.from(this.getStopTimeStamp().atZone(ZoneId.systemDefault()).toInstant());

		/*
		 * MILLION_MONTHLY_DECISIONS  	// We will report the QTY using a granularity of 3 decimals. So 4.999 = 4 million, 999 k executed decisions. 
		 * 								// This should keep us clean on the audit and the definition of the metric, and we only lose granularity for executions < 1k. 
		 * THOUSAND_MONTHLY_ARTIFACTS  	// We will report the QTY using a granularity of 3 decimals. So 2.503 = 2 thousand, 503 artifacts.
		 */
		double doubleMDecisions = this.getNbDecisions() / 1000000.0;
		doubleMDecisions = (double)Math.round(doubleMDecisions * 1000d) / 1000d; // To get 3 decimals
		
		double doubleKDecisions = this.getNbDecisions() / 1000.0;
		doubleKDecisions = (double)Math.round(doubleKDecisions * 1000d) / 1000d; // To get 3 decimals
		
		double doubleNbDecisions = 0;
		String metricName;
		
		// Set a minimum of 1 K usage for a batch processing
		if (doubleKDecisions < 1.0)  {
			doubleKDecisions = (float) 1.0;
		}
		
		if (doubleMDecisions >= 1.0) {
			metricName = "MILLION_MONTHLY_DECISIONS";
			doubleNbDecisions = doubleMDecisions;
		} else  {
			metricName = "THOUSAND_MONTHLY_ARTIFACTS";
			doubleNbDecisions = doubleKDecisions;
		} 
		
		Metric executionMetric = new Metric(Metric.VU_VALUE_UNIT, 
				metricName, doubleNbDecisions, startDate, stopDate); 

		try {
			logger.log(executionMetric);
			System.out.println("ILMT usage written at " + config.getOutputDir());
		} catch (MetricPersistenceException e) {
			e.printStackTrace();
		}
	}

	// Serialization

	@JsonIgnore
	public String getCSVUsage() {
		// Serialize the usage metering into a file
		StringBuffer sBuffer = new StringBuffer();
		sBuffer.append(startDateTime + ", " + nbDecisions + ", " + batchId);
		String data = sBuffer.toString();

		return data;
	}

	@JsonIgnore
	public String getJSONUsage() {
		// Serialize the usage metering into a string
		// String data = serializeToJSON(new String("MyStringObject"), "report", false);
		String data = serializeToJSON(this, "report", true);

		return data;
	}

	protected String serializeToJSON(Object object, String objectName, boolean prettyPrint) {
		ObjectMapper mapper = new ObjectMapper();
		HashMap<String, Object> results = new HashMap<String, Object>();
		results.put(objectName, object);
		String jsonInString = null;

		// @TODO Filter out unused fields in metrics
		try {
			// Convert object to JSON string and save into a file directly
			// mapper.writeValue(System.out, request);

			if (!prettyPrint) {
				// Convert object to JSON string
				jsonInString = mapper.writeValueAsString(results);
			} else {
				// Convert object to JSON string and pretty print
				jsonInString = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(results);
				// System.out.println(jsonInString);
			}

		} catch (JsonGenerationException e) {
			e.printStackTrace();
		} catch (JsonMappingException e) {
			e.printStackTrace();
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		return jsonInString;
	}

	public static DecisionMeteringReport parseAsJSON(String reportJSON) {
		ObjectMapper mapper = new ObjectMapper();
		DecisionMeteringReport reportFromJSON = null;
		try {
			// ToDo Remove this shortcut
			// mapper.writerWithDefaultPrettyPrinter();

			String originToken = "{\"report:";
			reportJSON = reportJSON.substring(reportJSON.indexOf(originToken) + originToken.length() + 2);
			reportJSON = reportJSON.substring(reportJSON.indexOf("{"));
			reportJSON = reportJSON.substring(0, reportJSON.lastIndexOf("}"));
			reportFromJSON = mapper.readValue(reportJSON, DecisionMeteringReport.class);
		} catch (JsonParseException e) {
			e.printStackTrace();
		} catch (JsonMappingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return reportFromJSON;
	}

	public UsageTimeSlice extractTimeSlice() {

		long timeSlice = (this.getStartTimeStamp().getYear() * 10000) + (this.getStartTimeStamp().getMonthValue() * 100)
				+ this.getStartTimeStamp().getDayOfMonth();
		return new UsageTimeSlice(timeSlice, this.getNbDecisions());
	}

}
