package org.pgprov.ast;

import org.pgprov.Globals;

import java.util.*;

public class SQLJoin extends SQLNode {

    private final SQLNode left;
    private final SQLNode right;
    private final Set<String> schemaAndSignatures;
    private final boolean subQueryInnerJoin;

    public SQLJoin(SQLNode left, SQLNode right, Set<String> schemaAndSignatures, boolean subQueryInnerJoin) {
        this.left = left;
        this.right = right;
        this.schemaAndSignatures = schemaAndSignatures;
        this.subQueryInnerJoin = subQueryInnerJoin;
    }


    @Override
    public String toString(int indent) {
        return "SQLJoin[ \n"
                + "  ".repeat(indent + 1)
                + "left=" + this.left.toString(indent + 2) + ", \n"
                + "  ".repeat(indent + 1)
                + "right=" + this.right.toString(indent + 2) + "]";


    }

    @Override
    public Map<String, String> storeWhereProvenanceEncodings(Globals.ProvenanceType provenanceModel, Map<String, String> returnColumns, Map<String,String> renames){
        // does not store
        // get provenance encodings and forwards
        Map<String, String> provenanceEncodings = new HashMap<>();

        if(!subQueryInnerJoin) {
            Map<String, String> leftProvenanceEncodings = this.left.storeWhereProvenanceEncodings(provenanceModel, returnColumns, renames);
            provenanceEncodings.putAll(leftProvenanceEncodings);
        }

        Map<String, String> rightProvenanceEncodings = this.right.storeWhereProvenanceEncodings(provenanceModel, returnColumns, renames);
        provenanceEncodings.putAll(rightProvenanceEncodings);

        return provenanceEncodings;
    }

    @Override
    public boolean updateWhereProvenanceEncodingVariable(String varName, SQLNode node) {
        boolean leftBool = this.left.updateWhereProvenanceEncodingVariable(varName, node);
        boolean rightBool = this.right.updateWhereProvenanceEncodingVariable(varName, node);
        return leftBool || rightBool;
    }

    @Override
    public Set<String> storeWhyProvenanceEncodings(Globals.ProvenanceType provenanceModel) {
        // does not store
        // get provenance encodings and forwards
        Set<String> provenanceEncodings = new HashSet<>();

        if(!subQueryInnerJoin) {
            Set<String> leftProvenanceEncodings = this.left.storeWhyProvenanceEncodings(provenanceModel);
            provenanceEncodings.addAll(leftProvenanceEncodings);
        }

        Set<String> rightProvenanceEncodings = this.right.storeWhyProvenanceEncodings(provenanceModel);
        provenanceEncodings.addAll(rightProvenanceEncodings);

        if(schemaAndSignatures != null) {
            provenanceEncodings.addAll(schemaAndSignatures);
        }

        return provenanceEncodings;
    }

    @Override
    public boolean updateWhyProvenanceEncodingVariable(String varName, SQLNode node) {
        boolean leftBool = this.left.updateWhyProvenanceEncodingVariable(varName, node);
        boolean rightBool = this.right.updateWhyProvenanceEncodingVariable(varName, node);
        return leftBool || rightBool;
    }

    @Override
    public void updateSchemaAndSignatures(Set<String> varSchemaAndSignatures){

        Set<String> tempSchemaAndSignatures = new HashSet<>();

        if(schemaAndSignatures != null) {
            tempSchemaAndSignatures.addAll(schemaAndSignatures);
        }

        tempSchemaAndSignatures.addAll(varSchemaAndSignatures);

        this.left.updateSchemaAndSignatures(tempSchemaAndSignatures);
        this.right.updateSchemaAndSignatures(tempSchemaAndSignatures);
    }
}
