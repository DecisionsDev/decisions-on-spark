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

import java.util.Date;
import java.util.HashMap;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JSONSerializer {

	public JSONSerializer() {
		
	}
	
	public static void main(String[] args) {
		JSONSerializer jsonSerialier = new JSONSerializer();
		
		java.util.Date birthDate = loan.DateUtil.makeDate(1950, 1, 1);
		java.util.Date startDate = new Date(); //now
		String SSNCode = "1222";
		String zipCode = "3333";
		LoanValidationRequest request = new LoanValidationRequest("John", "Doe", 550, 80000, birthDate, SSNCode, zipCode,
				250000, startDate, 240, 0.05d);
		//	public LoanValidationRequest(String firstName, String lastName, int borrowerCreditScore, int borrowerYearlyIncome, Date birthDate, String SSNCode, int loanAmount, int numberOfMonthlyPayments, double loanToValue) {

		//LoanValidationDecision decision = new LoanValidationDecision(request, response);
		//Decision
		System.out.println(jsonSerialier.serialize(request));
		
		//Request
		System.out.println(request.serializeAsJSON());

	}
	
	public String serialize(LoanValidationDecision decision) {
		return serialize(decision, "decision", false); 
	}
	
	public String serialize(LoanValidationRequest request) {
		return serialize(request, "request", false); 
	}
	
	protected String serialize(Object object, String objectName, boolean prettyPrint) {
		JSONSerializer runner = new JSONSerializer();
		ObjectMapper mapper = new ObjectMapper();
		HashMap<String, Object> results = new HashMap<String, Object>();
		results.put(objectName, object);
		String jsonInString = null;
		try {
			// Convert object to JSON string and save into a file directly
			// mapper.writeValue(System.out, request);

			if (!prettyPrint) {
				// Convert object to JSON string
				jsonInString = mapper.writeValueAsString(results);
			} else {
				// Convert object to JSON string and pretty print
				jsonInString =
						mapper.writerWithDefaultPrettyPrinter().writeValueAsString(results);
				// System.out.println(jsonInString);
			}

		} catch (JsonGenerationException e) {
			e.printStackTrace();
		} catch (JsonMappingException e) {
			e.printStackTrace();
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return jsonInString;
	}

}

