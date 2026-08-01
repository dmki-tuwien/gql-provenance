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
import org.pgprov.neo4j.result.Helper;
import org.pgprov.parser.GQLLexer;
import org.pgprov.parser.GQLParser;
import org.pgprov.processor.query.GQLQueryProcessor;
import org.pgprov.processor.result.Grouper;
import org.pgprov.processor.result.WhyProvResultRow;

import java.util.*;
import java.util.stream.Stream;

public class GetWhyProvenance {


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
    @Procedure(name = "org.pgprov.getWhyProvenance")
    @Description("Get the why-provenance of a query result.")
    public Stream<Row> getWhyProvenance(@Name("query") String query, @Name("params") Map<String, Object> params , @Name(value="provLevel", defaultValue = "FINE_GRAINED") String provLevel) throws Exception {
        CodePointCharStream charStream = CharStreams.fromString(query);
        GQLLexer lexer = new GQLLexer(charStream);
        CommonTokenStream tokenStream = new CommonTokenStream(lexer);

        GQLParser parser = new GQLParser(tokenStream);

        Globals.ProvenanceLevel level = Globals.ProvenanceLevel.valueOf(provLevel);

        GQLQueryProcessor processor = new GQLQueryProcessor(tokenStream, Globals.ProcessStage.SQL_TRANSLATION, level);
        ParseTree tree = parser.statementBlock();

        ParseTreeWalker.DEFAULT.walk(processor, tree);

       //  t2
        if(params.get("log").toString().equals("true")) {
            logger.info(params.get("dataset") + " | " +
                    params.get("scaleFactor") + " | " +
                    params.get("query") + " | " +
                    params.get("parameter") + " | " +
                    "translated | " +
                    System.nanoTime() + " | " +
                    0
            );
        }

//        processor.getSQLAST().updateSchemaAndSignatures(new HashSet<>());
        processor.getSQLAST().storeWhyProvenanceEncodings(Globals.ProvenanceType.WHY_PROV);
        processor.setProcessStage(Globals.ProcessStage.REWRITE_WHY_PROVENANCE);

        ParseTreeWalker.DEFAULT.walk(processor, tree);

        String updatedQuery = processor.getRewrittenQuery();
        updatedQuery = updatedQuery                 // replacing GQL syntax with Cypher Syntax
                .replace("CAST(", "date(")
                .replace("AS DATE)", ")");

        System.out.println("Updated query: " + updatedQuery);
        System.out.println("SQL AST: " + processor.getSQLAST().toString(0));

        // t3
        if(params.get("log").toString().equals("true")) {
            logger.info(params.get("dataset") + " | " +
                    params.get("scaleFactor") + " | " +
                    params.get("query") + " | " +
                    params.get("parameter") + " | " +
                    "rewritten | " +
                    System.nanoTime() + " | " +
                    0
            );
        }

        Result result = tx.execute(updatedQuery, params);

        Grouper<Map<String, Object>,List<List<String>>, InternalRow> grouper = new Grouper<>(processor.getSQLAST(), InternalRow::new, params.get("edgeMinimality").toString().equals("true"));
        return grouper.process(result.stream()).map(row-> new Row(row.getResult(), row.getProv()));
    }

    public static class Row{

        public Map<String, Object> result;
        public List<List<String>> prov;

        public Row(Map<String, Object> result, List<List<String>> prov) {
            this.result = result;
            this.prov = prov;
        }
    }

    public static class InternalRow extends WhyProvResultRow<Map<String, Object>> {

        public InternalRow(Map<String, Object> rowContext, SQLNode sqlNode) {
            super((Map<String, Object>) rowContext.get("row"), sqlNode, (boolean)rowContext.get("edgeMinimality"));
        }

        @Override
        public Map<String, Object> transformInputRow(Map<String, Object> row) {
            return Helper.transformInputRow(row);
        }

        @Override
        public Map<String, Object> updateResult(Map<String, Object> row, Set<String> returnVars) {
            return Helper.updateResult(row, returnVars);
        }
    }
}
