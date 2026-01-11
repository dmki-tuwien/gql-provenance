package org.pgprov.neo4j.transformer;

import org.neo4j.graphdb.Label;
import org.pgprov.graph.model.Node;
import org.pgprov.graph.transformer.NodeTransformer;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class Neo4jNodeTransformer implements NodeTransformer<org.neo4j.graphdb.Node, Node> {

    @Override
    public Node transform(org.neo4j.graphdb.Node source) {
        Set<String> labels = StreamSupport
                .stream(source.getLabels().spliterator(), false)
                .map(Label::name)
                .collect(Collectors.toSet());


        return new Node(source.getElementId(), labels, source.getAllProperties());
    }
}
