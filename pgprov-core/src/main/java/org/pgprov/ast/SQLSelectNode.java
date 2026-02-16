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
    public Map<String, String> storeWhereProvenanceEncodings(Globals.ProvenanceType provenanceModel, Map<String,String> returnColumns, Map<String,String> renames) {
        return this.fromNode.storeWhereProvenanceEncodings(provenanceModel, returnColumns, renames);
    }

    @Override
    public boolean updateWhereProvenanceEncodingVariable(String varName, SQLNode node) {
        return this.fromNode.updateWhereProvenanceEncodingVariable(varName, node);
    }

    @Override
    public Set<String> storeWhyProvenanceEncodings(Globals.ProvenanceType provenanceModel) {
        // does not store
        // get provenance encodings and forwards
        Set<String> provenanceEncodings = this.fromNode.storeWhyProvenanceEncodings(provenanceModel);
        Set<String> newProvenanceEncodings = this.criteriaNode.storeWhyProvenanceEncodings(provenanceModel);

        provenanceEncodings.addAll(newProvenanceEncodings);
        return provenanceEncodings;
    }

    @Override
    public boolean updateWhyProvenanceEncodingVariable(String varName, SQLNode node) {
        return this.fromNode.updateWhyProvenanceEncodingVariable(varName, node);
    }

    @Override
    public Set<String> getOriginalReturnVars() {
        return fromNode.getOriginalReturnVars();
    }

    @Override
    public void updateSchemaAndSignatures(Set<String> varSchemaAndSignatures){

        Set<String> schemaAndSignatures = criteriaNode.schemaAndSignatures();
        varSchemaAndSignatures.addAll(schemaAndSignatures);

        fromNode.updateSchemaAndSignatures(varSchemaAndSignatures);
    }
}
