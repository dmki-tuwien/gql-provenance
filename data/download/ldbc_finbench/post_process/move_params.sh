#!/usr/bin/env bash

set -euo pipefail

param_folder=$1

# mapping: target file -> source file
declare -A FILE_MAP=(
  [1]=1
  [1.1.1]=1
  [1.1]=1
  [1.2]=1
  [1.3]=1
  [1.4]=1
  [1.5]=1
  [1.6]=1
  [2]=2
  [2.1.1]=2
  [2.1]=2
  [2.2]=2
  [2.3]=2
  [2.4]=2
  [2.5]=2
  [2.6]=2
  [3]=4
  [4]=5
  [4.1.1]=5
  [4.1]=5
  [4.2]=5
  [4.3]=5
  [4.4]=5
  [4.5]=5
  [4.6]=5
  [5]=6
  [6]=7
  [7]=8
  [7.1.1]=8
  [7.1]=8
  [7.2]=8
  [7.3]=8
  [7.4]=8
  [7.5]=8
  [7.6]=8
  [8]=9
  [9]=10
  [9.1.1]=10
  [9.1]=10
  [9.2]=10
  [9.3]=10
  [9.4]=10
  [9.5]=10
  [9.6]=10
  [10]=11
  [11]=12
  [12]=1
  [13]=1
  [14]=7
  [15]=7
  [16]=7
  [17]=7
  [18]=1
  [19]=6
  [20]=1
#  [1]=1
#  [2]=1
#  [3]=3
#  [4]=7
#  [5]=7
#  [6]=7
#  [7]=6
#  [8]=1
#  [9]=2
#  [10]=1
)

for file in "${!FILE_MAP[@]}"; do
  dst="params_${file}.csv"
  dstfilepath="$param_folder/$dst"

  srcfilepath="$param_folder/complex_${FILE_MAP[$file]}_param.csv"

  if [[ ! -f "$srcfilepath" ]]; then
    echo "Source file missing: $srcfilepath"
    continue
  fi

  echo "Copying $srcfilepath -> $dstfilepath"
  cp "$srcfilepath" "$dstfilepath"
done

