/*
* Copyright IBM Corp. 1987, 2018
* 
* Licensed to the Apache Software Foundation (ASF) under one
* or more contributor license agreements.  See the NOTICE file
* distributed with this work for additional information
* regarding copyright ownership.  The ASF licenses this file
* to you under the Apache License, Version 2.0 (the
* "License"); you may not use this file except in compliance
* with the License.  You may obtain a copy of the License at
* 
* http://www.apache.org/licenses/LICENSE-2.0
* 
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
* 
**/

package com.ibm.decisions.spark.loanvalidation;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.HashMap;

import loan.*;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.Function;

import com.ibm.decisions.spark.analytics.CoverageAnalyzer;
import com.ibm.decisions.spark.core.*;
import com.ibm.decisions.spark.generation.RequestGenerator;

public class LoanValidationSparkRunner {

	static String applicationName = "LoanValidationSparkRunner";
	static String version = "1.0";

	public static void main(String[] args) {

		// Argument processing
		//
		final String usage = "Usage:\r\n" + "     " + applicationName
				+ " --input <input-file> --output <output-file> [options]\r\n" + "     " + applicationName
				+ " --inputgen <request-nb> --output <output-file> [options]\r\n" + "     " + applicationName
				+ " --version  \r\n" + "\r\n"
				+ "     input-file:                 json or csv format file that contains the loan applications \r\n"
				+ "                                 can be a local or hdfs file\r\n"
				+ "     request-nb                  the number of requests that the program generates and then processes\\r\\n\""
				+ "                                 an alternate to the input-file read"
				+ "     output-file:                json or csv format file that contains the loan applications \r\n"
				+ "                                 can be a local or hdfs file\r\n" + "\r\n" + " options:\r\n"
				+ "     --version                   Print out the version\r\n"
				+ "     --master                    Set the master \r\n"
				+ "									local[8] for standalone \r\n"
				+ "                                 specify no master option for a submit \r\n"
				+ "     --rulecoverage              Produce the rule coverage\r\n"
				+ "     --trace                     Activate the trace\r\n";


		// No args
		if (args.length == 0) {
			System.out.println(usage);
			System.exit(0);
		}

		// --version
		if ((args.length == 1) && (args[0].compareTo("--version") == 0)) {
			System.out.println(applicationName + " version: " + version);
			System.exit(0);
		}

		//Trace
		System.out.println("Executing with parameters: " + String.join(" ",args));

		HashMap<String, String> argMap = new HashMap<String, String>();
		int nbArg = args.length;
		int iArg = 0;
		String key = null;
		String arg = null;
		String value;
		
		while (iArg < nbArg) {
			arg = args[iArg];
			
			if (arg.startsWith("--") == true) {
				// Key detected
				key = arg;
				argMap.put(key, "");
				} else {
				// Value detected
				value = arg;
				argMap.put(key, value);
				}
			
			iArg++;
		}

		String inputFile = argMap.get("--input");
		String outputFile = argMap.get("--output");
		String masterConfig = argMap.get("--master");
		
		boolean inputGeneration = argMap.get("--inputgen") != null;
		long inputGenerationNumber = 0;
		if (inputGeneration)  {
			inputGenerationNumber = Long.parseLong(argMap.get("--inputgen"));
		}
		
		boolean ruleCoverage = argMap.get("--rulecoverage") != null;
		
		SparkConf conf = new SparkConf().setAppName("Loan Validation Decision Service");
		if ((masterConfig != null) && (masterConfig.isEmpty() == false)) {
			// conf.setMaster("local[8]");
			conf.setMaster(masterConfig);
		}

		JavaSparkContext sc = new JavaSparkContext(conf);

		// 1K
		// String requestFileName =
		// "data/loanvalidation/1K/loanvalidation-requests-1K.csv";
		// String requestFileName =
		// "https://raw.githubusercontent.com/ODMDev/decisions-on-spark/master/data/loanvalidation/loanvalidation-requests-1K.json";

		// String requestFileName =
		// "hdfs://chs-xuh-576-mn001.bi.services.us-south.bluemix.net:8020/user/clsadmin/loanvalidation/loanvalidation-requests-1K.csv";

		// String requestFileName =
		// "data/loanvalidation/loanvalidation-requests-1K.csv";
		// String requestFileName =
		// "hdfs://chs-xuh-576-mn002.bi.services.us-south.bluemix.net:8020/user/clsadmin/data/loanvalidation/loanvalidation-requests-1K.csv";
		String requestFileName = inputFile;

		// String decisionFileName =
		// "data/loanvalidation/1K/loanvalidation-decisions-1K.csv";
		String decisionFileName = outputFile;

		automateDecisions(sc, requestFileName, decisionFileName, inputGeneration, inputGenerationNumber, ruleCoverage);
	}

