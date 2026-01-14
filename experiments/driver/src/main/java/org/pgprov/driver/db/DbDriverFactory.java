package org.pgprov.driver.db;

import org.pgprov.driver.App;

public class DbDriverFactory {

    public static DbDriver createDriver() {
        switch(App.appSettings.getProperty("database")) {
            case "NEO4J": return new Neo4jDbDriver();
            //case "DUCKDB": return new MemgraphDriver(appSettings);
            default: throw new IllegalArgumentException("Unknown driver type: " + App.appSettings.getProperty("database"));
        }
    }
}