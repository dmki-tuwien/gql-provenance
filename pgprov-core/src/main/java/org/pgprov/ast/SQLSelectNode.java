package org.pgprov.ast;

import org.pgprov.Globals;

import java.util.*;

public class SQLSelectNode extends SQLNode {

    private final SQLNode fromNode;
    private final SQLSelectCriteriaNode criteriaNode;

    public SQLSelectNode(SQLNode fromNode, SQLSelectCriteriaNode criteriaNode) {
        this.fromNode = fromNode;
        this.criteriaNode = criteriaNode;
    }

    @Override
    public String toString(int indent) {
        return "SQLSelectNode[\n"
                + "  ".repeat(indent + 1)
                + "fromNode=" + fromNode.toString(indent + 2)
                + ",\n"
                + "  ".repeat(indent + 1)
                + " where=" + criteriaNode.where() + "]";
    }

    @Override
    public Set<String> getOriginalReturnVars() {
        return fromNode.getOriginalReturnVars();
    }

    @Override
    public Set<String> getReturnVarsForRewriting() {
        Set<String> returnVars = new HashSet<>();
        returnVars.addAll(fromNode.getReturnVarsForRewriting());
        returnVars.addAll(criteriaNode.getReturnVarsForRewriting());
        return returnVars;
    }

    @Override
    public void updateVarInSchemaAndSignatures(String varName) {
        fromNode.updateVarInSchemaAndSignatures(varName);
    }

    @Override
    public void updateSchemaAndSignatures(Map<String, List<String>> varSchemaAndSignatures){

        Map<String, List<String>> schemaAndSignatures = criteriaNode.schemaAndSignatures();

        for (Map.Entry<String, List<String>> entry : schemaAndSignatures.entrySet()) {

            varSchemaAndSignatures
                    .computeIfAbsent(entry.getKey(), k -> new ArrayList<>())
                    .addAll(entry.getValue());
        }

        fromNode.updateSchemaAndSignatures(varSchemaAndSignatures);
    }

    @Override
    public Set<Set<String>> calculateWhyProv(Map<String, Object> row) {

        return fromNode.calculateWhyProv(row);
    }

    @Override
    public String calculateHowProv(Map<String, Object> row) {

        String prov = fromNode.calculateHowProv(row);
        if (!prov.isEmpty()) {
            return fromNode.calculateHowProv(row) + " x 1";
        }
        return "";
    }
}
