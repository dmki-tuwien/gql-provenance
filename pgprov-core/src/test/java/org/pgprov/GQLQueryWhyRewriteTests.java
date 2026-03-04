package org.pgprov;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CodePointCharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.junit.jupiter.api.Test;
import org.pgprov.parser.GQLLexer;
import org.pgprov.parser.GQLParser;
import org.pgprov.processor.query.GQLQueryProcessor;

import java.util.HashSet;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;


public class GQLQueryWhyRewriteTests {

    private final Globals.ProvenanceType model = Globals.ProvenanceType.WHY_PROV;
    private GQLQueryProcessor getProcessorAtTranslationStage(GQLParser parser, CommonTokenStream tokenStream, ParseTree tree) {

        GQLQueryProcessor processor = new GQLQueryProcessor(tokenStream, Globals.ProcessStage.SQL_TRANSLATION);
        System.out.println(tree.toStringTree(parser));
        ParseTreeWalker.DEFAULT.walk(processor, tree);
        return processor;
    }

    @Test
    public void testSimpleQuery() {
        final String query = "MATCH (n:Person)-[]->(x) RETURN n, n.name";

        CodePointCharStream charStream = CharStreams.fromString(query);
        GQLLexer lexer = new GQLLexer(charStream);
        CommonTokenStream tokenStream = new CommonTokenStream(lexer);
        GQLParser parser = new GQLParser(tokenStream);
        ParseTree tree = parser.statementBlock();
        GQLQueryProcessor processor = getProcessorAtTranslationStage(parser, tokenStream, tree);

//        //processor.getSQLAST().updateSchemaAndSignatures(new HashSet<>());
        processor.getSQLAST().storeWhyProvenanceEncodings(model);
        processor.setProcessStage(Globals.ProcessStage.REWRITE_WHY_PROVENANCE);
        ParseTreeWalker.DEFAULT.walk(processor, tree);
        assertThat(processor.getRewrittenQuery())
                .isEqualTo("MATCH provpath_0 = (n:Person)-[]->(x) RETURN n, n.name, " +
                        "CASE WHEN n.name IS NULL THEN \"provvar\" ELSE elementId(n)+\".name\" END AS prov_n__k_name, "+
                        "[x IN nodes(provpath_0) | elementId(x)] + [r IN relationships(provpath_0) | elementId(r)] AS provpath_0, "+
                        "CASE WHEN \"Person\" IN labels(n) THEN elementId(n)+\":Person\" ELSE \"provvar\" END AS prov_n__l_Person"
                );
    }

    @Test
    public void testSimpleQueryWithOrderBy() {
        final String query = "MATCH (n:Person)-[s]->(x) RETURN n, n.name AS name ORDER BY name";

        CodePointCharStream charStream = CharStreams.fromString(query);
        GQLLexer lexer = new GQLLexer(charStream);
        CommonTokenStream tokenStream = new CommonTokenStream(lexer);
        GQLParser parser = new GQLParser(tokenStream);
        ParseTree tree = parser.statementBlock();
        GQLQueryProcessor processor = getProcessorAtTranslationStage(parser, tokenStream, tree);

        //processor.getSQLAST().updateSchemaAndSignatures(new HashSet<>());
        processor.getSQLAST().storeWhyProvenanceEncodings(model);
        processor.setProcessStage(Globals.ProcessStage.REWRITE_WHY_PROVENANCE);
        ParseTreeWalker.DEFAULT.walk(processor, tree);

        assertThat(processor.getRewrittenQuery())
                .isEqualTo("MATCH provpath_0 = (n:Person)-[s]->(x) " +
                        "RETURN n, n.name AS name, " +
                        "CASE WHEN n.name IS NULL THEN \"provvar\" ELSE elementId(n)+\".name\" END AS prov_n__k_name, "+
                        "[x IN nodes(provpath_0) | elementId(x)] + [r IN relationships(provpath_0) | elementId(r)] AS provpath_0, "+
                        "CASE WHEN \"Person\" IN labels(n) THEN elementId(n)+\":Person\" ELSE \"provvar\" END AS prov_n__l_Person "+
                        "ORDER BY name");
    }

