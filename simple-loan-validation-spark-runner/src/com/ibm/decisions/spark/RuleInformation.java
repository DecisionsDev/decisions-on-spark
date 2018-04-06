package com.ibm.decisions.spark;

import ilog.rules.res.session.ruleset.IlrRuleInformation;

public class RuleInformation {

	private String businessName;
	private String name;
	
	public RuleInformation( ) {
		
	}
	
	public RuleInformation(IlrRuleInformation ilrRuleInfo) {
		this.businessName = ilrRuleInfo.getBusinessName();
		this.name = ilrRuleInfo.getName();
	}
	
	public String getBusinessName() {
		return businessName;
	}
	
	public void setBusinessName(String businessName) {
		this.businessName = businessName;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
}
