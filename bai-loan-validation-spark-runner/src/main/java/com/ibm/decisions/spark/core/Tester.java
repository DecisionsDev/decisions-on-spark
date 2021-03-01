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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.Function2;
import org.apache.spark.api.java.function.PairFunction;
import org.apache.spark.api.java.function.VoidFunction;

import ilog.rules.res.session.ruleset.IlrRuleInformation;
import scala.Tuple2;

public class Tester {

	public static void main(String[] args) {
		
		VoidFunction<Tuple2<String, Integer>> printRuleCounter = new VoidFunction<Tuple2<String, Integer>>() {
			public void call(Tuple2<String, Integer> ruleCounter) {
				System.out.println("rule " + ruleCounter._1 + " : " + ruleCounter._2);
			}
		};
		
		SparkConf conf = new SparkConf().setAppName("MiniLoan Decision Service");
		conf.setMaster("local[4]");
		JavaSparkContext sc = new JavaSparkContext(conf);

		Map<String, Integer> map1 = new HashMap<String, Integer>();
		map1.put("toto", 1);
		map1.put("titi", 1);
		map1.put("tutu", 1);

		Map<String, Integer> map2 = new HashMap<String, Integer>();
		map2.put("titi", 1);
		map2.put("toto", 1);

		ArrayList<Map<String, Integer>> mapArray = new ArrayList<Map<String, Integer>>();
		mapArray.add(map1);
		mapArray.add(map2);

		JavaRDD<Map<String, Integer>> mapRDD = sc.parallelize(mapArray);
		long count = mapRDD.count();
		System.out.println("Rule Map RDD count is " + count);

		JavaRDD<String> ruleNames = mapRDD.flatMap(new FlatMapFunction<Map<String, Integer>, String>() {
			public Iterator<String> call(Map<String, Integer> s) {
				return s.keySet().iterator();
			}
		});

		count = ruleNames.count();
		System.out.println("Rule Map Name RDD count is " + count);
		
		JavaPairRDD<String, Integer> pairs = ruleNames.mapToPair(new PairFunction<String, String, Integer>() {
			public Tuple2<String, Integer> call(String s) {
				return new Tuple2<String, Integer>(s, 1);
			}
		});

		count = pairs.count();
		System.out.println("Rule Map Pair RDD count is " + count);
		
		JavaPairRDD<String, Integer> counts = pairs.reduceByKey(new Function2<Integer, Integer, Integer>() {
			public Integer call(Integer a, Integer b) {
				return a + b;
			}
		});
		
		count = counts.count();
		System.out.println("Unique Rule names count is " + count);
		
		counts.foreach(printRuleCounter);
		

		// List<String> nameList = new ArrayList<String>(map.keySet());
		// Tuple2<String, Long> tuple = new Tuple2<String, Long>("e", (long)
		// 12); //TODO sc.parallelize(nameList);
		// JavaPairRDD<String, Long> executedRulePairRDD = new
		// JavaPairRDD<String, Long>(null, null, null);

	}

	public static void main1(String[] args) {
		SparkConf conf = new SparkConf().setAppName("MiniLoan Decision Service");
		conf.setMaster("local[4]");
		JavaSparkContext sc = new JavaSparkContext(conf);

		ArrayList<String> lineArray = new ArrayList<String>();
		lineArray.add("toto titi");
		lineArray.add("toto tutu");

		JavaRDD<String> lines = sc.parallelize(lineArray);
		JavaRDD<String> words = lines.flatMap(new FlatMapFunction<String, String>() {
			public Iterator<String> call(String s) {
				return Arrays.asList(s.split(" ")).iterator();
			}
		});

		long count = words.count();
		System.out.println("Word count is " + count);

		JavaPairRDD<String, Integer> pairs = words.mapToPair(str -> new Tuple2<>(str, 1));
		long pairCount = pairs.count();

		JavaPairRDD<String, Integer> counterRDD = pairs.reduceByKey(Integer::sum);

		System.out.println("Pair count is " + counterRDD.count());
		

		
	}
}
