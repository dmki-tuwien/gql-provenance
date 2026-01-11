#!/usr/bin/env bash

OUT_DIR=./src/main/java/org/pgprov/parser
mkdir -p $OUT_DIR

# Download ANTLR library
curl -O https://www.antlr.org/download/antlr-4.13.1-complete.jar

# Generate parser executable
java -jar antlr-4.13.1-complete.jar -o $OUT_DIR -package org.pgprov.parser GQL.g4

