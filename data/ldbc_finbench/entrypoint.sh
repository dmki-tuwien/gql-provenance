#!/bin/sh
set -e

echo "LDBC Finbench Running.."
cp -r /home/user/ldbc_finbench_datagen/* /home/user/ldbc_finbench/
exec tail -f /dev/null