package org.pgprov.processor.result;

import org.pgprov.ast.SQLNode;

import java.util.*;
import java.util.stream.Collectors;

public abstract class WhyProvResultRow<S> extends ResultRow<S,List<List<String>>> {

    public WhyProvResultRow(S row, SQLNode sqlNode) {
        super(row, sqlNode);
    }

    @Override
    public List<List<String>> calculateProvenance(Map<String, Object> row) {
        Set<Set<String>> prov = this.getSqlNode().calculateWhyProv(row);
        return prov.stream()
                .map(ArrayList::new)   // each Set -> mutable List
                .collect(Collectors.toList());
    }

    @Override
    public void mergeProvenance (ResultRow<S, List<List<String>>> otherRow) {
        this.getProv().addAll(otherRow.getProv());
    }
}