	@SuppressWarnings("unused")
	public static void automateDecisions(JavaSparkContext sc, String requestFileName, String decisionFileName, boolean datasetGeneration, long inputGenerationNumber, boolean ruleCoverage) {

		Function<LoanValidationRequest, LoanValidationDecision> executeDecisionService = new Function<LoanValidationRequest, LoanValidationDecision>() {
			private static final long serialVersionUID = 1L;

			public LoanValidationDecision call(LoanValidationRequest request) {
				LoanValidationRESRunner runner = new LoanValidationRESRunner();
				return runner.execute(request);
			}
		};

		Function<LoanValidationDecision, String> serializeDecisionAsCSV = new Function<LoanValidationDecision, String>() {
			private static final long serialVersionUID = 1L;

			public String call(LoanValidationDecision decision) {
				return decision.serializeAsCSV();
			}
		};

		Function<LoanValidationDecision, String> serializeDecisionAsJSON = new Function<LoanValidationDecision, String>() {
			private static final long serialVersionUID = 1L;

			public String call(LoanValidationDecision decision) {
				return decision.serializeAsJSON();
			}
		};

		Function<String, LoanValidationRequest> deserializeRequestFromJSON = new Function<String, LoanValidationRequest>() {
			private static final long serialVersionUID = 1L;

			public LoanValidationRequest call(String json) {
				return LoanValidationRequest.parseAsJSON(json);
			}
		};

		Function<LoanValidationDecision, Boolean> isLoanApproved = new Function<LoanValidationDecision, Boolean>() {
			private static final long serialVersionUID = 1L;

			public Boolean call(LoanValidationDecision decision) {
				return decision.response.report.isApproved();
			}
		};

		Function<LoanRequest, Double> getLoanAmount = new Function<LoanRequest, Double>() {
			private static final long serialVersionUID = 1L;

			public Double call(LoanRequest loan) {
				return Double.valueOf(loan.getAmount());
			}
		};

		Function<LoanValidationDecision, Boolean> isYearlyInterestRatesGreaterThanX = new Function<LoanValidationDecision, Boolean>() {
			private static final long serialVersionUID = 1L;

			public Boolean call(LoanValidationDecision decision) {
				if (decision.response.report.getYearlyInterestRate() > 0.05d) {
					return true;
				} else {
					return false;
				}
			}
		};

		Function<String, LoanValidationRequest> deserializeRequestFromCSV = new Function<String, LoanValidationRequest>() {
			private static final long serialVersionUID = 1L;

			public LoanValidationRequest call(String line) {
				return LoanValidationRequest.parseAsCSV(line);
			}
		};

		long startTime = System.currentTimeMillis();

		// Local dir
		Path currentRelativePath = Paths.get(".");
		String s = currentRelativePath.toAbsolutePath().toString();
		System.out.println("Executing from: " + s);

		// Input data file
		System.out.println("Loading dataset file: " + requestFileName);

		// RDD creation
		JavaRDD<LoanValidationRequest> requestRDD = null;
		if (datasetGeneration == false) {
			JavaRDD<String> requestData = sc.textFile(requestFileName);
			requestData.count();
			if (requestFileName.endsWith(".json")) {
				requestRDD = requestData.map(deserializeRequestFromJSON);
				requestRDD.count();
			}
			if (requestFileName.endsWith(".csv")) {
				requestRDD = requestData.map(deserializeRequestFromCSV);
				requestRDD.count();
			}

			if ((requestFileName.endsWith(".json") == false) && (requestFileName.endsWith(".csv") == false)) {
				System.out.println("input file must have a .csv or .json suffix");
				System.exit(0);
			}
		} else {
			RequestGenerator requestGenerator = new RequestGenerator();
			requestRDD = requestGenerator.generateRandomRequestRequestRDD(inputGenerationNumber, sc);
		}

		requestRDD.count();
				
		// Produce a RDD of decisions
		//
		System.out.println("Starting decision automation...");
		System.out.println("Dataset generation: " + datasetGeneration);
		JavaRDD<LoanValidationDecision> decisions = requestRDD.map(executeDecisionService).cache();
		System.out.println("Automation ended with " + decisions.count()  + " decisions");

		long stopTime = System.currentTimeMillis();
		
		//Coverage
		//
		if (ruleCoverage) {
			CoverageAnalyzer coverageAnalyzer = new CoverageAnalyzer();
			CoverageAnalyzer.processRuleStatistics(sc, decisions) ;
		}
				
		// Write decision files
		//
		JavaRDD<String> serializedAnswers = null;
		if (decisionFileName.endsWith(".json")) {
			serializedAnswers = decisions.map(serializeDecisionAsJSON).cache();
		}
		if (decisionFileName.endsWith(".csv")) {
			serializedAnswers = decisions.map(serializeDecisionAsCSV).cache();
		}
		if ((decisionFileName.endsWith(".json") == false) && (decisionFileName.endsWith(".csv") == false)) {
			System.out.println("output file must have a .csv or .json suffix");
			System.exit(0);
		}

		Util.DeleteFileDirectory(decisionFileName);
		serializedAnswers.coalesce(1).saveAsTextFile(decisionFileName);

		// Display Metrics
		System.out.println("");
		System.out.println("Decision batch metrics");
		System.out.println("Number of loan applications processed: " + decisions.count() + " in "
				+ (stopTime - startTime) + " ms");
		double dps = decisions.count() * 1000 / (stopTime - startTime);
		System.out.println("Number of decision per sec: " + dps);

		// Compute the KPI that counts the number of approved loan applications
		// The KPI reduces the approval metric
		long approvedLoanCount = decisions.filter(isLoanApproved).count();

		// Count the number of loan applications with a credit score under 200
		long yearlyInterestRateGreaterThan005Count = decisions.filter(isLoanApproved)
				.filter(isYearlyInterestRatesGreaterThanX).count();

		System.out.println(
				"Number of approved loan applications: " + approvedLoanCount + " on a " + decisions.count() + " total");
		System.out.println(
				"Number of loans approved with a YearlyInterestRate > 5%: " + yearlyInterestRateGreaterThan005Count);

	}

}
