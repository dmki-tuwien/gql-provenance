package org.pgprov.ast;

import org.pgprov.Globals;

import java.util.*;

public class SQLSelectCriteriaNode extends SQLNode {

    private final String where;
    private final Set<String> schemaAndSignatures;
    private final Set<String> labelSignatures;

    public SQLSelectCriteriaNode(String where, Set<String> schemaAndSignatures, Set<String> labelSignatures) {

        this.where = where;
        this.schemaAndSignatures = schemaAndSignatures;
        this.labelSignatures = labelSignatures;
    }

    public Set<String> schemaAndSignatures() {
        return schemaAndSignatures;
    }

    public String where() {
        return where;
    }

    @Override
    public Map<String, String> storeWhereProvenanceEncodings(Globals.ProvenanceType provenanceModel, Map<String,String> returnColumns, Map<String,String> renames){
        return null;
    }

    @Override
    public boolean updateWhereProvenanceEncodingVariable(String varName, SQLNode node) {
        return false;
    }

    @Override
    public Set<String> storeWhyProvenanceEncodings(Globals.ProvenanceType provenanceModel) {
        // does not store
        // get provenance encodings and forwards
        Set<String> provenanceEncodings = new HashSet<>(schemaAndSignatures);
        provenanceEncodings.addAll(labelSignatures);
        return provenanceEncodings;
    }

    @Override
    public boolean updateWhyProvenanceEncodingVariable(String varName, SQLNode node) {
        // do nothing
        return false;
    }
}
