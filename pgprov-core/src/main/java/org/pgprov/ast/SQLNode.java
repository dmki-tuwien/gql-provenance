package org.pgprov.ast;

import org.pgprov.Globals;

import java.util.*;

public abstract class SQLNode {

    public String toString(int indent) {
        return "";
    }

    public Set<String> getOriginalReturnVars() {
        return new HashSet<>();
}

    public void updateSchemaAndSignatures(Set<String> varSchemaAndSignatures){
        //not used
    }

    public abstract Map<String, String> storeWhereProvenanceEncodings(Globals.ProvenanceType provenanceModel, Map<String, String> returnColumns, Map<String,String> renames);

    public abstract boolean updateWhereProvenanceEncodingVariable(String varName, SQLNode node);

    public abstract Set<String> storeWhyProvenanceEncodings(Globals.ProvenanceType provenanceModel);

    public abstract boolean updateWhyProvenanceEncodingVariable(String varName, SQLNode node);
}
