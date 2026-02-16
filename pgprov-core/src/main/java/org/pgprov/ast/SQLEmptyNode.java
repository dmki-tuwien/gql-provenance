package org.pgprov.ast;

import org.pgprov.Globals;

import java.util.*;

public class SQLEmptyNode extends SQLNode {

    @Override
    public String toString(int indent) {
        return "  ".repeat(indent) + "SQLEmptyNode";
    }

    @Override
    public Map<String, String> storeWhereProvenanceEncodings(Globals.ProvenanceType provenanceModel, Map<String, String> returnColumns, Map<String,String> renames){
        return new HashMap<>();
    }

    @Override
    public boolean updateWhereProvenanceEncodingVariable(String varName, SQLNode node) {
        return false;
    }

    @Override
    public Set<String> storeWhyProvenanceEncodings(Globals.ProvenanceType provenanceModel) {
        return new HashSet<>();
    }

    @Override
    public boolean updateWhyProvenanceEncodingVariable(String varName, SQLNode node) {
        return false;
    }

}
