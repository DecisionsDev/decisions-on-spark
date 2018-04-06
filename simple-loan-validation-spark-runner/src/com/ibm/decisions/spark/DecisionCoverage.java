package com.ibm.decisions.spark;

import ilog.rules.res.session.ruleset.IlrRuleInformation;

public class DecisionCoverage {

	public RuleInformation ruleInformation;
	public Long count;
	
	public DecisionCoverage() {
		
	}
	
	public DecisionCoverage(IlrRuleInformation ruleInfo, Long count) {
		this.ruleInformation = new RuleInformation(ruleInfo);
		this.count = count;
	}
	
	public RuleInformation getRuleInformation() {
		return ruleInformation;
	}
	
	public void setRuleInformation(RuleInformation ruleInfo) {
		this.ruleInformation = ruleInfo;
	}
}
