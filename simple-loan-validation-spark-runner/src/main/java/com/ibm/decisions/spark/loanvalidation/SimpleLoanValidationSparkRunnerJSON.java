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
import loan.*;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.Function;

import com.ibm.decisions.spark.core.*;

public class SimpleLoanValidationSparkRunnerJSON {

	public static void main(String[] args) {
		
		SparkConf conf = new SparkConf().setAppName("Loan Validation Decision Service").setMaster("local[8]");
		JavaSparkContext sc = new JavaSparkContext(conf);
	
		//1K
		String requestFileName = "../data/loanvalidation/loanvalidation-requests-1K.json";
		String decisionFileName = "../data/loanvalidation/loanvalidation-decisions-1K.json";
		automateDecisions(sc, requestFileName, decisionFileName);
		
		//10K
		/*
		requestFileName = "data/loanvalidation/10K/loanvalidation-requests-10K.csv";
		decisionFileName = "data/loanvalidation/10K/loanvalidation-decisions-10K.json";
		automateDecisions(sc, requestFileName, decisionFileName);
		*/
		
		//100K
		/*
		requestFileName = "data/loanvalidation/100K/loanvalidation-requests-100K.csv";
		decisionFileName = "data/loanvalidation/100K/loanvalidation-decisions-100K.json";
		automateDecisions(sc, requestFileName, decisionFileName);
		*/
		
		//200K
		/*
		requestFileName = "data/loanvalidation/200K/loanvalidation-requests-200K.csv";
		decisionFileName = "data/loanvalidation/200K/loanvalidation-decisions-200K.json";
		automateDecisions(sc, requestFileName, decisionFileName);
		*/
	}
	
	@SuppressWarnings("unused")
	public static void automateDecisions(JavaSparkContext sc, String requestFileName, String decisionFileName) {
		
		Function<LoanValidationRequest, LoanValidationDecision> executeDecisionService = new Function<LoanValidationRequest, LoanValidationDecision>() {
			private static final long serialVersionUID = 1L;

			public LoanValidationDecision call(LoanValidationRequest request) {
				LoanValidationRESRunner runner = new LoanValidationRESRunner();
				return runner.execute(request);
			}
		};
		
		Function<LoanValidationDecision, String> serializeDecisionAsJSON = new Function<LoanValidationDecision, String>() {
			private static final long serialVersionUID = 1L;

			public String call(LoanValidationDecision decision) {
				return decision.serializeAsJSON();
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
		
		Function<String, LoanValidationRequest> deserializeRequestFromJSON = new Function<String, LoanValidationRequest>() {
			private static final long serialVersionUID = 1L;

			public LoanValidationRequest call(String json) {
				return LoanValidationRequest.parseAsJSON(json);
			}
		};
		
		long startTime = System.currentTimeMillis();

		// Local dir
		Path currentRelativePath = Paths.get(".");
		String s = currentRelativePath.toAbsolutePath().toString();
		System.out.println("Executing from: " + s);

		// Input data file
		System.out.println("Loading csv file: " + requestFileName);
		
		// RDD creation
		JavaRDD<String> requestData = sc.textFile(requestFileName);
		requestData.count();
		JavaRDD<LoanValidationRequest> requestRDD = requestData.map(deserializeRequestFromJSON);
		
		// Produce a RDD of decisions
		//
		System.out.println("Starting decision automation...");
		JavaRDD<LoanValidationDecision> decisions = requestRDD.map(executeDecisionService).cache();
		System.out.println("Decision making ended");
		
		long stopTime = System.currentTimeMillis();
		
		// Write decision files
		//
		JavaRDD<String> csvAnswers = decisions.map(serializeDecisionAsJSON).cache();
		Util.DeleteFileDirectory(decisionFileName);
		csvAnswers.coalesce(1).saveAsTextFile(decisionFileName);

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
		long yearlyInterestRateGreaterThan005Count = decisions.filter(isLoanApproved).filter(isYearlyInterestRatesGreaterThanX).count();

		System.out.println("Number of approved loan applications: " + approvedLoanCount + " on a " + decisions.count() + " total");
		System.out.println(
				"Number of loans approved with a YearlyInterestRate > 5%: " + yearlyInterestRateGreaterThan005Count);
		
	}

}
