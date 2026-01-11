package org.pgprov.neo4j;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CodePointCharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.neo4j.graphdb.*;
import org.neo4j.logging.Log;
import org.neo4j.procedure.Context;
import org.neo4j.procedure.Description;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;
import org.pgprov.Globals;
import org.pgprov.parser.GQLLexer;
import org.pgprov.parser.GQLParser;
import org.pgprov.neo4j.transformer.Neo4jEdgeTransformer;
import org.pgprov.neo4j.transformer.Neo4jNodeTransformer;
import org.pgprov.processors.GQLQueryProcessor;
import org.pgprov.ast.SQLNode;

import java.util.*;
import java.util.stream.Stream;

public class GetWhereProvenance {


    @Context
    public Transaction tx;

    @Context
    public Log log;

    /**
     * This procedure takes a query and generates the why-provennace annotation for each result concatenates it
     *
     * @param query The query to generate the provenance polynomial for
     * @return Each row in the execution result with its set of why-provenance annotations
     */
    @Procedure(name = "org.pgprov.getWhereProvenance")
    @Description("Get the where-provenance of a query result.")
    public Stream<GetWhereProvenance.ResultRow> getWhereProvenance(@Name("query") String query, @Name("params") Map<String, Object> params) throws Exception {

        CodePointCharStream charStream = CharStreams.fromString(query);
        GQLLexer lexer = new GQLLexer(charStream);
        CommonTokenStream tokenStream = new CommonTokenStream(lexer);

        GQLParser parser = new GQLParser(tokenStream);
        GQLQueryProcessor processor = new GQLQueryProcessor(tokenStream, Globals.ProcessStage.SQL_TRANSLATION);
        ParseTree tree = parser.statementBlock();

        ParseTreeWalker.DEFAULT.walk(processor, tree);

        processor.setProcessStage(Globals.ProcessStage.REWRITE);
        ParseTreeWalker.DEFAULT.walk(processor, tree);

        String updatedQuery = processor.getRewrittenQuery();
        System.out.println("Updated query: " + updatedQuery);
        System.out.println("SQL AST: " + processor.getSQLAST());
        Result result = tx.execute(updatedQuery, params);
        Stream<GetWhereProvenance.ResultRow> processedResults = result.stream().map(row -> new GetWhereProvenance.ResultRow(row, processor.getSQLAST()));

        Map<Integer, GetWhereProvenance.ResultRow> grouped = new LinkedHashMap<>();

        processedResults.forEach(row -> {
            int key = row.hashCode();
            grouped.merge(
                    key,
                    row,
                    (existing, incoming) -> {

                        for(String provKey: incoming.prov.keySet()){
                            existing.prov
                                    .computeIfAbsent(provKey, k-> new HashSet<>())
                                    .addAll(incoming.prov.get(provKey));
                        }
                        return existing;
                    }
            );
        });

        return grouped.values().stream();
    }

    public static class ResultRow {

        public Map<String, Object> result;

        public Map<String, Set<Object>> prov;

        public ResultRow(Map<String, Object> row, SQLNode sqlNode) {

            Neo4jNodeTransformer nodeTransformer = new Neo4jNodeTransformer();
            Neo4jEdgeTransformer edgeTransformer = new Neo4jEdgeTransformer();

            Map<String, Object> tempRow = new HashMap<>();

            for(String key : row.keySet()) {

                Object value = row.get(key);

                if(value instanceof Node){
                    tempRow.put(key, nodeTransformer.transform((Node) value));
                }else if(value instanceof Relationship){
                    tempRow.put(key, edgeTransformer.transform((Relationship) value));
                }else if(value instanceof Path){

                    Path path = (Path) value;
                    List<org.pgprov.graph.model.Entity> pathElements = new ArrayList<>();

                    for(Entity entity: path){
                        if(entity instanceof Node){
                            pathElements.add(nodeTransformer.transform((Node) entity));
                        }else if(entity instanceof Relationship){
                            pathElements.add(edgeTransformer.transform((Relationship) entity));
                        }
                    }
                    tempRow.put(key, new org.pgprov.graph.model.Path(pathElements));
                }else if(value instanceof List<?> list){
                    List<org.pgprov.graph.model.Entity> repeatedVals = new ArrayList<>();

                    for(Object entity: list){
                        if(entity instanceof Node){
                            repeatedVals.add(nodeTransformer.transform((Node) entity));
                        }else if(entity instanceof Relationship){
                            repeatedVals.add(edgeTransformer.transform((Relationship) entity));
                        }
                    }
                    tempRow.put(key, repeatedVals);
                }
            }

            this.prov = sqlNode.calculateWhereProv(tempRow, new HashMap<>());


            Set<String> returnVars =  sqlNode.getOriginalReturnVars();
            Iterator<String> it = row.keySet().iterator();

            while (it.hasNext()) {
                String key = it.next();
                if(!returnVars.contains(key)) {
                    it.remove();
                }
            }

            this.result = row;
        }

        @Override
        public int hashCode() {
            return Objects.hash(result);
        }
    }

}
