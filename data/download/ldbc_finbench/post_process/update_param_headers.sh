#!/usr/bin/env bash

set -euo pipefail

param_folder=$1

## Update params headers
## For sf 0.1
##sed -i '1s/.*/id1|id2|startTime|endTime/' /data/pgprov/params/finbench/sf_0.3/sf0.3_read_params/complex_3_param.csv
##sed -i '1s/.*/id1|id2|startTime|endTime|truncationLimit|truncationOrder/' /data/pgprov/params/finbench/sf_0.3/sf0.3_read_params/complex_3_param.csv
##sed -i '1s/.*/id1|id2|startTime|endTime|truncationLimit|truncationOrder/' /data/pgprov/params/finbench/sf_0.3/sf0.3_read_params/complex_4_param.csv
##sed -i '1s/.*/pid1|pid2|startTime|endTime/' /data/pgprov/params/finbench/sf_0.1/sf0.1_read_params/complex_10_param.csv
##sed -i '1s/.*/id|threshold|startTime|endTime|truncationLimit|truncationOrder/' /data/pgprov/params/finbench/sf_1/sf1_read_params/complex_8_param.csv
##sed -i '1s/.*/id|startTime|endTime|truncationLimit|truncationOrder/' /data/pgprov/params/finbench/sf_1/sf1_read_params/complex_5_param.csv

# header mappings
declare -A HEADERS=(
  [1]="id|startTime|endTime|truncationLimit|truncationOrder"
  [2]="id|startTime|endTime|truncationLimit|truncationOrder"
  [3]="id1|id2|startTime|endTime|truncationLimit|truncationOrder"
  [4]="id1|id2|startTime|endTime"
  [5]="id|startTime|endTime|truncationLimit|truncationOrder"
#  [3]="id1|id2|startTime|endTime"
  [6]="id|threshold1|threshold2|startTime|endTime|truncationLimit|truncationOrder"
  [7]="id|threshold|startTime|endTime|truncationLimit|truncationOrder"
  [8]="id|threshold|startTime|endTime|truncationLimit|truncationOrder"
  [10]="pid1|pid2|startTime|endTime"

)

# replace header
for file in "${!HEADERS[@]}"; do
  filename="complex_${file}_param.csv"
  filepath="$param_folder/$filename"

  header="${HEADERS[$file]}"

  if [[ ! -f "$filepath" ]]; then
    echo "Skipping missing file: $filepath"
    continue
  fi

  first_line=$(head -n 1 "$filepath")

  tmp=$(mktemp)

  if [[ "$first_line" == "..." ]]; then
    echo "Replacing '...' header in $filename"
    {
      echo "$header"
      tail -n +2 "$filepath"
    } > "$tmp"

  else
    echo "Prepending header to $filename"
    {
      echo "$header"
      cat "$filepath"
    } > "$tmp"
  fi

  mv "$tmp" "$filepath"
done
