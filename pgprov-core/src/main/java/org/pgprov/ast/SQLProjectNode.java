package org.pgprov.ast;

import org.pgprov.Globals;

import java.util.*;

public class SQLProjectNode extends SQLNode {

    private final Set<String> columns;
    private final SQLNode fromNode;
    private final Set<String> schemaAndSignatures;
    private final Set<String> whyProvenanceEncodings = new HashSet<>();
    private final Map<String, String> whereProvenanceEncodings = new HashMap<>();


    private final Set<String> externalProvenanceEncodings = new HashSet<>();

    public Set<String> getWhyProvenanceEncodings() {

        return whyProvenanceEncodings;
    }

    public Map<String, String> getWhereProvenanceEncodings() {

        return whereProvenanceEncodings;
    }

    public Set<String> getExternalProvenanceEncodings() {
        return externalProvenanceEncodings;
    }

    public SQLProjectNode(Set<String> columns, SQLNode fromNode, Set<String> schemaAndSignatures) {

        this.columns = columns;
        this.fromNode = fromNode;
        this.schemaAndSignatures = schemaAndSignatures;
    }

    @Override
    public String toString(int indent) {
        return "SQLProjectNode[columns=" + columns + ", \n"
                + "  ".repeat(indent + 1)
                + "fromNode=" + fromNode.toString(indent + 2) + "]";
    }

    @Override
    public Map<String, String> storeWhereProvenanceEncodings(Globals.ProvenanceType provenanceModel, Map<String,String> returnColumns, Map<String,String> renames){

        boolean finalReturn = returnColumns==null || returnColumns.isEmpty();
        Set<String> outputValues = new HashSet<>();
        if(finalReturn){

            if(returnColumns==null){
                returnColumns = new HashMap<>();
            }

            for(String key: columns){
                returnColumns.put(key,key);
            }

            if(renames!=null){

                for(Map.Entry<String, String> entry: renames.entrySet()){
                    if(returnColumns.containsKey(entry.getKey())){
                        String value = entry.getValue();
                        String key = returnColumns.remove(entry.getKey());
                        returnColumns.put(value, key);
                    }
                }
                renames.clear();
            }
        }else{
            for(Map.Entry<String, String> entry: returnColumns.entrySet()){
                if( !entry.getKey().equals(entry.getValue()) && columns.contains(entry.getValue()) && entry.getValue().contains(".")){
                   outputValues.add(entry.getKey());
                }
            }
        }

        Map<String, String> fromProvenanceEncodings = this.fromNode.storeWhereProvenanceEncodings(provenanceModel, returnColumns, renames);

        if(finalReturn){
            this.whereProvenanceEncodings.putAll(fromProvenanceEncodings);
        }else if(!outputValues.isEmpty()){
            for(String key: outputValues){
                this.whereProvenanceEncodings.put(key, fromProvenanceEncodings.get(key));
            }
        }
        return fromProvenanceEncodings;

    }

    @Override
    public Set<String> storeWhyProvenanceEncodings(Globals.ProvenanceType provenanceModel) {

            // get provenance encodings and forwards
            Set<String> fromProvenanceEncodings = this.fromNode.storeWhyProvenanceEncodings(provenanceModel);

            Set<String> provenanceEncodings = new HashSet<>(fromProvenanceEncodings);

            if(schemaAndSignatures != null) {
                provenanceEncodings.addAll(schemaAndSignatures);
            }
            provenanceEncodings.removeIf(key -> (key.startsWith(Globals.TEMP_VAR_PREFIX) || key.startsWith(Globals.TEMP_VAR_LIST_PREFIX)) && !(key.contains(Globals.PROP_ANNOT_KEY_PREFIX) || key.contains(Globals.LBL_ANNOT_KEY_PREFIX)));
            this.whyProvenanceEncodings.addAll(provenanceEncodings);
            return provenanceEncodings;
    }

    @Override
    public boolean updateWhereProvenanceEncodingVariable(String varName, SQLNode node) {

        boolean bool = this.fromNode.updateWhereProvenanceEncodingVariable(varName,  node);
        if(node.equals(this) || bool) {
            String removed = this.whereProvenanceEncodings.remove(varName);

            if(removed==null) {
                return true;
            }

            String newVar = removed;
            if (removed.startsWith(Globals.TEMP_VAR_LIST_PREFIX)) {
                newVar = Globals.VAR_PREFIX + removed.substring(Globals.TEMP_VAR_LIST_PREFIX.length());
            } else if (removed.startsWith(Globals.TEMP_VAR_PREFIX)) {
                newVar = Globals.VAR_PREFIX + removed.substring(Globals.TEMP_VAR_PREFIX.length());
            } else if (removed.startsWith(Globals.TEMP_PATH_PREFIX)) {
                newVar = removed.substring(Globals.TEMP_PATH_PREFIX.length());
                if(!newVar.startsWith(Globals.PATH_PREFIX)) {
                    newVar = Globals.PATH_PREFIX + newVar;
                }
            }
            this.whereProvenanceEncodings.put(varName, newVar );
            return true;
        }
        return false;
    }

    @Override
    public boolean updateWhyProvenanceEncodingVariable(String varName, SQLNode node) {

        boolean bool = this.fromNode.updateWhyProvenanceEncodingVariable(varName,  node);
        if(node.equals(this) || bool) {
            boolean removed = this.whyProvenanceEncodings.remove(varName);

            if(!removed) {
                return true;
            }

            String newVar = varName;
            if (varName.startsWith(Globals.TEMP_VAR_LIST_PREFIX)) {
                newVar = Globals.VAR_PREFIX + varName.substring(Globals.TEMP_VAR_LIST_PREFIX.length());
            }else if (varName.startsWith(Globals.TEMP_VAR_PREFIX)) {
                newVar = Globals.VAR_PREFIX + varName.substring(Globals.TEMP_VAR_PREFIX.length());
            } else if (varName.startsWith(Globals.TEMP_PATH_PREFIX)) {
                newVar = varName.substring(Globals.TEMP_PATH_PREFIX.length());
                if(!newVar.startsWith(Globals.PATH_PREFIX)) {
                    newVar = Globals.PATH_PREFIX + newVar;
                }
            }
            this.whyProvenanceEncodings.add(newVar);
            return true;
        }
        return false;
    }

    public void storeProvenanceEncodingsFromUnion(Set<String> provenanceEncodings){

        this.externalProvenanceEncodings.addAll(provenanceEncodings);
      //  System.out.println(this.toString(0)+", provenanceEncodingsUnion: " + this.externalProvenanceEncodings);
    }
    @Override
    public void updateSchemaAndSignatures(Set<String> varSchemaAndSignatures){

        if(schemaAndSignatures!=null) {
            varSchemaAndSignatures.addAll(schemaAndSignatures);
        }
        fromNode.updateSchemaAndSignatures(varSchemaAndSignatures);

    }

    @Override
    public Set<String> getOriginalReturnVars() {
        return columns;
    }
}
