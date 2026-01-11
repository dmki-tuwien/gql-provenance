package org.pgprov.ast;

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
    public Set<String> getOriginalReturnVars() {
        return left.getOriginalReturnVars();
    }

    @Override
    public Set<String> getReturnVarsForRewriting() {
        Set<String> returnVars = new HashSet<>();
        returnVars.addAll(left.getReturnVarsForRewriting());
        returnVars.addAll(right.getReturnVarsForRewriting());
        return returnVars;
    }

    @Override
    public void setReturnVarsForRewriting(Set<String> returnVars) {
        left.setReturnVarsForRewriting(returnVars);
        right.setReturnVarsForRewriting(returnVars);
    }

    @Override
    public void updateVarInSchemaAndSignatures(String varName) {
        left.updateVarInSchemaAndSignatures(varName);
        right.updateVarInSchemaAndSignatures(varName);
    }

    @Override
    public Set<Set<String>> calculateWhyProv(Map<String, Object> row, Map<String, List<String>> varSchemaAndSignatures) {
        Set<Set<String>> whyProv = new HashSet<>();

        if (op.equals(SetOperator.UNION)) {
            whyProv.addAll(left.calculateWhyProv(row, varSchemaAndSignatures));
            whyProv.addAll(right.calculateWhyProv(row, varSchemaAndSignatures));
        }
        return whyProv;
    }

    @Override
    public String calculateHowProv(Map<String, Object> row, Map<String, List<String>> varSchemaAndSignatures) {
        String leftProv, rightProv;

        if (op.equals(SetOperator.UNION)) {
            leftProv = left.calculateHowProv(row, varSchemaAndSignatures);
            rightProv = right.calculateHowProv(row, varSchemaAndSignatures);

            if (!leftProv.isEmpty() && !rightProv.isEmpty()) {
                return "(" + leftProv + ") + (" + rightProv + ")";
            } else if (!leftProv.isEmpty()) {
                return leftProv;
            } else if (!rightProv.isEmpty()) {
                return rightProv;
            }

        }
        return "";
    }

    @Override
    public Map<String, Set<Object>> calculateWhereProv(Map<String, Object> row, Map<String, List<String>> varSchemaAndSignatures) {

        if (op.equals(SetOperator.UNION)) {
            Map<String, Set<Object>> whereLeftProv = left.calculateWhereProv(row, varSchemaAndSignatures);
            Map<String, Set<Object>> whereRightProv = right.calculateWhereProv(row, varSchemaAndSignatures);

            for (String key : whereRightProv.keySet()) {
                whereLeftProv
                        .computeIfAbsent(key, k -> new HashSet<>())
                        .addAll(whereRightProv.get(key));
            }
            return whereLeftProv;
        }
        return new HashMap<>();

    }

}
