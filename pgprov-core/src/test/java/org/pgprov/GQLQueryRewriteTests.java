package org.pgprov;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CodePointCharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.junit.jupiter.api.Test;
import org.pgprov.parser.GQLLexer;
import org.pgprov.parser.GQLParser;
import org.pgprov.processors.GQLQueryProcessor;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;


public class GQLQueryRewriteTests {

    private GQLQueryProcessor getProcessorAtTranslationStage(GQLParser parser, CommonTokenStream tokenStream, ParseTree tree, String query) {

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
        GQLQueryProcessor processor = getProcessorAtTranslationStage(parser, tokenStream, tree, query);

        processor.setProcessStage(Globals.ProcessStage.REWRITE);
        ParseTreeWalker.DEFAULT.walk(processor, tree);

        assertThat(processor.getRewrittenQuery())
                .isEqualTo("MATCH provpath_0 = (n:Person)-[]->(x) RETURN n, n.name, n AS prov_n, provpath_0");
    }

    @Test
    public void testSimpleQueryWithOrderBy() {
        final String query = "MATCH (n:Person)-[]->(x) RETURN n, n.name AS name ORDER BY name";

        CodePointCharStream charStream = CharStreams.fromString(query);
        GQLLexer lexer = new GQLLexer(charStream);
        CommonTokenStream tokenStream = new CommonTokenStream(lexer);
        GQLParser parser = new GQLParser(tokenStream);
        ParseTree tree = parser.statementBlock();
        GQLQueryProcessor processor = getProcessorAtTranslationStage(parser, tokenStream, tree, query);

        processor.setProcessStage(Globals.ProcessStage.REWRITE);
        ParseTreeWalker.DEFAULT.walk(processor, tree);

        assertThat(processor.getRewrittenQuery())
                .isEqualTo("MATCH provpath_0 = (n:Person)-[]->(x) RETURN n, n.name AS name, n AS prov_n, provpath_0 ORDER BY name");
    }

    @Test
    public void testSimpleQueryWithMissingVariable() {
        final String query = "MATCH (n:Person)-[:LANE]->() RETURN n, n.name";

        CodePointCharStream charStream = CharStreams.fromString(query);
        GQLLexer lexer = new GQLLexer(charStream);
        CommonTokenStream tokenStream = new CommonTokenStream(lexer);
        GQLParser parser = new GQLParser(tokenStream);
        ParseTree tree = parser.statementBlock();
        GQLQueryProcessor processor = getProcessorAtTranslationStage(parser, tokenStream, tree, query);

        processor.setProcessStage(Globals.ProcessStage.REWRITE);
        ParseTreeWalker.DEFAULT.walk(processor, tree);

        assertThat(processor.getRewrittenQuery())
                .isEqualTo("MATCH provpath_0 = (n:Person)-[prov_x0:LANE]->() RETURN n, n.name, n AS prov_n, provpath_0, prov_x0");
    }

    // TO be updated with complex things
    @Test
    public void testSimpleQueryWithRenaming() {
        final String query = "MATCH (n:Person) RETURN n, n.name AS name";
        CodePointCharStream charStream = CharStreams.fromString(query);
        GQLLexer lexer = new GQLLexer(charStream);
        CommonTokenStream tokenStream = new CommonTokenStream(lexer);
        GQLParser parser = new GQLParser(tokenStream);
        ParseTree tree = parser.statementBlock();
        GQLQueryProcessor processor = getProcessorAtTranslationStage(parser, tokenStream, tree, query);

        processor.setProcessStage(Globals.ProcessStage.REWRITE);
        ParseTreeWalker.DEFAULT.walk(processor, tree);

        assertThat(processor.getRewrittenQuery())
                .isEqualTo("MATCH provpath_0 = (n:Person) RETURN n, n.name AS name, n AS prov_n, provpath_0");
    }

    @Test
    public void testQueryWithPathPatternJoin() {
        final String query = "MATCH (n:Person), (x)->() RETURN n, n.name";
        CodePointCharStream charStream = CharStreams.fromString(query);
        GQLLexer lexer = new GQLLexer(charStream);
        CommonTokenStream tokenStream = new CommonTokenStream(lexer);
        GQLParser parser = new GQLParser(tokenStream);
        ParseTree tree = parser.statementBlock();
        GQLQueryProcessor processor = getProcessorAtTranslationStage(parser, tokenStream, tree, query);

        processor.setProcessStage(Globals.ProcessStage.REWRITE);
        ParseTreeWalker.DEFAULT.walk(processor, tree);

        assertThat(processor.getRewrittenQuery())
                .isEqualTo("MATCH provpath_0 = (n:Person), provpath_1 = (x)->() RETURN n, n.name, n AS prov_n, provpath_0, provpath_1");

    }

