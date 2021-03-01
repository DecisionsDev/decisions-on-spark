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

import ilog.rules.res.session.ruleset.IlrExecutionEvent;
import ilog.rules.res.session.ruleset.IlrExecutionTrace;
import ilog.rules.res.session.ruleset.IlrRuleEvent;
import ilog.rules.res.session.ruleset.IlrRuleInformation;
import ilog.rules.res.session.ruleset.IlrTaskEvent;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import scala.Tuple2;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class DecisionTrace implements Serializable {

	private static final long serialVersionUID = -7317490616256715733L;

	@JsonIgnore
	public IlrExecutionTrace executionTrace;
	@JsonIgnore
	private List<Tuple2<IlrRuleInformation, Long>> ruleTuples = null; // Make happen the JSON serialization or
																		// concentrage on ruleCoverages field
	private ArrayList<DecisionEvent> decisionEvents = new ArrayList<DecisionEvent>();
	
	private ArrayList<DecisionCoverage> ruleCoverages;

	// public HashMap<String, Long> ruleCoverageMap = new HashMap<String, Long>();

	public DecisionTrace() {

	}

	public DecisionTrace(IlrExecutionTrace executionTrace) {
		this.setExecutionTrace(executionTrace);
	}

	@JsonIgnore
	public void setExecutionTrace(IlrExecutionTrace executionTrace) {
		this.executionTrace = executionTrace;

		// Derived field
		computeRuleCountTuples();
	}

	@JsonIgnore
	public IlrExecutionTrace getExecutionTrace() {
		return executionTrace;
	}

	@JsonIgnore
	public void setRuleCountTuples(List<Tuple2<IlrRuleInformation, Long>> ruleCountTuples) {
		this.ruleTuples = ruleCountTuples;
	}

	// ToDo Handle when a rule is fired several times
	@JsonIgnore
	public List<Tuple2<IlrRuleInformation, Long>> getRuleCountTuples() {
		if ((executionTrace != null) && (ruleTuples == null)) {
			computeRuleCountTuples();
		}
		return ruleTuples;
	}

	// ToDo Handle when a rule is fired several times
	public List<DecisionCoverage> getRuleCoverages() {
		if ((executionTrace != null) && (ruleCoverages == null)) {
			computeRuleCountTuples();
		}
		return ruleCoverages;
	}

	public ArrayList<DecisionEvent> getDecisionEvents() {
		return decisionEvents;
	}

	public void setDecisionEvents(ArrayList<DecisionEvent> decisionEvents) {
		this.decisionEvents = decisionEvents;
	}

	@JsonIgnore
	private void computeRuleCountTuples() {
		ruleTuples = new ArrayList<Tuple2<IlrRuleInformation, Long>>();
		ruleCoverages = new ArrayList<DecisionCoverage>(); // @ToDo remove duplication of array

		// Rules not fired
		Set<IlrRuleInformation> notExecutedRules = getExecutionTrace().getRulesNotFired();
		Iterator<IlrRuleInformation> itNotExecutedRule = notExecutedRules.iterator();
		while (itNotExecutedRule.hasNext()) {
			IlrRuleInformation ruleInfo = itNotExecutedRule.next();
			ruleTuples.add(new Tuple2<IlrRuleInformation, Long>(ruleInfo, (long) 0));
			ruleCoverages.add(new DecisionCoverage(ruleInfo, (long) 0));
		}

		// Rules fired
		Map<String, IlrRuleInformation> allRuleMap = getExecutionTrace().getRules();
		Collection<IlrRuleInformation> allRules = allRuleMap.values();
		Iterator<IlrRuleInformation> ruleInfoIterator = allRules.iterator();
		while (ruleInfoIterator.hasNext()) {
			IlrRuleInformation ruleInfo = ruleInfoIterator.next();
			if (!notExecutedRules.contains(ruleInfo)) {
				ruleTuples.add(new Tuple2<IlrRuleInformation, Long>(ruleInfo, (long) 1));
				ruleCoverages.add(new DecisionCoverage(ruleInfo, (long) 1));
			}
		}

		// Events
		visitExecutionEventTree();
	}

	// Events
	@JsonIgnore
	public void visitExecutionEventTree() {
		List<IlrExecutionEvent> executionEvents = getExecutionTrace().getExecutionEvents();
		Iterator<IlrExecutionEvent> itExecutionEvent = executionEvents.iterator();
		
		while (itExecutionEvent.hasNext()) {
			IlrExecutionEvent executionEvent = itExecutionEvent.next();
			DecisionEvent decisionEvent = new DecisionEvent(executionEvent);
			decisionEvents.add(decisionEvent);
		}

		/*
		while (itExecutionEvent.hasNext()) {
			IlrExecutionEvent execEvent = itExecutionEvent.next();
			if (execEvent instanceof IlrTaskEvent) {
				IlrTaskEvent taskEvent = (IlrTaskEvent) execEvent;
				
				DecisionEvent decisionEvent = new DecisionEvent(taskEvent);
				decisionEvents.add(decisionEvent);
				
				List<IlrExecutionEvent> subEvents = taskEvent.getSubExecutionEvents();
				if (subEvents != null) {
					for (IlrExecutionEvent subEvent : subEvents) {
						DecisionEvent decisionSubEvent = new DecisionEvent(subEvent);
						decisionEvents.add(decisionEvent);
					}
				}
			} else if (execEvent instanceof IlrRuleEvent) {
				IlrRuleEvent ruleEvent = (IlrRuleEvent) execEvent;
			}
		}
		*/
		// ruleTuples.add(new Tuple2<IlrRuleInformation, Long>(ruleInfo, (long) 0));
		// ruleCoverages.add(new DecisionCoverage(ruleInfo, (long) 0));
	}

	// Events
	@JsonIgnore
	public void visitExecutionEvent(IlrExecutionEvent execEvent) {

		if (execEvent instanceof IlrTaskEvent) {
			IlrTaskEvent taskEvent = (IlrTaskEvent) execEvent;
			List<IlrExecutionEvent> subEvents = taskEvent.getSubExecutionEvents();
			if (subEvents != null) {
				for (IlrExecutionEvent subEvent : subEvents) {
					visitExecutionEvent(subEvent);
				}
			}
		} else if (execEvent instanceof IlrRuleEvent) {
			IlrRuleEvent ruleEvent = (IlrRuleEvent) execEvent;
		}
	}

	@JsonIgnore
	public void transformEvent(IlrExecutionEvent execEvent) {
	System.out.println("execEvent:" + execEvent.toString());
	DecisionEvent decisionEvent = new DecisionEvent(execEvent);
	decisionEvents.add(decisionEvent);
	}
	
}
