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

package com.ibm.decisions.spark.core;

import java.util.HashMap;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class DecisionSimulation {

	String decisionSetFilePath;
	HashMap<String, Object> kPIMap = new HashMap<String, Object>();
	
	@JsonIgnore public static void main(String[] args) {

	}
	
	public DecisionSimulation() {
		
	}
	
	public DecisionSimulation(String decisionSetFilePath) {
		this.decisionSetFilePath = decisionSetFilePath;
	}
	
	public void addKPI(KPI kpi) {
		kPIMap.put(kpi.getName(), kpi.getResult());
	}
	
	public HashMap<String, Object> getKPIHashMap() {
		return kPIMap;
	}

}
