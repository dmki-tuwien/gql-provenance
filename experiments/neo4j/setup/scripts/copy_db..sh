#!/bin/bash

cd ${HOME}

cp -r /var/lib/neo4j/data/databases/neo4j/* /var/lib/neo4j/data/databases/${DATASET}_${SCALE_FACTOR}/
cp -r /var/lib/neo4j/data/transactions/neo4j/* /var/lib/neo4j/data/transactions/${DATASET}_${SCALE_FACTOR}/