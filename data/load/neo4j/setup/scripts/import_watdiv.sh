#!/bin/bash

cd ${HOME}

rm -rf /var/lib/neo4j/data/databases/neo4j
rm -rf /var/lib/neo4j/data/transactions/neo4j

/var/lib/neo4j/bin/cypher-shell -u neo4j -p 12345678 < /import/watdiv/sf10/watdiv.cypher

echo "Done importing ${DATASET} data"