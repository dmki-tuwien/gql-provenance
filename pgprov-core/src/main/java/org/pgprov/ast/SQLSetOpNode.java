package org.pgprov.ast;

import org.pgprov.Globals;

import java.util.*;

public class SQLSetOpNode extends SQLNode {

    private final SQLNode left;
    private final SQLNode right;
    private final SetOperator op;

    public SQLSetOpNode(SQLNode left, SQLNode right, SetOperator op) {
        this.left = left;
        this.right = right;
        this.op = op;
    }

    @Override
    public String toString(int indent) {
        return "SQLSetOpNode[\n"
                + "  ".repeat(indent + 1)
                + "op=" + op + ",\n"
                + "  ".repeat(indent + 1)
                + "left=" + left.toString(indent + 2) + ", \n"
                + "  ".repeat(indent + 1)
                + "right=" + right.toString(indent + 2) + "]";
    }

    @Override
    public Map<String, String> storeWhereProvenanceEncodings(Globals.ProvenanceType provenanceModel, Map<String,String> returnColumns, Map<String,String> renames){

        Map<String, String>  provenanceEncodings = new HashMap<>();
        Map<String, String>  leftProvenanceEncodings = this.left.storeWhereProvenanceEncodings(provenanceModel, returnColumns, renames);
        Map<String, String>  rightProvenanceEncodings = this.right.storeWhereProvenanceEncodings(provenanceModel, returnColumns, renames);

        if(leftProvenanceEncodings != null ) {
            provenanceEncodings.putAll(leftProvenanceEncodings);
        }

        if(rightProvenanceEncodings != null ) {
            provenanceEncodings.putAll(rightProvenanceEncodings);
        }
        return provenanceEncodings;
    }

    @Override
    public boolean updateWhereProvenanceEncodingVariable(String varName, SQLNode node) {
        boolean leftBool = this.left.updateWhereProvenanceEncodingVariable(varName, node);
        boolean rightBool = this.right.updateWhereProvenanceEncodingVariable(varName, node);
        return (leftBool || rightBool);
    }

    @Override
    public Set<String> storeWhyProvenanceEncodings(Globals.ProvenanceType provenanceModel) {

        Set<String> provenanceEncodings = new HashSet<>();
        Set<String> leftProvenanceEncodings = this.left.storeWhyProvenanceEncodings(provenanceModel);
        Set<String> rightProvenanceEncodings = this.right.storeWhyProvenanceEncodings(provenanceModel);

        if(leftProvenanceEncodings != null ) {
            provenanceEncodings.addAll(leftProvenanceEncodings);
        }

        if(rightProvenanceEncodings != null ) {
            provenanceEncodings.addAll(rightProvenanceEncodings);
        }

        if(this.left instanceof SQLProjectNode leftNode){
            leftNode.storeProvenanceEncodingsFromUnion(provenanceEncodings);
        } else if (this.left instanceof SQLRenameNode leftNode) {
            leftNode.storeProvenanceEncodingsFromUnion(provenanceEncodings);
        } else if (this.left instanceof SQLSetOpNode leftNode) {
            leftNode.storeProvenanceEncodingsFromUnion(provenanceEncodings);
        }
        if(this.right instanceof SQLProjectNode rightNode){
            rightNode.storeProvenanceEncodingsFromUnion(provenanceEncodings);
        } else if (this.right instanceof SQLRenameNode rightNode) {
            rightNode.storeProvenanceEncodingsFromUnion(provenanceEncodings);
        }else if (this.right instanceof SQLSetOpNode rightNode) {
            rightNode.storeProvenanceEncodingsFromUnion(provenanceEncodings);
        }
        return provenanceEncodings;
    }

    @Override
    public boolean updateWhyProvenanceEncodingVariable(String varName, SQLNode node) {
        boolean leftBool = this.left.updateWhyProvenanceEncodingVariable(varName, node);
        boolean rightBool = this.right.updateWhyProvenanceEncodingVariable(varName, node);
        return (leftBool || rightBool);
    }

    public void storeProvenanceEncodingsFromUnion(Set<String> provenanceEncodings){

        if(this.left instanceof SQLProjectNode leftNode){
            leftNode.storeProvenanceEncodingsFromUnion(provenanceEncodings);
        } else if (this.left instanceof SQLRenameNode leftNode) {
            leftNode.storeProvenanceEncodingsFromUnion(provenanceEncodings);
        }
        if(this.right instanceof SQLProjectNode rightNode){
            rightNode.storeProvenanceEncodingsFromUnion(provenanceEncodings);
        } else if (this.right instanceof SQLRenameNode rightNode) {
            rightNode.storeProvenanceEncodingsFromUnion(provenanceEncodings);
        }
    }


    @Override
    public Set<String> getOriginalReturnVars() {
        return left.getOriginalReturnVars();
    }

    @Override
    public void updateSchemaAndSignatures(Set<String> varSchemaAndSignatures){

        left.updateSchemaAndSignatures(new HashSet<>(varSchemaAndSignatures));
        right.updateSchemaAndSignatures(new HashSet<>(varSchemaAndSignatures));
    }

}
