#!/usr/bin/env bash
set -e

echo "LDBC Finbench Running for scale factor ${SCALE_FACTOR}.."

if [ "$GENERATE_PARAMS" = "1" ]; then
  echo "Starting parameter generation..."
  cp -r /home/user/ldbc_finbench_datagen/* /home/user/ldbc_finbench/

  cd /home/user/ldbc_finbench
  chmod +x ./scripts/run_paramgen.sh

  FACTOR_DIR="/home/user/data/sf${SCALE_FACTOR}/"

  sed -i 's|^OUTPUT_DIR=out/|OUTPUT_DIR=${FACTOR_DIR}|' ./scripts/run_paramgen.sh
  ./scripts/run_paramgen.sh

  sed -i 's|28 \*|365 *|g' ./paramgen/time_select.py

  if [ "$SCALE_FACTOR" = "0.3" ]; then
    ## Adjust the random parameter ratio for small scale factors
    sed -i 's/final_first_items = search_params\.generate(first_array, 0\.01)/final_first_items = search_params.generate(first_array, 0.1)/' parameter_curation.py
  fi
  sed -i 's|chunks = np.array_split(neighbors_df, query_parallelism)|indices = np.array_split(neighbors_df.index, query_parallelism); chunks = [neighbors_df.loc[idx] for idx in indices]|' parameter_curation.py
  python3 paramgen/parameter_curation.py ${FACTOR_DIR}/factor_table/ /home/user/params/
else
  echo "Skipping parameter generation..."
fi

cd /home/user/scripts

echo "Move parameters"
./move_params.sh /home/user/params/

echo "Update the csv headers"
python3 update_headers.py "$SCALE_FACTOR"

echo "Done."

exec tail -f /dev/null