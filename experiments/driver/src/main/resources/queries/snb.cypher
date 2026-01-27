//snb-1
MATCH (message:Message)
  WHERE message.creationDate < $date
MATCH (message:Message)
  WHERE message.creationDate < $date
  AND message.content IS NOT NULL
RETURN
  message.length AS mlength,
  message.creationDate AS creationDate,
  message:Comment AS isComment
  ORDER BY
  creationDate DESC,
  isComment ASC,
  mlength ASC

//snb-2
MATCH (tag:Tag)-[:HAS_TYPE]->(:TagClass {name: $tagClass})
MATCH (message1:Message)-[:HAS_TAG]->(tag)
  WHERE $dateonly <= message1.creationDate
  AND message1.creationDate < $dateonly + duration({days: 100})
MATCH (message2:Message)-[:HAS_TAG]->(tag)
  WHERE $dateonly + duration({days: 100}) <= message2.creationDate
  AND message2.creationDate < $dateonly + duration({days: 200})
RETURN
  tag.name,
  message1.length AS message1Length,
  message2.length AS message2Length,
  abs(message1.length - message2.length) AS diff
  ORDER BY
  diff DESC,
  tag.name ASC
  LIMIT 100

//snb-3
MATCH
  (:Country {name: $country})<-[:IS_PART_OF]-(:City)<-[:IS_LOCATED_IN]-
  (person:Person)<-[:HAS_MODERATOR]-(forum:Forum)-[:CONTAINER_OF]->
  (post:Post)<-[:REPLY_OF]-{0,}(message:Message)-[:HAS_TAG]->(:Tag)-[:HAS_TYPE]->(:TagClass {name: $tagClass})
RETURN DISTINCT
forum.id,
forum.title,
forum.creationDate,
person.id,
message
ORDER BY
forum.id ASC
LIMIT 20

//snb-4
MATCH (country:Country)<-[:IS_PART_OF]-(:City)<-[:IS_LOCATED_IN]-(person:Person)<-[:HAS_MEMBER]-(forum:Forum)
WHERE forum.creationDate > $dateonly
ORDER BY forum.id ASC, country.id
LIMIT 100
CALL (forum){
MATCH (forum)-[:CONTAINER_OF]->(post:Post)<-[:REPLY_OF]-{0,}(message:Message)-[:HAS_CREATOR]->(person:Person)<-[:HAS_MEMBER]-(topForum2:Forum)
RETURN message
UNION ALL
MATCH (person:Person)<-[:HAS_MEMBER]-(forum:Forum)
RETURN 0 AS message
}
RETURN
person.id AS personId,
person.firstName AS personFirstName,
person.lastName AS personLastName,
person.creationDate AS personCreationDate,
message
ORDER BY
person.id ASC
LIMIT 100

//snb-5
MATCH (tag:Tag {name: $tag})<-[:HAS_TAG]-(message:Message)-[:HAS_CREATOR]->(person:Person)
MATCH (message)<-[likes:LIKES]-(:Person)
MATCH (message)<-[:REPLY_OF]-(reply:Comment)
RETURN
person.id,
reply, message,likes
ORDER BY
person.id ASC
LIMIT 100

//snb-6
MATCH (tag:Tag {name: $tag})<-[:HAS_TAG]-(message1:Message)-[:HAS_CREATOR]->(person1:Person)
MATCH (message1)<-[:LIKES]-(person2:Person)
MATCH (person2)<-[:HAS_CREATOR]-(message2:Message)<-[likeEdge:LIKES]-(person3:Person)
RETURN DISTINCT
person1.id,
// Using 'DISTINCT like' here ensures that each person2's popularity score is only added once for each person1
likeEdge
ORDER BY
person1.id ASC
LIMIT 100

//snb-7
MATCH
(tag:Tag {name: $tag})<-[:HAS_TAG]-(message:Message),
(message)<-[:REPLY_OF]-(comment:Comment)-[:HAS_TAG]->(relatedTag:Tag)
RETURN DISTINCT
relatedTag.name, comment
ORDER BY
relatedTag.name ASC
LIMIT 100

//snb-8
MATCH (tag:Tag {name: $tag})
// score
MATCH (tag)<-[interest:HAS_INTEREST]-(person:Person)
MATCH (tag)<-[:HAS_TAG]-(message:Message)-[:HAS_CREATOR]->(person:Person)
WHERE $startDateT < message.creationDate
AND message.creationDate < $endDateT
MATCH (person)-[:KNOWS]-(friend)
RETURN
person.id
ORDER BY
person.id ASC
LIMIT 100

