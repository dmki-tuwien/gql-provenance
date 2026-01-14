//tsr-1
MATCH (account:ACCOUNT {id: $ID})
RETURN account.createTime AS createTime, account.isBlocked AS isBlocked, account.type AS type;

//tsr-2
MATCH (src:ACCOUNT {id: $ID})
//OPTIONAL MATCH (src)-[edge1:TRANSFER]->(dst1:ACCOUNT)
MATCH (src)-[edge1:TRANSFER]->(dst1:ACCOUNT)
  WHERE $START_TIME < edge1.createTime < $END_TIME
//OPTIONAL MATCH (src)<-[edge2:TRANSFER]->(dst2:ACCOUNT)
MATCH (src)<-[edge2:TRANSFER]-(dst2:ACCOUNT)
  WHERE $START_TIME < edge2.createTime < $END_TIME
RETURN
  edge1.amount AS edge1Amount, edge2.amount AS edge2Amount;
//  edge1.amount AS sumEdge1Amount, edge1.amount AS maxEdge1Amount, edge1 AS numEdge1,
//  edge2.amount AS sumEdge2Amount, edge2.amount AS maxEdge2Amount, edge2 AS numEdge2;

//tsr-3
MATCH (src:ACCOUNT)-[edge2:TRANSFER]->(dst:ACCOUNT {id: $ID})
//OPTIONAL MATCH (blockedSrc:ACCOUNT {isBlocked: true})-[edge1:TRANSFER]->(dst)
MATCH (blockedSrc:ACCOUNT {isBlocked: true})-[edge1:TRANSFER]->(dst)
  WHERE $START_TIME< edge1.createTime < $END_TIME
RETURN edge1, edge2;
// removed rounding RETURN round(1000*count(edge1)/count(edge2)) / 1000 AS blockRatio

//tsr-4
MATCH (src:ACCOUNT {id: $ID})-[edge:TRANSFER]->(dst:ACCOUNT)
  WHERE $START_TIME < edge.createTime < $END_TIME
  AND edge.amount > $THRESHOLD
RETURN dst.id AS dstId, edge, edge.amount AS amount
  ORDER BY amount DESC, dstId ASC;

//tsr-5
MATCH (dst:ACCOUNT {id: $ID})<-[edge:TRANSFER]-(src:ACCOUNT)
  FILTER $START_TIME < edge.createTime < $END_TIME
  AND edge.amount > $THRESHOLD
RETURN src.id AS srcId, edge, edge.amount AS amount
  ORDER BY amount DESC, srcId ASC;

//tsr-6
MATCH (src:ACCOUNT {id: $ID})<-[e1:TRANSFER]-(mid:ACCOUNT)-[e2:TRANSFER]->(dst:ACCOUNT {isBlocked: true})
  WHERE src.id <> dst.id
  AND $START_TIME < e1.createTime < $END_TIME
  AND $START_TIME < e2.createTime < $END_TIME
RETURN dst.id AS dstId
  ORDER BY dstId ASC;
// RETURN collect(dst.id) AS dstId ORDER BY dstId ASC

//tsr-7
CALL(){
MATCH (src:ACCOUNT {id: $ID})
MATCH (src)-[edge1:TRANSFER]->(dst1:ACCOUNT)
  WHERE $START_TIME < edge1.createTime < $END_TIME
RETURN src, edge1
}
MATCH (src)<-[edge2:TRANSFER]-(dst2:ACCOUNT)
  WHERE $START_TIME < edge2.createTime < $END_TIME
RETURN
edge1.amount AS edge1Amount, edge2.amount AS edge2Amount;

//tsr-8
MATCH p=(account: ACCOUNT{id: $ID})-[edge1:TRANSFER*1..3]->(other:ACCOUNT),(other)<-[edge2:SIGN_IN]-(medium:MEDIUM {isBlocked: true})
WHERE $START_TIME < edge2.createTime < $END_TIME
RETURN other.id AS otherId, length(p) AS accountDistance, medium.id AS mediumId, medium.type AS mediumType
ORDER BY accountDistance ASC;

//tsr-9
MATCH (person:PERSON {id: $ID })-[edge1:OWN]->(accounts:ACCOUNT), p=(accounts)<-[edge2:TRANSFER*1..3]-(other:ACCOUNT),(other)<-[edge3:DEPOSIT]-(loan:LOAN)
WHERE $START_TIME < edge3.createTime < $END_TIME
RETURN other.id AS otherId, loan.loanAmount AS loanAmount, loan.balance AS loanBalance
ORDER BY loanAmount DESC, otherId ASC;

//tsr-10
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
