#!/bin/bash

PLUGIN=pgprov-neo4j-plugin-1.0.jar

# Copy the plugin
echo "Copy the plugin ${PLUGIN}"
cp ../../../plugin/target/${PLUGIN} ./plugins/${PLUGIN}

echo "Setup Database"
cp ../env/pre_import.env .env
docker compose up -d

echo "Waiting for Neo4j to be ready..."
sleep 15

docker compose down

echo "Importing data"
cp ../env/import.env .env
docker compose up -d