//snb-9
MATCH (person:Person)<-[:HAS_CREATOR]-(post:Post)<-[:REPLY_OF]-{0,}(reply:Message)
WHERE  post.creationDate >= $startDateT
AND  post.creationDate <= $endDateT
AND reply.creationDate >= $startDateT
AND reply.creationDate <= $endDateT
RETURN DISTINCT
person.id,
person.firstName,
person.lastName,
post AS threadCount,
reply AS messageCount
ORDER BY
messageCount DESC,
person.id ASC
LIMIT 100

//snb-10
MATCH (startPerson:Person {id: $personIdT})
MATCH (startPerson)-[:IS_LOCATED_IN]->(:City)-[:IS_PART_OF]->(:Country {name: $country}),
(startPerson)<-[:HAS_CREATOR]-(message:Message)-[:HAS_TAG]->(:Tag)-[:HAS_TYPE]->
(:TagClass {name: $tagClass})
MATCH (message)-[:HAS_TAG]->(tag:Tag)
RETURN DISTINCT startPerson.id,
tag.name,
message AS messageCount
ORDER BY
messageCount DESC,
tag.name ASC,
startPerson.id ASC
LIMIT 100

//snb-11
MATCH (a:Person)-[:IS_LOCATED_IN]->(:City)-[:IS_PART_OF]->(country:Country {name: $country}),
(a)-[k1:KNOWS]-(b:Person)
WHERE a.id < b.id
AND $startDateT <= k1.creationDate AND k1.creationDate <= $endDateT
MATCH (b)-[:IS_LOCATED_IN]->(:City)-[:IS_PART_OF]->(country)
MATCH (b)-[k2:KNOWS]-(c:Person),
(c)-[:IS_LOCATED_IN]->(:City)-[:IS_PART_OF]->(country)
WHERE b.id < c.id
AND $startDateT <= k2.creationDate AND k2.creationDate <= $endDateT
MATCH (c)-[k3:KNOWS]-(a)
WHERE $startDateT <= k3.creationDate AND k3.creationDate <= $endDateT
RETURN a, country, k1, b, k2, k3

//snb-12
MATCH (person:PERSON)<-[:HAS_CREATOR]-(message:Message)-[:REPLY_OF]->{0,}(post:Post)
WHERE message.content IS NOT NULL
AND message.length < $lengthThreshold
AND message.creationDate > $startDateT
RETURN
message AS messageCount,
person AS personCount
ORDER BY
personCount DESC,
messageCount DESC

//snb-13
MATCH (country:Country {name: $country})<-[:IS_PART_OF]-(:City)<-[:IS_LOCATED_IN]-(zombie:Person)
WHERE zombie.creationDate < $endDateT
MATCH (zombie)<-[:HAS_CREATOR]-(message:Message)
WHERE message.creationDate < $endDateT
MATCH
(zombie)<-[:HAS_CREATOR]-(message:Message)<-[:LIKES]-(likerZombie:Person)
MATCH
(zombie)<-[:HAS_CREATOR]-(message:Message)<-[:LIKES]-(likerPerson:Person)
WHERE likerPerson.creationDate < $endDateT
RETURN
zombie.id,
likerZombie,
likerPerson
ORDER BY
zombie.id ASC
LIMIT 100

//snb-14
MATCH
(country1:Country {name: $country1})<-[:IS_PART_OF]-(city1:City)<-[:IS_LOCATED_IN]-(person1:Person),
(country2:Country {name: $country2})<-[:IS_PART_OF]-(city2:City)<-[:IS_LOCATED_IN]-(person2:Person),
(person1)-[:KNOWS]-(person2)
MATCH (person1)<-[:HAS_CREATOR]-(c:Comment)-[:REPLY_OF]->(:Message)-[:HAS_CREATOR]->(person2)
MATCH (person1)<-[:HAS_CREATOR]-(m:Message)<-[:REPLY_OF]-(:Comment)-[:HAS_CREATOR]->(person2)
MATCH (person1)-[:LIKES]->(m:Message)-[:HAS_CREATOR]->(person2)
MATCH (person1)<-[:HAS_CREATOR]-(m:Message)<-[:LIKES]-(person2)
ORDER BY
city1.name ASC,
person1.id ASC,
person2.id ASC
RETURN
person1.id AS person1Id,
person2.id AS person2Id,
city1.name
ORDER BY
person1Id ASC,
person2Id ASC
LIMIT 100