    @Test
    public void testMultipleClauses() {
        final String query = "MATCH (n:Person) MATCH (n)-[x]->(m) FILTER m.age <> 25 RETURN n, n.name";
        CodePointCharStream charStream = CharStreams.fromString(query);
        GQLLexer lexer = new GQLLexer(charStream);
        CommonTokenStream tokenStream = new CommonTokenStream(lexer);
        GQLParser parser = new GQLParser(tokenStream);
        ParseTree tree = parser.statementBlock();
        GQLQueryProcessor processor = getProcessorAtTranslationStage(parser, tokenStream, tree, query);

        processor.setProcessStage(Globals.ProcessStage.REWRITE);
        ParseTreeWalker.DEFAULT.walk(processor, tree);

        assertThat(processor.getRewrittenQuery())
                .isEqualTo("MATCH provpath_0 = (n:Person) MATCH provpath_1 = (n)-[x]->(m) FILTER m.age <> 25 RETURN n, n.name, m AS prov_m, n AS prov_n, provpath_0, provpath_1");

    }

    @Test
    public void testSetOperators() {
        final String query = "MATCH (n:Person) ->() RETURN n, n.name UNION MATCH (n) RETURN n, n.name";
        CodePointCharStream charStream = CharStreams.fromString(query);
        GQLLexer lexer = new GQLLexer(charStream);
        CommonTokenStream tokenStream = new CommonTokenStream(lexer);
        GQLParser parser = new GQLParser(tokenStream);
        ParseTree tree = parser.statementBlock();
        GQLQueryProcessor processor = getProcessorAtTranslationStage(parser, tokenStream, tree, query);

        processor.setProcessStage(Globals.ProcessStage.REWRITE);
        ParseTreeWalker.DEFAULT.walk(processor, tree);

        assertThat(processor.getRewrittenQuery())
                .isEqualTo("MATCH provpath_0 = (n:Person) ->() " +
                        "RETURN n, n.name, provpath_0, n AS prov_n, \"provvar\" AS provpath_1 " +
                        "UNION " +
                        "MATCH provpath_1 = (n) " +
                        "RETURN n, n.name, \"provvar\" AS provpath_0, n AS prov_n, provpath_1");

    }

    @Test
    public void testMultipleSetOperators() {
        final String query = "MATCH (n:Person) ->() RETURN n, n.name UNION MATCH (n) RETURN n, n.name UNION MATCH (n) RETURN n, n.name";
        CodePointCharStream charStream = CharStreams.fromString(query);
        GQLLexer lexer = new GQLLexer(charStream);
        CommonTokenStream tokenStream = new CommonTokenStream(lexer);
        GQLParser parser = new GQLParser(tokenStream);
        ParseTree tree = parser.statementBlock();
        GQLQueryProcessor processor = getProcessorAtTranslationStage(parser, tokenStream, tree, query);

        processor.setProcessStage(Globals.ProcessStage.REWRITE);
        ParseTreeWalker.DEFAULT.walk(processor, tree);

        assertThat(processor.getRewrittenQuery())
                .isEqualTo("MATCH provpath_0 = (n:Person) ->() " +
                        "RETURN n, n.name, provpath_0, n AS prov_n, \"provvar\" AS provpath_1, \"provvar\" AS provpath_2 " +
                        "UNION " +
                        "MATCH provpath_1 = (n) " +
                        "RETURN n, n.name, \"provvar\" AS provpath_0, n AS prov_n, provpath_1, \"provvar\" AS provpath_2 " +
                        "UNION " +
                        "MATCH provpath_2 = (n) " +
                        "RETURN n, n.name, \"provvar\" AS provpath_0, n AS prov_n, \"provvar\" AS provpath_1, provpath_2");

    }

    @Test
    public void testSingleSubQuery() {
        final String query = "CALL {MATCH (n:Person) FILTER n.age = 25 RETURN n } MATCH (n) ->() RETURN n, n.name";
        CodePointCharStream charStream = CharStreams.fromString(query);
        GQLLexer lexer = new GQLLexer(charStream);
        CommonTokenStream tokenStream = new CommonTokenStream(lexer);
        GQLParser parser = new GQLParser(tokenStream);
        ParseTree tree = parser.statementBlock();
        GQLQueryProcessor processor = getProcessorAtTranslationStage(parser, tokenStream, tree, query);

        processor.setProcessStage(Globals.ProcessStage.REWRITE);
        ParseTreeWalker.DEFAULT.walk(processor, tree);

        assertThat(processor.getRewrittenQuery())
                .isEqualTo("CALL {MATCH provpath_0 = (n:Person) " +
                        "FILTER n.age = 25 " +
                        "RETURN n, n AS prov_n, provpath_0 } " +
                        "MATCH provpath_1 = (n) ->() " +
                        "RETURN n, n.name, prov_n, provpath_0, provpath_1");

    }