    @Test
    public void testSimpleQueryWithMissingVariable() {
        final String query = "MATCH (n:Person)-[:LANE]->() RETURN n, n.name";

        CodePointCharStream charStream = CharStreams.fromString(query);
        GQLLexer lexer = new GQLLexer(charStream);
        CommonTokenStream tokenStream = new CommonTokenStream(lexer);
        GQLParser parser = new GQLParser(tokenStream);
        ParseTree tree = parser.statementBlock();
        GQLQueryProcessor processor = getProcessorAtTranslationStage(parser, tokenStream, tree);

        //processor.getSQLAST().updateSchemaAndSignatures(new HashSet<>());
        processor.getSQLAST().storeWhyProvenanceEncodings(model);
        processor.setProcessStage(Globals.ProcessStage.REWRITE_WHY_PROVENANCE);
        ParseTreeWalker.DEFAULT.walk(processor, tree);

        assertThat(processor.getRewrittenQuery())
                .isEqualTo("MATCH provpath_0 = (n:Person)-[x0:LANE]->() " +
                        "RETURN n, n.name, " +
                                "CASE WHEN \"LANE\"=type(x0) THEN elementId(x0)+\":LANE\" ELSE \"provvar\" END AS prov_x0__l_LANE, "+
                                "CASE WHEN n.name IS NULL THEN \"provvar\" ELSE elementId(n)+\".name\" END AS prov_n__k_name, "+
                                "[x IN nodes(provpath_0) | elementId(x)] + [r IN relationships(provpath_0) | elementId(r)] AS provpath_0, "+
                                "CASE WHEN \"Person\" IN labels(n) THEN elementId(n)+\":Person\" ELSE \"provvar\" END AS prov_n__l_Person"
                        );
    }
//
//    // TO be updated with complex things
    @Test
    public void testSimpleQueryWithRenaming() {
        final String query = "MATCH (n:Person) RETURN n, n.name AS name";
        CodePointCharStream charStream = CharStreams.fromString(query);
        GQLLexer lexer = new GQLLexer(charStream);
        CommonTokenStream tokenStream = new CommonTokenStream(lexer);
        GQLParser parser = new GQLParser(tokenStream);
        ParseTree tree = parser.statementBlock();
        GQLQueryProcessor processor = getProcessorAtTranslationStage(parser, tokenStream, tree);

        //processor.getSQLAST().updateSchemaAndSignatures(new HashSet<>());
        processor.getSQLAST().storeWhyProvenanceEncodings(model);
        processor.setProcessStage(Globals.ProcessStage.REWRITE_WHY_PROVENANCE);
        ParseTreeWalker.DEFAULT.walk(processor, tree);

        assertThat(processor.getRewrittenQuery())
                .isEqualTo("MATCH provpath_0 = (n:Person) " +
                        "RETURN n, n.name AS name, " +
                                "CASE WHEN n.name IS NULL THEN \"provvar\" ELSE elementId(n)+\".name\" END AS prov_n__k_name, "+
                                "[x IN nodes(provpath_0) | elementId(x)] + [r IN relationships(provpath_0) | elementId(r)] AS provpath_0, "+
                        "CASE WHEN \"Person\" IN labels(n) THEN elementId(n)+\":Person\" ELSE \"provvar\" END AS prov_n__l_Person"
                );
    }

    @Test
    public void testQueryWithPathPatternJoin() {
        final String query = "MATCH (n:Person), (x)->() RETURN n, n.name";
        CodePointCharStream charStream = CharStreams.fromString(query);
        GQLLexer lexer = new GQLLexer(charStream);
        CommonTokenStream tokenStream = new CommonTokenStream(lexer);
        GQLParser parser = new GQLParser(tokenStream);
        ParseTree tree = parser.statementBlock();
        GQLQueryProcessor processor = getProcessorAtTranslationStage(parser, tokenStream, tree);

        //processor.getSQLAST().updateSchemaAndSignatures(new HashSet<>());
        processor.getSQLAST().storeWhyProvenanceEncodings(model);
        processor.setProcessStage(Globals.ProcessStage.REWRITE_WHY_PROVENANCE);
        ParseTreeWalker.DEFAULT.walk(processor, tree);

        assertThat(processor.getRewrittenQuery())
                .isEqualTo("MATCH provpath_0 = (n:Person), provpath_1 = (x)->() " +
                        "RETURN n, n.name, " +
                                "[x IN nodes(provpath_1) | elementId(x)] + [r IN relationships(provpath_1) | elementId(r)] AS provpath_1, "+
                        "CASE WHEN n.name IS NULL THEN \"provvar\" ELSE elementId(n)+\".name\" END AS prov_n__k_name, "+
                        "[x IN nodes(provpath_0) | elementId(x)] + [r IN relationships(provpath_0) | elementId(r)] AS provpath_0, "+
                        "CASE WHEN \"Person\" IN labels(n) THEN elementId(n)+\":Person\" ELSE \"provvar\" END AS prov_n__l_Person"
                );

    }

    @Test
    public void testMultipleClauses() {
        final String query = "MATCH (n:Person) MATCH (n)-[x]->(m) FILTER m.age <> 25 RETURN n, n.name";
        CodePointCharStream charStream = CharStreams.fromString(query);
        GQLLexer lexer = new GQLLexer(charStream);
        CommonTokenStream tokenStream = new CommonTokenStream(lexer);
        GQLParser parser = new GQLParser(tokenStream);
        ParseTree tree = parser.statementBlock();
        GQLQueryProcessor processor = getProcessorAtTranslationStage(parser, tokenStream, tree);

        //processor.getSQLAST().updateSchemaAndSignatures(new HashSet<>());
        processor.getSQLAST().storeWhyProvenanceEncodings(model);
        processor.setProcessStage(Globals.ProcessStage.REWRITE_WHY_PROVENANCE);
        ParseTreeWalker.DEFAULT.walk(processor, tree);

        assertThat(processor.getRewrittenQuery())
                .isEqualTo("MATCH provpath_0 = (n:Person) " +
                        "MATCH provpath_1 = (n)-[x]->(m) " +
                        "FILTER m.age <> 25 " +
                        "RETURN n, n.name, " +
                        "CASE WHEN m.age IS NULL THEN \"provvar\" ELSE elementId(m)+\".age\" END AS prov_m__k_age, "+
                        "[x IN nodes(provpath_1) | elementId(x)] + [r IN relationships(provpath_1) | elementId(r)] AS provpath_1, "+
                                "CASE WHEN n.name IS NULL THEN \"provvar\" ELSE elementId(n)+\".name\" END AS prov_n__k_name, "+
                                "[x IN nodes(provpath_0) | elementId(x)] + [r IN relationships(provpath_0) | elementId(r)] AS provpath_0, "+
                        "CASE WHEN \"Person\" IN labels(n) THEN elementId(n)+\":Person\" ELSE \"provvar\" END AS prov_n__l_Person"

);

    }

