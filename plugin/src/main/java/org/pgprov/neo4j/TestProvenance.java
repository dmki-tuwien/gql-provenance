package org.pgprov.neo4j;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CodePointCharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.neo4j.graphdb.*;
import org.neo4j.logging.Log;
import org.neo4j.procedure.Context;
import org.neo4j.procedure.Description;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;
import org.pgprov.Globals;
import org.pgprov.ast.SQLNode;
import org.pgprov.graph.model.Edge;
import org.pgprov.neo4j.result.Helper;
import org.pgprov.parser.GQLLexer;
import org.pgprov.parser.GQLParser;
import org.pgprov.processor.query.GQLQueryProcessor;
import org.pgprov.processor.result.Grouper;
import org.pgprov.processor.result.WhereProvResultRow;

import java.util.*;
import java.util.stream.Stream;

public class TestProvenance {


    @Context
    public Transaction tx;

    @Context
    public Log log;

    private static final Logger logger = LogManager.getLogger("pgprov.why_prov");
    /**
     * This procedure takes a query and generates the why-provennace annotation for each result concatenates it
     *
     * @param query The query to generate the provenance polynomial for
     * @return Each row in the execution result with its set of why-provenance annotations
     */
    @Procedure(name = "org.pgprov.testProvenance")
    @Description("Given a query test timings.")
    public Stream<Row> testProvenance(@Name("query") String query, @Name("params") Map<String, Object> params) throws Exception {

        if(params.get("log").toString().equals("true") && (params.get("query").toString().startsWith("prov_result")|| params.get("query").toString().startsWith("prov_coarse_result"))) {
            long start = System.nanoTime();

            Result result = tx.execute(query, params);
            long end = System.nanoTime();
            double durationMs = (end - start) / 1000000.0;

            int resultSize =0;
            while (result.hasNext()) {
                Map<String, Object> resultRow = result.next();

                List<List<String>> provenance = (List<List<String>>) resultRow.get("prov");

                if (provenance != null && !provenance.isEmpty()) {
                    resultRow.put("witnesses", provenance.size());

                    if (params.get("query").toString().startsWith("prov_coarse_")) {

                        List<List<String>> newProv = new ArrayList<>();

                        for (List<String> witness : provenance) {

                            List<String> newWitness = new ArrayList<>();

                            for (String element : witness) {

                                if (element.startsWith("4:")) {

                                    Node node = tx.getNodeByElementId(element);

                                    newWitness.add(element + "."
                                            + ((Collection<?>) node.getLabels()).size()
                                            + "."
                                            + node.getAllProperties().size());

                                } else if (element.startsWith("5:")) {

                                    Relationship edge = tx.getRelationshipByElementId(element);

                                    newWitness.add(element + ".1."
                                            + edge.getAllProperties().size());
                                }
                            }

                            newProv.add(newWitness);
                        }

                        resultRow.put("prov", newProv);
                    }
                }

                // 🔥 process + log immediately
                if (params.get("log").toString().equals("true")) {
                    logger.info(params.get("dataset") + " | " +
                            params.get("scaleFactor") + " | " +
                            params.get("query") + " | " +
                            params.get("parameter") + " | " +
                            "witness | " +
                            end + " | " +
                            resultRow
                    );
                }

                // row is now discarded after this iteration
                resultSize++;
            }

            return Stream.of(new Row(durationMs, resultSize));

        }else if(params.get("log").toString().equals("false")  || params.get("query").toString().startsWith("orig_") || params.get("query").toString().startsWith("prov_")|| params.get("query").toString().startsWith("prov_coarse_")) {
            long start = System.nanoTime();
            if (params.get("log").toString().equals("true")) {
                logger.info(params.get("dataset") + " | " +
                        params.get("scaleFactor") + " | " +
                        params.get("query") + " | " +
                        params.get("parameter") + " | " +
                        "start | " +
                        start + " | " +
                        0
                );
            }

            List<Map<String, Object>> result = tx.execute(query, params).stream().toList();
            long end = System.nanoTime();
            double durationMs = (end - start) / 1000000.0;

            int resultSize = result.size();

            // 🔥 process + log immediately
            if (params.get("log").toString().equals("true")) {
                logger.info(params.get("dataset") + " | " +
                        params.get("scaleFactor") + " | " +
                        params.get("query") + " | " +
                        params.get("parameter") + " | " +
                        "end | " +
                        end + " | " +
                        resultSize
                );
            }

            return Stream.of(new Row(durationMs, resultSize));

        } else{

            CodePointCharStream charStream = CharStreams.fromString(query);
            GQLLexer lexer = new GQLLexer(charStream);
            CommonTokenStream tokenStream = new CommonTokenStream(lexer);
            GQLParser parser = new GQLParser(tokenStream);
            ParseTree tree = parser.statementBlock();

            GQLQueryProcessor processor;
            if(params.get("query").toString().startsWith("rewritten_coarse_")){
                processor = new GQLQueryProcessor(tokenStream, Globals.ProcessStage.SQL_TRANSLATION, Globals.ProvenanceLevel.COARSE);
            }else {
                processor = new GQLQueryProcessor(tokenStream, Globals.ProcessStage.SQL_TRANSLATION, Globals.ProvenanceLevel.FINE_GRAINED);
            }
            System.out.println(tree.toStringTree(parser));
            ParseTreeWalker.DEFAULT.walk(processor, tree);

            //processor.getSQLAST().updateSchemaAndSignatures(new HashSet<>());
            processor.getSQLAST().storeWhyProvenanceEncodings(Globals.ProvenanceType.WHY_PROV);
            processor.setProcessStage(Globals.ProcessStage.REWRITE_WHY_PROVENANCE);
            ParseTreeWalker.DEFAULT.walk(processor, tree);

            String rewrittenQuery = processor.getRewrittenQuery();
            rewrittenQuery = rewrittenQuery.replace("CAST(", "date(")
                    .replace("AS DATE)", ")");

            long start = System.nanoTime();
            if (params.get("log").toString().equals("true")) {
                logger.info(params.get("dataset") + " | " +
                        params.get("scaleFactor") + " | " +
                        params.get("query") + " | " +
                        params.get("parameter") + " | " +
                        "start | " +
                        start + " | " +
                        0
                );
            }

            List<Map<String, Object>> result = tx.execute(rewrittenQuery, params).stream().toList();
            long end = System.nanoTime();
            double durationMs = (end - start) / 1000000.0;

            if (params.get("log").toString().equals("true")) {
                logger.info(params.get("dataset") + " | " +
                        params.get("scaleFactor") + " | " +
                        params.get("query") + " | " +
                        params.get("parameter") + " | " +
                        "end | " +
                        end + " | " +
                        result.size()
                );
            }

            return Stream.of(new Row(durationMs, result.size()));
        }
    }

    public static class Row{

        public Double durationMs;
        public Number size;

        public Row(double durationMs, int size) {
            this.durationMs = durationMs;
            this.size = size;
        }
    }

}
