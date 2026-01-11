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

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class GQLToSQLTests {

    private GQLQueryProcessor getProcessorAtPreProcessStage(String query) {
        CodePointCharStream charStream = CharStreams.fromString(query);
        GQLLexer lexer = new GQLLexer(charStream);
        CommonTokenStream tokenStream = new CommonTokenStream(lexer);

        GQLParser parser = new GQLParser(tokenStream);
        GQLQueryProcessor processor = new GQLQueryProcessor(tokenStream, Globals.ProcessStage.SQL_TRANSLATION);
        ParseTree tree = parser.statementBlock();

        System.out.println(tree.toStringTree(parser));
        ParseTreeWalker.DEFAULT.walk(processor, tree);
        return processor;
    }

    @Test
    public void testSimpleQuery() {
        final String query = "MATCH (n:Person) RETURN n, n.name";
        GQLQueryProcessor processor = getProcessorAtPreProcessStage(query);

        assertThat(processor.getSQLAST().toString(0))
                .isEqualTo("SQLProjectNode[columns=[n.name, n], \n" +
                        "  fromNode=SQLRelationNode[relation=provpath_0, variables=[n]]]");
    }

    @Test
    public void testSimpleQueryWithMissingVariable() {
        final String query = "MATCH (n:Person)-[:LANE]->() RETURN n, n.name";
        GQLQueryProcessor processor = getProcessorAtPreProcessStage(query);

        assertThat(processor.getSQLAST().toString(0))
                .isEqualTo("SQLProjectNode[columns=[n.name, n], \n" +
                        "  fromNode=SQLRelationNode[relation=provpath_0, variables=[n]]]");
    }

    @Test
    public void testSimpleQueryWithRenaming() {
        final String query = "MATCH (n:Person) RETURN n, n.name AS name";
        GQLQueryProcessor processor = getProcessorAtPreProcessStage(query);
        System.out.println(processor.getSQLAST().toString(0));
        assertThat(processor.getSQLAST().toString(0))
                .isEqualTo("SQLRenameNode[\n" +
                        "  from=SQLProjectNode[columns=[n.name, n], \n" +
                        "      fromNode=SQLRelationNode[relation=provpath_0, variables=[n]]],\n" +
                        "  rename={n.name=name}]");
    }

    @Test
    public void testQueryWithPathPatternJoin() {
        final String query = "MATCH (n:Person), (x)->() RETURN n, n.name";
        GQLQueryProcessor processor = getProcessorAtPreProcessStage(query);

        assertThat(processor.getSQLAST().toString(0))
                .isEqualTo("SQLProjectNode[columns=[n.name, n], \n" +
                        "  fromNode=SQLJoin[ \n" +
                        "      left=SQLRelationNode[relation=provpath_0, variables=[n]], \n" +
                        "      right=SQLRelationNode[relation=provpath_1, variables=[x]]]]");

    }

    @Test
    public void testMultipleMatchClauses() {
        final String query = "MATCH (n:Person) MATCH (n)->() RETURN n, n.name";
        GQLQueryProcessor processor = getProcessorAtPreProcessStage(query);

        assertThat(processor.getSQLAST().toString(0))
                .isEqualTo("SQLProjectNode[columns=[n.name, n], \n" +
                        "  fromNode=SQLJoin[ \n" +
                        "      left=SQLRelationNode[relation=provpath_0, variables=[n]], \n" +
                        "      right=SQLRelationNode[relation=provpath_1, variables=[n]]]]");

    }

    @Test
    public void testSingleSubQuery() {
        final String query = "CALL {MATCH (n:Person) FILTER n.age = 25 RETURN n } MATCH (n) ->() RETURN n, n.name";
        GQLQueryProcessor processor = getProcessorAtPreProcessStage(query);

        assertThat(processor.getSQLAST().toString(0))
                .isEqualTo("SQLProjectNode[columns=[n.name, n], \n" +
                        "  fromNode=SQLJoin[ \n" +
                        "      left=SQLProjectNode[columns=[n], \n" +
                        "          fromNode=SQLSelectNode[\n" +
                        "              fromNode=SQLRelationNode[relation=provpath_0, variables=[n]],\n" +
                        "               where=n.age=25]], \n" +
                        "      right=SQLRelationNode[relation=provpath_1, variables=[n]]]]");

    }

    @Test
    public void testMultipleSubQuery() {
        final String query = "CALL {MATCH (n) RETURN n } MATCH (n) CALL {MATCH (n:Person) FILTER n.age = 25 RETURN n } MATCH (n) ->() RETURN n, n.name";
        GQLQueryProcessor processor = getProcessorAtPreProcessStage(query);

        assertThat(processor.getSQLAST().toString(0))
                .isEqualTo("SQLProjectNode[columns=[n.name, n], \n" +
                        "  fromNode=SQLJoin[ \n" +
                        "      left=SQLJoin[ \n" +
                        "          left=SQLJoin[ \n" +
                        "              left=SQLProjectNode[columns=[n], \n" +
                        "                  fromNode=SQLRelationNode[relation=provpath_0, variables=[n]]], \n" +
                        "              right=SQLRelationNode[relation=provpath_1, variables=[n]]], \n" +
                        "          right=SQLProjectNode[columns=[n], \n" +
                        "              fromNode=SQLSelectNode[\n" +
                        "                  fromNode=SQLRelationNode[relation=provpath_2, variables=[n]],\n" +
                        "                   where=n.age=25]]], \n" +
                        "      right=SQLRelationNode[relation=provpath_3, variables=[n]]]]");

    }

    @Test
    public void testSetOperators() {
        final String query = "MATCH (n:Person) ->() RETURN n, n.name UNION MATCH (n) RETURN n, n.name";
        GQLQueryProcessor processor = getProcessorAtPreProcessStage(query);

        assertThat(processor.getSQLAST().toString(0))
                .isEqualTo("SQLSetOpNode[\n" +
                        "  op=UNION,\n" +
                        "  left=SQLProjectNode[columns=[n.name, n], \n" +
                        "      fromNode=SQLRelationNode[relation=provpath_0, variables=[n]]], \n" +
                        "  right=SQLProjectNode[columns=[n.name, n], \n" +
                        "      fromNode=SQLRelationNode[relation=provpath_1, variables=[n]]]]");

    }

    @Test
    public void testSubqueriesWithSetOperators() {
        final String query = "CALL{MATCH (n) ->() RETURN n, n.name UNION MATCH (n) RETURN n, n.name} RETURN n UNION CALL{MATCH (n) ->() RETURN n, n.name} RETURN n";
        GQLQueryProcessor processor = getProcessorAtPreProcessStage(query);

        assertThat(processor.getSQLAST().toString(0))
                .isEqualTo("SQLSetOpNode[\n" +
                        "  op=UNION,\n" +
                        "  left=SQLProjectNode[columns=[n], \n" +
                        "      fromNode=SQLSetOpNode[\n" +
                        "          op=UNION,\n" +
                        "          left=SQLProjectNode[columns=[n.name, n], \n" +
                        "              fromNode=SQLRelationNode[relation=provpath_0, variables=[n]]], \n" +
                        "          right=SQLProjectNode[columns=[n.name, n], \n" +
                        "              fromNode=SQLRelationNode[relation=provpath_1, variables=[n]]]]], \n" +
                        "  right=SQLProjectNode[columns=[n], \n" +
                        "      fromNode=SQLProjectNode[columns=[n.name, n], \n" +
                        "          fromNode=SQLRelationNode[relation=provpath_2, variables=[n]]]]]");

    }

    @Test
    public void testSubqueryWithEmptyVariableScope() {
        final String query = "CALL(){MATCH (n) ->() RETURN n, n.name UNION MATCH (n) RETURN n, n.name} RETURN n";
        GQLQueryProcessor processor = getProcessorAtPreProcessStage(query);

        assertThat(processor.getSQLAST().toString(0))
                .isEqualTo("SQLProjectNode[columns=[n], \n" +
                        "  fromNode=SQLSetOpNode[\n" +
                        "      op=UNION,\n" +
                        "      left=SQLProjectNode[columns=[n.name, n], \n" +
                        "          fromNode=SQLRelationNode[relation=provpath_0, variables=[n]]], \n" +
                        "      right=SQLProjectNode[columns=[n.name, n], \n" +
                        "          fromNode=SQLRelationNode[relation=provpath_1, variables=[n]]]]]");

    }

    @Test
    public void testSubqueryWithVariableScope() {
        final String query = "MATCH (n) CALL(n) {MATCH (n) ->() RETURN n.name UNION MATCH (n) RETURN n.name} RETURN n";
        GQLQueryProcessor processor = getProcessorAtPreProcessStage(query);
        System.out.println(processor.getSQLAST().toString(0));
        assertThat(processor.getSQLAST().toString(0))
                .isEqualTo("SQLProjectNode[columns=[n], \n" +
                        "  fromNode=SQLJoin[ \n" +
                        "      left=SQLRelationNode[relation=provpath_0, variables=[n]], \n" +
                        "      right=SQLSetOpNode[\n" +
                        "          op=UNION,\n" +
                        "          left=SQLProjectNode[columns=[n.name], \n" +
                        "              fromNode=SQLJoin[ \n" +
                        "                  left=SQLProjectNode[columns=[n], \n" +
                        "                      fromNode=SQLRelationNode[relation=provpath_0, variables=[n]]], \n" +
                        "                  right=SQLRelationNode[relation=provpath_1, variables=[n]]]], \n" +
                        "          right=SQLProjectNode[columns=[n.name], \n" +
                        "              fromNode=SQLJoin[ \n" +
                        "                  left=SQLProjectNode[columns=[n], \n" +
                        "                      fromNode=SQLRelationNode[relation=provpath_0, variables=[n]]], \n" +
                        "                  right=SQLRelationNode[relation=provpath_2, variables=[n]]]]]]]");

    }

    @Test
    public void testQueryWithNestedQuery() {
        final String query = "{MATCH (n) ->() RETURN n.name UNION MATCH (n) RETURN n.name}";
        GQLQueryProcessor processor = getProcessorAtPreProcessStage(query);
        System.out.println(processor.getSQLAST().toString(0));
        assertThat(processor.getSQLAST().toString(0))
                .isEqualTo("SQLSetOpNode[\n" +
                        "  op=UNION,\n" +
                        "  left=SQLProjectNode[columns=[n.name], \n" +
                        "      fromNode=SQLRelationNode[relation=provpath_0, variables=[n]]], \n" +
                        "  right=SQLProjectNode[columns=[n.name], \n" +
                        "      fromNode=SQLRelationNode[relation=provpath_1, variables=[n]]]]");

    }

    @Test
    public void testQueryWithNextOperator() {
        final String query = "{MATCH (n) ->() RETURN n.name} NEXT MATCH (n) RETURN n";
        GQLQueryProcessor processor = getProcessorAtPreProcessStage(query);
        System.out.println(processor.getSQLAST().toString(0));
        assertThat(processor.getSQLAST().toString(0))
                .isEqualTo("SQLProjectNode[columns=[n], \n" +
                        "  fromNode=SQLJoin[ \n" +
                        "      left=SQLProjectNode[columns=[n.name], \n" +
                        "          fromNode=SQLRelationNode[relation=provpath_0, variables=[n]]], \n" +
                        "      right=SQLRelationNode[relation=provpath_1, variables=[n]]]]");

    }

    @Test
    public void testQueryWithNestedSubqueries() {
        final String query = "MATCH (n) CALL (n) {MATCH (n)->() CALL (n){ MATCH (n)->()->() RETURN n.name } RETURN n.name} RETURN n";
        GQLQueryProcessor processor = getProcessorAtPreProcessStage(query);
        System.out.println(processor.getSQLAST().toString(0));
        assertThat(processor.getSQLAST().toString(0))
                .isEqualTo("SQLProjectNode[columns=[n], \n" +
                        "  fromNode=SQLJoin[ \n" +
                        "      left=SQLRelationNode[relation=provpath_0, variables=[n]], \n" +
                        "      right=SQLProjectNode[columns=[n.name], \n" +
                        "          fromNode=SQLJoin[ \n" +
                        "              left=SQLJoin[ \n" +
                        "                  left=SQLProjectNode[columns=[n], \n" +
                        "                      fromNode=SQLRelationNode[relation=provpath_0, variables=[n]]], \n" +
                        "                  right=SQLRelationNode[relation=provpath_1, variables=[n]]], \n" +
                        "              right=SQLProjectNode[columns=[n.name], \n" +
                        "                  fromNode=SQLJoin[ \n" +
                        "                      left=SQLProjectNode[columns=[n], \n" +
                        "                          fromNode=SQLJoin[ \n" +
                        "                              left=SQLProjectNode[columns=[n], \n" +
                        "                                  fromNode=SQLRelationNode[relation=provpath_0, variables=[n]]], \n" +
                        "                              right=SQLRelationNode[relation=provpath_1, variables=[n]]]], \n" +
                        "                      right=SQLRelationNode[relation=provpath_2, variables=[n]]]]]]]]");

    }

    @Test
    public void testCALLwithNoMatch() {
        final String query = "MATCH (n) CALL (n){ RETURN n.name AS name UNION FILTER n.age <> 25 RETURN n.name AS name } RETURN n, name";
        GQLQueryProcessor processor = getProcessorAtPreProcessStage(query);
        System.out.println(processor.getSQLAST().toString(0));
        assertThat(processor.getSQLAST().toString(0))
                .isEqualTo("SQLProjectNode[columns=[name, n], \n" +
                        "  fromNode=SQLJoin[ \n" +
                        "      left=SQLRelationNode[relation=provpath_0, variables=[n]], \n" +
                        "      right=SQLSetOpNode[\n" +
                        "          op=UNION,\n" +
                        "          left=SQLRenameNode[\n" +
                        "              from=SQLProjectNode[columns=[n.name], \n" +
                        "                  fromNode=SQLProjectNode[columns=[n], \n" +
                        "                      fromNode=SQLRelationNode[relation=provpath_0, variables=[n]]]],\n" +
                        "              rename={n.name=name}], \n" +
                        "          right=SQLRenameNode[\n" +
                        "              from=SQLProjectNode[columns=[n.name], \n" +
                        "                  fromNode=SQLSelectNode[\n" +
                        "                      fromNode=SQLProjectNode[columns=[n], \n" +
                        "                          fromNode=SQLRelationNode[relation=provpath_0, variables=[n]]],\n" +
                        "                       where=n.age<>25]],\n" +
                        "              rename={n.name=name}]]]]");

    }

}