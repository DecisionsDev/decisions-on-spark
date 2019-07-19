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
	
	private UsageTransfertJournal transfertJournal;

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
		
		//Testing report creation and writing
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
		
		//decisionMetering.writeCSV();
		decisionMetering.writeJSON();
		
		report.writeILMTFile();
		
		//

		String jsonReport = report.getJSONUsage();
		System.out.println(jsonReport);

		DecisionMeteringReport report2 = DecisionMeteringReport.parseAsJSON(jsonReport);
		String jsonReport2 = report2.getJSONUsage();
		System.out.println(jsonReport2);

		//Test with a stable report		
		
		DecisionMeteringReport report3 = decisionMetering.createUsageReport("123");
		decisionMetering.setDecisionMeteringReport(report3); //To be checked
		LocalDateTime start = LocalDateTime.of(2018, 12, 30, 12, 49, 55);
		report3.setStartTimeStamp(start);
		
		// Perform the batch processing...
		report3.setNbDecisions(400000);
		LocalDateTime stop = LocalDateTime.of(2018, 12, 30, 12, 55, 00);
		report3.setStopTimeStamp(stop);
		decisionMetering.writeJSON(report3);
		
		decisionMetering.initializeTransfertJournal();
		
		decisionMetering.scanDirectory();
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

	public void writeHDFS() {
		java.util.ArrayList<String> usageArrayList = new java.util.ArrayList<String>();
		usageArrayList.add(report.getCSVUsage());
		JavaRDD<String> decisionUsageRDD = sc.parallelize(usageArrayList);
		decisionUsageRDD.coalesce(1).saveAsTextFile(getFilePath());
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

	private Function<DecisionMeteringReport, UsageTimeSlice> extractUsageTimeSlice = new Function<DecisionMeteringReport, UsageTimeSlice>() {
		private static final long serialVersionUID = 1L;

		public UsageTimeSlice call(DecisionMeteringReport decisionMeteringReport) {
			UsageTimeSlice usageTimeSlice = decisionMeteringReport.extractTimeSlice();
			return usageTimeSlice;
		}
	};
	
	private VoidFunction<DecisionMeteringReport> writeIntoILMT = new VoidFunction<DecisionMeteringReport>() {
		private static final long serialVersionUID = 1L;

		public void call(DecisionMeteringReport decisionMeteringReport) {
			
			JournalEntry entry = new JournalEntry((long)1, (long)2);
			transfertJournal.add(entry);
			
			ILMTMetering.write(decisionMeteringReport);
		}
	};
	
	public void initializeTransfertJournal()  {
		this.transfertJournal = UsageTransfertJournal.GetInstance();
	}

	Function<DecisionMeteringReport, Boolean> isNotTransfered = new Function<DecisionMeteringReport, Boolean>() {
		private static final long serialVersionUID = 1L;

		public Boolean call(DecisionMeteringReport meteringReport) {
			UsageTransfertJournal journal = UsageTransfertJournal.GetInstance();
			boolean found = journal.containsKey(-1); //TODO Finish the implementation decisionMeteringId
			if (found) {
				System.out.println(meteringReport + " already marked as transfered in the journal ");
			} else {
				System.out.println(meteringReport + " not yet marked as transfered in the journal");
			}		
			
			return !found;
		}
	};
	
	Function<DecisionMeteringReport, Boolean> isTransfered = new Function<DecisionMeteringReport, Boolean>() {
		private static final long serialVersionUID = 1L;

		public Boolean call(DecisionMeteringReport meteringReport) {
			System.out.println(meteringReport + " transfered?");
			return false;
		}
	};
	
	public void scanDirectory() {
		SparkConf conf = new SparkConf().setAppName("Decision Metering Aggregation").setMaster("local[8]");
		sc = new JavaSparkContext(conf);

		// Open each file present in the directory and create a report rdd
		JavaPairRDD<String, String> reportFiles = sc.wholeTextFiles(getFilePath() + "//*.json");
		System.out.println(reportFiles.count() + " reports found at " + getFilePath() + "//*.json");

		reportFiles.collect().forEach(System.out::println);

		JavaRDD<DecisionMeteringReport> meteringReports = reportFiles.values().map(deserializeReportAsJSON);
		System.out.println("All metering reports " + meteringReports.count());
		
		//Filter out metering reports that were already transfered into ILMT file
		JavaRDD<DecisionMeteringReport> untransferedMeteringReports = meteringReports.filter(isNotTransfered);
		System.out.println("Untransfered metering reports XXZ2: " + untransferedMeteringReports.count());
		
		//Filter out metering reports that were not already transfered into ILMT file
		//JavaRDD<DecisionMeteringReport> transferedMeteringReports = meteringReports.filter(isTransfered);
		//System.out.println("XXX-Transfered metering reports: " + transferedMeteringReports.count());
		
		//Create an ILMT entry for each job usage
		untransferedMeteringReports.foreach(writeIntoILMT);
		
		JavaRDD<UsageTimeSlice> usageTimeSlices = meteringReports.map(extractUsageTimeSlice);
		System.out.println("Usage Time Slices: " + usageTimeSlices.count());
		System.out.println("MeteringReports: " + meteringReports.count());

		// reduce>
		JavaPairRDD<Long, Long> usageTimeSlicePairs = usageTimeSlices
				.mapToPair(t -> new Tuple2<Long, Long>(t.getTimeSlice(), t.getCount())); // ToDo
		System.out.println("Usage Time Slice Pairs: " + usageTimeSlicePairs.count());

		JavaPairRDD<Long, Long> reducedTimeSlicePairs = usageTimeSlicePairs
				.reduceByKey(new Function2<Long, Long, Long>() {
					private static final long serialVersionUID = 1L;

					public Long call(Long a, Long b) {
						return a + b;
					}
				});

		System.out.println("Usage Time Slice Pairs: " + reducedTimeSlicePairs.count());

		reducedTimeSlicePairs.collect()
				.forEach((a) -> System.out
						.println("Consumption in " + a._1 / 10000 + "-" + (a._1 / 100 - (a._1 / 100 / 100 * 100)) + "-"
								+ (a._1 - (a._1 / 100 * 100)) + " day : " + a._2 + " executions"));

	}

}
