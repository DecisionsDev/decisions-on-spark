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

import java.io.Serializable;
import java.util.Date;
import org.joda.time.Instant;

import loan.*;

public class LoanValidationRequest implements Serializable  {

	private static final long serialVersionUID = 1L;
	//Operation signature
	public Borrower borrower;
	public LoanRequest loanRequest;
	
	public LoanValidationRequest() {
	}
	
	public Borrower getBorrower() {
		return borrower;
	}
	
	public LoanRequest getLoan() {
		return loanRequest;
	}
	
	public void setBorrower(Borrower borrower) {
		this.borrower = borrower;
	}
	
	public void setLoan(LoanRequest loanRequest) {
		this.loanRequest = loanRequest;
	}
	
	
	public LoanValidationRequest(Borrower borrower, LoanRequest loan) {
		this.borrower = borrower;
		this.loanRequest = loan;
	}
	
	//Borrower(String firstName, String lastName, Date birthDate,	String SSNCode)
	public LoanValidationRequest(String firstName, String lastName, int borrowerCreditScore, int borrowerYearlyIncome, Date birthDate, String SSNCode, String zipCode, int loanAmount, Date startDate, int numberOfMonthlyPayments, double loanToValue) {
		this.borrower = new Borrower(firstName, lastName, birthDate, SSNCode);
		borrower.setCreditScore(borrowerCreditScore);
		borrower.setYearlyIncome(borrowerYearlyIncome);
		borrower.setZipCode(zipCode);
		//public LoanRequest(Date startDate, int numberOfMonthlyPayments, int amount,double loanToValue)
		this.loanRequest = new LoanRequest(startDate, numberOfMonthlyPayments, loanAmount, loanToValue);
	}
	
	public static LoanValidationRequest parseAsCSV(String s) {
		/*
		 * buffer.append(this.borrower.getFirstName() + ", ");
			buffer.append(this.borrower.getLastName() + ", ");
			buffer.append(this.borrower.getBirthDate() + ", ");
			buffer.append(this.borrower.getSSN() + ", ");
			buffer.append(this.borrower.getZipCode() + ", ");
			buffer.append(this.borrower.getCreditScore() + ", ");
			buffer.append(this.borrower.getYearlyIncome() + ", ");
			buffer.append(this.loanRequest.getAmount() + ", ");
			buffer.append(this.loanRequest.getStartDate() + ", ");
			buffer.append(this.loanRequest.getDuration() + ", ");
			buffer.append(this.loanRequest.getLoanToValue());
		 */
		
		String[] tokens = s.split(",");

		String borrowerFirstName = tokens[0];
		String borrowerLastName = tokens[1];
		String birthDateString = tokens[2].trim();
		Date birthDate = Instant.parse(birthDateString).toDate();
		String SSNCode = tokens[3].trim();
		String zipCode = tokens[4].trim();
		
		int borrowerCreditScore = Integer.parseInt(tokens[5].trim());
		int borrowerYearlyIncome = Integer.parseInt(tokens[6].trim());
		
		//Loan
		int amount = Integer.parseInt(tokens[7].trim());
		String startDateString = tokens[8].trim();
		Date startDate = Instant.parse(startDateString).toDate();
		
		int numberOfMonthlyPayments = Integer.parseInt(tokens[9].trim());
		double loanToValue = Double.parseDouble(tokens[10].trim());

		Borrower borrower = new Borrower(borrowerFirstName, borrowerLastName, birthDate,
				SSNCode);
		borrower.setZipCode(zipCode);
		borrower.setCreditScore(borrowerCreditScore);
		borrower.setYearlyIncome(borrowerYearlyIncome);
		
		LoanRequest loanRequest = new LoanRequest(startDate, numberOfMonthlyPayments, amount, loanToValue);
		LoanValidationRequest request = new LoanValidationRequest(borrower, loanRequest);

		return request;
	}

	public String serializeAsJSON() {
		JSONSerializer jsonSerializer = new JSONSerializer();
		String json = jsonSerializer.serialize(this);
		return json;
	}
	
	public String serializeAsCSV() {
			StringBuffer buffer = new StringBuffer();
			buffer.append(this.borrower.getFirstName() + ", ");
			buffer.append(this.borrower.getLastName() + ", ");
			buffer.append(this.borrower.getBirthDate().toInstant() + ", ");
			buffer.append(this.borrower.getSSN() + ", ");
			buffer.append(this.borrower.getZipCode() + ", ");
			buffer.append(this.borrower.getCreditScore() + ", ");
			buffer.append(this.borrower.getYearlyIncome() + ", ");
			buffer.append(this.loanRequest.getAmount() + ", ");
			buffer.append(this.loanRequest.getStartDate().toInstant() + ", ");
			buffer.append(this.loanRequest.getDuration() + ", ");
			buffer.append(this.loanRequest.getLoanToValue());
			//@ToDo Complete the list
			return buffer.toString();
		}

}
