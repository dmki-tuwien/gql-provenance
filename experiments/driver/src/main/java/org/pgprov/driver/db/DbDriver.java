
package org.pgprov.driver.db;


import java.util.Map;

public interface DbDriver extends AutoCloseable {

    void connect();
    double runQuery(String queryIdentifier, String query, Map<String,Object> params);

    Map<String, String> generateTestQuerySet(Map<String, String> originalQueries, String provenanceModel);
}