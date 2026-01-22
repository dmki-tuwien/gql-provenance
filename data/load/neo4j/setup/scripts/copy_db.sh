#!/bin/bash

cd ${HOME}

mkdir -p /var/lib/neo4j/data/databases/${DATASET}_${SCALE_FACTOR}/
mkdir -p /var/lib/neo4j/data/transactions/${DATASET}_${SCALE_FACTOR}/

cp -r /var/lib/neo4j/data/databases/neo4j/* /var/lib/neo4j/data/databases/${DATASET}_${SCALE_FACTOR}/
cp -r /var/lib/neo4j/data/transactions/neo4j/* /var/lib/neo4j/data/transactions/${DATASET}_${SCALE_FACTOR}/

chown -R --reference=/var/lib/neo4j/data /var/lib/neo4j/data/databases/${DATASET}_${SCALE_FACTOR}
chmod -R --reference=/var/lib/neo4j/data /var/lib/neo4j/data/databases/${DATASET}_${SCALE_FACTOR}

chown -R --reference=/var/lib/neo4j/data /var/lib/neo4j/data/transactions/${DATASET}_${SCALE_FACTOR}
chmod -R --reference=/var/lib/neo4j/data /var/lib/neo4j/data/transactions/${DATASET}_${SCALE_FACTOR}