    @Test
    public void testSetOperators() {
        final String query = "MATCH (n:Person) ->() RETURN n, n.name UNION MATCH (n) RETURN n, n.name";
        CodePointCharStream charStream = CharStreams.fromString(query);
        GQLLexer lexer = new GQLLexer(charStream);
        CommonTokenStream tokenStream = new CommonTokenStream(lexer);
        GQLParser parser = new GQLParser(tokenStream);
        ParseTree tree = parser.statementBlock();
        GQLQueryProcessor processor = getProcessorAtTranslationStage(parser, tokenStream, tree);

        //processor.getSQLAST().updateSchemaAndSignatures(new HashSet<>());
        processor.getSQLAST().storeWhyProvenanceEncodings(model);
        processor.setProcessStage(Globals.ProcessStage.REWRITE_WHY_PROVENANCE);
        ParseTreeWalker.DEFAULT.walk(processor, tree);

        assertThat(processor.getRewrittenQuery())
                .isEqualTo("MATCH provpath_0 = (n:Person) ->() " +
                        "RETURN n, n.name, " +
                        "CASE WHEN n.name IS NULL THEN \"provvar\" ELSE elementId(n)+\".name\" END AS prov_n__k_name, "+
                        "[x IN nodes(provpath_0) | elementId(x)] + [r IN relationships(provpath_0) | elementId(r)] AS provpath_0, "+
                                "CASE WHEN \"Person\" IN labels(n) THEN elementId(n)+\":Person\" ELSE \"provvar\" END AS prov_n__l_Person, "+
                        "\"provvar\" AS provpath_1 "+
                        "UNION " +
                        "MATCH provpath_1 = (n) " +
                        "RETURN n, n.name, "+
                                "[x IN nodes(provpath_1) | elementId(x)] + [r IN relationships(provpath_1) | elementId(r)] AS provpath_1, "+
                                "CASE WHEN n.name IS NULL THEN \"provvar\" ELSE elementId(n)+\".name\" END AS prov_n__k_name, "+
                                "\"provvar\" AS provpath_0, "+
                        "\"provvar\" AS prov_n__l_Person"

                        );

    }

    @Test
    public void testMultipleSetOperators() {
        final String query = "MATCH (n:Person) ->() RETURN n, n.name AS name UNION MATCH (n) RETURN n, n.name AS name UNION MATCH (n) RETURN n, n.age AS name";
        CodePointCharStream charStream = CharStreams.fromString(query);
        GQLLexer lexer = new GQLLexer(charStream);
        CommonTokenStream tokenStream = new CommonTokenStream(lexer);
        GQLParser parser = new GQLParser(tokenStream);
        ParseTree tree = parser.statementBlock();
        GQLQueryProcessor processor = getProcessorAtTranslationStage(parser, tokenStream, tree);

        //processor.getSQLAST().updateSchemaAndSignatures(new HashSet<>());
        processor.getSQLAST().storeWhyProvenanceEncodings(model);
        processor.setProcessStage(Globals.ProcessStage.REWRITE_WHY_PROVENANCE);
        ParseTreeWalker.DEFAULT.walk(processor, tree);

        assertThat(processor.getRewrittenQuery())
                .isEqualTo("MATCH provpath_0 = (n:Person) ->() " +
                        "RETURN n, n.name AS name, " +
                        "CASE WHEN n.name IS NULL THEN \"provvar\" ELSE elementId(n)+\".name\" END AS prov_n__k_name, "+
                        "[x IN nodes(provpath_0) | elementId(x)] + [r IN relationships(provpath_0) | elementId(r)] AS provpath_0, "+
                        "CASE WHEN \"Person\" IN labels(n) THEN elementId(n)+\":Person\" ELSE \"provvar\" END AS prov_n__l_Person, "+
                        "\"provvar\" AS prov_n__k_age, " +
                        "\"provvar\" AS provpath_1, "+
                        "\"provvar\" AS provpath_2 " +
                        "UNION " +
                        "MATCH provpath_1 = (n) " +
                        "RETURN n, n.name AS name, " +
                        "[x IN nodes(provpath_1) | elementId(x)] + [r IN relationships(provpath_1) | elementId(r)] AS provpath_1, "+
                        "CASE WHEN n.name IS NULL THEN \"provvar\" ELSE elementId(n)+\".name\" END AS prov_n__k_name, "+
                        "\"provvar\" AS prov_n__k_age, " +
                        "\"provvar\" AS provpath_2, " +
                        "\"provvar\" AS provpath_0, "+
                        "\"provvar\" AS prov_n__l_Person "+
                        "UNION " +
                        "MATCH provpath_2 = (n) " +
                        "RETURN n, n.age AS name, " +
                        "CASE WHEN n.age IS NULL THEN \"provvar\" ELSE elementId(n)+\".age\" END AS prov_n__k_age, "+
                        "[x IN nodes(provpath_2) | elementId(x)] + [r IN relationships(provpath_2) | elementId(r)] AS provpath_2, "+
                        "\"provvar\" AS provpath_1, "+
                                "\"provvar\" AS prov_n__k_name, "+
                        "\"provvar\" AS provpath_0, "+
                        "\"provvar\" AS prov_n__l_Person");

    }

