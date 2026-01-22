//snb-1
MATCH (p:Person {id: $personId}), (friend:Person {firstName: $firstName})
  WHERE NOT p=friend
MATCH path1 = (p)-[:KNOWS]-{1,3}(friend)
ORDER BY friend.lastName ASC,
friend.id ASC
LIMIT 20
MATCH (friend)-[:IS_LOCATED_IN]->(friendCity:City)
MATCH (friend)-[studyAt:STUDY_AT]->(uni:University)-[:IS_LOCATED_IN]->(uniCity:City)
MATCH (friend)-[workAt:WORK_AT]->(company:Company)-[:IS_LOCATED_IN]->(companyCountry:Country)
RETURN
friend.id AS friendId,
friend.lastName AS friendLastName,
friend.birthday AS friendBirthday,
friend.creationDate AS friendCreationDate,
friend.gender AS friendGender,
friend.browserUsed AS friendBrowserUsed,
friend.locationIP AS friendLocationIp,
friend.email AS friendEmails,
friend.speaks AS friendLanguages,
friendCity.name AS friendCityName,
uni AS friendUniversity,
company AS friendCompany
ORDER BY
friendLastName ASC,
friendId ASC
LIMIT 20

//snb-2
MATCH (:Person {id: $personId })-[:KNOWS]-(friend:Person)<-[:HAS_CREATOR]-(message:Message)
WHERE message.creationDate <= $maxDate
RETURN
friend.id AS personId,
friend.firstName AS personFirstName,
friend.lastName AS personLastName,
message.id AS postOrCommentId,
message.creationDate AS postOrCommentCreationDate
ORDER BY
postOrCommentCreationDate DESC,
postOrCommentId ASC
LIMIT 20

//snb-3
MATCH (countryX:Country {name: $countryXName }),
(countryY:Country {name: $countryYName }),
(person:Person {id: $personId })
LIMIT 1
MATCH (city:City)-[:IS_PART_OF]->(country:Country)
WHERE country = countryX OR country = countryY
MATCH (person)-[:KNOWS]-{1,2}(friend)-[:IS_LOCATED_IN]->(city)
WHERE NOT person=friend
WITH DISTINCT friend, countryX, countryY
MATCH (friend)<-[:HAS_CREATOR]-(message),
(message)-[:IS_LOCATED_IN]->(country)
WHERE $endDate > message.creationDate >= $startDate AND
country = countryX OR country = countryY
RETURN friend.id AS friendId,
friend.firstName AS friendFirstName,
friend.lastName AS friendLastName,
ORDER BY friendId ASC
LIMIT 20

//snb-4
MATCH (person:Person {id: $personId })-[:KNOWS]-(friend:Person),
(friend)<-[:HAS_CREATOR]-(post:Post)-[:HAS_TAG]->(tag)
WITH DISTINCT tag, post
RETURN tag.name AS tagName
ORDER BY tagName ASC
LIMIT 10

//snb-5
MATCH (person:Person { id: $personId })-[:KNOWS]-{1,2}(friend)
WHERE NOT person=friend
WITH DISTINCT friend
MATCH (friend)<-[membership:HAS_MEMBER]-(forum)
WHERE membership.joinDate > $minDate
MATCH (friend)<-[:HAS_CREATOR]-(post)<-[:CONTAINER_OF]-(forum)
RETURN
forum.title AS forumName
ORDER BY forum.id ASC
LIMIT 20

//snb-6
MATCH (knownTag:Tag { name: $tagName })
MATCH (person:Person { id: $personId })-[:KNOWS]-{1,2}(friend)
WHERE NOT person=friend
MATCH (friend)<-[:HAS_CREATOR]-(post:Post),
(post)-[:HAS_TAG]->(t:Tag{id: knownTagId.id}),
(post)-[:HAS_TAG]->(tag:Tag)
WHERE NOT t = tag
RETURN tagName
ORDER BY tagName ASC
LIMIT 10

//snb-7
MATCH (person:Person {id: $personId})<-[:HAS_CREATOR]-(message:Message)<-[like:LIKES]-(liker:Person)
WITH liker, message, like.creationDate AS likeTime, person
ORDER BY likeTime DESC, message.id ASC
RETURN
liker.id AS personId,
liker.firstName AS personFirstName,
liker.lastName AS personLastName,
NOT((liker)-[:KNOWS]-(person)) AS isNew
ORDER BY personId ASC
LIMIT 20

//snb-8
MATCH (start1:Person {id: $personId})<-[:HAS_CREATOR]-(:Message)<-[:REPLY_OF]-(comment:Comment)-[:HAS_CREATOR]->(person:Person)
RETURN
person.id AS personId,
person.firstName AS personFirstName,
person.lastName AS personLastName,
comment.creationDate AS commentCreationDate,
comment.id AS commentId,
comment.content AS commentContent
ORDER BY
commentCreationDate DESC,
commentId ASC
LIMIT 20

//snb-9
MATCH (root:Person {id: $personId })-[:KNOWS*1..2]-(friend:Person)
WHERE NOT friend = root
MATCH (friend)<-[:HAS_CREATOR]-(message:Message)
WHERE message.creationDate < $maxDate
RETURN
friend.id AS personId,
friend.firstName AS personFirstName,
friend.lastName AS personLastName,
message.id AS commentOrPostId,
message.creationDate AS commentOrPostCreationDate
ORDER BY commentOrPostCreationDate DESC,
message.id ASC
LIMIT 20

