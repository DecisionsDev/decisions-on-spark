package com.ibm.decisions.spark.core;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonInclude;

import ilog.rules.res.session.ruleset.IlrTaskInformation;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class TaskInformation implements Serializable {

	private static final long serialVersionUID = -833734059230947653L;
	private String businessName = null;
	
	public TaskInformation() {
	}
	
	public TaskInformation(IlrTaskInformation taskInformation) {
		setBusinessName(taskInformation.getBusinessName());
	}

	public String getBusinessName() {
		return businessName;
	}

	public void setBusinessName(String businessName) {
		this.businessName = businessName;
	}

}
