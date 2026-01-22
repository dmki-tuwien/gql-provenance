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
import org.pgprov.neo4j.result.Helper;
import org.pgprov.parser.GQLLexer;
import org.pgprov.parser.GQLParser;
import org.pgprov.processor.query.GQLQueryProcessor;
import org.pgprov.ast.SQLNode;
import org.pgprov.processor.result.Grouper;
import org.pgprov.processor.result.HowProvResultRow;

import java.util.*;
import java.util.stream.Stream;

public class GetHowProvenance {


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
    @Procedure(name = "org.pgprov.getHowProvenance")
    @Description("Get the how-provenance of a query result.")
    public Stream<GetHowProvenance.Row> getHowProvenance(@Name("query") String query, @Name("params") Map<String, Object> params) throws Exception {

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
        System.out.println("SQL AST: " + processor.getSQLAST().toString(0));
        Result result = tx.execute(updatedQuery, params);

        processor.getSQLAST().updateSchemaAndSignatures( new HashMap<>());

        Grouper<Map<String, Object>, String, InternalRow> grouper = new Grouper<>(processor.getSQLAST(), InternalRow::new);
        return grouper.process(result.stream()).map(row-> new GetHowProvenance.Row(row.getResult(), row.getProv()));
    }

    public static class Row{

        public Map<String, Object> result;
        public String prov;

        public Row(Map<String, Object> result, String prov) {
            this.result = result;
            this.prov = prov;
        }
    }

    public static class InternalRow extends HowProvResultRow<Map<String, Object>> {

        public InternalRow(Map<String, Object> row, SQLNode sqlNode) {
            super(row, sqlNode);
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
