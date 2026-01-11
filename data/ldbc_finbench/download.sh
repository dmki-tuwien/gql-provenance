#!/bin/bash

OUTPUT_DIR=$1/finbench
#/data/pgprov
#/home/user/data/

declare -A DATASET_MAPPING=(
  ["0.1"]="https://datasets.ldbcouncil.org/finbench/sf0.1.tar.gz"
  ["1"]="https://datasets.ldbcouncil.org/finbench/sf1.tar.gz"
  ["10"]="https://datasets.ldbcouncil.org/finbench/sf10.tar.gz"
)

declare -A PARAMS_MAPPING=(
  ["1"]="https://datasets.ldbcouncil.org/finbench/sf1_read_params.zip"
  ["10"]="https://datasets.ldbcouncil.org/finbench/sf10_read_params.zip"
)

mkdir -p OUTPUT_DIR
cd $OUTPUT_DIR

for scale in "${!DATASET_MAPPING[@]}"; do
  url="${DATASET_MAPPING[$scale]}"
  archive="$(basename "$url")"
  extract_dir="${archive%.tar.gz}"

  echo "Dataset scale factor: $scale"

  # Download
  if [ ! -f "$archive" ]; then
    echo "Downloading $archive"
    curl -L -o "$archive" "$url"
  else
    echo "✓ $archive already downloaded"
  fi

  # Extract
  if [ ! -d "$extract_dir" ]; then
    echo "Extracting $archive"
    tar -xzf "$archive"
  else
    echo "✓ $extract_dir already extracted"
  fi

  echo
done

for scale in "${!PARAMS_MAPPING[@]}"; do
  url="${PARAMS_MAPPING[$scale]}"
  zip_file="$(basename "$url")"

  dataset_dir="$OUTPUT_DIR/sf$scale"
  param_dir="$dataset_dir/param"

  echo "Params for scale factor $scale"

  # Ensure dataset folder exists
  if [ ! -d "$dataset_dir" ]; then
    echo "Dataset directory $dataset_dir does not exist — skipping"
    continue
  fi

  mkdir -p "$param_dir"
  cd "$param_dir"

  # Download zip
  if [ ! -f "$zip_file" ]; then
    echo "Downloading $zip_file"
    curl -L -o "$zip_file" "$url"
  else
    echo "✓ $zip_file already downloaded"
  fi

  # Extract zip
  if [ -z "$(ls -A . | grep -v "$zip_file")" ]; then
    echo "📂 Extracting $zip_file"
    unzip -o "$zip_file"
  else
    echo "Params already extracted"
  fi

  echo

done