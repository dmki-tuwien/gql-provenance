package org.pgprov.processor.result;

import org.pgprov.Globals;
import org.pgprov.ast.SQLNode;

import java.util.*;

public abstract class WhyProvResultRow<S> extends ResultRow<S,List<List<String>>> {

    public WhyProvResultRow(S row, SQLNode sqlNode) {
        super(row, sqlNode);
    }

    @Override
    public List<List<String>> calculateProvenance(Map<String, Object> row) {
        Set<String> provenance = new HashSet<>();
        for(Map.Entry<String, Object> entry : row.entrySet()) {
            if((entry.getKey().startsWith(Globals.VAR_PREFIX) || entry.getKey().startsWith(Globals.PATH_PREFIX) )&&!entry.getValue().equals(Globals.EXTERNAL_VAR_VALUE)) {
                if(entry.getValue() instanceof String ) {
                    provenance.add((String)entry.getValue());
                }else{
                    List<String> values = (List<String>) entry.getValue();
                    values.removeIf(e -> e.equals(Globals.EXTERNAL_VAR_VALUE));
                    provenance.addAll(values);

                }

            }
        }
        List<List<String>> finalProvenance = new ArrayList<>();
        finalProvenance.add(provenance.stream().toList());
        return finalProvenance;
    }

    @Override
    public void mergeProvenance (ResultRow<S, List<List<String>>> otherRow) {
        this.getProv().addAll(otherRow.getProv());
    }
}
