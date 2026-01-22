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

//finbench-3
MATCH (src:ACCOUNT {id: $ID1})-[edge1:TRANSFER]->(dst:ACCOUNT {id: $ID2}),
(src)<-[edge2:TRANSFER]-(other:ACCOUNT)<-[edge3:TRANSFER]-(dst)
WHERE $START_TIME < edge1.createTime < $END_TIME
AND $START_TIME < edge2.createTime < $END_TIME
AND $START_TIME < edge3.createTime < $END_TIME
RETURN other.id, edge2, edge2.amount, edge3, edge3.amount
ORDER BY edge2.amount+edge3.amount DESC;

//finbench-4.3
MATCH (person:PERSON {id: $ID})-[edge1:OWN]->(src:ACCOUNT),
p=(src)-[edge2:TRANSFER]->{1,3}(dst:ACCOUNT)
RETURN p AS path1
ORDER BY path1 DESC;

//finbench-4.1
MATCH (person:PERSON {id: $ID})-[edge1:OWN]->(src:ACCOUNT),
p=(src)-[edge2:TRANSFER]->(dst:ACCOUNT)
RETURN p AS path1;

//finbench-4.2
MATCH (person:PERSON {id: $ID})-[edge1:OWN]->(src:ACCOUNT),
p=(src)-[edge2:TRANSFER]->{1,2}(dst:ACCOUNT)
RETURN p AS path1;

//finbench-4.4
MATCH (person:PERSON {id: $ID})-[edge1:OWN]->(src:ACCOUNT),
p=(src)-[edge2:TRANSFER]->{1,4}(dst:ACCOUNT)
RETURN p AS path1;

//finbench-5
MATCH (src1:ACCOUNT)-[edge1:TRANSFER]->(mid:ACCOUNT)-[edge2:WITHDRAW]->(dstCard:ACCOUNT {id: $ID, accoutType: 'debit card'})
WHERE $START_TIME < edge1.createTime < $END_TIME AND edge1.amount > $THRESHOLD1
AND $START_TIME < edge2.createTime < $END_TIME AND edge2.amount > $THRESHOLD2
RETURN mid.id AS midId, edge1.amount AS edge1Amount, edge2.amount AS edge2Amount
ORDER BY edge2Amount DESC;

//finbench-6
MATCH (src:ACCOUNT)-[edge1:TRANSFER|WITHDRAW]->(mid:ACCOUNT {id: $ID})-[edge2:TRANSFER|WITHDRAW]->(dst:ACCOUNT)
WHERE $START_TIME < edge1.createTime < $END_TIME AND edge1.amount > $THRESHOLD
AND $START_TIME < edge2.createTime < $END_TIME AND edge2.amount > $THRESHOLD
RETURN src AS Src, dst AS Dst, edge1.amount AS edge1Amount, edge2.amount AS edge2Amount;

//finbench-7.3
MATCH
(loan:LOAN {id: $ID})-[edge1:DEPOSIT]->(src:ACCOUNT),
p=(src)-[edge234:TRANSFER|WITHDRAW]->{1,3}(dst:ACCOUNT)
WHERE // enforce that the timestamps of edge1 and all edge234 edges are within the selected window
$START_TIME < edge1.createTime < $END_TIME
RETURN dst.id AS dstId, loan.loanAmount AS loanAmount
ORDER BY loanAmount DESC;

//finbench-7.1
MATCH
(loan:LOAN {id: $ID})-[edge1:DEPOSIT]->(src:ACCOUNT),
p=(src)-[edge234:TRANSFER|WITHDRAW]->(dst:ACCOUNT)
WHERE // enforce that the timestamps of edge1 and all edge234 edges are within the selected window
$START_TIME < edge1.createTime < $END_TIME
RETURN dst.id AS dstId, loan.loanAmount AS loanAmount
ORDER BY loanAmount DESC;

//finbench-7.2
MATCH
(loan:LOAN {id: $ID})-[edge1:DEPOSIT]->(src:ACCOUNT),
p=(src)-[edge234:TRANSFER|WITHDRAW]->{1,2}(dst:ACCOUNT)
WHERE // enforce that the timestamps of edge1 and all edge234 edges are within the selected window
$START_TIME < edge1.createTime < $END_TIME
RETURN dst.id AS dstId, loan.loanAmount AS loanAmount
ORDER BY loanAmount DESC;

//finbench-7.4
MATCH
(loan:LOAN {id: $ID})-[edge1:DEPOSIT]->(src:ACCOUNT),
p=(src)-[edge234:TRANSFER|WITHDRAW]->{1,4}(dst:ACCOUNT)
WHERE // enforce that the timestamps of edge1 and all edge234 edges are within the selected window
$START_TIME < edge1.createTime < $END_TIME
RETURN dst.id AS dstId, loan.loanAmount AS loanAmount
ORDER BY loanAmount DESC;

