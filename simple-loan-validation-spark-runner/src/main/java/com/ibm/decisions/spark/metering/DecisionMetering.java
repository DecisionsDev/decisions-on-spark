package com.ibm.decisions.spark.metering;

import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.nio.file.Files;
import java.time.LocalDateTime;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.Function2;
import org.apache.spark.api.java.function.VoidFunction;

import scala.Tuple2;

public class DecisionMetering implements Serializable {

	private static final long serialVersionUID = -7905907440371034978L;
	protected static final String productId = "b1a07d4dc0364452aa6206bb6584061d";         //ODM Persistent ID / ICP productID
	private transient JavaSparkContext sc;
	private String version = "1.0";
	private String filePath;

	private DecisionMeteringReport report;

	public DecisionMetering(JavaSparkContext sc, String filePath) {
		this.sc = sc;
		this.filePath = filePath;
	}

	public DecisionMetering(String filePath) {
		this.filePath = filePath;
	}

	public static void main(String[] args) {
		DecisionMetering decisionMetering = new DecisionMetering("dba-metering");
		
		//Testing report creation and writing the native ILMT file
		//

		String batchId = "12221212";
		DecisionMeteringReport report = decisionMetering.createUsageReport(batchId);
		// Emulate the batch processing...
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		report.setNbDecisions(4999111);
		report.setStopTimeStamp();
		report.writeILMTFile();

		// Test JSON serialization and deserialization
		//
		
		// Write the same usage in JSON
		//decisionMetering.writeCSV();
		decisionMetering.writeJSON();

		// Read the same usage in JSON
		String jsonReport = report.getJSONUsage();
		System.out.println(jsonReport);

		// Load the same usage in JSON
		DecisionMeteringReport report2 = DecisionMeteringReport.parseAsJSON(jsonReport);
		String jsonReport2 = report2.getJSONUsage();
		System.out.println(jsonReport2);

		//Test with a single report and multiple usages
		//
		
		DecisionMeteringReport report3 = decisionMetering.createUsageReport("123");
		decisionMetering.setDecisionMeteringReport(report3); //To be checked
		LocalDateTime start = LocalDateTime.of(2018, 12, 30, 12, 49, 55);
		report3.setStartTimeStamp(start);
		
		// Perform the batch processing...
		report3.setNbDecisions(400000);
		LocalDateTime stop = LocalDateTime.of(2018, 12, 30, 12, 55, 00);
		report3.setStopTimeStamp(stop);
		decisionMetering.writeJSON(report3);
		
	}

	public DecisionMeteringReport createUsageReport(String batchId) {
		report = new DecisionMeteringReport(batchId);
		return report;
	}
	
	public void setDecisionMeteringReport(DecisionMeteringReport report) {
		this.report = report;
	}
	
	public DecisionMeteringReport getReport() {
		return report;
	}

	public void writeHDFS() {
		java.util.ArrayList<String> usageArrayList = new java.util.ArrayList<String>();
		usageArrayList.add(report.getCSVUsage());
		JavaRDD<String> decisionUsageRDD = sc.parallelize(usageArrayList);
		decisionUsageRDD.coalesce(1).saveAsTextFile(getFilePath());
	}

	public void writeHDFS2() {
		URI uri = URI.create("hdfs://host:port/file path" + "/" + getFilePath());
		Configuration conf = new Configuration();
		FileSystem file;
		try {
			file = FileSystem.get(uri, conf);
			FSDataInputStream in = file.open(new org.apache.hadoop.fs.Path(uri));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private String getFilePath() {
		return filePath;
	}

	private String getReportCSVFilePath() {
		return filePath + "/ibm-decisions-metering-" + getFormatedStartTimeStamp() + ".csv";
	}

	private String getReportJSONFilePath() {
		return filePath + "/ibm-decisions-metering-" + getFormatedStartTimeStamp() + ".json";
	}

	private String getFormatedStartTimeStamp() {
		java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter
				.ofPattern("yyyy-MM-dd--H-m-s--S");
		return report.getStartTimeStamp().format(formatter);
	}

	private String getFormatedStopTimeStamp() {
		java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter
				.ofPattern("yyyy-MM-dd--H-m-s--S");
		return report.getStopTimeStamp().format(formatter);
	}

	public void writeCSV() {

		// Serialize the usage metering into a file
		String data = report.getCSVUsage();

		// Use try-with-resource to get auto-closeable writer instance
		try {
			Files.write(java.nio.file.Paths.get(getReportCSVFilePath()), data.getBytes());
			System.out.println("Usage written at " + getFilePath());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void writeJSON() {

		 writeJSON(report);
	}
	
	public void writeJSON(DecisionMeteringReport theReport) {

		// Serialize the usage metering into a file
		String data = theReport.getJSONUsage();

		// Use try-with-resource to get auto-closeable writer instance
		try {
			Files.write(java.nio.file.Paths.get(getReportJSONFilePath()), data.getBytes());
			System.out.println("Usage written at " + getReportJSONFilePath());
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println("Make sure that folders have been already created on the file path");
		}
	}

	private Function<String, DecisionMeteringReport> deserializeReportAsJSON = new Function<String, DecisionMeteringReport>() {
		private static final long serialVersionUID = 1L;

		public DecisionMeteringReport call(String decisionMeteringReportJSON) {
			System.out.println("decisionMeteringReportJSON:" + decisionMeteringReportJSON);

			return DecisionMeteringReport.parseAsJSON(decisionMeteringReportJSON);
		}
	};

}
