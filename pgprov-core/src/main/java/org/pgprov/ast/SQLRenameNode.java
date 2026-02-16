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
    public Map<String, String> storeWhereProvenanceEncodings(Globals.ProvenanceType provenanceModel, Map<String,String> returnColumns, Map<String,String> renames){

        if(returnColumns != null){
            for(Map.Entry<String, String> entry : returnColumns.entrySet()){

                for(Map.Entry<String, String> renameEntry : rename.entrySet()){

                    if(renameEntry.getValue().equals(entry.getValue())){
                        entry.setValue(renameEntry.getKey());
                    }else if(entry.getValue().contains(".")){
                        String[] varSplits = entry.getValue().split("\\.");
                        if(varSplits[0].equals(renameEntry.getValue())){
                            entry.setValue(renameEntry.getKey()+"."+ varSplits[1]);
                        }
                    }
                }
            }
        }

        if(renames == null){
            renames = new HashMap<>();
        }
        renames.putAll(rename);

        Map<String, String> provenanceEncodings = from.storeWhereProvenanceEncodings(provenanceModel, returnColumns, renames);
        for(Map.Entry<String, String> entry : provenanceEncodings.entrySet()){
            if(entry.getValue().startsWith(Globals.TEMP_PATH_PREFIX)){
                String varNameSuffix = entry.getValue().substring(Globals.TEMP_PATH_PREFIX.length());
                String renamedVar = renames.get(varNameSuffix);
                if(renamedVar != null){
                    entry.setValue(Globals.TEMP_PATH_PREFIX+renamedVar);
                }
            }else if(entry.getValue().startsWith(Globals.TEMP_VAR_LIST_PREFIX)){
                String varNameSuffix;
                if(entry.getValue().contains(Globals.NODE_ANNOT_PREFIX)||entry.getValue().contains(Globals.EDGE_ANNOT_PREFIX)){
                    varNameSuffix =  entry.getValue().substring(Globals.TEMP_VAR_LIST_PREFIX.length()+Globals.NODE_ANNOT_PREFIX.length());
                }else {
                    varNameSuffix =  entry.getValue().substring(Globals.TEMP_VAR_LIST_PREFIX.length());
                }

                String renamedVar = renames.get(varNameSuffix);
                if(renamedVar != null){

                    String prefixPattern ="";
                    if(entry.getValue().contains(Globals.NODE_ANNOT_PREFIX)||entry.getValue().contains(Globals.EDGE_ANNOT_PREFIX)){
                        prefixPattern = entry.getValue().substring(Globals.TEMP_VAR_LIST_PREFIX.length()).startsWith(Globals.NODE_ANNOT_PREFIX)? Globals.NODE_ANNOT_PREFIX :Globals.EDGE_ANNOT_PREFIX;
                    }
                    entry.setValue(Globals.TEMP_VAR_LIST_PREFIX + prefixPattern+ renamedVar);
                }
            }else if(entry.getValue().startsWith(Globals.TEMP_VAR_PREFIX)){
                String varNameSuffix;
                String prefixPattern ="";
                if(entry.getValue().contains(Globals.NODE_ANNOT_PREFIX)||entry.getValue().contains(Globals.EDGE_ANNOT_PREFIX)){
                    varNameSuffix =  entry.getValue().substring(Globals.TEMP_VAR_PREFIX.length()+Globals.NODE_ANNOT_PREFIX.length());
                    prefixPattern = entry.getValue().substring(Globals.TEMP_VAR_PREFIX.length()).startsWith(Globals.NODE_ANNOT_PREFIX)? Globals.NODE_ANNOT_PREFIX :Globals.EDGE_ANNOT_PREFIX;
                }else {
                    varNameSuffix =  entry.getValue().substring(Globals.TEMP_VAR_PREFIX.length());
                }
                if(varNameSuffix.contains(Globals.PROP_ANNOT_KEY_PREFIX)) {
                    String[] varSplits = varNameSuffix.split(Globals.PROP_ANNOT_KEY_PREFIX);
                    String renamedVar = renames.get(varSplits[0]);

                    if(renamedVar != null){
                        entry.setValue(Globals.TEMP_VAR_PREFIX+prefixPattern+renamedVar+Globals.PROP_ANNOT_KEY_PREFIX+varSplits[1]);
                    }
                }else{
                    String renamedVar = renames.get(varNameSuffix);
                    if(renamedVar != null){
                        entry.setValue(Globals.TEMP_VAR_PREFIX + prefixPattern+ renamedVar);
                    }
                }

            }
        }
        return provenanceEncodings;

    }

    @Override
    public boolean updateWhereProvenanceEncodingVariable(String varName, SQLNode node) {
        boolean bool = from.updateWhereProvenanceEncodingVariable(varName, node);
        if(node.equals(this) || bool){
            return true;
        }
        return false;
    }

    @Override
    public Set<String> storeWhyProvenanceEncodings(Globals.ProvenanceType provenanceModel) {

        return from.storeWhyProvenanceEncodings(provenanceModel );
    }

    @Override
    public boolean updateWhyProvenanceEncodingVariable(String varName, SQLNode node) {

        boolean bool = from.updateWhyProvenanceEncodingVariable(varName, node);
        if(node.equals(this) || bool){
            return true;
        }
        return false;
    }

    public void storeProvenanceEncodingsFromUnion(Set<String> provenanceEncodings){

        if(this.from instanceof SQLProjectNode node){
            node.storeProvenanceEncodingsFromUnion(provenanceEncodings);
        } else if (this.from instanceof SQLRenameNode node) {
            node.storeProvenanceEncodingsFromUnion(provenanceEncodings);
        }else if (this.from instanceof SQLSetOpNode node) {
            node.storeProvenanceEncodingsFromUnion(provenanceEncodings);
        }
    }

    public Set<String> getWhyProvenanceEncodings() {
        if(this.from instanceof SQLProjectNode node){
            return node.getWhyProvenanceEncodings();
        } else if (this.from instanceof SQLRenameNode node) {
            return node.getWhyProvenanceEncodings();
        }
        return null;
    }

    public Map<String, String> getWhereProvenanceEncodings() {

        if(this.from instanceof SQLProjectNode node){
            return node.getWhereProvenanceEncodings();
        } else if (this.from instanceof SQLRenameNode node) {
            return node.getWhereProvenanceEncodings();
        }
        return null;
    }

    public Set<String> getExternalProvenanceEncodings() {
        if(this.from instanceof SQLProjectNode node){
            return node.getExternalProvenanceEncodings();
        } else if (this.from instanceof SQLRenameNode node) {
            return node.getExternalProvenanceEncodings();
        }
        return null;
    }

    @Override
    public void updateSchemaAndSignatures(Set<String> varSchemaAndSignatures){

        for (String key : rename.keySet()) {
            if (!key.contains(".")) {
                for(String key2 : varSchemaAndSignatures) {
                    if(key2.startsWith(Globals.VAR_PREFIX + Globals.NODE_ANNOT_PREFIX+rename.get(key)) || key2.startsWith(Globals.EDGE_ANNOT_PREFIX+rename.get(key))) {
                        boolean removed = varSchemaAndSignatures.remove(key2);
                        if(removed){

                            String newName;
                            String prefixPattern = "";
                            if(key2.contains(Globals.NODE_ANNOT_PREFIX) || key2.contains(Globals.EDGE_ANNOT_PREFIX)){
                                newName = key2.substring((Globals.VAR_PREFIX + rename.get(key)).length() + Globals.NODE_ANNOT_PREFIX.length());
                                prefixPattern = key2.substring(Globals.VAR_PREFIX.length()).startsWith(Globals.NODE_ANNOT_PREFIX)? Globals.NODE_ANNOT_PREFIX :Globals.EDGE_ANNOT_PREFIX;
                            } else {
                                newName = key2.substring((Globals.VAR_PREFIX + rename.get(key)).length());
                            }
                            varSchemaAndSignatures.add(Globals.VAR_PREFIX+ prefixPattern+ key +newName);
                        }
                    }
                }
            }
        }
        from.updateSchemaAndSignatures(varSchemaAndSignatures);

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

}
