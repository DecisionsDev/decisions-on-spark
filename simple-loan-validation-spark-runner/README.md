# Simple loan validation on Apache Spark
This folder contains the source code to execute the ODM loan validation sample in an Apache Spark cluster.

## Get the code
```console
git clone
```

## Build
```console
mvn clean install -Dodm.install=<INSTALLDIR>
```

## Run
Automate loan validation on a CSV applications dataset to produce a CSV decision set
```console
java -cp target/simpleloanvalidationsparkrunner-1.0-SNAPSHOT-execution.jar com.ibm.decisions.spark.loanvalidation.SimpleLoanValidationSparkRunnerCSV 
```

Automate loan validation on a JSON applications dataset to produce a JSON decision set
```console
java -cp target/simpleloanvalidationsparkrunner-1.0-SNAPSHOT-execution.jar com.ibm.decisions.spark.loanvalidation.SimpleLoanValidationSparkRunnerJSON 
```

```console
mvn exec:java -Dodm.install=<INSTALLDIR>
```
