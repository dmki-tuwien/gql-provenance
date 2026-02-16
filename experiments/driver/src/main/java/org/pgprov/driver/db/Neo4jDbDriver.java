package org.pgprov.driver.db;

import org.apache.commons.lang3.tuple.Pair;
import org.neo4j.driver.*;
import org.neo4j.driver.Record;

import java.util.*;

import static org.pgprov.driver.App.appSettings;

public class Neo4jDbDriver implements DbDriver {

    private Driver driver;

    private final Map<String, List<Pair<Map<String, Object>, Double>>> results = new HashMap<>();

    public Neo4jDbDriver() {
    }

    @Override
    public void connect() {
        String url = appSettings.getProperty("database.url");
        String user = appSettings.getProperty("database.user");
        String pass = appSettings.getProperty("database.password");
        this.driver = GraphDatabase.driver(url, AuthTokens.basic(user, pass));
        System.out.println("Neo4j trying to connect..");
    }

    public void clearResults() {
        this.results.clear();
    }

    public Map<String, List<Pair<Map<String, Object>, Double>>> getResults() {
        return results;
    }

    public double runQuery(String queryIdentifier, String query, Map<String,Object> params){

        try (Session session = driver.session(SessionConfig.forDatabase(appSettings.getProperty("database.dbName")))) {

            String resultKey = queryIdentifier;
            if(queryIdentifier.startsWith("orig_") || queryIdentifier.startsWith("prov_")) {
                resultKey = queryIdentifier.substring(0, queryIdentifier.lastIndexOf('_'));
            }

            // Get all records
            List<org.neo4j.driver.Record> records = null;
            long start = 0;
            if (queryIdentifier.startsWith("prov_")) {
                Map<String, Object> map = new HashMap<>();
                map.put("PARAMS", params);

                start = System.nanoTime();
                records = session.executeRead(tx -> tx.run(query, map).list());

            } else {
                start = System.nanoTime();
                records = session.executeRead(tx -> tx.run(query, params).list());
            }

            long end = System.nanoTime();
            double durationMs = (end - start) / 1_000_000.0;

            List<Pair<Map<String, Object>, Double>> numberOfRecords = results.getOrDefault(resultKey, new ArrayList<>());

            numberOfRecords.add(Pair.of(params, durationMs));

            if(!queryIdentifier.startsWith(appSettings.getProperty("dataset"))) {
                results.put(resultKey, numberOfRecords);
                if(!records.isEmpty()) {
                    records.getLast().toString();
                }
                System.out.println(queryIdentifier+ ", " + params + ", " + durationMs+", "+  records.size());
            }

            return durationMs;

        }
    }

    public Pair<Double, Integer> runTestProcedureQuery(String queryIdentifier, String query, Map<String,Object> params){

        try (Session session = driver.session(SessionConfig.forDatabase(appSettings.getProperty("database.dbName")))) {

            String resultKey = queryIdentifier;
            if(queryIdentifier.startsWith("orig_") || queryIdentifier.startsWith("prov_")) {
                resultKey = queryIdentifier.substring(0, queryIdentifier.lastIndexOf('_'));
            }

            String queryString = "CALL org.pgprov.testProvenance($QUERY, $PARAMS)";

            Map<String, Object> map = new HashMap<>();
            map.put("QUERY", query);



            // Get all records
            List<org.neo4j.driver.Record> records = null;
            long start = 0;
            if (queryIdentifier.startsWith("prov_")) {
                Map<String, Object> nestedMap = new HashMap<>();
                nestedMap.put("PARAMS", params);
                map.put("PARAMS", nestedMap);


                start = System.nanoTime();
                records = session.executeRead(tx -> tx.run(queryString, map).list());

            } else {

                map.put("PARAMS", params);
                start = System.nanoTime();
                records = session.executeRead(tx -> tx.run(queryString, map).list());
            }

//            long end = System.nanoTime();
//            double durationMs = (end - start) / 1_000_000.0;
//
//            List<Pair<Map<String, Object>, Double>> numberOfRecords = results.getOrDefault(resultKey, new ArrayList<>());
//
//            numberOfRecords.add(Pair.of(params, durationMs));

            Record rec = records.getFirst();

            if(!queryIdentifier.startsWith(appSettings.getProperty("dataset"))) {
//                results.put(resultKey, numberOfRecords);
//                if(!records.isEmpty()) {
//                    records.getLast().toString();
//                }
                System.out.println("Results: "+queryIdentifier+ ", " + params + ", " + rec.get("durationMs").asDouble()+", "+  records.size());
            }

            return Pair.of(rec.get("durationMs").asDouble(), rec.get("size").asInt());

        }
    }

    @Override
    public Map<String, Pair<String, Map<String,Object>>> generateTestQuerySet(Map<String, String> originalQueries, String provenanceModel, Map<String, List<Map<String, Object>>> map) {
        Random rng = new Random();
        Map<String, Pair<String, Map<String,Object>>> finalQueries = new LinkedHashMap<>();

        int execCount = Integer.parseInt(appSettings.getProperty("test_param_count"));
        // loop the query set an execCount times
            //Construct map of all queries (latency checked)
        List<String> repetitions = List.of(".1",".2",".3",".4",".5");
        for (Map.Entry<String, String> entry : originalQueries.entrySet()) {

            if(!entry.getKey().contains(".") || entry.getKey().contains(".1.1")) {

                List<Map<String, Object>> params = map.get(entry.getKey());
                for (int i = 0; i <execCount; i++) {

                    int randomNum = rng.nextInt(0, params.size());
                    Map<String, Object> param = params.get(randomNum);

                    Pair<String, Map<String, Object>> queryMap = Pair.of(entry.getValue(), param);

                    finalQueries.put("orig_" + entry.getKey()+"_"+i, queryMap);

                    String query = "CALL org.pgprov.get" + provenanceModel + "Provenance(\"" + entry.getValue() + "\", $PARAMS);";

                    Pair<String, Map<String, Object>> provQueryMap = Pair.of(query, param);
                    finalQueries.put("prov_" + entry.getKey()+"_"+i, provQueryMap);

                    if(entry.getKey().contains(".1.1")) {
                        String[] parts = entry.getKey().split("\\."); // split by dot (escape it)
                        String majorMinor = parts[0];

                        for (String rep : repetitions) {
                            if (originalQueries.containsKey(majorMinor + rep)) {

                                String newKey = majorMinor + rep;
                                Pair<String, Map<String, Object>> queryMap1 = Pair.of(originalQueries.get(newKey), param);

                                finalQueries.put("orig_" + newKey + "_" + i, queryMap1);

                                String newQuery = "CALL org.pgprov.get" + provenanceModel + "Provenance(\"" + originalQueries.get(newKey) + "\", $PARAMS);";

                                Pair<String, Map<String, Object>> newprovQueryMap = Pair.of(newQuery, param);
                                finalQueries.put("prov_" + newKey + "_" + i, newprovQueryMap);

                            }
                        }
                    }
                }

            }
        }

        System.out.println("Generated query count: "+finalQueries.size());
        return finalQueries;

    }

    @Override
    public void close() throws Exception {

    }
}