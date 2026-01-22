package org.pgprov.ast;

import org.pgprov.Globals;

import java.util.*;

public class SQLRenameNode extends SQLNode {

    private final SQLNode from;
    private final Map<String, String> rename;

    public SQLRenameNode(SQLNode from, Map<String, String> rename) {

        this.from = from;
        this.rename = rename;
    }

    @Override
    public String toString(int indent) {
        return "SQLRenameNode[\n"
                + "  ".repeat(indent + 1)
                + "from=" + from.toString(indent + 2) + ",\n"
                + "  ".repeat(indent + 1)
                + "rename=" + rename + "]";
    }

    @Override
    public Set<String> getOriginalReturnVars() {

        Set<String> origReturnVars = new HashSet<>(from.getOriginalReturnVars());

        for (Map.Entry<String, String> entry : rename.entrySet()) {
            origReturnVars.add(entry.getValue());
            origReturnVars.remove(entry.getKey());
        }
        return origReturnVars;
    }

    @Override
    public Set<String> getReturnVarsForRewriting() {
        return from.getReturnVarsForRewriting();
    }

    public void setReturnVarsForRewriting(Set<String> returnVars) {
        from.setReturnVarsForRewriting(returnVars);
    }

    @Override
    public Set<String> getExternalVarsForRewriting() {
        return from.getExternalVarsForRewriting();
    }

    @Override
    public void updateVarInSchemaAndSignatures(String varName) {
        from.updateVarInSchemaAndSignatures(varName);
    }

    @Override
    public void updateSchemaAndSignatures(Map<String, List<String>> varSchemaAndSignatures){

        for (String key : rename.keySet()) {
            if (!key.contains(".")) {
                List<String> value = varSchemaAndSignatures.remove(Globals.VAR_PREFIX + rename.get(key));
                if(value!=null){
                    varSchemaAndSignatures.put(Globals.VAR_PREFIX + key, value);
                }
            }
        }
        from.updateSchemaAndSignatures(varSchemaAndSignatures);

    }

    @Override
    public Set<Set<String>> calculateWhyProv(Map<String, Object> row) {
        return from.calculateWhyProv(row);
    }

    @Override
    public String calculateHowProv(Map<String, Object> row) {

        for (String key : rename.keySet()) {
            if (!key.contains(".")) {
                Object rowVal = row.remove(Globals.VAR_PREFIX + rename.get(key));
                row.put(Globals.VAR_PREFIX + key, rowVal);
            }
        }
        return from.calculateHowProv(row);
    }

    @Override
    public Map<String, Set<Object>> calculateWhereProv(Map<String, Object> row) {

        Map<String, Set<Object>> whereProv = from.calculateWhereProv(row);

        for (String key : rename.keySet()) {
            Set<Object> prov = whereProv.remove(key);
            whereProv.put(rename.get(key), prov);
        }
        return whereProv;
    }

}
