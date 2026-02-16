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
import org.pgprov.ast.SQLNode;
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

    /**
     * This procedure takes a query and generates the why-provennace annotation for each result concatenates it
     *
     * @param query The query to generate the provenance polynomial for
     * @return Each row in the execution result with its set of why-provenance annotations
     */
    @Procedure(name = "org.pgprov.testProvenance")
    @Description("Given a query test timings.")
    public Stream<Row> testProvenance(@Name("query") String query, @Name("params") Map<String, Object> params) throws Exception {



        long start = System.nanoTime();
        List<Map<String,Object>> result = tx.execute(query, params).stream().toList();
        long end = System.nanoTime();
        double durationMs = (end - start) / 1000000.0;

        return Stream.of(new Row(durationMs, result.size()));
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
