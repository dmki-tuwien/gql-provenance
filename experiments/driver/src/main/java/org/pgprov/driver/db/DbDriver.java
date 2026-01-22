
package org.pgprov.driver.db;


import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.Map;

public interface DbDriver extends AutoCloseable {

    void connect();
    double runQuery(String queryIdentifier, String query, Map<String,Object> params);

    Map<String, Pair<String, Map<String,Object>>> generateTestQuerySet(Map<String, String> originalQueries, String provenanceModel, Map<String, List<Map<String, Object>>> map);

    Map<String, List<Pair<Map<String, Object>, Double>>> getResults() ;
    void clearResults();

}