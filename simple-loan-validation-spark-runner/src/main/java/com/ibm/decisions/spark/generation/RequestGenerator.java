package com.ibm.decisions.spark.generation;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.Function;

import com.ibm.decisions.spark.loanvalidation.LoanValidationRequest;
import com.ibm.decisions.spark.core.*;

public class RequestGenerator {
	
	private JavaSparkContext sc;
	
	public static Function<LoanValidationRequest, String> serializeRequestAsJSON = new Function<LoanValidationRequest, String>() {
		public String call(LoanValidationRequest request) {
			return request.serializeAsJSON();
		}
	};
	
	public static Function<LoanValidationRequest, String> serializeRequestAsCSV = new Function<LoanValidationRequest, String>() {
		public String call(LoanValidationRequest request) {
			return request.serializeAsCSV();
		}
	};
	
	@SuppressWarnings("unused")
	public static void main(String[] args) {

		Logger log = LogManager.getRootLogger();
		log.setLevel(Level.WARN);
		
		Logger decisionBatch = LogManager.getLogger("decisionBatch");
		decisionBatch.setLevel(Level.TRACE);
				   
		SparkConf conf = new SparkConf().setAppName("Loan Validation Decision Service").setMaster("local[8]");;
		JavaSparkContext sc = new JavaSparkContext(conf);
		
		RequestGenerator requestGenerator = new RequestGenerator(sc);
		JavaRDD<LoanValidationRequest> requestRDD = requestGenerator.generateRandomRequestRequestRDD(1000);
		requestGenerator.writeRandomRequestFiles(requestRDD, "1K");
		
		requestRDD = requestGenerator.generateRandomRequestRequestRDD(10000);
		requestGenerator.writeRandomRequestFiles(requestRDD, "10K");
		
		requestRDD = requestGenerator.generateRandomRequestRequestRDD(100000);
		requestGenerator.writeRandomRequestFiles(requestRDD, "100K");
		
		/*
		requestRDD = requestGenerator.generateRandomRequestRequestRDD(200000);
		requestGenerator.writeRandomRequestFiles(requestRDD, "200K");
		

		requestRDD = requestGenerator.generateRandomRequestRequestRDD(500000);
		requestGenerator.writeRandomRequestFiles(requestRDD, "500K");
		
		requestRDD = requestGenerator.generateRandomRequestRequestRDD(1000000);
		requestGenerator.writeRandomRequestFiles(requestRDD, "1M");
		*/

	}
	
	public RequestGenerator(JavaSparkContext sc)  {
		this.sc = sc;
	}

	@SuppressWarnings("unused")
	public void writeRandomRequestFiles(JavaRDD<LoanValidationRequest> requestRDD, String sizeAlias) {

		JavaRDD<String> requestJSONRDD = requestRDD.map(serializeRequestAsJSON);
		writeRandomRequestFile(requestJSONRDD, sizeAlias, ".json");
		
		JavaRDD<String> requestCSVRDD = requestRDD.map(serializeRequestAsCSV);
		writeRandomRequestFile(requestCSVRDD, sizeAlias, ".csv");
		
	}
	
	@SuppressWarnings("unused")
	public void writeRandomRequestFile(JavaRDD<String> serializedRequestRDD, String sizeAlias, String serializationFileType) {

		// Local dir
		Path currentRelativePath = Paths.get(".");
		String s = currentRelativePath.toAbsolutePath().toString();
		System.out.println("Executing from: " + s);
		String root = "data/loanvalidation/";
			
		//Coalescence
		int coalescence = 1;
		long datasetCount = serializedRequestRDD.count();
		long maxFileSize = 100000;
		if (datasetCount < maxFileSize) {
			coalescence = 1;
		} else {
			coalescence = (int) (datasetCount/maxFileSize);
		}
		
		//Serialize decision requests
		String outputFilename = root + sizeAlias + File.separatorChar + "loanvalidation-requests-" + sizeAlias + serializationFileType;
		Util.DeleteFileDirectory(outputFilename);
		serializedRequestRDD.coalesce(coalescence).saveAsTextFile(outputFilename);
		System.out.println("Number of loan application requests generated: " + serializedRequestRDD.count() );
		System.out.println("Request file available at: " + outputFilename); 
		
	}
	
	
	public JavaRDD<LoanValidationRequest> generateRandomRequestRequestRDD(long nbGenerated) {

		// RDD creation
		JavaRDD<String> requestData = null;
		JavaRDD<LoanValidationRequest> requestRDD = null;
		
		requestRDD = RequestGenerator.generateRandomRequests(sc, nbGenerated);
		System.out.println("Generating randomly " + nbGenerated + " decision requests");
		
		return requestRDD;
	}
	
	public static LoanValidationRequest generateRandomRequest(JavaSparkContext sc) {

		// Borrower name
		String borrowerFirstName = "John";
		String borrowerLastName = "Doe";

		//Credit score between 300 and 850
		int borrowerCreditScore = (int) (300 + ((850 - 300)*Math.random()));
		int borrowerYearlyIncome = (int) (400000 * Math.random());
		int loanAmount = (int) (1500000 * Math.random());
		int loanDuration = (int) (360 * Math.random()); //60 to 360 months
		double yearlyInterestRate = 0.03d + ((0.03d * Math.random()));

		Date birthDate = loan.DateUtil.makeDate(2018 - (int)(Math.random() * 120), (int)(Math.random() * 12), (int)(Math.random() * 29));
		String SSNCode = "471-78-1234"; //@ToDo generate more cases
		String zipCode = "12345"; //Must be 5 digits
		double loanToValue = 1.0 + (Math.random()) ; //@ToDo Check accuracy
		Date startDate = loan.DateUtil.makeDate((int)(2018 + (5*Math.random())), (int)(Math.random() * 12), (int)(Math.random() * 29));
		
		return new LoanValidationRequest(borrowerFirstName, borrowerLastName, borrowerCreditScore, borrowerYearlyIncome, birthDate, SSNCode, zipCode, loanAmount, startDate, loanDuration, loanToValue);
	}
	
	public static JavaRDD<LoanValidationRequest> generateRandomRequests(JavaSparkContext sc, long requestTargetNumber) {
		JavaRDD<LoanValidationRequest> requests;
		ArrayList<LoanValidationRequest> requestArray = new ArrayList<LoanValidationRequest>();
		for (int i=0; i < requestTargetNumber; i++) {
			LoanValidationRequest request = generateRandomRequest(sc);
			requestArray.add(request);
		}
		
		return sc.parallelize(requestArray);
	}

}
