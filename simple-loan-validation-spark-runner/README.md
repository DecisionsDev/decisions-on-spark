# Simple loan validation on Apache Spark
This folder contains the source code to execute the ODM loan validation sample in an Apache Spark cluster.

## Get the code
```console
git pull
```

## Build
```console
mvn clean install -Dodm.install=<INSTALLDIR>
```

## Run
```console
mvn  exec:java -Dodm.install=<INSTALLDIR>
```
