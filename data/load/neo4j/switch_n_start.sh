#!/bin/bash

echo "Run Neo4j"
cp ./env/switch.env .env
docker compose up -d

echo "Waiting for Neo4j to be ready..."
sleep 15

docker compose exec --user root neo4j sh -c "/scripts/activate_db.sh"

echo "Waiting..."
sleep 15

docker compose exec --user root neo4j sh -c "/scripts/fix_access_rights.sh"

docker compose down

echo "Waiting..."
sleep 15

echo "Run Neo4j"
docker compose up -d

