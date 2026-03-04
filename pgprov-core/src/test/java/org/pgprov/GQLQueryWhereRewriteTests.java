package org.pgprov;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CodePointCharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.junit.jupiter.api.Test;
import org.pgprov.ast.SQLNode;
import org.pgprov.ast.SQLProjectNode;
import org.pgprov.ast.SQLRenameNode;
import org.pgprov.parser.GQLLexer;
import org.pgprov.parser.GQLParser;
import org.pgprov.processor.query.GQLQueryProcessor;

import java.util.HashSet;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;


public class GQLQueryWhereRewriteTests {

    private final Globals.ProvenanceType model = Globals.ProvenanceType.WHERE_PROV;
    private GQLQueryProcessor getProcessorAtTranslationStage(GQLParser parser, CommonTokenStream tokenStream, ParseTree tree) {

        GQLQueryProcessor processor = new GQLQueryProcessor(tokenStream, Globals.ProcessStage.SQL_TRANSLATION_WHERE_PROVENANCE);
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

//        processor.getSQLAST().updateSchemaAndSignatures(new HashSet<>());
        processor.getSQLAST().storeWhereProvenanceEncodings(model, null, null);
        processor.setProcessStage(Globals.ProcessStage.REWRITE_WHERE_PROVENANCE);
        ParseTreeWalker.DEFAULT.walk(processor, tree);
        assertThat(processor.getRewrittenQuery())
                .isEqualTo("MATCH provpath_0 = (n:Person)-[]->(x) RETURN n, n.name, " +
                        "elementId(n) AS prov_n, "+
                        "CASE WHEN n.name IS NULL THEN \"provvar\" ELSE elementId(n)+\".name\" END AS prov_n_name"
                );
    }

