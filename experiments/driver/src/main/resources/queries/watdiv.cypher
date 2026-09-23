//watdiv-1
MATCH (v0)-[:ns5__hasReview]->(v4)-[:ns5__reviewer]->(v6)<-[:sch__actor]-(v7)-[:sch__language]->(v8)
WHERE v0.sch__caption IS NOT NULL AND v0.sch__text IS NOT NULL
AND v0.sch__contentRating IS NOT NULL
AND v4.ns5__title IS NOT NULL
RETURN  v0, v4, v6, v7

//watdiv-2
MATCH (v0)-[:ns0__offers]->(v2)-[:sch__eligibleRegion]->()
WHERE v0.sch__legalName IS NOT NULL
MATCH (v2)-[:ns0__includes]->(v3)
MATCH (v4)-[:ns6__homepage]->(v6)
WHERE v4.sch__jobTitle IS NOT NULL
MATCH (v4)-[:ns3__makesPurchase]->(v7)-[:ns3__purchaseFor]->(v3)
MATCH (v3)-[:ns5__hasReview]->(v8)
WHERE v8.ns5__totalVotes IS NOT NULL
RETURN v0, v3, v4, v8

//watdiv-3
MATCH (v0:ns3__Role0)-[:ns3__likes]->(v1)
WHERE v0.ns6__givenName IS NOT NULL
MATCH (v0)-[:ns3__friendOf]->(v2)
MATCH (v0)-[:ns7__Location]->(v3)
MATCH (v0)-[:ns6__age]->(v4)
MATCH (v0)-[:ns3__gender]->(v5)
RETURN v0

//watdiv-4
MATCH (v0)-[:ns2__tag]->({uri:'http://db.uwaterloo.ca/~galuc/wsdbm/'+$v1})
MATCH (v3)-[:sch__trailer]->(v4)
MATCH (v3:ns3__ProductCategory2)-[:ns3__hasGenre]->(v0)
RETURN v0, v0.uri AS v2, v3, v4, v3.sch__keywords AS v5

//watdiv-5
MATCH (v0)-[:ns6__homepage]->(v1)           //v0 has type is removed
MATCH (v0)-[:ns3__hasGenre]->({uri:'http://db.uwaterloo.ca/~galuc/wsdbm/'+$v8})
RETURN v0, v1, v0.ns5__title AS v2, v0.sch__caption AS v4, v0.sch__description AS v5, v1.sch__url AS v6, v1.ns3__hits AS v7

//watdiv-6
MATCH (v0)-[:ns3__hasGenre]->(v3)
MATCH (v4)-[:ns3__makesPurchase]->(v5)-[:ns3__purchaseFor]->(v0)
RETURN v0, v0.sch__contentRating AS v1, v0.sch__contentSize AS v2, v4, v5, v5.purchaseDate AS v6

//watdiv-7
MATCH (v0)-[:ns6__homepage]->(v1)-[:sch__language]->({uri:'http://db.uwaterloo.ca/~galuc/wsdbm/Language0'})
MATCH (v2)-[:ns0__includes]->(v0)
MATCH (v0)-[:ns2__tag]->({uri:'http://db.uwaterloo.ca/~galuc/wsdbm/'+$v3})
MATCH (v7)-[:ns3__likes]->(v0)
RETURN v0, v1, v2, v0.sch__description AS v4, v1.sch__url AS v5, v1.ns3__hits AS v6, v7, v0.sch__contentSize AS v8

//watdiv-8
MATCH (v0)-[:ns0__includes]->(v1)
MATCH ()-[:ns0__offers]->(v0)
RETURN v0, v1, v0.ns0__price AS v3, v0.ns0__validThrough AS v4, v1.ns2__title AS v5, v1.uri AS v6

//watdiv-9
MATCH (v0)-[:ns3__subscribes]->({uri:'http://db.uwaterloo.ca/~galuc/wsdbm/'+$v1})
MATCH (v0)-[:ns3__likes]->(v2)
RETURN v0, v2, v2.sch__caption AS v3

//watdiv-10
MATCH ({uri:'http://db.uwaterloo.ca/~galuc/wsdbm/'+$v0})-[:ns1__parentCountry]->(v1)
MATCH (v2)-[:ns3__likes]-({uri:'http://db.uwaterloo.ca/~galuc/wsdbm/Product0'})
MATCH (v2)-[:sch__nationality]->(v1)
RETURN v1, v2

//watdiv-11
MATCH (v0)-[:ns3__likes]->(v1)
MATCH (v0)-[:ns3__subscribes]->({uri:'http://db.uwaterloo.ca/~galuc/wsdbm/'+$v3})
RETURN v0

//watdiv-12
MATCH (v0)-[:ns2__tag]->({uri:'http://db.uwaterloo.ca/~galuc/wsdbm/'+$v1})
RETURN v0, v0.sch__caption AS v2

//watdiv-13
MATCH ({uri:'http://db.uwaterloo.ca/~galuc/wsdbm/'+$v2})-[:ns1__parentCountry]->(v3)
MATCH (v0)-[:sch__nationality]->(v3)
RETURN v0, v0.sch__jobTitle AS v1, v3

//watdiv-14
MATCH (v0)-[:ns0__includes]->(v1)
MATCH ({uri:'http://db.uwaterloo.ca/~galuc/wsdbm/'+$v2})-[:ns0__offers]->(v0)-[:sch__eligibleRegion]->(v8)
RETURN v0, v1, v0.ns0__price AS v3, v0.ns0__serialNumber AS v4, v0.ns0__validFrom AS v5, v0.ns0__validThrough AS v6, v0.sch__eligibleQuantity AS v7, v8, v0.sch__priceValidUntil AS v9

//watdiv-15
MATCH (v0:ns3__Role2)-[:ns7__Location]->(v1)
MATCH (v0)-[:sch__nationality]->({uri:'http://db.uwaterloo.ca/~galuc/wsdbm/'+$v2})
MATCH (v0)-[:ns3__gender]->(v3)
RETURN v0

//watdiv-16
MATCH (v0:ns3__ProductCategory4 )-[:ns3__hasGenre]->(v3)
WHERE v0.sch__caption IS NOT NULL AND v0.sch__publisher IS NOT NULL
RETURN v0

//watdiv-17
MATCH (v0)-[:ns6__age]->({uri:'http://db.uwaterloo.ca/~galuc/wsdbm/'+$v1})
MATCH (v3)-[:ns4__artist]->(v0)<-[:sch__nationality]-({uri:'http://db.uwaterloo.ca/~galuc/wsdbm/Country1'})
RETURN v0, v0.ns6__familyName AS v2, v3

//watdiv-18
MATCH (v0:ns3__ProductCategory2)-[:sch__language]->({uri:'http://db.uwaterloo.ca/~galuc/wsdbm/Language0'})
WHERE v0.sch__description IS NOT NULL AND v0.sch__keywords IS NOT NULL
RETURN v0

//watdiv-19
MATCH (v0)-[:ns4__conductor]->(v1)
MATCH (v0)-[:ns3__hasGenre]->({uri:'http://db.uwaterloo.ca/~galuc/wsdbm/'+$v3})
RETURN v0

//watdiv-20
MATCH ({uri:'http://db.uwaterloo.ca/~galuc/wsdbm/'+$v3})-[:ns3__likes]->(v0)
RETURN v0, v0.sch__text AS v2
