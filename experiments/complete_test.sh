#!/usr/bin/env bash

set -euo pipefail

DATE=$(date +%F)

CONFIGS=(
  "finbench 0.1"
  "finbench 0.3"
  "finbench 1"
  "finbench 3"
  "snb 1"
  "snb 3"
)

ENV_TEMPLATE=".env_tmp"
ENV_FILE=".env"
LOG_FILE="${DATE}-test_runs.log"


for cfg in "${CONFIGS[@]}"; do
  read -r DATASET SCALE_FACTOR <<< "$cfg"

  export DATE
  export DATASET
  export SCALE_FACTOR

  # Replace variables and create .env
  envsubst < "$ENV_TEMPLATE" > "$ENV_FILE"

  START_TIME=$(date +"%Y-%m-%d %H:%M:%S")
  echo "[$START_TIME] START  DATASET=$DATASET SCALE_FACTOR=$SCALE_FACTOR DATE=$DATE" | tee -a "$LOG_FILE"

  # Run the test
  ./test_neo4j.sh

  END_TIME=$(date +"%Y-%m-%d %H:%M:%S")
  echo "[$END_TIME] END    DATASET=$DATASET SCALE_FACTOR=$SCALE_FACTOR DATE=$DATE" | tee -a "$LOG_FILE"
done