    @Test
    public void testSimpleQueryWithOrderBy() {
        final String query = "MATCH (n:Person)-[]->(x) RETURN n, n.name AS name ORDER BY name";

        CodePointCharStream charStream = CharStreams.fromString(query);
        GQLLexer lexer = new GQLLexer(charStream);
        CommonTokenStream tokenStream = new CommonTokenStream(lexer);
        GQLParser parser = new GQLParser(tokenStream);
        ParseTree tree = parser.statementBlock();
        GQLQueryProcessor processor = getProcessorAtTranslationStage(parser, tokenStream, tree);

        processor.getSQLAST().updateSchemaAndSignatures(new HashSet<>());
        processor.getSQLAST().storeWhereProvenanceEncodings(model, null, null);
        processor.setProcessStage(Globals.ProcessStage.REWRITE_WHERE_PROVENANCE);
        ParseTreeWalker.DEFAULT.walk(processor, tree);

        assertThat(processor.getRewrittenQuery())
                .isEqualTo("MATCH provpath_0 = (n:Person)-[]->(x) " +
                        "RETURN n, n.name AS name, " +
                        "CASE WHEN n.name IS NULL THEN \"provvar\" ELSE elementId(n)+\".name\" END AS prov_name, "+
                                "elementId(n) AS prov_n "+
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

        processor.getSQLAST().updateSchemaAndSignatures(new HashSet<>());
        processor.getSQLAST().storeWhereProvenanceEncodings(model, null, null);
        processor.setProcessStage(Globals.ProcessStage.REWRITE_WHERE_PROVENANCE);
        ParseTreeWalker.DEFAULT.walk(processor, tree);

        assertThat(processor.getRewrittenQuery())
                .isEqualTo("MATCH provpath_0 = (n:Person)-[x0:LANE]->() " +
                        "RETURN n, n.name, " +
                                "elementId(n) AS prov_n, "+
                        "CASE WHEN n.name IS NULL THEN \"provvar\" ELSE elementId(n)+\".name\" END AS prov_n_name"
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

        processor.getSQLAST().updateSchemaAndSignatures(new HashSet<>());
        processor.getSQLAST().storeWhereProvenanceEncodings(model, null, null);
        processor.setProcessStage(Globals.ProcessStage.REWRITE_WHERE_PROVENANCE);
        ParseTreeWalker.DEFAULT.walk(processor, tree);

        assertThat(processor.getRewrittenQuery())
                .isEqualTo("MATCH provpath_0 = (n:Person) " +
                        "RETURN n, n.name AS name, " +
                                "CASE WHEN n.name IS NULL THEN \"provvar\" ELSE elementId(n)+\".name\" END AS prov_name, "+
                        "elementId(n) AS prov_n"
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

        processor.getSQLAST().updateSchemaAndSignatures(new HashSet<>());
        processor.getSQLAST().storeWhereProvenanceEncodings(model, null, null);
        processor.setProcessStage(Globals.ProcessStage.REWRITE_WHERE_PROVENANCE);
        ParseTreeWalker.DEFAULT.walk(processor, tree);

        assertThat(processor.getRewrittenQuery())
                .isEqualTo("MATCH provpath_0 = (n:Person), provpath_1 = (x)->() " +
                        "RETURN n, n.name, " +
                                "elementId(n) AS prov_n, "+
                        "CASE WHEN n.name IS NULL THEN \"provvar\" ELSE elementId(n)+\".name\" END AS prov_n_name"
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

        processor.getSQLAST().updateSchemaAndSignatures(new HashSet<>());
        processor.getSQLAST().storeWhereProvenanceEncodings(model, null, null);
        processor.setProcessStage(Globals.ProcessStage.REWRITE_WHERE_PROVENANCE);
        ParseTreeWalker.DEFAULT.walk(processor, tree);

        assertThat(processor.getRewrittenQuery())
                .isEqualTo("MATCH provpath_0 = (n:Person) ->() " +
                        "RETURN n, n.name, " +
                                "elementId(n) AS prov_n, "+
                                "CASE WHEN n.name IS NULL THEN \"provvar\" ELSE elementId(n)+\".name\" END AS prov_n_name "+
                        "UNION " +
                        "MATCH provpath_1 = (n) " +
                        "RETURN n, n.name, "+
                                "elementId(n) AS prov_n, "+
                                "CASE WHEN n.name IS NULL THEN \"provvar\" ELSE elementId(n)+\".name\" END AS prov_n_name"

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

        processor.getSQLAST().updateSchemaAndSignatures(new HashSet<>());
        processor.getSQLAST().storeWhereProvenanceEncodings(model, null, null);
        processor.setProcessStage(Globals.ProcessStage.REWRITE_WHERE_PROVENANCE);
        ParseTreeWalker.DEFAULT.walk(processor, tree);

        assertThat(processor.getRewrittenQuery())
                .isEqualTo("MATCH provpath_0 = (n:Person) ->() " +
                        "RETURN n, n.name AS name, " +
                        "CASE WHEN n.name IS NULL THEN \"provvar\" ELSE elementId(n)+\".name\" END AS prov_name, "+
                                "elementId(n) AS prov_n "+
                        "UNION " +
                        "MATCH provpath_1 = (n) " +
                        "RETURN n, n.name AS name, " +
                        "CASE WHEN n.name IS NULL THEN \"provvar\" ELSE elementId(n)+\".name\" END AS prov_name, "+
                        "elementId(n) AS prov_n "+
                        "UNION " +
                        "MATCH provpath_2 = (n) " +
                        "RETURN n, n.age AS name, " +
                        "CASE WHEN n.age IS NULL THEN \"provvar\" ELSE elementId(n)+\".age\" END AS prov_name, "+
                        "elementId(n) AS prov_n");

    }

    @Test
    public void testSingleSubQuery() {
        final String query = "CALL {MATCH (n:Person) FILTER n.age = 25 RETURN n.name AS name} MATCH (m) ->() RETURN m, name";
        CodePointCharStream charStream = CharStreams.fromString(query);
        GQLLexer lexer = new GQLLexer(charStream);
        CommonTokenStream tokenStream = new CommonTokenStream(lexer);
        GQLParser parser = new GQLParser(tokenStream);
        ParseTree tree = parser.statementBlock();
        GQLQueryProcessor processor = getProcessorAtTranslationStage(parser, tokenStream, tree);

        processor.getSQLAST().updateSchemaAndSignatures(new HashSet<>());
        processor.getSQLAST().storeWhereProvenanceEncodings(model, null, null);
        processor.setProcessStage(Globals.ProcessStage.REWRITE_WHERE_PROVENANCE);
        ParseTreeWalker.DEFAULT.walk(processor, tree);

        assertThat(processor.getRewrittenQuery())
                .isEqualTo("CALL {MATCH provpath_0 = (n:Person) " +
                        "FILTER n.age = 25 " +
                        "RETURN n.name AS name, " +
                                "CASE WHEN n.name IS NULL THEN \"provvar\" ELSE elementId(n)+\".name\" END AS prov_name"+
                        "} " +
                        "MATCH provpath_1 = (m) ->() " +
                        "RETURN m, name, " +
                        "prov_name, "+
                        "elementId(m) AS prov_m"
                        );

    }

    @Test
    public void testMultipleSubQuery() {
        final String query = "CALL {MATCH (n) RETURN n } MATCH (n) CALL {MATCH (n:Person) FILTER n.age = 25 RETURN n.age } MATCH (n)-[]->(m) RETURN n, m.name";
        CodePointCharStream charStream = CharStreams.fromString(query);
        GQLLexer lexer = new GQLLexer(charStream);
        CommonTokenStream tokenStream = new CommonTokenStream(lexer);
        GQLParser parser = new GQLParser(tokenStream);
        ParseTree tree = parser.statementBlock();
        GQLQueryProcessor processor = getProcessorAtTranslationStage(parser, tokenStream, tree);

        processor.getSQLAST().updateSchemaAndSignatures(new HashSet<>());
        processor.getSQLAST().storeWhereProvenanceEncodings(model, null, null);
        processor.setProcessStage(Globals.ProcessStage.REWRITE_WHERE_PROVENANCE);
        ParseTreeWalker.DEFAULT.walk(processor, tree);

        assertThat(processor.getRewrittenQuery())
                .isEqualTo("CALL {MATCH provpath_0 = (n) RETURN n " +
                        "} " +
                        "MATCH provpath_1 = (n) " +
                        "CALL {MATCH provpath_2 = (n:Person) FILTER n.age = 25 " +
                        "RETURN n.age " +
                        "} " +
                        "MATCH provpath_3 = (n)-[]->(m) " +
                        "RETURN n, m.name, "+
                        "elementId(n) AS prov_n, "+
                        "CASE WHEN m.name IS NULL THEN \"provvar\" ELSE elementId(m)+\".name\" END AS prov_m_name"
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

        processor.getSQLAST().updateSchemaAndSignatures(new HashSet<>());
        processor.getSQLAST().storeWhereProvenanceEncodings(model, null, null);
        processor.setProcessStage(Globals.ProcessStage.REWRITE_WHERE_PROVENANCE);
        ParseTreeWalker.DEFAULT.walk(processor, tree);

        assertThat(processor.getRewrittenQuery())
                .isEqualTo("CALL{MATCH provpath_0 = (n) ->() " +
                        "RETURN n, n.name " +
                        "UNION " +
                        "MATCH provpath_1 = (n) " +
                        "RETURN n, n.name" +
                        "} " +
                        "RETURN n, " +
                                "elementId(n) AS prov_n "+
                        "UNION " +
                        "CALL{MATCH provpath_2 = (n) ->() " +
                        "RETURN n, n.name" +
                        "} " +
                        "RETURN n, "+
                        "elementId(n) AS prov_n"
                );

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

        processor.getSQLAST().updateSchemaAndSignatures(new HashSet<>());
        processor.getSQLAST().storeWhereProvenanceEncodings(model, null, null);
        processor.setProcessStage(Globals.ProcessStage.REWRITE_WHERE_PROVENANCE);
        ParseTreeWalker.DEFAULT.walk(processor, tree);

        assertThat(processor.getRewrittenQuery())
                .isEqualTo("MATCH provpath_0 = (n) " +
                        "FILTER n.age>25 "+
                        "CALL(n) {" +
                        "MATCH provpath_1 = (n) ->() " +
                        "RETURN n.name " +
                        "UNION " +
                        "MATCH provpath_2 = (n) " +
                        "RETURN n.name" +
                        "} " +
                        "RETURN n, " +
                        "elementId(n) AS prov_n");

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

        processor.getSQLAST().updateSchemaAndSignatures(new HashSet<>());
        processor.getSQLAST().storeWhereProvenanceEncodings(model, null, null);
        processor.setProcessStage(Globals.ProcessStage.REWRITE_WHERE_PROVENANCE);
        ParseTreeWalker.DEFAULT.walk(processor, tree);

        assertThat(processor.getRewrittenQuery())
                .isEqualTo("{MATCH provpath_0 = (n) ->() " +
                        "RETURN n.name, " +
                                "CASE WHEN n.name IS NULL THEN \"provvar\" ELSE elementId(n)+\".name\" END AS prov_n_name "+
                        "UNION " +
                        "MATCH provpath_1 = (n) " +
                        "RETURN n.name, " +
                        "CASE WHEN n.name IS NULL THEN \"provvar\" ELSE elementId(n)+\".name\" END AS prov_n_name"+
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

        processor.getSQLAST().updateSchemaAndSignatures(new HashSet<>());
        processor.getSQLAST().storeWhereProvenanceEncodings(model, null, null);
        processor.setProcessStage(Globals.ProcessStage.REWRITE_WHERE_PROVENANCE);
        ParseTreeWalker.DEFAULT.walk(processor, tree);

        assertThat(processor.getRewrittenQuery())
                .isEqualTo("{MATCH provpath_0 = (n) ->() " +
                        "RETURN n.name" +
                        "} " +
                        "NEXT " +
                        "MATCH provpath_1 = (n) RETURN n, " +
                        "elementId(n) AS prov_n"
            );

    }

    @Test
    public void testQueryWithNestedSubqueries() {
        final String query = "MATCH (n) CALL (n) {MATCH (n)->() CALL (n){ MATCH (n)->()->() RETURN n.name AS name, n AS node } RETURN name, node AS node1} RETURN node1, name, node1.age";
        CodePointCharStream charStream = CharStreams.fromString(query);
        GQLLexer lexer = new GQLLexer(charStream);
        CommonTokenStream tokenStream = new CommonTokenStream(lexer);
        GQLParser parser = new GQLParser(tokenStream);
        ParseTree tree = parser.statementBlock();
        GQLQueryProcessor processor = getProcessorAtTranslationStage(parser, tokenStream, tree);

        processor.getSQLAST().updateSchemaAndSignatures(new HashSet<>());
        processor.getSQLAST().storeWhereProvenanceEncodings(model, null, null);
        processor.setProcessStage(Globals.ProcessStage.REWRITE_WHERE_PROVENANCE);
        ParseTreeWalker.DEFAULT.walk(processor, tree);

        assertThat(processor.getRewrittenQuery())
                .isEqualTo("MATCH provpath_0 = (n) " +
                            "CALL (n) {MATCH provpath_1 = (n)->() " +
                                "CALL (n){ " +
                                "MATCH provpath_2 = (n)->()->() " +
                                "RETURN n.name AS name, n AS node, " +
                        "CASE WHEN n.name IS NULL THEN \"provvar\" ELSE elementId(n)+\".name\" END AS prov_name "+
                                "} " +
                            "RETURN name, node AS node1" +
                            "} " +
                        "RETURN node1, name, node1.age, "+
                        "prov_name, "+
                        "elementId(node1) AS prov_node1, "+
                        "CASE WHEN node1.age IS NULL THEN \"provvar\" ELSE elementId(node1)+\".age\" END AS prov_node1_age"

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

        processor.getSQLAST().updateSchemaAndSignatures(new HashSet<>());
        processor.getSQLAST().storeWhereProvenanceEncodings(model, null, null);
        processor.setProcessStage(Globals.ProcessStage.REWRITE_WHERE_PROVENANCE);
        ParseTreeWalker.DEFAULT.walk(processor, tree);

        assertThat(processor.getRewrittenQuery())
                .isEqualTo("MATCH provpath_0 = (n) " +
                        "CALL (n){ " +
                        "RETURN n.name AS name, " +
                        "CASE WHEN n.name IS NULL THEN \"provvar\" ELSE elementId(n)+\".name\" END AS prov_name "+
                        "UNION " +
                        "FILTER n.age <> 25 " +
                        "RETURN n.name AS name, " +
                        "CASE WHEN n.name IS NULL THEN \"provvar\" ELSE elementId(n)+\".name\" END AS prov_name "+
                        "} " +
                        "RETURN n, name, prov_name, " +
                        "elementId(n) AS prov_n"
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

        processor.getSQLAST().updateSchemaAndSignatures(new HashSet<>());
        processor.getSQLAST().storeWhereProvenanceEncodings(model, null, null);
        processor.setProcessStage(Globals.ProcessStage.REWRITE_WHERE_PROVENANCE);
        ParseTreeWalker.DEFAULT.walk(processor, tree);

        assertThat(processor.getRewrittenQuery())
                .isEqualTo("MATCH provpath_0 = (n:Person) " +
                        "MATCH provpath_1 = (n)-[x]->(m) " +
                        "FILTER m.age <> 25 RETURN *, " +
                        "elementId(x) AS prov_x, "+
                        "elementId(m) AS prov_m, "+
                        "elementId(n) AS prov_n"

                );

    }

    @Test
    public void testExistingPathVars() {
        final String query = "MATCH p = (n:Person) MATCH (l)-[x]->(m) FILTER m.age <> 25 RETURN *";
        CodePointCharStream charStream = CharStreams.fromString(query);
        GQLLexer lexer = new GQLLexer(charStream);
        CommonTokenStream tokenStream = new CommonTokenStream(lexer);
        GQLParser parser = new GQLParser(tokenStream);
        ParseTree tree = parser.statementBlock();
        GQLQueryProcessor processor = getProcessorAtTranslationStage(parser, tokenStream, tree);

        processor.getSQLAST().updateSchemaAndSignatures(new HashSet<>());
        processor.getSQLAST().storeWhereProvenanceEncodings(model, null, null);
        processor.setProcessStage(Globals.ProcessStage.REWRITE_WHERE_PROVENANCE);
        ParseTreeWalker.DEFAULT.walk(processor, tree);

        assertThat(processor.getRewrittenQuery())
                .isEqualTo("MATCH p = (n:Person) " +
                        "MATCH provpath_0 = (l)-[x]->(m) " +
                        "FILTER m.age <> 25 RETURN *, " +
                        "[x IN nodes(p) | elementId(x)] + [r IN relationships(p) | elementId(r)] AS prov_p, "+
                        "elementId(x) AS prov_x, "+
                        "elementId(l) AS prov_l, "+
                        "elementId(m) AS prov_m, "+
                        "elementId(n) AS prov_n"
                );

    }

    @Test
    public void testPathVariablesWithSetOperators() {
        final String query = "MATCH p = (n:Person) ->() RETURN n, n.name UNION MATCH (n) RETURN n, n.name";
        CodePointCharStream charStream = CharStreams.fromString(query);
        GQLLexer lexer = new GQLLexer(charStream);
        CommonTokenStream tokenStream = new CommonTokenStream(lexer);
        GQLParser parser = new GQLParser(tokenStream);
        ParseTree tree = parser.statementBlock();
        GQLQueryProcessor processor = getProcessorAtTranslationStage(parser, tokenStream, tree);

        processor.getSQLAST().updateSchemaAndSignatures(new HashSet<>());
        processor.getSQLAST().storeWhereProvenanceEncodings(model, null, null);
        processor.setProcessStage(Globals.ProcessStage.REWRITE_WHERE_PROVENANCE);
        ParseTreeWalker.DEFAULT.walk(processor, tree);

        assertThat(processor.getRewrittenQuery())
                .isEqualTo("MATCH p = (n:Person) ->() " +
                        "RETURN n, n.name, " +
                        "elementId(n) AS prov_n, "+
                                "CASE WHEN n.name IS NULL THEN \"provvar\" ELSE elementId(n)+\".name\" END AS prov_n_name "+
                        "UNION " +
                        "MATCH provpath_0 = (n) " +
                        "RETURN n, n.name, " +
                                "elementId(n) AS prov_n, "+
                        "CASE WHEN n.name IS NULL THEN \"provvar\" ELSE elementId(n)+\".name\" END AS prov_n_name"
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

        processor.getSQLAST().updateSchemaAndSignatures(new HashSet<>());
        processor.getSQLAST().storeWhereProvenanceEncodings(model, null, null);
        processor.setProcessStage(Globals.ProcessStage.REWRITE_WHERE_PROVENANCE);
        ParseTreeWalker.DEFAULT.walk(processor, tree);

        assertThat(processor.getRewrittenQuery())
                .isEqualTo("MATCH provpath_0 = (n) " +
                        "CALL(n) {" +
                            "MATCH p=(n) ->() " +
                            "RETURN n.name " +
                            "UNION " +
                            "MATCH provpath_1 = (n) " +
                            "RETURN n.name" +
                        "} " +
                        "RETURN n, " +
                        "elementId(n) AS prov_n"
                );

    }

    @Test
    public void testPathsWithRepetitions() {
        final String query = "MATCH (n:Person)-[r:LANE]->{0,5}() RETURN n, n.name, r";

        CodePointCharStream charStream = CharStreams.fromString(query);
        GQLLexer lexer = new GQLLexer(charStream);
        CommonTokenStream tokenStream = new CommonTokenStream(lexer);
        GQLParser parser = new GQLParser(tokenStream);
        ParseTree tree = parser.statementBlock();
        GQLQueryProcessor processor = getProcessorAtTranslationStage(parser, tokenStream, tree);

        processor.getSQLAST().updateSchemaAndSignatures(new HashSet<>());
        processor.getSQLAST().storeWhereProvenanceEncodings(model, null, null);
        processor.setProcessStage(Globals.ProcessStage.REWRITE_WHERE_PROVENANCE);
        ParseTreeWalker.DEFAULT.walk(processor, tree);

        assertThat(processor.getRewrittenQuery())
                .isEqualTo("MATCH provpath_0 = (n:Person)-[r:LANE]->{0,5}() " +
                        "RETURN n, n.name, r, " +
                        "[x IN r | elementId(x)] AS prov_r, "+
                        "elementId(n) AS prov_n, "+
                        "CASE WHEN n.name IS NULL THEN \"provvar\" ELSE elementId(n)+\".name\" END AS prov_n_name"
                );
    }
}