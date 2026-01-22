#!/bin/bash

DATASET=$1
PLUGIN=pgprov-neo4j-plugin-1.0.jar

echo "Env variables"
cat .env

# Give access to folders
#chmod -R 777 /data/pgprov
chmod -R 777 ./setup/

# Copy the plugin
echo
echo "Copy the plugin ${PLUGIN}"
cp ../../plugin/target/${PLUGIN} ./setup/plugins/

echo "Setup Database and import data"
cp ./env/import.env .env

echo "Env variables"
cat .env

docker compose up -d

echo "Waiting for Neo4j to be ready..."
sleep 15

docker compose exec neo4j sh -c "/scripts/import_${DATASET}.sh"

echo "Waiting for Neo4j to be ready..."
sleep 15

docker compose exec --user root neo4j sh -c "/scripts/copy_db.sh"

echo "Waiting..."
sleep 15

docker compose exec --user root neo4j sh -c "/scripts/fix_access_rights.sh"

echo "Waiting for Neo4j to be ready..."
sleep 15

docker compose down