    @Test
    public void testSingleSubQuery() {
        final String query = "CALL {MATCH (n:Person) FILTER n.age = 25 RETURN n AS node, n.name AS name} MATCH (m) ->() RETURN m, name";
        CodePointCharStream charStream = CharStreams.fromString(query);
        GQLLexer lexer = new GQLLexer(charStream);
        CommonTokenStream tokenStream = new CommonTokenStream(lexer);
        GQLParser parser = new GQLParser(tokenStream);
        ParseTree tree = parser.statementBlock();
        GQLQueryProcessor processor = getProcessorAtTranslationStage(parser, tokenStream, tree);

        //processor.getSQLAST().updateSchemaAndSignatures(new HashSet<>());
        processor.getSQLAST().storeWhyProvenanceEncodings(model);
        processor.setProcessStage(Globals.ProcessStage.REWRITE_WHY_PROVENANCE);
        ParseTreeWalker.DEFAULT.walk(processor, tree);

        assertThat(processor.getRewrittenQuery())
                .isEqualTo("CALL {MATCH provpath_0 = (n:Person) " +
                        "FILTER n.age = 25 " +
                        "RETURN n AS node, n.name AS name, " +
                        "CASE WHEN n.age IS NULL THEN \"provvar\" ELSE elementId(n)+\".age\" END AS prov_n__k_age, "+
                                "CASE WHEN n.name IS NULL THEN \"provvar\" ELSE elementId(n)+\".name\" END AS prov_n__k_name, "+
                        "[x IN nodes(provpath_0) | elementId(x)] + [r IN relationships(provpath_0) | elementId(r)] AS provpath_0, "+
                        "CASE WHEN \"Person\" IN labels(n) THEN elementId(n)+\":Person\" ELSE \"provvar\" END AS prov_n__l_Person"+
                        "} " +
                        "MATCH provpath_1 = (m) ->() " +
                        "RETURN m, name, " +
                        "prov_n__l_Person, "+
                        "prov_n__k_age, " +
                                "provpath_0, "+
                                "[x IN nodes(provpath_1) | elementId(x)] + [r IN relationships(provpath_1) | elementId(r)] AS provpath_1, "+
                        "prov_n__k_name");

    }

    @Test
    public void testMultipleSubQuery() {
        final String query = "CALL {MATCH (n) RETURN n } MATCH (n) CALL {MATCH (n:Person) FILTER n.age = 25 RETURN n.age } MATCH (n) ->(m) RETURN n, m.name";
        CodePointCharStream charStream = CharStreams.fromString(query);
        GQLLexer lexer = new GQLLexer(charStream);
        CommonTokenStream tokenStream = new CommonTokenStream(lexer);
        GQLParser parser = new GQLParser(tokenStream);
        ParseTree tree = parser.statementBlock();
        GQLQueryProcessor processor = getProcessorAtTranslationStage(parser, tokenStream, tree);

        //processor.getSQLAST().updateSchemaAndSignatures(new HashSet<>());
        processor.getSQLAST().storeWhyProvenanceEncodings(model);
        processor.setProcessStage(Globals.ProcessStage.REWRITE_WHY_PROVENANCE);
        ParseTreeWalker.DEFAULT.walk(processor, tree);

        assertThat(processor.getRewrittenQuery())
                .isEqualTo("CALL {MATCH provpath_0 = (n) RETURN n, " +
                        "[x IN nodes(provpath_0) | elementId(x)] + [r IN relationships(provpath_0) | elementId(r)] AS provpath_0 "+
                        "} " +
                        "MATCH provpath_1 = (n) " +
                        "CALL {MATCH provpath_2 = (n:Person) FILTER n.age = 25 " +
                        "RETURN n.age, " +
                        "CASE WHEN n.age IS NULL THEN \"provvar\" ELSE elementId(n)+\".age\" END AS prov_n__k_age, "+
                        "[x IN nodes(provpath_2) | elementId(x)] + [r IN relationships(provpath_2) | elementId(r)] AS provpath_2, "+
                        "CASE WHEN \"Person\" IN labels(n) THEN elementId(n)+\":Person\" ELSE \"provvar\" END AS prov_n__l_Person "+
                        "} " +
                        "MATCH provpath_3 = (n) ->(m) " +
                        "RETURN n, m.name, " +
                        "prov_n__l_Person, "+
                        "prov_n__k_age, " +
                                "provpath_0, "+
                        "[x IN nodes(provpath_1) | elementId(x)] + [r IN relationships(provpath_1) | elementId(r)] AS provpath_1, "+
                        "provpath_2, " +
                        "CASE WHEN m.name IS NULL THEN \"provvar\" ELSE elementId(m)+\".name\" END AS prov_m__k_name, "+
                        "[x IN nodes(provpath_3) | elementId(x)] + [r IN relationships(provpath_3) | elementId(r)] AS provpath_3"
                );

    }


