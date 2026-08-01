package org.pgprov.processor.result;

import org.pgprov.Globals;
import org.pgprov.ast.SQLNode;
import org.pgprov.graph.model.Edge;
import org.pgprov.graph.model.Entity;
import org.pgprov.graph.model.Path;

import java.util.*;
import java.util.regex.Pattern;

public abstract class WhyProvResultRow<S> extends ResultRow<S,List<List<String>>> {


    public WhyProvResultRow(S row, SQLNode sqlNode, boolean edgeMinimality) {
        super(row, sqlNode, edgeMinimality);
    }

    private static final Pattern PATTERN =
            Pattern.compile("^\\d+:[0-9a-fA-F\\-]{36}:\\d+$");

    public static boolean isValid(String input) {
        return PATTERN.matcher(input).matches();
    }

    @Override
    public List<List<String>> calculateProvenance(Map<String, Object> row, boolean edgeMinimality) {
        Set<String> provenance = new HashSet<>();
        for(Map.Entry<String, Object> entry : row.entrySet()) {
            if((entry.getKey().startsWith(Globals.VAR_PREFIX) || entry.getKey().startsWith(Globals.PATH_PREFIX) )&&!entry.getValue().equals(Globals.EXTERNAL_VAR_VALUE)) {
                if(entry.getValue() instanceof String ) {
                    provenance.add((String)entry.getValue());
                }
                else{

                    List<String> values = (List<String>) entry.getValue();
                    values.removeIf(e -> e.equals(Globals.EXTERNAL_VAR_VALUE));

                    if(edgeMinimality) {
                        List<String> edges = new ArrayList<>();
                        for (String pathElement : values) {
                            if (pathElement.startsWith("5") && isValid(pathElement)) {
                                edges.add(pathElement);
                            }
                        }
                        this.addEdges(edges);
                    }

                    provenance.addAll(values);

                }

            }
        }
        List<List<String>> finalProvenance = new ArrayList<>();
        finalProvenance.add(provenance.stream().toList());
        return finalProvenance;
    }

    @Override
    public void mergeProvenance(ResultRow<S, List<List<String>>> otherRow, boolean edgeMinimality) {

        if (edgeMinimality) {
            List<List<String>> edgeGroup = this.getEdges();
            List<String> otherEdges = otherRow.getEdges().getFirst();

            Iterator<List<String>> iterator = edgeGroup.iterator();

            List<List<String>> toAddEdges = new ArrayList<>();
            List<List<String>> toRemoveEdges = new ArrayList<>();
            List<Object> toRemoveProv = new ArrayList<>();

            while (iterator.hasNext()) {

                List<String> listEdge = iterator.next();

                if (otherEdges.size() > listEdge.size()) {

                    boolean isSubset = true;

                    for (int i = 0; i < listEdge.size(); i++) {
                        String edge = listEdge.get(i);
                        if (!otherEdges.contains(edge)) {
                            isSubset = false;
                            break;
                        }
                    }

//                System.out.println(otherEdges);
//                System.out.println(listEdge);

                    if (!isSubset) {
                        this.getProv().addAll(otherRow.getProv());
                        toAddEdges.add(otherEdges);
                    }

                } else {

                    boolean isSubset = true;

                    for (int i = 0; i < otherEdges.size(); i++) {
                        String edge = otherEdges.get(i);
                        if (!listEdge.contains(edge)) {
                            isSubset = false;
                            break;
                        }
                    }

//                System.out.println(otherEdges);
//                System.out.println(listEdge);

                    if (isSubset && listEdge.size() != otherEdges.size()) {

                        toRemoveProv.add(this.getEdgeProvMap().get(listEdge));
                        toRemoveEdges.add(listEdge);

                        this.getProv().addAll(otherRow.getProv());
                        toAddEdges.add(otherEdges);
                    }
                }
            }

            // apply structural modifications AFTER iteration
            this.getEdges().removeAll(toRemoveEdges);
            this.getEdges().addAll(toAddEdges);

            for (Object prov : toRemoveProv) {
                this.getProv().remove(prov);
            }
        }
        else{
            this.getProv().addAll(otherRow.getProv());
        }
    }
}
