#!/bin/bash

cd ${HOME}

rm -rf /var/lib/neo4j/data/databases/neo4j
rm -rf /var/lib/neo4j/data/transactions/neo4j

/var/lib/neo4j/bin/neo4j-admin database import full \
     --nodes "/import/${DATASET}/sf${SCALE_FACTOR}/snapshot/Account.csv" \
     --nodes "/import/${DATASET}/sf${SCALE_FACTOR}/snapshot/Person.csv" \
     --nodes "/import/${DATASET}/sf${SCALE_FACTOR}/snapshot/Company.csv" \
     --nodes "/import/${DATASET}/sf${SCALE_FACTOR}/snapshot/Loan.csv" \
     --nodes "/import/${DATASET}/sf${SCALE_FACTOR}/snapshot/Medium.csv" \
     --relationships "/import/${DATASET}/sf${SCALE_FACTOR}/snapshot/MediumSignInAccount.csv" \
     --relationships "/import/${DATASET}/sf${SCALE_FACTOR}/snapshot/PersonOwnAccount.csv" \
     --relationships "/import/${DATASET}/sf${SCALE_FACTOR}/snapshot/CompanyOwnAccount.csv" \
     --relationships "/import/${DATASET}/sf${SCALE_FACTOR}/snapshot/AccountTransferAccount.csv" \
     --relationships "/import/${DATASET}/sf${SCALE_FACTOR}/snapshot/AccountWithdrawAccount.csv" \
     --relationships "/import/${DATASET}/sf${SCALE_FACTOR}/snapshot/PersonApplyLoan.csv" \
     --relationships "/import/${DATASET}/sf${SCALE_FACTOR}/snapshot/CompanyApplyLoan.csv" \
     --relationships "/import/${DATASET}/sf${SCALE_FACTOR}/snapshot/LoanDepositAccount.csv" \
     --relationships "/import/${DATASET}/sf${SCALE_FACTOR}/snapshot/AccountRepayLoan.csv" \
     --relationships "/import/${DATASET}/sf${SCALE_FACTOR}/snapshot/PersonInvestCompany.csv" \
     --relationships "/import/${DATASET}/sf${SCALE_FACTOR}/snapshot/CompanyInvestCompany.csv" \
     --relationships "/import/${DATASET}/sf${SCALE_FACTOR}/snapshot/PersonGuaranteePerson.csv" \
     --relationships "/import/${DATASET}/sf${SCALE_FACTOR}/snapshot/CompanyGuaranteeCompany.csv" \
     --delimiter "|" neo4j --verbose

echo "Done importing ${DATASET} data"