    @Test
    public void testSubqueriesWithSetOperators() {
        final String query = "CALL{MATCH (n) ->() RETURN n, n.name UNION MATCH (n) RETURN n, n.name} RETURN n UNION CALL{MATCH (n) ->() RETURN n, n.name} RETURN n";
        CodePointCharStream charStream = CharStreams.fromString(query);
        GQLLexer lexer = new GQLLexer(charStream);
        CommonTokenStream tokenStream = new CommonTokenStream(lexer);
        GQLParser parser = new GQLParser(tokenStream);
        ParseTree tree = parser.statementBlock();
        GQLQueryProcessor processor = getProcessorAtTranslationStage(parser, tokenStream, tree);

        //processor.getSQLAST().updateSchemaAndSignatures(new HashSet<>());
        processor.getSQLAST().storeWhyProvenanceEncodings(model);
        processor.setProcessStage(Globals.ProcessStage.REWRITE_WHY_PROVENANCE);
        ParseTreeWalker.DEFAULT.walk(processor, tree);

        assertThat(processor.getRewrittenQuery())
                .isEqualTo("CALL{MATCH provpath_0 = (n) ->() " +
                        "RETURN n, n.name, " +
                        "CASE WHEN n.name IS NULL THEN \"provvar\" ELSE elementId(n)+\".name\" END AS prov_n__k_name, "+
                        "[x IN nodes(provpath_0) | elementId(x)] + [r IN relationships(provpath_0) | elementId(r)] AS provpath_0, "+
                        "\"provvar\" AS provpath_1 " +
                        "UNION " +
                        "MATCH provpath_1 = (n) " +
                        "RETURN n, n.name, " +
                        "[x IN nodes(provpath_1) | elementId(x)] + [r IN relationships(provpath_1) | elementId(r)] AS provpath_1, "+
                        "CASE WHEN n.name IS NULL THEN \"provvar\" ELSE elementId(n)+\".name\" END AS prov_n__k_name, "+
                        "\"provvar\" AS provpath_0" +
                        "} " +
                        "RETURN n, " +
                        "provpath_0, "+
                        "prov_n__k_name, " +
                        "provpath_1, \"provvar\" AS provpath_2 " +
                        "UNION " +
                        "CALL{MATCH provpath_2 = (n) ->() " +
                        "RETURN n, n.name, " +
                        "[x IN nodes(provpath_2) | elementId(x)] + [r IN relationships(provpath_2) | elementId(r)] AS provpath_2, "+
                        "CASE WHEN n.name IS NULL THEN \"provvar\" ELSE elementId(n)+\".name\" END AS prov_n__k_name"+
                        "} " +
                        "RETURN n, prov_n__k_name, provpath_2, \"provvar\" AS provpath_1, \"provvar\" AS provpath_0");

    }

    @Test
    public void testSubqueryWithVariableScope() {
        final String query = "MATCH (n) FILTER n.age>25 CALL(n) {MATCH (n) ->() RETURN n.name UNION MATCH (n) RETURN n.name} RETURN n";
        CodePointCharStream charStream = CharStreams.fromString(query);
        GQLLexer lexer = new GQLLexer(charStream);
        CommonTokenStream tokenStream = new CommonTokenStream(lexer);
        GQLParser parser = new GQLParser(tokenStream);
        ParseTree tree = parser.statementBlock();
        GQLQueryProcessor processor = getProcessorAtTranslationStage(parser, tokenStream, tree);

        //processor.getSQLAST().updateSchemaAndSignatures(new HashSet<>());
        processor.getSQLAST().storeWhyProvenanceEncodings(model);
        processor.setProcessStage(Globals.ProcessStage.REWRITE_WHY_PROVENANCE);
        ParseTreeWalker.DEFAULT.walk(processor, tree);

        assertThat(processor.getRewrittenQuery())
                .isEqualTo("MATCH provpath_0 = (n) " +
                        "FILTER n.age>25 "+
                        "CALL(n) {" +
                        "MATCH provpath_1 = (n) ->() " +
                        "RETURN n.name, " +
                        "[x IN nodes(provpath_1) | elementId(x)] + [r IN relationships(provpath_1) | elementId(r)] AS provpath_1, " +
                        "CASE WHEN n.name IS NULL THEN \"provvar\" ELSE elementId(n)+\".name\" END AS prov_n__k_name, "+
                        "\"provvar\" AS provpath_2 " +
                        "UNION " +
                        "MATCH provpath_2 = (n) " +
                        "RETURN n.name, " +
                        "[x IN nodes(provpath_2) | elementId(x)] + [r IN relationships(provpath_2) | elementId(r)] AS provpath_2, " +
                        "CASE WHEN n.name IS NULL THEN \"provvar\" ELSE elementId(n)+\".name\" END AS prov_n__k_name, "+
                        "\"provvar\" AS provpath_1" +
                        "} " +
                        "RETURN n, " +
                        "CASE WHEN n.age IS NULL THEN \"provvar\" ELSE elementId(n)+\".age\" END AS prov_n__k_age, "+
                        "provpath_1, "+
                        "prov_n__k_name, "+
                        "provpath_2, "+
                        "[x IN nodes(provpath_0) | elementId(x)] + [r IN relationships(provpath_0) | elementId(r)] AS provpath_0" );

    }

    @Test
    public void testQueryWithNestedQuery() {
        final String query = "{MATCH (n) ->() RETURN n.name UNION MATCH (n) RETURN n.name}";
        CodePointCharStream charStream = CharStreams.fromString(query);
        GQLLexer lexer = new GQLLexer(charStream);
        CommonTokenStream tokenStream = new CommonTokenStream(lexer);
        GQLParser parser = new GQLParser(tokenStream);
        ParseTree tree = parser.statementBlock();
        GQLQueryProcessor processor = getProcessorAtTranslationStage(parser, tokenStream, tree);

        //processor.getSQLAST().updateSchemaAndSignatures(new HashSet<>());
        processor.getSQLAST().storeWhyProvenanceEncodings(model);
        processor.setProcessStage(Globals.ProcessStage.REWRITE_WHY_PROVENANCE);
        ParseTreeWalker.DEFAULT.walk(processor, tree);

        assertThat(processor.getRewrittenQuery())
                .isEqualTo("{MATCH provpath_0 = (n) ->() " +
                        "RETURN n.name, " +
                        "CASE WHEN n.name IS NULL THEN \"provvar\" ELSE elementId(n)+\".name\" END AS prov_n__k_name, "+
                        "[x IN nodes(provpath_0) | elementId(x)] + [r IN relationships(provpath_0) | elementId(r)] AS provpath_0, " +
                        "\"provvar\" AS provpath_1 " +
                        "UNION " +
                        "MATCH provpath_1 = (n) " +
                        "RETURN n.name, " +
                        "[x IN nodes(provpath_1) | elementId(x)] + [r IN relationships(provpath_1) | elementId(r)] AS provpath_1, " +
                        "CASE WHEN n.name IS NULL THEN \"provvar\" ELSE elementId(n)+\".name\" END AS prov_n__k_name, "+
                        "\"provvar\" AS provpath_0" +
                        "}");

    }