//snb-15
CALL{
MATCH (person1:Person)<-[:HAS_CREATOR]-(message1:Message)-[:HAS_TAG]->(tag:Tag {name: $tagA})
WHERE message1.creationDate = $dateA
// filter out Persons with more than $maxKnowsLimit friends who created the same kind of Message
MATCH (person1)-[:KNOWS]-(person2:Person)<-[:HAS_CREATOR]-(message2:Message)-[:HAS_TAG]->(tag)
WHERE message2.creationDate = $dateA

// return count
RETURN person1, message1
UNION
MATCH (person1:Person)<-[:HAS_CREATOR]-(message1:Message)-[:HAS_TAG]->(tag:Tag {name: $tagB})
WHERE message1.creationDate = $dateB
// filter out Persons with more than $maxKnowsLimit friends who created the same kind of Message
MATCH (person1)-[:KNOWS]-(person2:Person)<-[:HAS_CREATOR]-(message2:Message)-[:HAS_TAG]->(tag)
WHERE message2.creationDate = $dateB
// return count
RETURN person1, message1
}
RETURN
person1.id,
message1
ORDER BY message1 DESC, person1.id ASC
LIMIT 20

//snb-16
MATCH
(tag:Tag {name: $tag}),
(person1:Person)<-[:HAS_CREATOR]-(message1:Message)-[:REPLY_OF]->{0,}(post1:Post)<-[:CONTAINER_OF]-(forum1:Forum),
(message1)-[:HAS_TAG]->(tag),
// Having two HAS_MEMBER edges in the same MATCH clause ensures that person2 and person3 are different
// as Cypher's edge-isomorphic matching does not allow for such a match in a single MATCH clause.
(forum1)<-[:HAS_MEMBER]->(person2:Person)<-[:HAS_CREATOR]-(comment:Comment)-[:HAS_TAG]->(tag),
(forum1)<-[:HAS_MEMBER]->(person3:Person)<-[:HAS_CREATOR]-(message2:Message),
(comment)-[:REPLY_OF]->(message2)-[:REPLY_OF]->{0,}(post2:Post)<-[:CONTAINER_OF]-(forum2:Forum)
// The query allows message2 = post2. If this is the case, their HAS_TAG edges to tag overlap,
// and Cypher's edge-isomorphic matching does not allow for such a match in a single MATCH clause.
// To work around this, we add them in separate MATCH clauses.
MATCH (comment)-[:HAS_TAG]->(tag)
MATCH (message2)-[:HAS_TAG]->(tag)
WHERE forum1 <> forum2
AND message2.creationDate > message1.creationDate + duration({hours: $delta})
RETURN DISTINCT person1.id, message2 AS messageCount
ORDER BY messageCount DESC, person1.id ASC
LIMIT 10

//snb-17
MATCH (tag:Tag {name: $tag})<-[:HAS_INTEREST]-(person1:Person)-[:KNOWS]-(mutualFriend:Person)-[:KNOWS]-(person2:Person)-[:HAS_INTEREST]->(tag)
WHERE person1 <> person2
RETURN DISTINCT person1.id AS person1Id, person2.id AS person2Id, mutualFriend
ORDER BY mutualFriend DESC, person1Id ASC, person2Id ASC
LIMIT 20

//snb-18
MATCH
(person1:Person)-[:IS_LOCATED_IN]->(city1:City {id: $city1Id}),
(person2:Person)-[:IS_LOCATED_IN]->(city2:City {id: $city2Id})
RETURN person1.id AS person1Id, person2.id AS person2Id
ORDER BY person1Id, person2Id

//snb-19
MATCH
(company:Company {name: $company})<-[:WORK_AT]-(person1:Person),
(person2:Person {id: $person2Id})
WHERE person1.id <> $person2Id
RETURN person1.id AS person1Id
ORDER BY person1Id ASC
LIMIT 20

//snb-20
MATCH (tag:Tag)-[:HAS_TYPE|IS_SUBCLASS_OF]->{0,}(baseTagClass:TagClass)
Filter tag.name = $tagClass OR baseTagClass.name = $tagClass
MATCH (:Person {id: $personIdT })-[:KNOWS]-(friend:Person)<-[:HAS_CREATOR]-(comment:Comment)-[:REPLY_OF]->(:Post)-[:HAS_TAG]->(tag:Tag)
RETURN DISTINCT
friend.id AS personId,
friend.firstName AS personFirstName,
friend.lastName AS personLastName,
tag.name AS tagNames, comment AS replyCount
LIMIT 20