#!/bin/bash

cd ${HOME}

rm -rf /var/lib/neo4j/data/databases/${DB_NAME}
rm -rf /var/lib/neo4j/data/transactions/${DB_NAME}

./bin/neo4j-admin database import full \
     --nodes "/import/${DATASET}/sf${SCALE_FACTOR}/raw/account/part-([A-Za-z0-9-]+).csv" \
     --nodes "/import/${DATASET}/sf${SCALE_FACTOR}/raw/person/part-([A-Za-z0-9-]+).csv" \
     --nodes "/import/${DATASET}/sf${SCALE_FACTOR}/raw/company/part-([A-Za-z0-9-]+).csv" \
     --nodes "/import/${DATASET}/sf${SCALE_FACTOR}/raw/loan/part-([A-Za-z0-9-]+).csv" \
     --nodes "/import/${DATASET}/sf${SCALE_FACTOR}/raw/medium/part-([A-Za-z0-9-]+).csv" \
     --relationships "/import/${DATASET}/sf${SCALE_FACTOR}/raw/signIn/part-([A-Za-z0-9-]+).csv" \
     --relationships "/import/${DATASET}/sf${SCALE_FACTOR}/raw/personOwnAccount/part-([A-Za-z0-9-]+)\.csv" \
     --relationships "/import/${DATASET}/sf${SCALE_FACTOR}/raw/companyOwnAccount/part-([A-Za-z0-9-]+)\.csv" \
     --relationships "/import/${DATASET}/sf${SCALE_FACTOR}/raw/transfer/part-([A-Za-z0-9-]+)\.csv" \
     --relationships "/import/${DATASET}/sf${SCALE_FACTOR}/raw/withdraw/part-([A-Za-z0-9-]+)\.csv" \
     --relationships "/import/${DATASET}/sf${SCALE_FACTOR}/raw/personApplyLoan/part-([A-Za-z0-9-]+)\.csv" \
     --relationships "/import/${DATASET}/sf${SCALE_FACTOR}/raw/companyApplyLoan/part-([A-Za-z0-9-]+)\.csv" \
     --relationships "/import/${DATASET}/sf${SCALE_FACTOR}/raw/deposit/part-([A-Za-z0-9-]+)\.csv" \
     --relationships "/import/${DATASET}/sf${SCALE_FACTOR}/raw/repay/part-([A-Za-z0-9-]+)\.csv" \
     --relationships "/import/${DATASET}/sf${SCALE_FACTOR}/raw/personInvest/part-([A-Za-z0-9-]+)\.csv" \
     --relationships "/import/${DATASET}/sf${SCALE_FACTOR}/raw/companyInvest/part-([A-Za-z0-9-]+)\.csv" \
     --relationships "/import/${DATASET}/sf${SCALE_FACTOR}/raw/personGuarantee/part-([A-Za-z0-9-]+)\.csv" \
     --relationships "/import/${DATASET}/sf${SCALE_FACTOR}/raw/companyGuarantee/part-([A-Za-z0-9-]+)\.csv" \
     --delimiter "|" ${DB_NAME} --verbose

echo "Done importing data"