    @Test
    public void testQueryWithNextOperator() {
        final String query = "{MATCH (n) ->() RETURN n.name} NEXT MATCH (n) RETURN n";
        CodePointCharStream charStream = CharStreams.fromString(query);
        GQLLexer lexer = new GQLLexer(charStream);
        CommonTokenStream tokenStream = new CommonTokenStream(lexer);
        GQLParser parser = new GQLParser(tokenStream);
        ParseTree tree = parser.statementBlock();
        GQLQueryProcessor processor = getProcessorAtTranslationStage(parser, tokenStream, tree);

        //processor.getSQLAST().updateSchemaAndSignatures(new HashSet<>());
        processor.getSQLAST().storeWhyProvenanceEncodings(model);

        processor.setProcessStage(Globals.ProcessStage.REWRITE_WHY_PROVENANCE);
        ParseTreeWalker.DEFAULT.walk(processor, tree);

        assertThat(processor.getRewrittenQuery())
                .isEqualTo("{MATCH provpath_0 = (n) ->() " +
                        "RETURN n.name, " +
                        "CASE WHEN n.name IS NULL THEN \"provvar\" ELSE elementId(n)+\".name\" END AS prov_n__k_name, "+
                        "[x IN nodes(provpath_0) | elementId(x)] + [r IN relationships(provpath_0) | elementId(r)] AS provpath_0" +
                        "} " +
                        "NEXT " +
                        "MATCH provpath_1 = (n) RETURN n, " +
                                "provpath_0, "+
                                "[x IN nodes(provpath_1) | elementId(x)] + [r IN relationships(provpath_1) | elementId(r)] AS provpath_1, "+
                        "prov_n__k_name");

    }

    @Test
    public void testQueryWithNestedSubqueries() {
        final String query = "MATCH (n) CALL (n) {MATCH (n)->() CALL (n){ MATCH (n)->()->() RETURN n.name } RETURN n.name} RETURN n";
        CodePointCharStream charStream = CharStreams.fromString(query);
        GQLLexer lexer = new GQLLexer(charStream);
        CommonTokenStream tokenStream = new CommonTokenStream(lexer);
        GQLParser parser = new GQLParser(tokenStream);
        ParseTree tree = parser.statementBlock();
        GQLQueryProcessor processor = getProcessorAtTranslationStage(parser, tokenStream, tree);

        //processor.getSQLAST().updateSchemaAndSignatures(new HashSet<>());
        processor.getSQLAST().storeWhyProvenanceEncodings(model);
        processor.setProcessStage(Globals.ProcessStage.REWRITE_WHY_PROVENANCE);
        ParseTreeWalker.DEFAULT.walk(processor, tree);

        assertThat(processor.getRewrittenQuery())
                .isEqualTo("MATCH provpath_0 = (n) " +
                            "CALL (n) {MATCH provpath_1 = (n)->() " +
                                "CALL (n){ " +
                                "MATCH provpath_2 = (n)->()->() " +
                                "RETURN n.name, " +
                        "[x IN nodes(provpath_2) | elementId(x)] + [r IN relationships(provpath_2) | elementId(r)] AS provpath_2, " +
                                "CASE WHEN n.name IS NULL THEN \"provvar\" ELSE elementId(n)+\".name\" END AS prov_n__k_name "+
                                "} " +
                            "RETURN n.name, " +
                            "[x IN nodes(provpath_1) | elementId(x)] + [r IN relationships(provpath_1) | elementId(r)] AS provpath_1, " +
                        "prov_n__k_name, " +
                            "provpath_2" +
                            "} " +
                        "RETURN n, " +
                        "prov_n__k_name, " +
                        "provpath_1, " +
                        "provpath_2, "+
                        "[x IN nodes(provpath_0) | elementId(x)] + [r IN relationships(provpath_0) | elementId(r)] AS provpath_0"
                );
    }

