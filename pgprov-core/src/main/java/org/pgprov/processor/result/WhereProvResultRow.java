package org.pgprov.processor.result;

import org.pgprov.ast.SQLNode;

import java.util.*;
import java.util.stream.Collectors;

public abstract class WhereProvResultRow<S> extends ResultRow<S,Map<String, Set<Object>>> {

    public WhereProvResultRow(S row, SQLNode sqlNode) {
        super(row, sqlNode);
    }

    @Override
    public Map<String, Set<Object>> calculateProvenance(Map<String, Object> row) {
        return this.getSqlNode().calculateWhereProv(row);
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
