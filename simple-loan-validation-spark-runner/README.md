# Simple loan validation on Apache Spark
This folder contains the source code to execute the ODM loan validation sample in an Apache Spark cluster.

![Flow](docs/images/decision_automation_in_map_reduce.png "Architecture")

## Pre requisites
You need an IBM ODM 892 installation to build the application. Root of your ODM installation is referred as <INSTALLDIR> in the instructions below. Maven files will look for the ODM jars under <INSTALLDIR>/executionserver/libs directory.

## Get the code
Clone this repository.
```console
git clone
```
Open an terminal where your have cloned this repository.
```console
cd decisions-on-spark/simple-loan-validation-spark-runner
```
## Build
```console
mvn clean install -Dodm.install=<INSTALLDIR>
```
<INSTALLDIR> is the ODM 892 installation directory.

## Run locally


Automate loan validation on a CSV applications dataset to produce a CSV decision set.
```console
java -cp target/simpleloanvalidationsparkrunner-1.0-SNAPSHOT-withspark.jar com.ibm.decisions.spark.loanvalidation.LoanValidationSparkRunner --input ../data/loanvalidation/1K/loanvalidation-requests-1K.csv --output ../data/loanvalidation/1K/loanvalidation-decisions-1K.csv --master local[8]
```

Automate loan validation on a JSON applications dataset to produce a JSON decision set.
```console
java -cp target/simpleloanvalidationsparkrunner-1.0-SNAPSHOT-withspark.jar com.ibm.decisions.spark.loanvalidation.LoanValidationSparkRunner --input ../data/loanvalidation/1K/loanvalidation-requests-1K.json --output ../data/loanvalidation/1K/loanvalidation-decisions-1K.json --master local[8]
```

Automate loan validation on a JSON applications dataset to produce a JSON decision set and to display a Rule coverage.
```console
java -cp target/simpleloanvalidationsparkrunner-1.0-SNAPSHOT-withspark.jar com.ibm.decisions.spark.loanvalidation.LoanValidationSparkRunner --input ../data/loanvalidation/1K/loanvalidation-requests-1K.json --output ../data/loanvalidation/1K/loanvalidation-decisions-1K.json --master local[8] --rulecoverage
```
## Run in a cluster
Rule based automation works in a cluster with the same integration pattern and code than in standalone.
Only differences of the application are about:
- the access to the datasets, as the Spark driver and executors run on different machines and local file systems. In consequence data have to be stored in hdfs or other shared persistence.
- the packaging, as Spark jars are not needed in the uber jar but already deployed in the cluster.

The target/simpleloanvalidationsparkrunner-1.0-SNAPSHOT-withodmrt.jar contains required classes to submit a Spark job.

The LoanValidationSparkRunner application can read or generate in memory the requests, then applies the loan validation decision logic, and computes metrics and finally KPIs.

### Running in IBM Spark service
Below is the submit command as tested with the IBM Cloud Spark service with a random generation of the requests.
```console
./spark-submit.sh \
--vcap ./vcap-odm123.json \
--name “loan-validation”  \
--deploy-mode cluster \
--conf spark.service.spark_version=2.1 \
--class com.ibm.decisions.spark.loanvalidation.LoanValidationSparkRunner \
target/simpleloanvalidationsparkrunner-1.0-SNAPSHOT-withodmrt.jar \
--inputgen 1000  \
--output loanvalidation-decisions-1K.json
```
By submitting the application you get a trace similar to this one.

![Flow](docs/images/submit-trace.png "submit trace")

When opening the stdout file you can check the loan approval traces and obtain the KPIs.

```console
...
Loan approved=false with a yearly repayment=0.0 insurance required:false messages= [The borrower's age is not valid.,  The loan amount is under the maximum authorized] executed in thread Executor task launch worker for task 8
Loan approved=true with a yearly repayment=1464.7636429039499 insurance required:true messages= [ The loan amount is under the maximum authorized, Low risk loan, Congratulations! Your loan has been approved] executed in thread Executor task launch worker for task 8

Decision batch metrics
Number of loan applications processed: 1000 in 2995 ms
Number of decision per sec: 333.0
Number of approved loan applications: 291 on a 1000 total
Number of loans approved with a YearlyInterestRate > 5%: 291
```
### Running Business Rules on IBM Analytic Engine
Below is the submit command as tested with the public IBM Analylic Engine with a read of a request dataset file.

```console
...
spark-submit \
--name “loan-validation” \
--conf spark.service.spark_version=2.1 \
--class com.ibm.decisions.spark.loanvalidation.LoanValidationSparkRunner \
/home/wce/clsadmin/simpleloanvalidationsparkrunner-1.0-SNAPSHOT-withodmrt.jar \
--input hdfs://machine2.bi.services.us-south.bluemix.net:8020/user/clsadmin/data/loanvalidation/loanvalidation-requests-1K.csv  \
--output hdfs://machine2.bi.services.us-south.bluemix.net:8020/user/clsadmin/data/loanvalidation/loanvalidation-decisions-1K.csv
```
