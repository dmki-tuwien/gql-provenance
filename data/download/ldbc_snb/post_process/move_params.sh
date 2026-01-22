#!/usr/bin/env bash

set -euo pipefail

param_folder=$1

# mapping: target file -> source file
declare -A FILE_MAP=(
  [1]=1
  [2]=1
  [3]=2
  [4]=5
  [5]=8
  [6]=12
  [7]=3
  [8]=6
  [9]=8
  [10]=11
)

for file in "${!FILE_MAP[@]}"; do
  dst="params_${file}.csv"
  dstfilepath="$param_folder/$dst"

  srcfilepath="$param_folder/interactive_${FILE_MAP[$file]}_param.txt"

  if [[ ! -f "$srcfilepath" ]]; then
    echo "Source file missing: $srcfilepath"
    continue
  fi

  echo "Copying $srcfilepath -> $dstfilepath"
  cp "$srcfilepath" "$dstfilepath"
done

