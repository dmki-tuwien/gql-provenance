package org.pgprov.graph.transformer;

public interface NodeTransformer<S, Node> {
    Node transform(S source);
}
