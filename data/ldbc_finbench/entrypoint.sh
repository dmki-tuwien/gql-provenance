#!/usr/bin/env bash
set -e

echo "LDBC Finbench Running.."
cp -r /home/user/ldbc_finbench_datagen/* /home/user/ldbc_finbench/

cd /home/user/scripts

IFS=',' read -ra SCALES <<< "$SCALE_FACTORS"

for scale in "${SCALES[@]}"; do
  echo "Running for scale factor: $scale"
  python3 update_headers.py "$scale"
done

exec tail -f /dev/null