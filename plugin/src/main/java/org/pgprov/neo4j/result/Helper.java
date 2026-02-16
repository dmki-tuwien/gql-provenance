package org.pgprov.neo4j.result;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;
import org.pgprov.graph.model.Entity;
import org.pgprov.neo4j.transformer.Neo4jEdgeTransformer;
import org.pgprov.neo4j.transformer.Neo4jNodeTransformer;

import java.util.*;

public record Helper(){

    public static Map<String, Object> transformInputRow(Map<String, Object> row) {
        Map<String, Object> tempRow = new HashMap<>();

        Neo4jNodeTransformer nodeTransformer = new Neo4jNodeTransformer();
        Neo4jEdgeTransformer edgeTransformer = new Neo4jEdgeTransformer();

        for(String key : row.keySet()) {

            Object value = row.get(key);

            if(value instanceof Node){
                tempRow.put(key, nodeTransformer.transform((Node) value));
            }else if(value instanceof Relationship){
                tempRow.put(key, edgeTransformer.transform((Relationship) value));
            }else if(value instanceof Path){

                Path path = (Path) value;
                List<Entity> pathElements = new ArrayList<>();

                for(org.neo4j.graphdb.Entity entity: path){
                    if(entity instanceof Node){
                        pathElements.add(nodeTransformer.transform((Node) entity));
                    }else if(entity instanceof Relationship){
                        pathElements.add(edgeTransformer.transform((Relationship) entity));
                    }
                }
                tempRow.put(key, new org.pgprov.graph.model.Path(pathElements));
            }else if(value instanceof List<?> list){
                List<String> repeatedVals = new ArrayList<>();

                for(Object entity: list){
                    if(entity instanceof String){
                        repeatedVals.add((String) entity);
                    }else if(entity instanceof Long ent){
                        repeatedVals.add(Long.toString(ent));
                    }else if(entity instanceof Integer ent){
                        repeatedVals.add(Integer.toString(ent));
                    }
                }
                tempRow.put(key, repeatedVals);
            }else if(value != null){
                    tempRow.put(key, value.toString());
            }
        }

        return tempRow;
    }

    public static Map<String, Object> updateResult(Map<String, Object> row, Set<String> returnVars) {
        Iterator<String> it = row.keySet().iterator();

        while (it.hasNext()) {
            String key = it.next();
            if(!returnVars.contains(key)) {
                it.remove();
            }
        }
        return row;
    }
}
