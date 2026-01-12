#!/bin/bash

OUTPUT_DIR=$1/snb
#/data/pgprov
#/home/user/data/

declare -A DATASET_MAPPING=(
#  ["0.1"]="https://repository.surfsara.nl/datasets/cwi/ldbc-snb-interactive-v1-datagen-v100/files/social_network-sf0.1-CsvBasic-LongDateFormatter.tar.zst"
  ["1"]="https://repository.surfsara.nl/datasets/cwi/ldbc-snb-interactive-v1-datagen-v100/files/social_network-sf1-CsvBasic-LongDateFormatter.tar.zst"
  ["10"]="https://repository.surfsara.nl/datasets/cwi/ldbc-snb-interactive-v1-datagen-v100/files/social_network-sf10-CsvBasic-LongDateFormatter.tar.zst"
)

declare -A PARAMS_MAPPING=(
#  ["0.1"]="https://repository.surfsara.nl/datasets/cwi/snb/files/substitution_parameters/substitution_parameters-sf0.1.tar.zst"
  ["1"]="https://repository.surfsara.nl/datasets/cwi/snb/files/substitution_parameters/substitution_parameters-sf1.tar.zst"
  ["10"]="https://repository.surfsara.nl/datasets/cwi/snb/files/substitution_parameters/substitution_parameters-sf10.tar.zst"
)

mkdir -p $OUTPUT_DIR

for scale in "${!DATASET_MAPPING[@]}"; do

  cd $OUTPUT_DIR

  url="${DATASET_MAPPING[$scale]}"
  archive="$(basename "$url")"
  extract_dir="${archive%.tar.zst}"

  echo "Dataset scale factor: $scale"

  # Download
  if [ ! -f "$archive" ]; then
    echo "Downloading $archive"

  echo "Preparing to download ${url}"
  while [[ $(curl -k -sI ${url} | grep -q 'HTTP/1.1 409 Conflict') ]]; do
      echo "Data set is not staged, attempting to stage..."
      STAGING_URL=$(curl --silent ${url} | grep -Eo 'https:\\/\\/repository.surfsara.nl\\/api\\/objects\\/cwi\\/[A-Za-z0-9_-]+\\/stage\\/[0-9]+' | sed 's#\\##g')

      if [[ -z ${STAGING_URL} ]]; then
          echo "Could not retrieve staging URL, exiting..."
          exit 1
      fi
      curl ${STAGING_URL} --data-raw 'share-token='
      echo "Staging initiated through ${STAGING_URL}"
      echo "Wait for 30 seconds"
      sleep 30
  done

  echo "Downloading data set"
  wget --no-check-certificate -O $OUTPUT_DIR/$archive ${url}

  else
    echo "✓ $archive already downloaded"
  fi

  # Extract
  if [ ! -d "$extract_dir" ]; then
    echo "Extracting $archive"
    tar -xvf "$archive" --use-compress-program=unzstd
  else
    echo "✓ $extract_dir already extracted"
  fi

  url="${PARAMS_MAPPING[$scale]}"
  zip_file="$(basename "$url")"

  dataset_dir="$OUTPUT_DIR/$extract_dir"
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

    echo "Preparing to download ${url}"
    while [[ $(curl -k -sI ${url} | grep -q 'HTTP/1.1 409 Conflict') ]]; do
        echo "Data set is not staged, attempting to stage..."
        STAGING_URL=$(curl --silent ${url} | grep -Eo 'https:\\/\\/repository.surfsara.nl\\/api\\/objects\\/cwi\\/[A-Za-z0-9_-]+\\/stage\\/[0-9]+' | sed 's#\\##g')

        if [[ -z ${STAGING_URL} ]]; then
            echo "Could not retrieve staging URL, exiting..."
            exit 1
        fi
        curl ${STAGING_URL} --data-raw 'share-token='
        echo "Staging initiated through ${STAGING_URL}"
        echo "Wait for 30 seconds"
        sleep 30
    done

    echo "Downloading data set"
    wget --no-check-certificate -O $param_dir/$zip_file ${url}

  else
    echo "$zip_file already downloaded"
  fi

  # Extract zip
  if [ -z "$(ls -A . | grep -v "$zip_file")" ]; then
    echo "Extracting $zip_file"
    tar -xvf "$zip_file" --use-compress-program=unzstd
  else
    echo "Params already extracted"
  fi

  echo

done