    @Test
    public void testMultipleSubQuery() {
        final String query = "CALL {MATCH (n) RETURN n } MATCH (n) CALL {MATCH (n:Person) FILTER n.age = 25 RETURN n } MATCH (n) ->(m) RETURN n, m.name";
        CodePointCharStream charStream = CharStreams.fromString(query);
        GQLLexer lexer = new GQLLexer(charStream);
        CommonTokenStream tokenStream = new CommonTokenStream(lexer);
        GQLParser parser = new GQLParser(tokenStream);
        ParseTree tree = parser.statementBlock();
        GQLQueryProcessor processor = getProcessorAtTranslationStage(parser, tokenStream, tree, query);

        processor.setProcessStage(Globals.ProcessStage.REWRITE);
        ParseTreeWalker.DEFAULT.walk(processor, tree);

        assertThat(processor.getRewrittenQuery())
                .isEqualTo("CALL {MATCH provpath_0 = (n) RETURN n, provpath_0 } " +
                        "MATCH provpath_1 = (n) " +
                        "CALL {MATCH provpath_2 = (n:Person) FILTER n.age = 25 RETURN n, n AS prov_n, provpath_2 } " +
                        "MATCH provpath_3 = (n) ->(m) " +
                        "RETURN n, m.name, prov_n, m AS prov_m, provpath_0, provpath_1, provpath_2, provpath_3");

    }


    @Test
    public void testSubqueriesWithSetOperators() {
        final String query = "CALL{MATCH (n) ->() RETURN n, n.name UNION MATCH (n) RETURN n, n.name} RETURN n UNION CALL{MATCH (n) ->() RETURN n, n.name} RETURN n";
        CodePointCharStream charStream = CharStreams.fromString(query);
        GQLLexer lexer = new GQLLexer(charStream);
        CommonTokenStream tokenStream = new CommonTokenStream(lexer);
        GQLParser parser = new GQLParser(tokenStream);
        ParseTree tree = parser.statementBlock();
        GQLQueryProcessor processor = getProcessorAtTranslationStage(parser, tokenStream, tree, query);

        processor.setProcessStage(Globals.ProcessStage.REWRITE);
        ParseTreeWalker.DEFAULT.walk(processor, tree);

        assertThat(processor.getRewrittenQuery())
                .isEqualTo("CALL{MATCH provpath_0 = (n) ->() " +
                        "RETURN n, n.name, provpath_0, n AS prov_n, \"provvar\" AS provpath_1 " +
                        "UNION " +
                        "MATCH provpath_1 = (n) " +
                        "RETURN n, n.name, \"provvar\" AS provpath_0, n AS prov_n, provpath_1} " +
                        "RETURN n, prov_n, provpath_0, provpath_1, \"provvar\" AS provpath_2 " +
                        "UNION " +
                        "CALL{MATCH provpath_2 = (n) ->() " +
                        "RETURN n, n.name, n AS prov_n, provpath_2} " +
                        "RETURN n, prov_n, \"provvar\" AS provpath_0, \"provvar\" AS provpath_1, provpath_2");

    }

    @Test
    public void testSubqueryWithVariableScope() {
        final String query = "MATCH (n) CALL(n) {MATCH (n) ->() RETURN n.name UNION MATCH (n) RETURN n.name} RETURN n";
        CodePointCharStream charStream = CharStreams.fromString(query);
        GQLLexer lexer = new GQLLexer(charStream);
        CommonTokenStream tokenStream = new CommonTokenStream(lexer);
        GQLParser parser = new GQLParser(tokenStream);
        ParseTree tree = parser.statementBlock();
        GQLQueryProcessor processor = getProcessorAtTranslationStage(parser, tokenStream, tree, query);

        processor.setProcessStage(Globals.ProcessStage.REWRITE);
        ParseTreeWalker.DEFAULT.walk(processor, tree);

        assertThat(processor.getRewrittenQuery())
                .isEqualTo("MATCH provpath_0 = (n) " +
                        "CALL(n) {" +
                        "MATCH provpath_1 = (n) ->() " +
                        "RETURN n.name, n AS prov_n, provpath_1, \"provvar\" AS provpath_2 " +
                        "UNION " +
                        "MATCH provpath_2 = (n) " +
                        "RETURN n.name, n AS prov_n, \"provvar\" AS provpath_1, provpath_2} " +
                        "RETURN n, prov_n, provpath_0, provpath_1, provpath_2");

    }