    @Test
    public void testCALLwithNoMatch() {
        final String query = "MATCH (n) CALL (n){ RETURN n.name AS name UNION FILTER n.age <> 25 RETURN n.name AS name } RETURN n, name";
        CodePointCharStream charStream = CharStreams.fromString(query);
        GQLLexer lexer = new GQLLexer(charStream);
        CommonTokenStream tokenStream = new CommonTokenStream(lexer);
        GQLParser parser = new GQLParser(tokenStream);
        ParseTree tree = parser.statementBlock();
        GQLQueryProcessor processor = getProcessorAtTranslationStage(parser, tokenStream, tree);

        //processor.getSQLAST().updateSchemaAndSignatures(new HashSet<>());
        processor.getSQLAST().storeWhyProvenanceEncodings(model);
        processor.setProcessStage(Globals.ProcessStage.REWRITE_WHY_PROVENANCE);
        ParseTreeWalker.DEFAULT.walk(processor, tree);

        assertThat(processor.getRewrittenQuery())
                .isEqualTo("MATCH provpath_0 = (n) " +
                        "CALL (n){ " +
                        "RETURN n.name AS name, " +
                        "CASE WHEN n.name IS NULL THEN \"provvar\" ELSE elementId(n)+\".name\" END AS prov_n__k_name, "+
                        "[x IN nodes(provpath_0) | elementId(x)] + [r IN relationships(provpath_0) | elementId(r)] AS provpath_0, " +
                        "\"provvar\" AS prov_n__k_age "+
                        "UNION " +
                        "FILTER n.age <> 25 " +
                        "RETURN n.name AS name, " +
                        "CASE WHEN n.age IS NULL THEN \"provvar\" ELSE elementId(n)+\".age\" END AS prov_n__k_age, "+
                        "CASE WHEN n.name IS NULL THEN \"provvar\" ELSE elementId(n)+\".name\" END AS prov_n__k_name, "+
                        "[x IN nodes(provpath_0) | elementId(x)] + [r IN relationships(provpath_0) | elementId(r)] AS provpath_0 " +
                        "} " +
                        "RETURN n, name, " +
                        "prov_n__k_age, " +
                                "provpath_0, "+
                        "prov_n__k_name"
                );


    }

    @Test
    public void testReturnWithAsterisk() {
        final String query = "MATCH (n:Person) MATCH (n)-[x]->(m) FILTER m.age <> 25 RETURN *";
        CodePointCharStream charStream = CharStreams.fromString(query);
        GQLLexer lexer = new GQLLexer(charStream);
        CommonTokenStream tokenStream = new CommonTokenStream(lexer);
        GQLParser parser = new GQLParser(tokenStream);
        ParseTree tree = parser.statementBlock();
        GQLQueryProcessor processor = getProcessorAtTranslationStage(parser, tokenStream, tree);

        //processor.getSQLAST().updateSchemaAndSignatures(new HashSet<>());
        processor.getSQLAST().storeWhyProvenanceEncodings(model);
        processor.setProcessStage(Globals.ProcessStage.REWRITE_WHY_PROVENANCE);
        ParseTreeWalker.DEFAULT.walk(processor, tree);

        assertThat(processor.getRewrittenQuery())
                .isEqualTo("MATCH provpath_0 = (n:Person) " +
                        "MATCH provpath_1 = (n)-[x]->(m) " +
                        "FILTER m.age <> 25 RETURN *, " +
                        "CASE WHEN m.age IS NULL THEN \"provvar\" ELSE elementId(m)+\".age\" END AS prov_m__k_age, " +
                        "[x IN nodes(provpath_1) | elementId(x)] + [r IN relationships(provpath_1) | elementId(r)] AS provpath_1, "+
                        "[x IN nodes(provpath_0) | elementId(x)] + [r IN relationships(provpath_0) | elementId(r)] AS provpath_0, " +
                        "CASE WHEN \"Person\" IN labels(n) THEN elementId(n)+\":Person\" ELSE \"provvar\" END AS prov_n__l_Person"
                );

    }

    @Test
    public void testExistingPathVars() {
        final String query = "MATCH p = (n:Person) MATCH (n)-[x]->(m) FILTER m.age <> 25 RETURN *";
        CodePointCharStream charStream = CharStreams.fromString(query);
        GQLLexer lexer = new GQLLexer(charStream);
        CommonTokenStream tokenStream = new CommonTokenStream(lexer);
        GQLParser parser = new GQLParser(tokenStream);
        ParseTree tree = parser.statementBlock();
        GQLQueryProcessor processor = getProcessorAtTranslationStage(parser, tokenStream, tree);

        //processor.getSQLAST().updateSchemaAndSignatures(new HashSet<>());
        processor.getSQLAST().storeWhyProvenanceEncodings(model);
        processor.setProcessStage(Globals.ProcessStage.REWRITE_WHY_PROVENANCE);
        ParseTreeWalker.DEFAULT.walk(processor, tree);

        assertThat(processor.getRewrittenQuery())
                .isEqualTo("MATCH p = (n:Person) " +
                        "MATCH provpath_0 = (n)-[x]->(m) " +
                        "FILTER m.age <> 25 RETURN *, " +
                        "[x IN nodes(p) | elementId(x)] + [r IN relationships(p) | elementId(r)] AS provpath_p, "+
                        "CASE WHEN m.age IS NULL THEN \"provvar\" ELSE elementId(m)+\".age\" END AS prov_m__k_age, "+
                        "[x IN nodes(provpath_0) | elementId(x)] + [r IN relationships(provpath_0) | elementId(r)] AS provpath_0, " +
                        "CASE WHEN \"Person\" IN labels(n) THEN elementId(n)+\":Person\" ELSE \"provvar\" END AS prov_n__l_Person"
                );

    }

