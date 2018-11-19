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

import java.io.IOException;
import java.io.Serializable;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class LoanValidationDecision implements Serializable {

	private static final long serialVersionUID = 1L;

	public LoanValidationDecision() {
	}
	
	public LoanValidationDecision(LoanValidationRequest request, LoanValidationResponse response) {
		this.id = request.hashCode() + "-" + response.hashCode();
		this.request = request;
		this.response = response;
	}

	public String id;
	public LoanValidationRequest request;
	public LoanValidationResponse response;
	
	public String serializeAsJSON() {
		JSONSerializer jsonSerializer = new JSONSerializer();
		String json = jsonSerializer.serialize(this);
		return json;
	}

	public static LoanValidationDecision parseAsJSON(String decisionJSON) {
		ObjectMapper mapper = new ObjectMapper();
		LoanValidationDecision decisionFromJSON = null;
		try {
			//ToDo Remove this shortcut
			//mapper.writerWithDefaultPrettyPrinter();
			
			String originToken = "{\"decision:";
			decisionJSON = decisionJSON.substring(decisionJSON.indexOf(originToken) + originToken.length() + 2);
			decisionJSON = decisionJSON.substring(0, decisionJSON.lastIndexOf("}"));
			decisionFromJSON = mapper.readValue(decisionJSON, LoanValidationDecision.class);
		} catch (JsonParseException e) {
			e.printStackTrace();
		} catch (JsonMappingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return decisionFromJSON;
	}
	
	public String serializeAsCSV() {
		StringBuffer buffer = new StringBuffer();
		buffer.append(this.request.serializeAsCSV() + ", ");
		buffer.append(this.response.serializeAsCSV());

		//@ToDo CHeck if YearlyRepayment written twice
		return buffer.toString();
	}
}
