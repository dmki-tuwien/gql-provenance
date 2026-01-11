package org.pgprov.neo4j.transformer;

import org.neo4j.graphdb.Relationship;
import org.pgprov.graph.model.Edge;
import org.pgprov.graph.transformer.EdgeTransformer;


public class Neo4jEdgeTransformer implements EdgeTransformer<Relationship, Edge> {

    @Override
    public Edge transform(org.neo4j.graphdb.Relationship source) {
        return new Edge(source.getElementId(), source.getType().name(), source.getAllProperties());
    }
}