//finbench-8
MATCH (loan:LOAN)-[edge1:DEPOSIT]->(mid:ACCOUNT {id: $ID})-[edge2:REPAY]->(loan),
(up:ACCOUNT)-[edge3:TRANSFER]->(mid)-[edge4:TRANSFER]->(down:ACCOUNT)
WHERE edge1.amount > $THRESHOLD AND $START_TIME < edge1.createTime < $END_TIME
AND edge2.amount > $THRESHOLD AND $START_TIME < edge2.createTime < $END_TIME
AND edge3.amount > $THRESHOLD AND $START_TIME < edge3.createTime < $END_TIME
AND edge4.amount > $THRESHOLD AND $START_TIME < edge4.createTime < $END_TIME
RETURN edge1.amount AS edge1Amount, edge2.amount AS edge2Amount, edge3.amount AS edge3Amount, edge4.amount AS edge4Amount;

//finbench-9
MATCH path1=(comp:COMPANY)<-[:INVEST]-{1,3}(investor{id: $PID})
WHERE (investor:COMPANY) OR (investor:PERSON)
RETURN comp.id, investor, investor.business as type
ORDER BY comp.id DESC;

//finbench-10
MATCH path1=(p1:PERSON{id: $ID})-[:GUARANTEE]->{0,}(pX:PERSON)
MATCH (pX)-[:APPLY]->(loan:LOAN)
RETURN loan.loanAmount AS loanAmount, pX.id AS id;

//finbench-11
MATCH (person:PERSON {id: $ID})-[edge1:OWN]->(pAcc:ACCOUNT)-[edge2:TRANSFER]->(compAcc:ACCOUNT)<-[edge3:OWN]-(company:COMPANY)
WHERE $START_TIME < edge2.createTime < $END_TIME
RETURN compAcc.id AS compAccountId, edge2.amount AS edge2Amount
ORDER BY edge2Amount DESC;

//finbench-12
MATCH (account:ACCOUNT {id: $ID})
RETURN account.createTime, account.isBlocked, account.accoutType;

//finbench-13
MATCH (src:ACCOUNT {id: $ID})
MATCH (src)-[edge1:TRANSFER]->(dst1:ACCOUNT)
WHERE $START_TIME < edge1.createTime < $END_TIME
MATCH (src)<-[edge2:TRANSFER]-(dst2:ACCOUNT)
WHERE $START_TIME < edge2.createTime < $END_TIME
RETURN edge1.amount AS edge1Amount, edge2.amount AS edge2Amount;

//finbench-14
MATCH (src:ACCOUNT)-[edge2:TRANSFER]->(dst:ACCOUNT {id: $ID})
MATCH (blockedSrc:ACCOUNT {accountLevel:'Basic level'})-[edge1:TRANSFER]->(dst)
WHERE $START_TIME < edge1.createTime < $END_TIME
AND edge1.amount > $THRESHOLD
RETURN edge1, edge2

//finbench-15
MATCH (src:ACCOUNT {id: $ID})-[edge:TRANSFER]->(dst:ACCOUNT)
WHERE $START_TIME < edge.createTime < $END_TIME
AND edge.amount > $THRESHOLD
RETURN dst.id AS dstId, edge AS edge, edge.amount AS amount

//finbench-16
MATCH (dst:ACCOUNT {id: $ID})<-[edge:TRANSFER]-(src:ACCOUNT)
WHERE $START_TIME < edge.createTime < $END_TIME
AND edge.amount > $THRESHOLD
RETURN src.id AS srcId, edge, edge.amount AS Amount

//finbench-17
MATCH (src:ACCOUNT {id: $ID})<-[e1:TRANSFER]-(mid:ACCOUNT)-[e2:TRANSFER]->(dst:ACCOUNT {isBlocked: true})
WHERE src.id <> dst.id
AND $START_TIME < e1.createTime < $END_TIME
AND $START_TIME < e2.createTime < $END_TIME
RETURN dst.id AS dstId

//finbench-18
MATCH (src:ACCOUNT {id: $ID})
MATCH (src)<-[edge1:TRANSFER]-(dst1:ACCOUNT), (src)-[edge2:TRANSFER]->(dst2:ACCOUNT)
FILTER $START_TIME < edge1.createTime < $END_TIME
AND $START_TIME < edge2.createTime < $END_TIME
AND edge1.createTime < edge2.createTime
RETURN edge1.amount AS edge1Amount, edge2.amount AS edge2Amount;

//finbench-19
CALL(){
MATCH (mid:ACCOUNT)-[edge2:WITHDRAW]->(dstCard:ACCOUNT {id: $ID ,accoutType: 'debit card'})
WHERE $START_TIME < edge2.createTime < $END_TIME AND edge2.amount > $THRESHOLD2
RETURN mid, edge2
}
MATCH (src1:ACCOUNT)-[edge1:TRANSFER]->(mid)
WHERE $START_TIME < edge1.createTime < $END_TIME AND edge1.amount > $THRESHOLD1
RETURN mid.id AS id, edge1.amount AS edge1Amount, edge2.amount AS edge2Amount
ORDER BY edge2Amount DESC;

//finbench-20
MATCH (src:ACCOUNT {id: $ID})
CALL (src){
  MATCH (src)-[edge:TRANSFER]->(dst1:ACCOUNT)
  WHERE $START_TIME < edge.createTime < $END_TIME
  RETURN edge.amount AS amount
  UNION
  MATCH (src)<-[edge:TRANSFER]-(dst2:ACCOUNT)
  WHERE $START_TIME < edge.createTime < $END_TIME
  RETURN edge.amount AS amount
}
RETURN src, amount;