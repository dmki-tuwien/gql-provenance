package org.pgprov.processor.result;

import org.pgprov.ast.SQLNode;

import java.util.*;

public abstract class ResultRow<S,T> {

    private final S result;

    private T prov;

    private final SQLNode sqlNode;

    public ResultRow(S row, SQLNode sqlNode) {

        this.sqlNode = sqlNode;

        Map<String, Object> tempRow =  transformInputRow(row);
        this.prov = calculateProvenance(tempRow);
        Set<String> returnVars =  sqlNode.getOriginalReturnVars();
        this.result = updateResult(row, returnVars);
    }

    public abstract Map<String, Object> transformInputRow(S row);

    public abstract S updateResult(S row, Set<String> returnVars);

    public abstract T calculateProvenance(Map<String, Object> row);

    public abstract void mergeProvenance (ResultRow<S, T> otherRow);

    public S getResult() {
        return result;
    }
    public T getProv() {
        return prov;
    }

    public void setProv(T prov) {
        this.prov = prov ;
    }

    public SQLNode getSqlNode() {
        return sqlNode;
    }

    @Override
    public int hashCode() {
        return Objects.hash(result);
    }
}
