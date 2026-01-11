package org.pgprov.processors;

import org.antlr.v4.runtime.tree.ParseTreeListener;

public interface QueryProcessor extends ParseTreeListener {

    String getRewrittenQuery();
}
