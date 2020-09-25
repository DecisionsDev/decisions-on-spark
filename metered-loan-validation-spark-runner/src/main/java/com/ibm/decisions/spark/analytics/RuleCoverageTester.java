package com.ibm.decisions.spark.analytics;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.Function;

import com.ibm.decisions.spark.core.DecisionTrace;
import com.ibm.decisions.spark.loanvalidation.JSONSerializer;
import com.ibm.decisions.spark.loanvalidation.LoanValidationDecision;

public class RuleCoverageTester {

	public static void main(String[] args) {
		testCoverage();
	}
	
	static void testCoverage() {
		Function<String, LoanValidationDecision> deserializeDecisionFromJSON = new Function<String, LoanValidationDecision>() {
			private static final long serialVersionUID = 1L;

			public LoanValidationDecision call(String json) {
				return LoanValidationDecision.parseAsJSON(json);
			}
		};
		
		//fail("Not yet implemented");
		
		//Read decision file
		SparkConf conf = new SparkConf().setAppName("Loan Validation Decision Service").setMaster("local[8]");
		JavaSparkContext sc = new JavaSparkContext(conf);
			
		String decisionFileName = "data/loanvalidation/1K/loanvalidation-decisions-1K.json";

		// Decisions data file
		System.out.println("Loading decisions file: " + decisionFileName);

		// RDD creation
		JavaRDD<LoanValidationDecision> decisionRDD = null;
		JavaRDD<String> decisionData = sc.textFile(decisionFileName);
		decisionData.count();

		decisionRDD = decisionData.map(deserializeDecisionFromJSON);
		decisionRDD.count();
		System.out.println("Deserialized decisions: " + decisionRDD.count());
		
		System.out.println("RuleCountTuples:" + decisionRDD.first().response.decisionTrace.getRuleCountTuples());
		
		StringBuffer buffer = new StringBuffer();
		
		for (int index=0;index < decisionRDD.first().response.decisionTrace.getRuleCoverages().size();index++) {
			buffer.append(decisionRDD.first().response.decisionTrace.getRuleCoverages().get(index).ruleInformation.getBusinessName() + " : " + decisionRDD.first().response.decisionTrace.getRuleCoverages().get(index).count);
			buffer.append("\n");
			}
		
		buffer = new StringBuffer();
		DecisionTrace decisionTrace = decisionRDD.first().response.decisionTrace;
		int decisionEventSize = decisionTrace.getDecisionEvents().size();
		System.out.println("decisionEventSize:" + decisionEventSize);
		
		JSONSerializer jsonSerializer = new JSONSerializer();
		String decisionTraceJSON = jsonSerializer.serialize(decisionTrace);
		System.out.println("decisionTrace:" + decisionTraceJSON);
		
	}
}
