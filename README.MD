# Encoding converter

Simple JavaFX-based util to convert folder full of text files to target encoding.

## Prerequisites

* Java 1.8.0_51 or above
* Maven 3.1.1 or above

## Usage
Make sure you have correct environment variables **JAVA_HOME** and **M2_HOME**.

To run program execute following command.

 ```mvn
 mvn exec:java
 ```

## Binary distribution
To create archive with binaries in the root folder of the project execute following command.

 ```mvn
 mvn clean package
 ```

 File **encoding-converter-1.0.zip** will be created at the folder /target.
 To run application, extract the archive and run file **run.bat**.


