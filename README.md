# PGProv Provenance for Property Graph Queries: Why, Where and How? 

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
2. Build both pgprov core and neo4j plugin
```
mvn clean package
```

## Development