    @Test
    public void testPathVariablesWithSetOperators() {
        final String query = "MATCH p = (n:Person) ->(s) RETURN n, n.name UNION MATCH (n) RETURN n, n.name";
        CodePointCharStream charStream = CharStreams.fromString(query);
        GQLLexer lexer = new GQLLexer(charStream);
        CommonTokenStream tokenStream = new CommonTokenStream(lexer);
        GQLParser parser = new GQLParser(tokenStream);
        ParseTree tree = parser.statementBlock();
        GQLQueryProcessor processor = getProcessorAtTranslationStage(parser, tokenStream, tree);

        //processor.getSQLAST().updateSchemaAndSignatures(new HashSet<>());
        processor.getSQLAST().storeWhyProvenanceEncodings(model);
        processor.setProcessStage(Globals.ProcessStage.REWRITE_WHY_PROVENANCE);
        ParseTreeWalker.DEFAULT.walk(processor, tree);

        assertThat(processor.getRewrittenQuery())
                .isEqualTo("MATCH p = (n:Person) ->(s) " +
                        "RETURN n, n.name, " +
                        "[x IN nodes(p) | elementId(x)] + [r IN relationships(p) | elementId(r)] AS provpath_p, "+
                        "CASE WHEN n.name IS NULL THEN \"provvar\" ELSE elementId(n)+\".name\" END AS prov_n__k_name, "+
                        "CASE WHEN \"Person\" IN labels(n) THEN elementId(n)+\":Person\" ELSE \"provvar\" END AS prov_n__l_Person, "+
                        "\"provvar\" AS provpath_0 " +
                        "UNION " +
                        "MATCH provpath_0 = (n) " +
                        "RETURN n, n.name, " +
                                "CASE WHEN n.name IS NULL THEN \"provvar\" ELSE elementId(n)+\".name\" END AS prov_n__k_name, "+
                        "[x IN nodes(provpath_0) | elementId(x)] + [r IN relationships(provpath_0) | elementId(r)] AS provpath_0, "+
                        "\"provvar\" AS provpath_p, " +
                        "\"provvar\" AS prov_n__l_Person"
                );

    }

    @Test
    public void testPathVariablesWithSubquery() {
        final String query = "MATCH (n) " +
                "CALL(n) {MATCH p=(n) ->() " +
                "RETURN n.name " +
                "UNION " +
                "MATCH (n) RETURN n.name} " +
                "RETURN n";
        CodePointCharStream charStream = CharStreams.fromString(query);
        GQLLexer lexer = new GQLLexer(charStream);
        CommonTokenStream tokenStream = new CommonTokenStream(lexer);
        GQLParser parser = new GQLParser(tokenStream);
        ParseTree tree = parser.statementBlock();
        GQLQueryProcessor processor = getProcessorAtTranslationStage(parser, tokenStream, tree);

        //processor.getSQLAST().updateSchemaAndSignatures(new HashSet<>());
        processor.getSQLAST().storeWhyProvenanceEncodings(model);
        processor.setProcessStage(Globals.ProcessStage.REWRITE_WHY_PROVENANCE);
        ParseTreeWalker.DEFAULT.walk(processor, tree);

        assertThat(processor.getRewrittenQuery())
                .isEqualTo("MATCH provpath_0 = (n) " +
                        "CALL(n) {" +
                            "MATCH p=(n) ->() " +
                            "RETURN n.name, " +
                        "[x IN nodes(p) | elementId(x)] + [r IN relationships(p) | elementId(r)] AS provpath_p, "+
                        "CASE WHEN n.name IS NULL THEN \"provvar\" ELSE elementId(n)+\".name\" END AS prov_n__k_name, "+
                            "\"provvar\" AS provpath_1 " +
                            "UNION " +
                            "MATCH provpath_1 = (n) " +
                            "RETURN n.name, " +
                        "[x IN nodes(provpath_1) | elementId(x)] + [r IN relationships(provpath_1) | elementId(r)] AS provpath_1, "+
                        "CASE WHEN n.name IS NULL THEN \"provvar\" ELSE elementId(n)+\".name\" END AS prov_n__k_name, "+
                        "\"provvar\" AS provpath_p" +
                        "} " +
                        "RETURN n, " +
                                "provpath_p, "+
                        "prov_n__k_name, " +
                                "provpath_1, "+
                        "[x IN nodes(provpath_0) | elementId(x)] + [r IN relationships(provpath_0) | elementId(r)] AS provpath_0"
                );

    }

    @Test
    public void testPathsWithRepetitions() {
        final String query = "MATCH (n:Person)-[:LANE]->{0,5}() RETURN n, n.name";

        CodePointCharStream charStream = CharStreams.fromString(query);
        GQLLexer lexer = new GQLLexer(charStream);
        CommonTokenStream tokenStream = new CommonTokenStream(lexer);
        GQLParser parser = new GQLParser(tokenStream);
        ParseTree tree = parser.statementBlock();
        GQLQueryProcessor processor = getProcessorAtTranslationStage(parser, tokenStream, tree);

        //processor.getSQLAST().updateSchemaAndSignatures(new HashSet<>());
        processor.getSQLAST().storeWhyProvenanceEncodings(model);
        processor.setProcessStage(Globals.ProcessStage.REWRITE_WHY_PROVENANCE);
        ParseTreeWalker.DEFAULT.walk(processor, tree);

        assertThat(processor.getRewrittenQuery())
                .isEqualTo("MATCH provpath_0 = (n:Person)-[x0:LANE]->{0,5}() " +
                        "RETURN n, n.name, " +
                        "[x IN x0 | CASE WHEN \"LANE\"=type(x) THEN elementId(x)+\":LANE\" ELSE \"provvar\" END] AS prov_x0__l_LANE, "+
                        "CASE WHEN n.name IS NULL THEN \"provvar\" ELSE elementId(n)+\".name\" END AS prov_n__k_name, "+
                                "[x IN nodes(provpath_0) | elementId(x)] + [r IN relationships(provpath_0) | elementId(r)] AS provpath_0, "+
                        "CASE WHEN \"Person\" IN labels(n) THEN elementId(n)+\":Person\" ELSE \"provvar\" END AS prov_n__l_Person"
                );
    }
}