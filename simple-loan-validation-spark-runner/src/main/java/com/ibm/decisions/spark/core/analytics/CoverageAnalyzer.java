/*
 *
 *   Copyright IBM Corp. 2017
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package com.ibm.decisions.spark.analytics;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import loan.*;
import scala.Tuple2;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.Function2;
import org.apache.spark.api.java.function.PairFunction;
import org.apache.spark.api.java.function.VoidFunction;

import com.ibm.decisions.spark.loanvalidation.*;

import ilog.rules.res.session.ruleset.*;

public class CoverageAnalyzer {

	public enum RddCreation {
		FILE, MEMORY
	};

	public enum DeployMode {
		NONE, LOCAL, MASTER, CLUSTER
	};

	public enum SparkMaster {
		LOCALHOST_SPARK_MASTER, ODMTEST14U
	};

	@SuppressWarnings("unused")
	public static void main(String[] args) {

		Function<LoanValidationDecision, Map<String, IlrRuleInformation>> getRuleCoverage = new Function<LoanValidationDecision, Map<String, IlrRuleInformation>>() {
			private static final long serialVersionUID = -4393164475372435508L;

			public Map<String, IlrRuleInformation> call(LoanValidationDecision decision) {
				Map<String, IlrRuleInformation> executedRules = decision.response.decisionTrace.getExecutionTrace().getRules();

				// JavaRDD<String, IlrRuleInformation> executedRuleRDD =
				// sc.parallelize(executedRules);
				return executedRules;
			}
		};
		
		Function<String, LoanValidationDecision> deserializeDecisionFromJSON = new Function<String, LoanValidationDecision>() {
			private static final long serialVersionUID = 1L;

			public LoanValidationDecision call(String json) {
				return LoanValidationDecision.parseAsJSON(json);
			}
		};

		long startTime = System.currentTimeMillis();

		//
		// CONFIGURATION
		//
		RddCreation rddCreation = RddCreation.MEMORY;
		DeployMode deployMode = DeployMode.LOCAL;
		SparkMaster sparkMaster = SparkMaster.LOCALHOST_SPARK_MASTER;

		final String CLUSTER_MASTER_URL = "spark://localhost:7077"; // Client
		final String MASTER_CLUSTER_URL = "spark://localhost:6066"; // Cluster
		final String LOLO_MASTER_URL = "spark://odmtest14u:7077"; // Remote
																	// master
																	// odmtest14u

		// Local dir
		Path currentRelativePath = Paths.get(".");
		String s = currentRelativePath.toAbsolutePath().toString();
		System.out.println("Executing from: " + s);

		// Input data file
		String decisionFileName = "data" + File.separatorChar + "loanvalidation" + File.separatorChar + "1K" + File.separatorChar + "loanvalidation-decisions-1K.json";

		System.out.println("args: {");
		for (int i = 0; i < args.length; i++) {
			System.out.println(args[i]);
		}
		System.out.println("}");

		if (args.length >= 1) {
			decisionFileName = args[0];
		}

		SparkConf conf = new SparkConf().setAppName("Loan Validation Decision Service");

		String[] jars = null; //{ "deploy/loanvalidation-res-sparkapp.jar" };

		switch (deployMode) {
		case NONE:
			break;
		case LOCAL:
			conf.setMaster("local[8]");
			break;
		case MASTER:
			switch (sparkMaster) {
			case LOCALHOST_SPARK_MASTER:
				conf.setMaster(MASTER_CLUSTER_URL);
				break;
			case ODMTEST14U:
				conf.setMaster(LOLO_MASTER_URL);
				break;
			}
			conf.setMaster(LOLO_MASTER_URL);
			conf.setJars(jars);
			break;
		case CLUSTER:
			conf.setMaster(MASTER_CLUSTER_URL);
			conf.setJars(jars);
			break;
		}

		@SuppressWarnings("resource")
		JavaSparkContext sc = new JavaSparkContext(conf);
		
		// Read Decision RDD
		//
		JavaRDD<String> decisionsJsonRDD = sc.textFile(decisionFileName).cache();
		JavaRDD<LoanValidationDecision> decisions = decisionsJsonRDD.map(deserializeDecisionFromJSON).cache();

		// Rule Statistics
		//

		processRuleStatistics(sc, decisions); //DISABLED WHILE RuleTupleCount not refactored

	}

	// Rule Statistics
	//
	
	public static void processRuleStatistics(JavaSparkContext sc, JavaRDD<LoanValidationDecision> decisions) {

		Function<LoanValidationDecision, List<Tuple2<IlrRuleInformation, Long>>> getRuleExecutionTuples = new Function<LoanValidationDecision, List<Tuple2<IlrRuleInformation, Long>>>() {
			private static final long serialVersionUID = 1L;

			public List<Tuple2<IlrRuleInformation, Long>> call(LoanValidationDecision decision) {
				
				// Create new Trace instances
				return decision.response.decisionTrace.getRuleCountTuples();
			}
		};

		// Function<Tuple2<IlrRuleInformation, Long>>, JavaPairR>
		// getRuleExecutionTuples = new Function<loanvalidationDecision,
		// List<Tuple2<IlrRuleInformation, Long>>>() {
		// public List<Tuple2<IlrRuleInformation, Long>> call(loanvalidationDecision
		// decision) {
		// List<Tuple2<IlrRuleInformation, Long>> ruleTuples = new
		// ArrayList<Tuple2<IlrRuleInformation, Long>>();
		// }
		// }

		VoidFunction<Tuple2<String, Long>> printRuleCounter = new VoidFunction<Tuple2<String, Long>>() {
			/**
			 * 
			 */
			private static final long serialVersionUID = -7909008433242426887L;

			public void call(Tuple2<String, Long> ruleCounter) {
				System.out.println("rule " + ruleCounter._1 + " : " + ruleCounter._2);
			}
		};

		System.out.println("Computing Rule execution aggregates");

		// List of (ruleinfo, execcount) tuples for all decisions
		JavaRDD<List<Tuple2<IlrRuleInformation, Long>>> ruleExecutionListRDD = decisions.map(getRuleExecutionTuples);
		long count = ruleExecutionListRDD.count();
		System.out.println("Execution Rule tuple RDD count is " + count);

		// Flat tuples
		JavaRDD<Tuple2<IlrRuleInformation, Long>> ruleExecutionRDD = ruleExecutionListRDD.flatMap(
				new FlatMapFunction<List<Tuple2<IlrRuleInformation, Long>>, Tuple2<IlrRuleInformation, Long>>() {

					private static final long serialVersionUID = 1542126691599764220L;

					public Iterator<Tuple2<IlrRuleInformation, Long>> call(
							List<Tuple2<IlrRuleInformation, Long>> infoSet) {
						ArrayList<Tuple2<IlrRuleInformation, Long>> ruleExecTupleArray = new ArrayList<Tuple2<IlrRuleInformation, Long>>();
						Iterator<Tuple2<IlrRuleInformation, Long>> itRuleExecTuple = infoSet.iterator();
						while (itRuleExecTuple.hasNext()) {
							ruleExecTupleArray.add(itRuleExecTuple.next());
						}
						return ruleExecTupleArray.iterator();
					}
				});

		JavaPairRDD<String, Long> pairs = ruleExecutionRDD.mapToPair(t -> new Tuple2(t._1.getBusinessName(), t._2)); // ToDo
																														// Improve
																														// this
																														// weird
																														// code

		JavaPairRDD<String, Long> ruleExecutionCoverageRDD = pairs.reduceByKey(new Function2<Long, Long, Long>() {
			private static final long serialVersionUID = 7825299016123219106L;

			public Long call(Long a, Long b) {
				return a + b;
			}
		});

		count = ruleExecutionCoverageRDD.count();
		System.out.println("Unique Rule names count is " + count);

		ruleExecutionCoverageRDD.foreach(printRuleCounter);
	}

	static void processRuleStatistics2(JavaSparkContext sc, JavaRDD<LoanValidationDecision> decisions) {

		Function<LoanValidationDecision, Set<IlrRuleInformation>> getNotExecutedRules = new Function<LoanValidationDecision, Set<IlrRuleInformation>>() {

			private static final long serialVersionUID = -1630305867764238302L;

			public Set<IlrRuleInformation> call(LoanValidationDecision decision) {
				Set<IlrRuleInformation> notExecutedRules = decision.response.decisionTrace.getExecutionTrace().getRulesNotFired();
				return notExecutedRules;
			}
		};

		VoidFunction<Tuple2<String, Integer>> printRuleCounter = new VoidFunction<Tuple2<String, Integer>>() {

			private static final long serialVersionUID = -5254319871211156089L;

			public void call(Tuple2<String, Integer> ruleCounter) {
				System.out.println("rule " + ruleCounter._1 + " : " + ruleCounter._2);
			}
		};

		// Not executed rules
		System.out.println("Not executed Rules");

		JavaRDD<Set<IlrRuleInformation>> notExecutedRDD = decisions.map(getNotExecutedRules);

		JavaRDD<String> notExecutedRuleNames = notExecutedRDD
				.flatMap(new FlatMapFunction<Set<IlrRuleInformation>, String>() {

					private static final long serialVersionUID = -6628220686218488642L;

					public Iterator<String> call(Set<IlrRuleInformation> infoSet) {
						ArrayList<String> nameArray = new ArrayList<String>();
						Iterator<IlrRuleInformation> itInfo = infoSet.iterator();
						while (itInfo.hasNext()) {
							nameArray.add(itInfo.next().getBusinessName());
						}
						return nameArray.iterator();
					}
				});

		long count = notExecutedRuleNames.count();
		System.out.println("Not executed Rule Map Name RDD count is " + count);

		JavaPairRDD<String, Integer> pairs = notExecutedRuleNames
				.mapToPair(new PairFunction<String, String, Integer>() {
					private static final long serialVersionUID = 3384491921583658387L;
					public Tuple2<String, Integer> call(String s) {
						return new Tuple2<String, Integer>(s, 1);
					}
				});

		count = pairs.count();
		System.out.println("Rule Map Pair RDD count is " + count);

		JavaPairRDD<String, Integer> counts = pairs.reduceByKey(new Function2<Integer, Integer, Integer>() {
			/**
			 * 
			 */
			private static final long serialVersionUID = -8935959062099150965L;

			public Integer call(Integer a, Integer b) {
				return a + b;
			}
		});

		count = counts.count();
		System.out.println("Unique Rule names count is " + count);

		counts.foreach(printRuleCounter);
	}

}
