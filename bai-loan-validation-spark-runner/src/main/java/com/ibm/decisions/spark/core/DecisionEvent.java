package com.ibm.decisions.spark.core;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import ilog.rules.res.session.ruleset.IlrExecutionEvent;
import ilog.rules.res.session.ruleset.IlrRuleEvent;
import ilog.rules.res.session.ruleset.IlrTaskEvent;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class DecisionEvent implements Serializable {

	private static final long serialVersionUID = 632261149341960619L;
	private ArrayList<DecisionEvent> subEvents = new ArrayList<DecisionEvent>();
	
	public enum EventType {
	    TASK, RULE, UNKNOWN }
	
	private EventType eventType = EventType.UNKNOWN;
	private TaskInformation taskInformation;
	private RuleInformation ruleInformation;
	
	public DecisionEvent()  {
		
	}
	
	private void initFromTaskEvent(IlrTaskEvent taskEvent) {
		setEventType(EventType.TASK);
		taskInformation = new TaskInformation(taskEvent.getTaskInformation());
		
		List<IlrExecutionEvent> subEvents = taskEvent.getSubExecutionEvents();
		if (subEvents != null) {
			for (IlrExecutionEvent subEvent : subEvents) {
				System.out.println(">> " + subEvent.toString());
				DecisionEvent decisionSubEvent = new DecisionEvent(subEvent);
				this.addSubEvent(decisionSubEvent);
			}
		}
	}

	private void initFromRuleEvent(IlrRuleEvent ruleEvent) {
		setEventType(EventType.RULE);
		ruleInformation = new RuleInformation(ruleEvent.getRuleInformation());
	}
	
	public DecisionEvent(IlrExecutionEvent execEvent) {
		if (execEvent instanceof IlrTaskEvent) {
			IlrTaskEvent taskEvent = (IlrTaskEvent) execEvent;
			System.out.println("TaskEvent:" + execEvent.toString());
			this.initFromTaskEvent(taskEvent);
		} else if (execEvent instanceof IlrRuleEvent) {
			IlrRuleEvent ruleEvent = (IlrRuleEvent) execEvent;
			System.out.println("RuleEvent:" + ruleEvent.toString());
			this.initFromRuleEvent(ruleEvent);
		}
	}

	private EventType getEventType() {
		return eventType;
	}
	
	private void setEventType(EventType value)  {
		this.eventType = value;
	}
	
	public List<DecisionEvent> getSubEvents() {
		return subEvents;
	}
	
	public void addSubEvent(DecisionEvent decisionEvent)  {
		subEvents.add(decisionEvent);
	}
	
	//Returns all rules fired by this task.
	@JsonIgnore
	public List<DecisionEvent> getRuleEvents() {
		return getSubEventsByType(EventType.RULE);
	}
	
	//Returns all events fired by this task.
	@JsonIgnore
	private List<DecisionEvent> getSubEventsByType(EventType eventType) {
		ArrayList<DecisionEvent> filteredEvents = new ArrayList<DecisionEvent>();
		Iterator<DecisionEvent> itDecisionEvent = subEvents.iterator();
		while(itDecisionEvent.hasNext())  {
			DecisionEvent decisionEvent = itDecisionEvent.next();
			if (decisionEvent.getEventType() == eventType)  {
				filteredEvents.add(decisionEvent);
			}
		}
		return filteredEvents;
	}

	//Returns all sub tasks executed by this task.
	@JsonIgnore
	public List<DecisionEvent> getTaskEvents() {
		return getSubEventsByType(EventType.TASK);
	}
	
	//Returns task information.
	public TaskInformation 	getTaskInformation() {
		return taskInformation;
	}
	
	//Returns rule information.
	public RuleInformation 	getRuleInformation() {
		return ruleInformation;
	}
	
}
