package org.pgprov.ast;

import org.pgprov.Globals;

import java.util.*;

public class SQLRelationNode extends SQLNode {

    private final String relation;
    private final Set<String> columns;
    private final Set<String> schemaAndSignatures;
    private final Set<String> labelSignatures;

    public SQLRelationNode(String relation, Set<String> columns,
                           Set<String> schemaAndSignatures, Set<String> labelSignatures) {

        this.relation = relation;
        this.columns = columns;
        this.schemaAndSignatures = schemaAndSignatures;
        this.labelSignatures = labelSignatures;
    }

    @Override
    public String toString(int indent) {
        return "SQLRelationNode[relation=" + relation
                + ", variables=" + this.columns
                + "]";
    }

    @Override
    public Map<String, String> storeWhereProvenanceEncodings(Globals.ProvenanceType provenanceModel, Map<String,String> returnColumns, Map<String,String> renames){
        Map<String, String> provenanceEncodings = new HashMap<>();
        if(renames==null){
            renames = new HashMap<>();
        }

        if(returnColumns != null){
            for(Map.Entry<String, String> entry : returnColumns.entrySet()){
                if(entry.getValue().contains(".")){
                    String[] varSplits = entry.getValue().split("\\.");
                    String potentialProvEncoding = Globals.TEMP_VAR_PREFIX + varSplits[0];
                    if(schemaAndSignatures.contains(potentialProvEncoding) || schemaAndSignatures.contains(potentialProvEncoding  + Globals.PROP_ANNOT_KEY_PREFIX + varSplits[1]) ){
                        String renamedVar = varSplits[0];
                        if(renamedVar == null){
                            renamedVar = varSplits[0];
                        }
                        provenanceEncodings.put(entry.getKey(),  Globals.TEMP_VAR_PREFIX + renamedVar + Globals.PROP_ANNOT_KEY_PREFIX + varSplits[1]);
                    }
                }else if(!entry.getValue().endsWith("*")){
                    String renamedVar = renames.get(entry.getValue());
                    if(renamedVar == null){
                        renamedVar = entry.getValue();
                    }
                    if(schemaAndSignatures.contains(Globals.TEMP_PATH_PREFIX + entry.getValue())){
                        provenanceEncodings.put(entry.getKey(),  Globals.TEMP_PATH_PREFIX + renamedVar);
                    }else if(schemaAndSignatures.contains(Globals.TEMP_VAR_LIST_PREFIX + entry.getValue())){
                        provenanceEncodings.put(entry.getKey(),  Globals.TEMP_VAR_LIST_PREFIX + renamedVar);
                    }
                    else if(schemaAndSignatures.contains(Globals.TEMP_VAR_PREFIX + entry.getValue())){
                        provenanceEncodings.put(entry.getKey(),  Globals.TEMP_VAR_PREFIX + renamedVar);
                    }
                }else{
                    for(String varName : schemaAndSignatures){
                        if(!((varName.contains(Globals.TEMP_PATH_PREFIX) && varName.substring(Globals.TEMP_PATH_PREFIX.length()).contains(Globals.PATH_PREFIX)) || varName.contains(Globals.PROP_ANNOT_KEY_PREFIX) || varName.contains(Globals.LBL_ANNOT_KEY_PREFIX))){
                            String varNameSuffix;
                            if(varName.startsWith(Globals.TEMP_PATH_PREFIX)){
                                varNameSuffix = varName.substring(Globals.TEMP_PATH_PREFIX.length());
                            }else if(varName.startsWith(Globals.TEMP_VAR_LIST_PREFIX)){
                                varNameSuffix = varName.substring(Globals.TEMP_VAR_LIST_PREFIX.length());
                            }else{
                                varNameSuffix = varName.substring(Globals.TEMP_VAR_PREFIX.length());
                            }
                            provenanceEncodings.put(varNameSuffix,  varName);
                        }
                    }
                }
            }
        }
        return provenanceEncodings;
    }

    @Override
    public boolean updateWhereProvenanceEncodingVariable(String varName, SQLNode node) {
        return false;
    }

    @Override
    public Set<String> storeWhyProvenanceEncodings(Globals.ProvenanceType provenanceModel) {
        // does not store
        // get provenance encodings and forwards

        Set<String> provenanceEncodings = new HashSet<>();
        provenanceEncodings.addAll(schemaAndSignatures);
        provenanceEncodings.addAll(labelSignatures);
        return provenanceEncodings;
    }

    @Override
    public boolean updateWhyProvenanceEncodingVariable(String varName, SQLNode node) {
        // do nothing
        return false;
    }

    @Override
    public Set<String> getOriginalReturnVars() {
        return columns;
    }

    public void updateSchemaAndSignatures(Set<String> varSchemaAndSignatures) {

        for (String varSchemaAndSignature : varSchemaAndSignatures) {
            if((varSchemaAndSignature.length() > Globals.TEMP_VAR_PREFIX.length()&& columns.contains(varSchemaAndSignature.substring(Globals.TEMP_VAR_PREFIX.length())))||
                    (varSchemaAndSignature.length() > Globals.TEMP_VAR_LIST_PREFIX.length() && columns.contains(varSchemaAndSignature.substring(Globals.TEMP_VAR_LIST_PREFIX.length()))) ||
                    (varSchemaAndSignature.length() > Globals.TEMP_VAR_PREFIX.length()+ Globals.NODE_ANNOT_PREFIX.length() && columns.contains(varSchemaAndSignature.substring(Globals.TEMP_VAR_PREFIX.length()+ Globals.NODE_ANNOT_PREFIX.length())))||
                    (varSchemaAndSignature.length() > Globals.TEMP_VAR_LIST_PREFIX.length()+ Globals.NODE_ANNOT_PREFIX.length() && columns.contains(varSchemaAndSignature.substring(Globals.TEMP_VAR_LIST_PREFIX.length()+ Globals.NODE_ANNOT_PREFIX.length())))
            ){
                schemaAndSignatures.addAll(varSchemaAndSignatures);
            }
        }
    }
}