//snb-10
MATCH (person:Person {id: $personId})-[:KNOWS]-{2,2}(friend),
(friend)-[:IS_LOCATED_IN]->(city:City)
WHERE NOT friend=person AND NOT (friend)-[:KNOWS]-(person)
WITH person, city, friend, datetime({epochMillis: friend.birthday}) AS birthday
WHERE  (birthday.month=$month AND birthday.day>=21) OR (birthday.month=($month%12)+1 AND birthday.day<22)
WITH DISTINCT friend, city, person
MATCH (friend)<-[:HAS_CREATOR]-(post:Post)
RETURN friend.id AS personId,
friend.firstName AS personFirstName,
friend.lastName AS personLastName,
friend.gender AS personGender,
city.name AS personCityName
ORDER BY personId ASC
LIMIT 10

//snb-11
MATCH (person:Person {id: $personId })-[:KNOWS]-{1,2}(friend:Person)
WHERE NOT(person=friend)
WITH DISTINCT friend
MATCH (friend)-[workAt:WORK_AT]->(company:Company)-[:IS_LOCATED_IN]->(:Country {name: $countryName })
WHERE workAt.workFrom < $workFromYear
RETURN
friend.id AS personId,
friend.firstName AS personFirstName,
friend.lastName AS personLastName,
company.name AS organizationName,
workAt.workFrom AS organizationWorkFromYear
ORDER BY
organizationWorkFromYear ASC, personId ASC,
organizationName DESC
LIMIT 10

//snb-12
MATCH (tag:Tag)-[:HAS_TYPE|IS_SUBCLASS_OF]->{0,}(baseTagClass:TagClass)
filter tag.name = $tagClassName OR baseTagClass.name = $tagClassName
MATCH (:Person {id: $personId })-[:KNOWS]-(friend:Person)<-[:HAS_CREATOR]-(comment:Comment)-[:REPLY_OF]->(:Post)-[:HAS_TAG]->(tag:Tag)
RETURN
friend.id AS personId,
friend.firstName AS personFirstName,
friend.lastName AS personLastName,
DISTINCT tag.name AS tagNames,
DISTINCT comment AS replyCount
ORDER BY
replyCount DESC, personId ASC
LIMIT 20

//snb-13
MATCH (n:Person {id: $personId })-[:IS_LOCATED_IN]->(p:City)
RETURN
n.firstName AS firstName,
n.lastName AS lastName,
n.birthday AS birthday,
n.locationIP AS locationIP,
n.browserUsed AS browserUsed,
p.id AS cityId,
n.gender AS gender,
n.creationDate AS creationDate

//snb-14
MATCH (:Person {id: $personId})<-[:HAS_CREATOR]-(message)
WITH
message,
message.id AS messageId,
message.creationDate AS messageCreationDate
ORDER BY messageCreationDate DESC, messageId ASC
LIMIT 10
MATCH (message)-[:REPLY_OF]->{0,}(post:Post),
(post)-[:HAS_CREATOR]->(person)
RETURN
messageId,
message.imageFile,message.content AS messageContent,
messageCreationDate,
post.id AS postId,
person.id AS personId,
person.firstName AS personFirstName,
person.lastName AS personLastName
ORDER BY messageCreationDate DESC, messageId ASC

//snb-15
MATCH (n:Person {id: $personId })-[r:KNOWS]-(friend)
RETURN
friend.id AS personId,
friend.firstName AS firstName,
friend.lastName AS lastName,
r.creationDate AS friendshipCreationDate
ORDER BY
friendshipCreationDate DESC, personId ASC

//snb-16
MATCH (m:Message {id:  $messageId })
RETURN
m.creationDate AS messageCreationDate, m.content AS messageContent

//snb-17
MATCH (m:Message {id:  $messageId })-[:HAS_CREATOR]->(p:Person)
RETURN
p.id AS personId,
p.firstName AS firstName,
p.lastName AS lastName

//snb-18
MATCH (m:Message {id: $messageId })-[:REPLY_OF]->{0,}(p:Post)<-[:CONTAINER_OF]-(f:Forum)-[:HAS_MODERATOR]->(mod:Person)
RETURN
f.id AS forumId,
f.title AS forumTitle,
mod.id AS moderatorId,
mod.firstName AS moderatorFirstName,
mod.lastName AS moderatorLastName


//snb-19
MATCH (m:Message {id: $messageId })<-[:REPLY_OF]-(c:Comment)-[:HAS_CREATOR]->(p:Person)
MATCH (m)-[:HAS_CREATOR]->(a:Person)-[r:KNOWS]-(p)
RETURN c.id AS commentId,
c.content AS commentContent,
c.creationDate AS commentCreationDate,
p.id AS replyAuthorId,
p.firstName AS replyAuthorFirstName,
p.lastName AS replyAuthorLastName
ORDER BY commentCreationDate DESC, replyAuthorId


//snb-20
CALL(){
MATCH (knownTag:Tag {name:$tagName})
MATCH (person:Person {id:$personId})- [:KNOWS] - {1, 2}(friend)
WHERE NOT person = friend
RETURN friend
}
MATCH (friend)<-[:HAS_CREATOR]-(post:Post),
(post)-[:HAS_TAG]->(t:Tag{id: knownTagId.id}),
(post)-[:HAS_TAG]->(tag:Tag)
WHERE NOT t = tag
RETURN tagName
ORDER BY tagName ASC
LIMIT 10


