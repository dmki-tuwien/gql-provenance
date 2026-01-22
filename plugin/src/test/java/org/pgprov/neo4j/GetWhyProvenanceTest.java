package org.pgprov.neo4j;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.json.JSONException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.neo4j.driver.*;
import org.neo4j.driver.Record;
import org.neo4j.harness.Neo4j;
import org.neo4j.harness.Neo4jBuilders;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class GetWhyProvenanceTest {

    private Driver driver;
    private Neo4j embeddedDatabaseServer;
    private final String provModel = "Why";

    @BeforeAll
    void initializeNeo4j() {

        embeddedDatabaseServer = Neo4jBuilders.newInProcessBuilder()
                .withDisabledServer()
                .withProcedure(GetWhyProvenance.class)
                .withFixture(
                        "MERGE (p1:Person:Leader {name: 'Alice', age: 30, __n: 'n1001', __k_name: 'k2001', __k_age: 'k3001', __l_Person: 'l4001'})\n"
                                + "MERGE (p2:Person {name: 'Bob', age: 28, __n: 'n1002', __k_name: 'k2002', __k_age: 'k3002', __l_Person: 'l4002'})\n"
                                + "MERGE (p3:Person {name: 'Charlie', age: 35, __n: 'n1003', __k_name: 'k2003', __k_age: 'k3003', __l_Person: 'l4003'})\n"
                                + "MERGE (p4:Person {name: 'Dan', age: 15, __n: 'n1004', __k_name: 'k2004', __k_age: 'k3004', __l_Person: 'l4004'})\n"
                                + "MERGE (p5:Person {name: 'Evan', __n: 'n1005', __k_name: 'k2005', __l_Person: 'l4005'})\n"
                                + "MERGE (p4)-[t3:Transfer {amount: 800, __l_Transfer: 'l5003', __k_amount: 'k6003', __e: 'e7003'}]->(p5)\n"
                                + "MERGE (p1)-[t1:Transfer {amount: 1000, __l_Transfer: 'l5001', __k_amount: 'k6001', __e: 'e7001'}]->(p2)\n"
                                + "MERGE (p2)-[t2:Transfer {amount: 1500, __l_Transfer: 'l5002', __k_amount: 'k6002', __e: 'e7002'}]->(p3)"
                )
                .build();

        driver = GraphDatabase.driver(embeddedDatabaseServer.boltURI(), AuthTokens.none());
    }

    @AfterAll
    void closeDriver(){
        driver.close();
        embeddedDatabaseServer.close();
    }

    @Test
    public void testForSimpleQuery() throws IOException {

        TestUtil util = new TestUtil();
        util.runTest(driver, "simpleQuery", provModel);

    }

    @Test
    public void testForSimpleQueryWithRenaming() throws IOException {

        TestUtil util = new TestUtil();
        util.runTest(driver, "simpleQueryWithRenaming", provModel);

    }

    @Test
    public void testForNullValues() throws IOException {

        TestUtil util = new TestUtil();
        util.runTest(driver, "nullValues", provModel);

    }

    @Test
    public void testQueryWithPathVariable() throws IOException {

        TestUtil util = new TestUtil();
        util.runTest(driver, "queryWithPathVariable", provModel);

    }

    @Test
    public void testQueryWithPathJoin() throws IOException {

        TestUtil util = new TestUtil();
        util.runTest(driver, "queryWithPathJoin", provModel);

    }

    @Test
    public void testUnionQuery() throws IOException {

        TestUtil util = new TestUtil();
        util.runTest(driver, "unionQuery", provModel);

    }

    @Test
    public void testDoubleMatch() throws IOException {

        TestUtil util = new TestUtil();
        util.runTest(driver, "doubleMatch", provModel);

    }

    @Test
    public void testSubQuery() throws IOException {

        TestUtil util = new TestUtil();
        util.runTest(driver, "subQuery", provModel);

    }

    @Test
    public void testQueryWithPathRepetition() throws IOException {

        TestUtil util = new TestUtil();
        util.runTest(driver, "queryWithPathRepetition", provModel);

    }

    @Test
    public void runTest( ) throws IOException {
        try (Session session = driver.session()) {

            String query = "MATCH (person:PERSON {id: '10995116278938'})-[edge1:OWN]->(src:ACCOUNT)," +
                    " p=(src)-[edge2:TRANSFER]->{1,3}(dst:ACCOUNT)" +
                    " RETURN p AS path1" +
                    " ORDER BY path1 DESC";
            Result record = session.run("CALL org.pgprov.getWhyProvenance(\""+query+"\" , {})");

//            ArrayNode actualSet = mapper.createArrayNode();
//
//            while (record.hasNext()) {
//                Record rec = record.next();
//                Map<String,Object> recordMap = rec.asMap().entrySet().stream()
//                        .collect(Collectors.toMap(
//                                Map.Entry::getKey,
//                                e -> convertNeoValue(e.getValue())
//                        ));
//
//                actualSet.add(mapper.valueToTree(recordMap));
//            }

            System.out.println(record);
//            System.out.println(actualSet);
//
//            JSONAssert.assertEquals(actualSet.toString(), testValue.toString(), JSONCompareMode.LENIENT);
        }

    }
}

