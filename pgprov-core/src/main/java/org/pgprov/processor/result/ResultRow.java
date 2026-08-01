package org.pgprov.processor.result;

import org.pgprov.ast.SQLNode;
import org.pgprov.graph.model.Edge;

import java.util.*;

public abstract class ResultRow<S,T> {

    private final S result;

    private T prov;

    private final SQLNode sqlNode;

    private final List<List<String>> edges = new ArrayList<>();
    private final Map<List<List<String>>, T > edgeProvMap = new HashMap<>();

    public ResultRow(S row, SQLNode sqlNode, boolean edgeMinimality) {

        this.sqlNode = sqlNode;

        Map<String, Object> tempRow =  transformInputRow(row);
        this.prov = calculateProvenance(tempRow, edgeMinimality);
        Set<String> returnVars =  sqlNode.getOriginalReturnVars();
        this.result = updateResult(row, returnVars);
    }

    public abstract Map<String, Object> transformInputRow(S row);

    public abstract S updateResult(S row, Set<String> returnVars);

    public abstract T calculateProvenance(Map<String, Object> row, boolean edgeMinimality);

    public abstract void mergeProvenance (ResultRow<S, T> otherRow, boolean edgeMinimality);

    public S getResult() {
        return result;
    }
    public T getProv() {
        return prov;
    }

    public List<List<String>> getEdges() {return edges;}
    public void addEdges(List<String> listEdge){edges.add(listEdge);}
    public Map<List<List<String>>, T> getEdgeProvMap() {return edgeProvMap;}

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
