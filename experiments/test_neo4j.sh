#!/usr/bin/env bash

set -a
source .env
set +a

sed -e "s|{{DATASET}}|$DATASET|g" \
-e "s|{{SCALE_FACTOR}}|$SCALE_FACTOR|g" \
-e "s|{{TEST_RUN}}|$TEST_RUN|g" \
./driver/configs/config.properties.tmp > ./driver/configs/config.properties

docker-compose up -d

echo "Waiting for Neo4j to be ready..."
sleep 15

echo "Switch db to $DATASET\_$SCALE_FACTOR"
docker compose exec --user root neo4j sh -c "/scripts/activate_db.sh"

echo "Waiting..."
sleep 15

docker compose exec --user root neo4j sh -c "/scripts/fix_access_rights.sh"

docker compose down

echo "Waiting..."
sleep 15

echo "Restart Neo4j"
docker compose up -d

echo "Waiting..."
sleep 15

docker compose exec test-driver sh -c "java -jar app.jar"


