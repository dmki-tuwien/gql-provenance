package org.pgprov.graph.transformer;

public interface EdgeTransformer<S, Edge> {
    Edge transform(S source);
}