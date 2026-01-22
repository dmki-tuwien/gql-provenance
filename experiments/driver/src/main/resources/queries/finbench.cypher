//finbench-1
MATCH p=(account:ACCOUNT {id: $ID})-[edge1:TRANSFER]->{1,3}(other:ACCOUNT),
(other)<-[edge2:SIGN_IN]-(medium:MEDIUM {isBlocked: true})
//WITH p, other, medium
WHERE $START_TIME < edge2.createTime < $END_TIME
RETURN other.id AS otherId, p AS accountDistance, medium.id AS mediumId, medium.type AS mediumType
ORDER BY accountDistance ASC;

//finbench-2
MATCH (person:PERSON {id: $ID})-[edge1:OWN]->(accounts:ACCOUNT), p=(accounts)<-[edge2:TRANSFER]-{1,3}(other:ACCOUNT),
(other)<-[edge3:DEPOSIT]-(loan:LOAN)
//WITH p, other, loan
WHERE $START_TIME < edge3.createTime < $END_TIME
RETURN other.id AS otherId, loan.loanAmount AS sumLoanAmount, loan.balance AS sumLoanBalance
ORDER BY sumLoanAmount DESC;

//finbench-2.1
MATCH (person:PERSON {id: $ID})-[edge1:OWN]->(accounts:ACCOUNT)
MATCH p=(accounts)<-[edge2:TRANSFER]-{1,3}(other:ACCOUNT)
MATCH (other)<-[edge3:DEPOSIT]-(loan:LOAN)
//WITH p, other, loan
WHERE $START_TIME < edge3.createTime < $END_TIME
RETURN other.id AS otherId, loan.loanAmount AS sumLoanAmount, loan.balance AS sumLoanBalance
ORDER BY sumLoanAmount DESC;

//finbench-3
MATCH (src:ACCOUNT {id: $ID1})-[edge1:TRANSFER]->(dst:ACCOUNT {id: $ID2}),
(src)<-[edge2:TRANSFER]-(other:ACCOUNT)<-[edge3:TRANSFER]-(dst)
WHERE $START_TIME < edge1.createTime < $END_TIME
AND $START_TIME < edge2.createTime < $END_TIME
AND $START_TIME < edge3.createTime < $END_TIME
RETURN other.id, edge2, edge2.amount, edge3, edge3.amount
ORDER BY edge2.amount+edge3.amount DESC;

//finbench-4
MATCH (person:PERSON {id: $ID})-[edge1:OWN]->(src:ACCOUNT),
p=(src)-[edge2:TRANSFER]->{1,3}(dst:ACCOUNT)
RETURN p AS path1
ORDER BY path1 DESC;
