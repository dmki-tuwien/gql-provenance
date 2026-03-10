#!/bin/bash

#https://repository.surfsara.nl/datasets/cwi/ldbc-snb-bi#files
OUTPUT_DIR=$1/snb
PARAMS_DIR=$1/params/snb
#/data/pgprov

declare -A DATASET_MAPPING=(
  ["1"]="https://repository.surfsara.nl/datasets/cwi/ldbc-snb-bi/files/bi-sf1-composite-projected-fk.tar.zst"
  ["3"]="https://repository.surfsara.nl/datasets/cwi/ldbc-snb-bi/files/bi-sf3-composite-projected-fk.tar.zst"
  ["10"]="https://repository.surfsara.nl/datasets/cwi/ldbc-snb-bi/files/bi-sf10-composite-projected-fk.tar.zst"
)

PARAMS_URL="https://repository.surfsara.nl/datasets/cwi/ldbc-snb-bi/files/bi-parameters-sf1-to-sf30000.zip"

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

    echo "Removing headers.."
    find . -type f -name '*.gz' -exec gunzip -f {} \;
    find . -type f -name 'part-*.csv' -exec sh -c 'tail -n +2 "$0" > "$0.tmp" && mv "$0.tmp" "$0"' {} \;
    echo "Done."
  else
    echo "✓ $extract_dir already extracted"
  fi
  echo

done

url="${PARAMS_URL}"
zip_file="$(basename "$url")"

mkdir -p "${PARAMS_DIR}/"
cd "${PARAMS_DIR}/"

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
#        curl ${STAGING_URL} --data-raw 'share-token='
        echo "Staging initiated through ${STAGING_URL}"
        echo "Wait for 30 seconds"
        sleep 30
  done

  echo "Downloading parameters"
  wget --no-check-certificate -O $zip_file ${url}

else
  echo "$zip_file already downloaded"
fi

# Extract zip
if [ -z "$(ls -A . | grep -v "$zip_file")" ]; then
  echo "Extracting $zip_file"
  python3 -m zipfile -e $zip_file .
else
  echo "Params already extracted"
fi