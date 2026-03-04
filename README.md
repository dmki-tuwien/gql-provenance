# PGProv : Provenance for Property Graph Queries

## Overview

**PGProv** is a Java library that can be used to add provenance support for GQL-compliant graph databases.
Provenance captures the data and transformations that contributed to a particular query result.
A GQL query in a graph database results in a relation and with PGProv, 
for each row in a GQL query result we capture the provenance details for each result.

## Getting Started

### Prerequisites
* Maven
* Java

This repository contains 
* PGProv as a library

1. Run the following script to generate the executable parser code for GQL with Antlr
```
cd gql
./parser-generator.sh
```
* Neo4j plugin implementation which uses PGProv
2. Build both `pgprov-core` and neo4j `plugin`
```
mvn clean package
```

## Use PGProv

1. Place the plugin in `plugin\target` inside the `plugins` folder of neo4j and start your Neo4J database
```
mvn clean package
```
2. You may use `org.pgprov.getWhyProvenance(query, params)` procedure to test the query.

## Run Evaluation

1. Build `experiments\driver` module.

2. Run docker setup
   ```
   ```




