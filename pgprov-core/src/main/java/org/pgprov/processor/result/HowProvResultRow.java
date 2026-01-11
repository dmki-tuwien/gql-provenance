package org.pgprov.processor.result;

import org.pgprov.ast.SQLNode;

import java.util.*;

public abstract class HowProvResultRow<S> extends ResultRow<S,String> {

    public HowProvResultRow(S row, SQLNode sqlNode) {
        super(row, sqlNode);
    }

    @Override
    public String calculateProvenance(Map<String, Object> row, Map<String, List<String>> varSchemaAndSignatures) {
        return this.getSqlNode().calculateHowProv(row, new HashMap<>());
    }

    @Override
    public void mergeProvenance (ResultRow<S, String> otherRow) {

        this.setProv("("+this.getProv()+") + ("+otherRow.getProv()+")");
    }
}
