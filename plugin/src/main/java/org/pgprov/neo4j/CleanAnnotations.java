package org.pgprov.neo4j;


import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Transaction;
import org.neo4j.logging.Log;
import org.neo4j.procedure.Context;
import org.neo4j.procedure.Description;
import org.neo4j.procedure.Mode;
import org.neo4j.procedure.Procedure;
import org.pgprov.Globals;

import java.util.Iterator;


// TODO: Update to a batch procedure
public class CleanAnnotations {

    @Context
    public Transaction tx;

    @Context
    public Log log;

    @Procedure(name = "org.pgprov.cleanAnnotations" , mode = Mode.WRITE)
    @Description("Remove all annotations from data")
    public void cleanAnnotations() {

        tx.getAllNodes().forEach(n -> {
            n.removeProperty(Globals.NODE_ANNOT_KEY);

            for(Label lbl : n.getLabels()) {
                n.removeProperty(Globals.LBL_ANNOT_KEY_PREFIX + lbl.name());
            }

            Iterator<String> propIterator = n.getPropertyKeys().iterator();

            while(propIterator.hasNext()) {

                String propName = propIterator.next();
                n.removeProperty(Globals.PROP_ANNOT_KEY_PREFIX + propName);
            }
        });

        tx.getAllRelationships().forEach(r -> {
            r.removeProperty(Globals.NODE_ANNOT_KEY);

            if(r.getType()!=null) {
                r.removeProperty(Globals.LBL_ANNOT_KEY_PREFIX + r.getType().name());
            }

            Iterator<String> propIterator = r.getPropertyKeys().iterator();

            while(propIterator.hasNext()) {

                String propName = propIterator.next();
                r.removeProperty(Globals.PROP_ANNOT_KEY_PREFIX + propName);
            }
        });
    }
}
