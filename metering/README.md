# Metering decisions automated with IBM DBA in an Apache Spark grid

IBM DBA empowers to track a decision automation performed in an Apache Spark cluster. 
The approachs is straightforward and leverages ILMT installed on a single machine.

![Metering architecture](docs/images/decision_metering_spark_1.png "Metering architecture")

### Usage metering
The helper code is provided in the simple app project. It includes a DecisionMeteringService class responsible for metering the decision automation usage made in the grid.
Approach is straigtforward as showed in the sample. Typical invocation is as follows:
```console

DecisionMetering decisionMetering = null;
DecisionMeteringReport report = null;

decisionMetering = new DecisionMetering("dba-metering");
String batchId = sc.getConf().getAppId() + "-" + System.currentTimeMillis();
report = decisionMetering.createUsageReport(batchId);
		
// Produce a RDD of decisions
JavaRDD<LoanValidationDecision> decisions = requestRDD.map(executeDecisionService).cache();

long stopTime = System.currentTimeMillis();

//Usage metering
report.setNbDecisions(decisions.count());
report.setStopTimeStamp();
report.writeILMTFile();
```
The writeILMTFile method writes the usage report on the local file system of the Spark driver under a var/ibm/slmtags relative path. 

Sequenced executed Spark batches reuses the same smltag file and extend the list of report items. Here is an example of slmtag file generated:

```console
<SchemaVersion>2.1.1</SchemaVersion>
<SoftwareIdentity>
	<PersistentId>b1a07d4dc0364452aa6206bb6584061d</PersistentId>
	<Name>IBM Operational Decision Manager Server</Name>
	<InstanceId>/usr/IBM/TAMIT</InstanceId>
</SoftwareIdentity>
<Metric logTime="2019-07-18T16:39:34+02:00">
	<Type>VU_VALUE_UNIT</Type>
	<SubType>MILLION_MONTHLY_DECISIONS</SubType>
	<Value>1.235</Value>
	<Period>
		<StartTime>2019-07-18T16:39:34+02:00</StartTime>
		<EndTime>2019-07-18T16:39:34+02:00</EndTime>
	</Period>
</Metric>
<Metric logTime="2019-07-19T14:32:21+02:00">
	<Type>VU_VALUE_UNIT</Type>
	<SubType>THOUSAND_MONTHLY_ARTIFACTS</SubType>
	<Value>23.456</Value>
	<Period>
		<StartTime>2019-07-19T14:30:04+02:00</StartTime>
		<EndTime>2019-07-19T14:32:21+02:00</EndTime>
	</Period>
</Metric>
```
This slmtag file directory has to be scanned by the IBM ILMT tool to consolidate and report the product usages.

In the IBM ILMT console go to Reports -> Resource Utilization to see the "IBM Operational Decision Manager" usages.