    @Test
    public void testQueryWithNestedQuery() {
        final String query = "{MATCH (n) ->() RETURN n.name UNION MATCH (n) RETURN n.name}";
        CodePointCharStream charStream = CharStreams.fromString(query);
        GQLLexer lexer = new GQLLexer(charStream);
        CommonTokenStream tokenStream = new CommonTokenStream(lexer);
        GQLParser parser = new GQLParser(tokenStream);
        ParseTree tree = parser.statementBlock();
        GQLQueryProcessor processor = getProcessorAtTranslationStage(parser, tokenStream, tree, query);

        processor.setProcessStage(Globals.ProcessStage.REWRITE);
        ParseTreeWalker.DEFAULT.walk(processor, tree);

        assertThat(processor.getRewrittenQuery())
                .isEqualTo("{MATCH provpath_0 = (n) ->() " +
                        "RETURN n.name, provpath_0, n AS prov_n, \"provvar\" AS provpath_1 " +
                        "UNION " +
                        "MATCH provpath_1 = (n) " +
                        "RETURN n.name, \"provvar\" AS provpath_0, n AS prov_n, provpath_1}");

    }

    @Test
    public void testQueryWithNextOperator() {
        final String query = "{MATCH (n) ->() RETURN n.name} NEXT MATCH (n) RETURN n";
        CodePointCharStream charStream = CharStreams.fromString(query);
        GQLLexer lexer = new GQLLexer(charStream);
        CommonTokenStream tokenStream = new CommonTokenStream(lexer);
        GQLParser parser = new GQLParser(tokenStream);
        ParseTree tree = parser.statementBlock();
        GQLQueryProcessor processor = getProcessorAtTranslationStage(parser, tokenStream, tree, query);

        processor.setProcessStage(Globals.ProcessStage.REWRITE);
        ParseTreeWalker.DEFAULT.walk(processor, tree);

        assertThat(processor.getRewrittenQuery())
                .isEqualTo("{MATCH provpath_0 = (n) ->() RETURN n.name, provpath_0, n AS prov_n} NEXT MATCH provpath_1 = (n) RETURN n, prov_n, provpath_0, provpath_1");

    }

    @Test
    public void testQueryWithNestedSubqueries() {
        final String query = "MATCH (n) CALL (n) {MATCH (n)->() CALL (n){ MATCH (n)->()->() RETURN n.name } RETURN n.name} RETURN n";
        CodePointCharStream charStream = CharStreams.fromString(query);
        GQLLexer lexer = new GQLLexer(charStream);
        CommonTokenStream tokenStream = new CommonTokenStream(lexer);
        GQLParser parser = new GQLParser(tokenStream);
        ParseTree tree = parser.statementBlock();
        GQLQueryProcessor processor = getProcessorAtTranslationStage(parser, tokenStream, tree, query);

        processor.setProcessStage(Globals.ProcessStage.REWRITE);
        ParseTreeWalker.DEFAULT.walk(processor, tree);

        assertThat(processor.getRewrittenQuery())
                .isEqualTo("MATCH provpath_0 = (n) " +
                        "CALL (n) {MATCH provpath_1 = (n)->() " +
                        "CALL (n){ MATCH provpath_2 = (n)->()->() " +
                        "RETURN n.name, n AS prov_n, provpath_2 } " +
                        "RETURN n.name, prov_n, provpath_1, provpath_2} " +
                        "RETURN n, prov_n, provpath_0, provpath_1, provpath_2");
    }

    @Test
    public void testCALLwithNoMatch() {
        final String query = "MATCH (n) CALL (n){ RETURN n.name AS name UNION FILTER n.age <> 25 RETURN n.name AS name } RETURN n, name";
        CodePointCharStream charStream = CharStreams.fromString(query);
        GQLLexer lexer = new GQLLexer(charStream);
        CommonTokenStream tokenStream = new CommonTokenStream(lexer);
        GQLParser parser = new GQLParser(tokenStream);
        ParseTree tree = parser.statementBlock();
        GQLQueryProcessor processor = getProcessorAtTranslationStage(parser, tokenStream, tree, query);

        processor.setProcessStage(Globals.ProcessStage.REWRITE);
        ParseTreeWalker.DEFAULT.walk(processor, tree);

        assertThat(processor.getRewrittenQuery())
                .isEqualTo("MATCH provpath_0 = (n) " +
                        "CALL (n){ " +
                        "RETURN n.name AS name, n AS prov_n " +
                        "UNION " +
                        "FILTER n.age <> 25 " +
                        "RETURN n.name AS name, n AS prov_n } " +
                        "RETURN n, name, prov_n, provpath_0");


    }

