#!/usr/bin/env bash
set -e

echo "LDBC Finbench Running.."
cp -r /home/user/ldbc_finbench_datagen/* /home/user/ldbc_finbench/





IFS=',' read -ra SCALES <<< "$SCALE_FACTORS"

for scale in "${SCALES[@]}"; do

  cd /home/user/ldbc_finbench
#  chmod +x ./scripts/run_paramgen.sh
#  sed -i 's|^OUTPUT_DIR=out/|OUTPUT_DIR=/home/user/data/sf0.3/|' ./scripts/run_paramgen.sh
#  cat ./scripts/run_paramgen.sh
#
#  ./scripts/run_paramgen.sh

  sed -i 's|28 \*|365 *|g' ./paramgen/time_select.py
  python3 paramgen/parameter_curation.py /home/user/data/sf${scale}/factor_table/ /home/user/params/

  cd /home/user/scripts
  echo "Running for scale factor: $scale"
  python3 update_headers.py "$scale"
done

exec tail -f /dev/null