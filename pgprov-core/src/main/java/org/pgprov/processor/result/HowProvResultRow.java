package org.pgprov.processor.result;

import org.pgprov.ast.SQLNode;

import java.util.*;

public abstract class HowProvResultRow<S> extends ResultRow<S,String> {

    public HowProvResultRow(S row, SQLNode sqlNode) {
        super(row, sqlNode, false);
    }

    @Override
    public String calculateProvenance(Map<String, Object> row, boolean edgeMinimality) {
        return "";
//        return this.getSqlNode().calculateHowProv(row);
    }

    @Override
    public void mergeProvenance (ResultRow<S, String> otherRow, boolean edgeMinimality) {

        this.setProv("("+this.getProv()+") + ("+otherRow.getProv()+")");
    }
}
