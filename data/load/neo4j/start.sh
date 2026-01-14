#!/bin/bash

echo "Run Neo4j"
cp ./env/run.env .env
docker compose up -d

echo "Waiting for Neo4j to be ready..."
sleep 15

docker compose exec --user root neo4j sh -c "/scripts/fix_access_rights.sh"

