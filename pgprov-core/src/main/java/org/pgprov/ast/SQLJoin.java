package org.pgprov.ast;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SQLJoin extends SQLNode {

    private final SQLNode left;
    private final SQLNode right;

    public SQLJoin(SQLNode left, SQLNode right) {
        this.left = left;
        this.right = right;
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
    public Set<String> getOriginalReturnVars() {
        Set<String> returnVars = new HashSet<>();
        returnVars.addAll(this.left.getOriginalReturnVars());
        returnVars.addAll(this.right.getOriginalReturnVars());
        return returnVars;
    }

    @Override
    public Set<String> getReturnVarsForRewriting() {
        Set<String> returnVars = new HashSet<>();
        returnVars.addAll(this.left.getReturnVarsForRewriting());
        returnVars.addAll(this.right.getReturnVarsForRewriting());
        return returnVars;
    }

    @Override
    public void updateVarInSchemaAndSignatures(String varName) {
        this.left.updateVarInSchemaAndSignatures(varName);
        this.right.updateVarInSchemaAndSignatures(varName);
    }

    @Override
    public Set<Set<String>> calculateWhyProv(Map<String, Object> row, Map<String, List<String>> varSchemaAndSignatures) {

        Set<Set<String>> whyProv = new HashSet<>();
        Set<Set<String>> leftWhyProv = this.left.calculateWhyProv(row, varSchemaAndSignatures);
        Set<Set<String>> rightWhyProv = this.right.calculateWhyProv(row, varSchemaAndSignatures);

        for (Set<String> set : leftWhyProv) {
            for (Set<String> set2 : rightWhyProv) {
                Set<String> union = new HashSet<>(set);
                union.addAll(set2);
                whyProv.add(union);
            }
        }
        return whyProv;
    }

    @Override
    public String calculateHowProv(Map<String, Object> row, Map<String, List<String>> varSchemaAndSignatures) {

        String leftProv = this.left.calculateHowProv(row, varSchemaAndSignatures);
        String rightProv = this.right.calculateHowProv(row, varSchemaAndSignatures);

        if (!leftProv.isEmpty() && !rightProv.isEmpty()) {
            return "(" + leftProv + ") x (" + rightProv + ")";
        } else if (!leftProv.isEmpty()) {
            return leftProv;
        } else if (!rightProv.isEmpty()) {
            return rightProv;
        }
        return "";
    }
}
