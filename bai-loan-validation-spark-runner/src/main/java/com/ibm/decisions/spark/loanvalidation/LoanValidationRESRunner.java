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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import ilog.rules.res.model.IlrPath;
import ilog.rules.res.session.IlrJ2SESessionFactory;
import ilog.rules.res.session.IlrSessionRequest;
import ilog.rules.res.session.IlrSessionResponse;
import ilog.rules.res.session.IlrStatelessSession;
import java.util.Map;
import ilog.rules.res.session.config.IlrPluginConfig;
import ilog.rules.res.session.config.IlrSessionFactoryConfig;
//import com.ibm.dba.bai.events.odm.OdmEventBuilder;
import ilog.rules.res.session.config.IlrPersistenceType;
import java.io.File;
import java.io.PrintWriter;
import loan.Borrower;
import loan.LoanRequest;

import static java.lang.Thread.*;

public class LoanValidationRESRunner {

	private static IlrJ2SESessionFactory ruleSessionFactory = null;
	
	private static IlrJ2SESessionFactory GetRuleSessionFactory() {
		if (ruleSessionFactory == null) {
			
			IlrSessionFactoryConfig config = ruleSessionFactory.createDefaultConfig();
			System.out.println("===== getFactory -> after default config creaation");
			
			config.getXUConfig().getConnectionPoolConfig().setMaxSize(50);
			PrintWriter writer = new PrintWriter(System.out);
			config.getXUConfig().setLogWriter(writer);
			config.getXUConfig().getPersistenceConfig().setPersistenceType(IlrPersistenceType.MEMORY);
			config.getXUConfig().getPersistenceConfig().getFilePersistenceConfig()
					.setDirectory(new File("repo/"));
			if(config.getClass().getClassLoader().getResourceAsStream("plugin-configuration.properties")!=null) {
				if (config.getXUConfig().getPluginConfigs() != null) {

					IlrPluginConfig baiIlrPluginConfig = config.getXUConfig().createPluginConfig("ODMEmitterForBAI");

					List<IlrPluginConfig> pluginsConfigList = new ArrayList<IlrPluginConfig>();
					pluginsConfigList.add(baiIlrPluginConfig);
					config.getXUConfig().setPluginConfigs(pluginsConfigList);
				}
			} else {
				System.err.println("Error: ODM BAI plugin-configuration.properties file not found");
				System.exit(0);
				}
			ruleSessionFactory = new IlrJ2SESessionFactory(config);
		}
		return ruleSessionFactory;
	}
	
	public static void main(String[] args) {
		LoanValidationRESRunner runner = new LoanValidationRESRunner();
		
		//Borrower
		java.util.Date birthDate = loan.DateUtil.makeDate(1950, 1, 1);
		loan.Borrower borrower = new loan.Borrower("Smith", "John", birthDate,
					"123121234");
		borrower.setZipCode("12345"); //12345
		borrower.setCreditScore(800);
		borrower.setYearlyIncome(100000);
		
		//Loan
		java.util.Date loanDate = new java.util.Date();
		loan.LoanRequest loan = new loan.LoanRequest(loanDate, 48, 20000, 0.05);
		
		//LoanValidationRequest request = new LoanValidationRequest("John", "Doe", 550, 80000, birthDate, "123-121234", 250000, 240, 0.05d);
		LoanValidationRequest request = new LoanValidationRequest(borrower, loan);
		LoanValidationDecision decision = runner.execute(request);

	//	System.out.println("Messages:" + decision.response.report.getMessage());
	//	System.out.println(decision.serializeAsJSON());
	}
	
	public LoanValidationDecision executeAsString(String s) {

		LoanValidationRequest request = LoanValidationRequest.parseAsCSV(s);
		return execute(request);
	}
	
	public LoanValidationDecision execute(LoanValidationRequest request) {

		LoanValidationResponse response = execute2(request);
		
		double yearlyRepayment = response.getReport().getMonthlyRepayment();
		
//		System.out.print("Loan approved=" + response.report.isApproved() + " with a yearly repayment=" + yearlyRepayment + " insurance required:" + response.getReport().isInsuranceRequired() + " messages= " + response.getReport().getMessages());
//		System.out.println(" executed in thread " + Thread.currentThread().getName());
		
		return new LoanValidationDecision(request, response);
	}
	
	public LoanValidationResponse execute2(LoanValidationRequest request) {
		try {
			IlrSessionResponse sessionResponse = execute(request.borrower, request.loanRequest);
			// Hack to have something working
		    //	Thread.sleep(2000);
			//long t3 = System.currentTimeMillis();
			LoanValidationResponse miniLoanResponse = new LoanValidationResponse(sessionResponse);
			return miniLoanResponse;

		} catch (Exception exception) {
			exception.printStackTrace(System.err);
		}
		return null;
	}
	
	public IlrSessionResponse execute(Borrower borrower, LoanRequest loan) {
		try {

			IlrJ2SESessionFactory sessionFactory =  GetRuleSessionFactory();

			// Creating the decision request
			IlrSessionRequest sessionRequest = sessionFactory.createRequest();
			String rulesetPath = "/loanvalidation/loan_validation_with_score_and_grade";
			sessionRequest.setRulesetPath(IlrPath.parsePath(rulesetPath));

			sessionRequest.setTraceEnabled(true);
			//sessionRequest.getTraceFilter().setInfoAllFilters(true);
			sessionRequest.getTraceFilter().setInfoRules(true);
			sessionRequest.getTraceFilter().setInfoRulesNotFired(true);
			sessionRequest.getTraceFilter().setInfoTasks(true);
			sessionRequest.getTraceFilter().setInfoTotalTasksNotExecuted(true);
			sessionRequest.getTraceFilter().setInfoExecutionEvents(true);

			Map<String, Object> inputParameters = sessionRequest
					.getInputParameters();
			inputParameters.put("loan", loan);
			inputParameters.put("borrower", borrower);

			// Creating the rule session
			IlrStatelessSession session = sessionFactory
					.createStatelessSession();

			IlrSessionResponse response = session.execute(sessionRequest);
			
			return response;

		} catch (Exception exception) {
			exception.printStackTrace(System.err);
		}
		return null;
	}

}
