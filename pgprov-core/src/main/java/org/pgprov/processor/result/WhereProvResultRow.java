package org.pgprov.processor.result;

import org.pgprov.Globals;
import org.pgprov.ast.SQLNode;

import java.util.*;
import java.util.stream.Collectors;

public abstract class WhereProvResultRow<S> extends ResultRow<S,Map<String, Set<Object>>> {

    public WhereProvResultRow(S row, SQLNode sqlNode) {
        super(row, sqlNode);
    }

    @Override
    public Map<String, Set<Object>> calculateProvenance(Map<String, Object> row) {
        Map<String, Set<Object>> provenance = new HashMap<>();
        for(Map.Entry<String, Object> entry : row.entrySet()) {
            if(entry.getKey().startsWith(Globals.VAR_PREFIX) || entry.getKey().startsWith(Globals.PATH_PREFIX)) {
                if(!(entry.getValue() instanceof String && entry.getValue().equals(Globals.EXTERNAL_VAR_VALUE))) {

                    Set<Object> values = new HashSet<>();
                    values.add(entry.getValue());
                    provenance.put(entry.getKey(),  values);
                }
            }
        }
        return provenance;
    }

    @Override
    public void mergeProvenance (ResultRow<S, Map<String, Set<Object>>> otherRow) {

        Set<String> otherProv = otherRow.getProv().keySet();
        for(String provKey: otherProv){
            this.getProv()
                    .computeIfAbsent(provKey, k-> new HashSet<>())
                    .addAll(otherRow.getProv().get(provKey));
        }
    }
}
