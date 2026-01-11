package org.pgprov.neo4j;

import org.neo4j.graphdb.*;
import org.neo4j.logging.Log;
import org.neo4j.procedure.*;
import org.pgprov.Globals;

import java.util.Iterator;
import java.util.Map;

public class AnnotateData {

    @Context
    public Transaction tx;

    @Context
    public Log log;

    @Procedure(name = "org.pgprov.annotateBatchData", mode = Mode.WRITE)
    @Description("Assign unique identifiers for all nodes, edges and their labels and properties.")
    public void annotateData(@Name("skip") long skip, @Name("limit") long limit) {

        Result res = tx.execute(
                "MATCH (n) RETURN n ORDER BY id(n) SKIP $skip LIMIT $limit",
                Map.of("skip", skip, "limit", limit)
        );

        int id = 0;

        while (res.hasNext()) {
            Node node = (Node) res.next().get("n");
            String nodeAnnotation = Globals.NODE_ANNOT_PREFIX + id;

            if(!node.getAllProperties().isEmpty()){
                Iterator<String> propIterator = node.getPropertyKeys().iterator();
                while(propIterator.hasNext()) {

                    String propName = propIterator.next();

                    if(!node.hasProperty(Globals.PROP_ANNOT_KEY_PREFIX + propName)){
                        node.setProperty(Globals.PROP_ANNOT_KEY_PREFIX + propName, nodeAnnotation + Globals.PROP_ANNOT_PREFIX + propName);

                    }
                }
            }

            if(!node.hasProperty(Globals.NODE_ANNOT_KEY)){
                node.setProperty(Globals.NODE_ANNOT_KEY, nodeAnnotation);

            }

            for(Label label : node.getLabels()) {

                if(!node.hasProperty(Globals.LBL_ANNOT_KEY_PREFIX + label.name())){
                    node.setProperty(Globals.LBL_ANNOT_KEY_PREFIX + label.name(), nodeAnnotation + Globals.LBL_ANNOT_PREFIX + label.name());

                }

            }

            id++;
        }

        id = 0;
        Result relRes = tx.execute(
                "MATCH ()-[e]->() RETURN e ORDER BY id(e) SKIP $skip LIMIT $limit",
                Map.of("skip", skip, "limit", limit)
        );

        while (relRes.hasNext()) {
            Relationship rel = (Relationship) relRes.next().get("e");
            String relAnnotation = Globals.EDGE_ANNOT_PREFIX + id;

            if(!rel.getAllProperties().isEmpty()) {
                Iterator<String> propIterator = rel.getPropertyKeys().iterator();
                while (propIterator.hasNext()) {

                    String propName = propIterator.next();

                    if(!rel.hasProperty(Globals.PROP_ANNOT_KEY_PREFIX + propName)){
                        rel.setProperty(Globals.PROP_ANNOT_KEY_PREFIX + propName, relAnnotation + Globals.PROP_ANNOT_PREFIX + propName);

                    }
                }
            }

            if(!rel.hasProperty(Globals.EDGE_ANNOT_KEY)){
                rel.setProperty(Globals.EDGE_ANNOT_KEY, relAnnotation);

            }

            if(rel.getType()!=null) {

                if(!rel.hasProperty(Globals.LBL_ANNOT_KEY_PREFIX + rel.getType().name())){
                    rel.setProperty(Globals.LBL_ANNOT_KEY_PREFIX + rel.getType().name(), relAnnotation + Globals.LBL_ANNOT_PREFIX + rel.getType().name());
                }
            }

            id++;

        }


    }

}