    @Test
    public void testReturnWithAsterisk() {
        final String query = "MATCH (n:Person) MATCH (n)-[x]->(m) FILTER m.age <> 25 RETURN *";
        CodePointCharStream charStream = CharStreams.fromString(query);
        GQLLexer lexer = new GQLLexer(charStream);
        CommonTokenStream tokenStream = new CommonTokenStream(lexer);
        GQLParser parser = new GQLParser(tokenStream);
        ParseTree tree = parser.statementBlock();
        GQLQueryProcessor processor = getProcessorAtTranslationStage(parser, tokenStream, tree, query);

        processor.setProcessStage(Globals.ProcessStage.REWRITE);
        ParseTreeWalker.DEFAULT.walk(processor, tree);

        assertThat(processor.getRewrittenQuery())
                .isEqualTo("MATCH provpath_0 = (n:Person) MATCH provpath_1 = (n)-[x]->(m) FILTER m.age <> 25 RETURN *, m AS prov_m, n AS prov_n, provpath_0, provpath_1");

    }

    @Test
    public void testExistingPathVars() {
        final String query = "MATCH p = (n:Person) MATCH (n)-[x]->(m) FILTER m.age <> 25 RETURN *";
        CodePointCharStream charStream = CharStreams.fromString(query);
        GQLLexer lexer = new GQLLexer(charStream);
        CommonTokenStream tokenStream = new CommonTokenStream(lexer);
        GQLParser parser = new GQLParser(tokenStream);
        ParseTree tree = parser.statementBlock();
        GQLQueryProcessor processor = getProcessorAtTranslationStage(parser, tokenStream, tree, query);

        processor.setProcessStage(Globals.ProcessStage.REWRITE);
        ParseTreeWalker.DEFAULT.walk(processor, tree);

        assertThat(processor.getRewrittenQuery())
                .isEqualTo("MATCH p = (n:Person) " +
                        "MATCH provpath_0 = (n)-[x]->(m) " +
                        "FILTER m.age <> 25 RETURN *, " +
                        "p AS provpath_p, m AS prov_m, n AS prov_n, provpath_0");

    }

    @Test
    public void testPathVariablesWithSetOperators() {
        final String query = "MATCH p = (n:Person) ->() RETURN n, n.name UNION MATCH (n) RETURN n, n.name";
        CodePointCharStream charStream = CharStreams.fromString(query);
        GQLLexer lexer = new GQLLexer(charStream);
        CommonTokenStream tokenStream = new CommonTokenStream(lexer);
        GQLParser parser = new GQLParser(tokenStream);
        ParseTree tree = parser.statementBlock();
        GQLQueryProcessor processor = getProcessorAtTranslationStage(parser, tokenStream, tree, query);

        processor.setProcessStage(Globals.ProcessStage.REWRITE);
        ParseTreeWalker.DEFAULT.walk(processor, tree);

        assertThat(processor.getRewrittenQuery())
                .isEqualTo("MATCH p = (n:Person) ->() " +
                        "RETURN n, n.name, p AS provpath_p, \"provvar\" AS provpath_0, n AS prov_n " +
                        "UNION " +
                        "MATCH provpath_0 = (n) " +
                        "RETURN n, n.name, \"provvar\" AS provpath_p, provpath_0, n AS prov_n");

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
        GQLQueryProcessor processor = getProcessorAtTranslationStage(parser, tokenStream, tree, query);

        processor.setProcessStage(Globals.ProcessStage.REWRITE);
        ParseTreeWalker.DEFAULT.walk(processor, tree);

        assertThat(processor.getRewrittenQuery())
                .isEqualTo("MATCH provpath_0 = (n) " +
                        "CALL(n) {" +
                        "MATCH p=(n) ->() " +
                        "RETURN n.name, p AS provpath_p, n AS prov_n, \"provvar\" AS provpath_1 " +
                        "UNION " +
                        "MATCH provpath_1 = (n) " +
                        "RETURN n.name, \"provvar\" AS provpath_p, n AS prov_n, provpath_1} " +
                        "RETURN n, prov_n, provpath_0, provpath_p, provpath_1");

    }

}