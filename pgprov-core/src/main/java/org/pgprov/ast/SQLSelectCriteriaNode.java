package org.pgprov.ast;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class SQLSelectCriteriaNode extends SQLNode {

    private final String where;
    private final Map<String, List<String>> schemaAndSignatures;

    public SQLSelectCriteriaNode(String where, Map<String, List<String>> schemaAndSignatures) {

        this.where = where;
        this.schemaAndSignatures = schemaAndSignatures;
    }

    @Override
    public Set<String> getReturnVarsForRewriting() {
        return schemaAndSignatures.keySet();
    }

    public String where() {
        return where;
    }

    public Map<String, List<String>> schemaAndSignatures() {
        return schemaAndSignatures;
    }

}
