# Simple loan validation on Apache Spark
This folder contains the source code to execute the ODM loan validation sample in an Apache Spark cluster.

![Flow](docs/images/decision_automation_in_map_reduce.png "Architecture")

## Get the code
```console
git clone
```

## Build
```console
mvn clean install -Dodm.install=<INSTALLDIR>
```

## Run locally
Automate loan validation on a CSV applications dataset to produce a CSV decision set
```console
java -cp target/simpleloanvalidationsparkrunner-1.0-SNAPSHOT-withspark.jar com.ibm.decisions.spark.loanvalidation.SimpleLoanValidationSparkRunnerCSV 
```

Automate loan validation on a JSON applications dataset to produce a JSON decision set
```console
java -cp target/simpleloanvalidationsparkrunner-1.0-SNAPSHOT-withspark.jar com.ibm.decisions.spark.loanvalidation.SimpleLoanValidationSparkRunnerJSON 
```

Automate loan validation on a JSON applications dataset to produce a JSON decision set and to display a Rule coverage
```console
java -cp target/simpleloanvalidationsparkrunner-1.0-SNAPSHOT-withspark.jar com.ibm.decisions.spark.loanvalidation.SimpleLoanValidationSparkRunnerJSONWithCoverage
```
## Run in a cluster
Rule based automation works in a cluster with the same integration pattern and code.
Only differences of the application are about:
- the access to the datasets, as the Spark driver and executors run on different machines and local file systems. In consequence data have to be stored in hdfs or other shared persistence.
- the packaging, as Spark jars are not needed in the uber jar but already deployed in the cluster.

The target/simpleloanvalidationsparkrunner-1.0-SNAPSHOT-withodmrt.jar contains required classes to submit a Spark job.

The SimpleLoanValidationSparkRunnerGenCSV application generates in memory the requests, apply the loan validation decision logic, and computes metrics plus KPIs.

Below is the submit command as tested with the IBM Cloud Spark service.
```console
./spark-submit.sh \
--vcap ./vcap-odm123.json \
--name “loan-validation”  \
--deploy-mode cluster \
--conf spark.service.spark_version=2.1 \
--class com.ibm.decisions.spark.loanvalidation.SimpleLoanValidationSparkRunnerGenCSV \
target/simpleloanvalidationsparkrunner-1.0-SNAPSHOT-withodmrt.jar
```

```console
mvn exec:java -Dodm.install=<INSTALLDIR>
```



