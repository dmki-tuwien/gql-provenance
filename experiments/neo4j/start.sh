#!/bin/bash

echo "Run Neo4j"
cp ../env/run.env .env
docker compose up -d

