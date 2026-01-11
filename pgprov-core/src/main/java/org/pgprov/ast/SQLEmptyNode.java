package org.pgprov.ast;

import java.util.HashSet;
import java.util.Set;

public class SQLEmptyNode extends SQLNode {

    @Override
    public String toString(int indent) {
        return "  ".repeat(indent) + "SQLEmptyNode";
    }

    @Override
    public Set<String> getReturnVarsForRewriting() {
        return new HashSet<>();
    }

}
