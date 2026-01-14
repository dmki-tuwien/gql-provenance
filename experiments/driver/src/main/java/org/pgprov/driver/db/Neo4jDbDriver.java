package org.pgprov.driver.db;

import org.pgprov.driver.App;
import org.neo4j.driver.*;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Neo4jDbDriver implements DbDriver {

    private Driver driver;

    public Neo4jDbDriver() {
    }

    @Override
    public void connect() {
        String url = App.appSettings.getProperty("database.url");
        String user = App.appSettings.getProperty("database.user");
        String pass = App.appSettings.getProperty("database.password");
        this.driver = GraphDatabase.driver(url, AuthTokens.basic(user, pass));
        System.out.println("Neo4j connected");
    }

    public double runQuery(String queryIdentifier, String query, Map<String,Object> params){

        try (Session session = driver.session(SessionConfig.forDatabase(App.appSettings.getProperty("database.dbName")))) {

            // Get all records
            List<Record> records = null;
            long start = System.nanoTime();

            if (queryIdentifier.startsWith("prov_")) {
                Map<String, Object> map = new HashMap<>();
                map.put("PARAMS", params);

                records = session.executeRead(tx -> tx.run(query, map).list());

            } else {
                records = session.executeRead(tx -> tx.run(query, params).list());
            }

//            List<Integer> times = results1.getOrDefault(queryIdentifier, new ArrayList<>());
//            times.add(records.size());
//            results.put(queryIdentifier, times);

            long end = System.nanoTime();
            double durationMs = (end - start) / 1_000_000.0;

            return durationMs;

        }
    }

    public Map<String, String> generateTestQuerySet(Map<String, String> originalQueries, String provenanceModel){

        Map<String, String> finalQueries = new LinkedHashMap<>();

        //Construct map of all queries (latency checked)
        for (Map.Entry<String, String> entry : originalQueries.entrySet()) {
            finalQueries.put("orig_" +entry.getKey(), entry.getValue());

            String query = "CALL org.pgprov.get"+provenanceModel+"Provenance(\""+entry.getValue()+"\", $PARAMS);";
            finalQueries.put("prov_" +entry.getKey(), query);
        }

        return finalQueries;

    }

    @Override
    public void close() throws Exception {

    }
}