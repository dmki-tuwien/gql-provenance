#!/bin/sh
set -e

echo "LDBC Finbench Running.."
cp -r /home/user/ldbc_finbench_datagen/* /home/user/ldbc_finbench/

cd /home/user/scripts
python3 update_headers.py ${SCALE_FACTOR}

exec tail -f /dev/null