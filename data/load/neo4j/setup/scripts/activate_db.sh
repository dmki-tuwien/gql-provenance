#!/bin/bash

cd ${HOME}

rm -rf /var/lib/neo4j/data/databases/neo4j/
rm -rf /var/lib/neo4j/data/transactions/neo4j/

cp -r /var/lib/neo4j/data/databases/${DATASET}_${SCALE_FACTOR}/. /var/lib/neo4j/data/databases/neo4j/
cp -r /var/lib/neo4j/data/transactions/${DATASET}_${SCALE_FACTOR}/. /var/lib/neo4j/data/transactions/neo4j/

chown -R --reference=/var/lib/neo4j/data /var/lib/neo4j/data/databases/neo4j/
chmod -R --reference=/var/lib/neo4j/data /var/lib/neo4j/data/databases/neo4j/

chown -R --reference=/var/lib/neo4j/data /var/lib/neo4j/data/transactions/neo4j/
chmod -R --reference=/var/lib/neo4j/data /var/lib/neo4j/data/transactions/neo4j/