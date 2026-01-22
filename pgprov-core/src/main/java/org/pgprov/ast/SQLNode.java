package org.pgprov.ast;

import java.util.*;

public abstract class SQLNode {

    public String toString(int indent) {
        return "";
    }

    public Set<String> getOriginalReturnVars() {
        return new HashSet<>();
    }

    // get all initially returned variables to the parent
    // 1. binding variables that are used for accessing properties and labels
    // 2. path variables
    public abstract Set<String> getReturnVarsForRewriting();

    // set the return variable list to parent's return variables
    // (only used with operations that follows Projections -
    // to set return varibales in union queries)
    public void setReturnVarsForRewriting(Set<String> returnVars) {
        // not used
    }

    // get new variables after
    // setting the return variables with parent's variables
    public Set<String> getExternalVarsForRewriting() {
        return new HashSet<>();

    }

    // update the temporary variables names in schema and signatures during rewriting to pass on through queries
    public void updateVarInSchemaAndSignatures(String varName) {
        // not used
    }

    ;

    public Set<Set<String>> calculateWhyProv(Map<String, Object> row) {
        return new HashSet<>();
    }

    public Map<String, Set<Object>> calculateWhereProv(Map<String, Object> row) {
        return new HashMap<>();
    }

    public String calculateHowProv(Map<String, Object> row) {
        return "";
    }

    public void updateSchemaAndSignatures(Map<String, List<String>> varSchemaAndSignatures){
        //not used
    }
}
