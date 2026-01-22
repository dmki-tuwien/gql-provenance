package org.pgprov.ast;

import org.pgprov.Globals;
import org.pgprov.graph.model.Entity;
import org.pgprov.graph.model.Path;

import java.util.*;

public class SQLProjectNode extends SQLNode {

    private final Set<String> columns;
    private final SQLNode fromNode;
    private final Map<String, List<String>> schemaAndSignatures;
    private final Set<String> newVars;
    private final boolean passToSubquery;

    public SQLProjectNode(Set<String> columns, SQLNode fromNode, Map<String, List<String>> schemaAndSignatures, Set<String> newVars, boolean passToSubquery) {

        this.columns = columns;
        this.fromNode = fromNode;
        this.schemaAndSignatures = schemaAndSignatures;
        this.newVars = newVars;
        this.passToSubquery = passToSubquery;
    }

    @Override
    public String toString(int indent) {
        return "SQLProjectNode[columns=" + columns + ", \n"
                + "  ".repeat(indent + 1)
                + "fromNode=" + fromNode.toString(indent + 2) + "]";
    }


    @Override
    public Set<String> getOriginalReturnVars() {
        return columns;
    }

    @Override
    public Set<String> getReturnVarsForRewriting() {

        Set<String> returnVars = new HashSet<>();
        if (!passToSubquery) {
            returnVars.addAll(fromNode.getReturnVarsForRewriting());
            returnVars.addAll(schemaAndSignatures.keySet());
        }
        return returnVars;
    }

    @Override
    public void setReturnVarsForRewriting(Set<String> returnVars) {

        for (String returnVar : returnVars) {
            if (!schemaAndSignatures.containsKey(returnVar)) {
                newVars.add(returnVar);
            }
        }
    }

    @Override
    public Set<String> getExternalVarsForRewriting() {
        return newVars;
    }

    @Override
    public void updateVarInSchemaAndSignatures(String varName) {

        if (schemaAndSignatures.containsKey(varName)) {
            List<String> entry = schemaAndSignatures.remove(varName);

            String newVar = varName;
            if (varName.startsWith(Globals.TEMP_VAR_PREFIX)) {
                newVar = Globals.VAR_PREFIX + varName.substring(Globals.TEMP_VAR_PREFIX.length());
            } else if (varName.startsWith(Globals.TEMP_PATH_PREFIX)) {
                newVar = Globals.PATH_PREFIX + varName.substring(Globals.TEMP_PATH_PREFIX.length());
            }
            schemaAndSignatures.put(newVar, entry);
        }
        fromNode.updateVarInSchemaAndSignatures(varName);
    }

    @Override
    public void updateSchemaAndSignatures(Map<String, List<String>> varSchemaAndSignatures){

        for (String varName : schemaAndSignatures.keySet()) {

            List<String> schemaList = varSchemaAndSignatures.getOrDefault(varName, new ArrayList<>());
            schemaList.addAll(schemaAndSignatures.get(varName));
            varSchemaAndSignatures.put(varName, schemaList);
        }
        fromNode.updateSchemaAndSignatures(varSchemaAndSignatures);

    }

    @Override
    public Set<Set<String>> calculateWhyProv(Map<String, Object> row) {
        return fromNode.calculateWhyProv(row);
    }

    @Override
    public String calculateHowProv(Map<String, Object> row) {
        return fromNode.calculateHowProv(row);
    }

    @Override
    public Map<String, Set<Object>> calculateWhereProv(Map<String, Object> row) {

        Map<String, Set<Object>> whereProv = new HashMap<>();

        for (String varName : columns) {
            Set<Object> annotation = new HashSet<>();
            if (!varName.contains(".")) {
                if (row.containsKey(Globals.VAR_PREFIX + varName)) {
                    Entity entity = (Entity) row.get(Globals.VAR_PREFIX + varName);
                    annotation.add(entity.getAnnotation());
                } else if (row.containsKey(Globals.PATH_PREFIX + varName)) {
                    Path path = (Path) row.get(Globals.PATH_PREFIX + varName);

                    for (Entity entity : path) {
                        annotation.add(entity.getAnnotation());
                    }
                }

            } else {
                String var = varName.substring(0, varName.indexOf("."));
                if (row.containsKey(Globals.VAR_PREFIX + var)) {
                    Entity entity = (Entity) row.get(Globals.VAR_PREFIX + var);
                    String ann = (String) entity.getPropertyAnnotation(varName.substring(varName.indexOf(".") + 1));
                    if (ann != null) {
                        annotation.add(ann);
                    }
                }
            }
            whereProv.put(varName, annotation);
        }
        return whereProv;
    }

}
