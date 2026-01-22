package org.pgprov.ast;

import org.pgprov.Globals;

import java.util.*;

public class SQLJoin extends SQLNode {

    private final SQLNode left;
    private final SQLNode right;
    private final Map<String, List<String>> schemaAndSignatures;

    public SQLJoin(SQLNode left, SQLNode right, Map<String, List<String>> schemaAndSignatures) {
        this.left = left;
        this.right = right;
        this.schemaAndSignatures = schemaAndSignatures;
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
        if(schemaAndSignatures != null) {
            returnVars.addAll(schemaAndSignatures.keySet());
        }
        returnVars.addAll(this.left.getReturnVarsForRewriting());
        returnVars.addAll(this.right.getReturnVarsForRewriting());
        return returnVars;
    }

    @Override
    public void updateVarInSchemaAndSignatures(String varName) {

        if (schemaAndSignatures!= null && schemaAndSignatures.containsKey(varName)) {
            List<String> entry = schemaAndSignatures.remove(varName);

            String newVar = varName;
            if (varName.startsWith(Globals.TEMP_VAR_PREFIX)) {
                newVar = Globals.VAR_PREFIX + varName.substring(Globals.TEMP_VAR_PREFIX.length());
            } else if (varName.startsWith(Globals.TEMP_PATH_PREFIX)) {
                newVar = Globals.PATH_PREFIX + varName.substring(Globals.TEMP_PATH_PREFIX.length());
            }
            schemaAndSignatures.put(newVar, entry);
        }

        this.left.updateVarInSchemaAndSignatures(varName);
        this.right.updateVarInSchemaAndSignatures(varName);
    }

    @Override
    public void updateSchemaAndSignatures(Map<String, List<String>> varSchemaAndSignatures){

        Map<String, List<String>> tempSchemaAndSignatures = new HashMap<>();

        if(schemaAndSignatures != null) {
            for (Map.Entry<String,List<String>> entry : schemaAndSignatures.entrySet()) {

                tempSchemaAndSignatures
                        .computeIfAbsent(entry.getKey(), k -> new ArrayList<>())
                        .addAll(entry.getValue());
            }
        }

        for (Map.Entry<String,List<String>> entry : varSchemaAndSignatures.entrySet()) {
            tempSchemaAndSignatures
                    .computeIfAbsent(entry.getKey(), k->new ArrayList<>())
                    .addAll(entry.getValue());
        }

        this.left.updateSchemaAndSignatures(tempSchemaAndSignatures);
        this.right.updateSchemaAndSignatures(tempSchemaAndSignatures);
    }

    @Override
    public Set<Set<String>> calculateWhyProv(Map<String, Object> row) {

        Set<Set<String>> whyProv = new HashSet<>();
        Set<Set<String>> leftWhyProv = this.left.calculateWhyProv(row);
        Set<Set<String>> rightWhyProv = this.right.calculateWhyProv(row);

        if (leftWhyProv.isEmpty() && !rightWhyProv.isEmpty()) {
            whyProv.addAll(rightWhyProv);
        }else if (!leftWhyProv.isEmpty() && rightWhyProv.isEmpty()) {
            whyProv.addAll(leftWhyProv);
        }else{
            for (Set<String> set : leftWhyProv) {
                for (Set<String> set2 : rightWhyProv) {
                    Set<String> union = new HashSet<>(set);
                    union.addAll(set2);
                    whyProv.add(union);
                }
            }
        }
        return whyProv;
    }

    @Override
    public String calculateHowProv(Map<String, Object> row) {

        String leftProv = this.left.calculateHowProv(row);
        String rightProv = this.right.calculateHowProv(row);

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
