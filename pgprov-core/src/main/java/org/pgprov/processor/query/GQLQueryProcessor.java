package org.pgprov.processor.query;

// Date : 16/12/2025  - Building AST

// Excluded in AST Building
// statement : Catalog Modifying and Data Modifying Procedures
// nextStatements : YIELD clauses with NEXT (CYPHER25)
// linearQueryStatement : focusedLinearQueryStatement
// primitiveQueryStatement : forStatement | orderByAndPageStatement
// callProcedureStatement : OPTIONAL procedure calls
// procedureCall : namedProcedureCall
// setOperators : INTERSECT, and EXCEPT (as they are not supported by CYPHER25 yet)
//graphPattern : YIELD clauses within graph patterns

import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.TokenStreamRewriter;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import org.pgprov.Globals;
import org.pgprov.parser.GQLBaseListener;
import org.pgprov.parser.GQLParser;
import org.pgprov.ast.*;

import java.util.*;
import java.util.stream.Collectors;

public class GQLQueryProcessor extends GQLBaseListener implements QueryProcessor {

    private final TokenStreamRewriter rewriter;
    private Globals.ProcessStage processStage;
    private int counter = 0;
    private int varCounter = 0;
    private final Map<String, List<String>> schemasAndsignatures = new HashMap<>();
    private final Set<String> varsInMatchClause = new HashSet<>();

    // AST Build related
    private final ParseTreeProperty<SQLNode> sqlNodes = new ParseTreeProperty<>();
    private GQLParser.StatementBlockContext rootContext;
    private boolean init = false;
    private final Map<ParserRuleContext, ParserRuleContext> subqueryScopes = new HashMap<>();
    private SQLNode initialRelation = null;
    private ParserRuleContext preStatementContext = null;
    private final List<ParserRuleContext> storedPreStatements = new ArrayList<>();

    public GQLQueryProcessor(CommonTokenStream tokens, Globals.ProcessStage processStage) {

        this.processStage = processStage;
        this.rewriter = new TokenStreamRewriter(tokens);
    }

    public SQLNode getSQLAST() {

        return sqlNodes.get(rootContext);
    }

    public void setProcessStage(Globals.ProcessStage processStage) {
        this.processStage = processStage;
    }

    @Override
    public void enterStatementBlock(GQLParser.StatementBlockContext ctx) {
        if (processStage.equals(Globals.ProcessStage.SQL_TRANSLATION) && !init) {
            this.rootContext = ctx;
            init = true;
        }
    }

    @Override
    public void exitStatementBlock(GQLParser.StatementBlockContext ctx) {
        if (processStage.equals(Globals.ProcessStage.SQL_TRANSLATION)) {

            SQLNode sqlNode;
            List<GQLParser.NextStatementContext> nextStatementContext = ctx.nextStatement();
            if (nextStatementContext != null && !nextStatementContext.isEmpty()) {
                sqlNode = sqlNodes.get(nextStatementContext.getLast());
            } else {
                sqlNode = sqlNodes.get(ctx.statement());
            }

            sqlNodes.put(ctx, sqlNode);
        }
    }

    @Override
    public void exitStatement(GQLParser.StatementContext ctx) {
        if (processStage.equals(Globals.ProcessStage.SQL_TRANSLATION)) {
            if (ctx.compositeQueryStatement() != null) {
                sqlNodes.put(ctx, sqlNodes.get(ctx.compositeQueryStatement()));
                initialRelation = sqlNodes.get(ctx);

            } else {
                throw new RuntimeException("linearCatalogModifyingStatements and linearDataModifyingStatement statements are not supported.");
            }

        }
    }

    @Override
    public void exitNextStatement(GQLParser.NextStatementContext ctx) {
        if (processStage.equals(Globals.ProcessStage.SQL_TRANSLATION)) {
            if (ctx.yieldClause() != null) {
                throw new RuntimeException("YIELD clause is not supported.");
            }
            sqlNodes.put(ctx, sqlNodes.get(ctx.statement()));
        }
    }

    @Override
    public void exitCompositeQueryStatement(GQLParser.CompositeQueryStatementContext ctx) {
        if (processStage.equals(Globals.ProcessStage.SQL_TRANSLATION)) {
            sqlNodes.put(ctx, sqlNodes.get(ctx.compositeQueryExpression()));
        }
    }

    @Override
    public void enterCompositeQueryExpression(GQLParser.CompositeQueryExpressionContext ctx) {
        if (processStage.equals(Globals.ProcessStage.REWRITE) && ctx.queryConjunction() != null && ctx.queryConjunction().setOperator() != null) {
            SQLNode sqlNode = sqlNodes.get(ctx);
            Set<String> returnVars = sqlNode.getReturnVarsForRewriting();
            SQLNode left = sqlNodes.get(ctx.compositeQueryExpression());
            left.setReturnVarsForRewriting(returnVars);
            SQLNode right = sqlNodes.get(ctx.compositeQueryPrimary());
            right.setReturnVarsForRewriting(returnVars);
        }
    }

    @Override
    public void exitCompositeQueryExpression(GQLParser.CompositeQueryExpressionContext ctx) {

        if (processStage.equals(Globals.ProcessStage.SQL_TRANSLATION) && ctx.queryConjunction() != null && ctx.queryConjunction().setOperator() != null) {
            SQLNode left = sqlNodes.get(ctx.compositeQueryExpression());
            SQLNode right = sqlNodes.get(ctx.compositeQueryPrimary());
            //Cypher25 only supports UNION operator
            SetOperator op = ctx.queryConjunction().setOperator().UNION() != null ? SetOperator.UNION : null;
            sqlNodes.put(ctx, new SQLSetOpNode(left, right, op));
        } else if (processStage.equals(Globals.ProcessStage.SQL_TRANSLATION) && ctx.queryConjunction() == null) {
            sqlNodes.put(ctx, sqlNodes.get(ctx.compositeQueryPrimary()));
        }
    }

    @Override
    public void exitCompositeQueryPrimary(GQLParser.CompositeQueryPrimaryContext ctx) {
        if (processStage.equals(Globals.ProcessStage.SQL_TRANSLATION)) {
            sqlNodes.put(ctx, sqlNodes.get(ctx.linearQueryStatement()));
        }
    }

    @Override
    public void exitLinearQueryStatement(GQLParser.LinearQueryStatementContext ctx) {
        if (processStage.equals(Globals.ProcessStage.SQL_TRANSLATION) && ctx.ambientLinearQueryStatement() != null) {
            sqlNodes.put(ctx, sqlNodes.get(ctx.ambientLinearQueryStatement()));
        } else if (processStage.equals(Globals.ProcessStage.SQL_TRANSLATION) && ctx.focusedLinearQueryStatement() != null) {
            throw new RuntimeException("Focused Linear Query Statements are not supported.");
            //sqlNodes.put(ctx, sqlNodes.get(ctx.focusedLinearQueryStatement()));
            //TBD
        }
    }

    @Override
    public void exitAmbientLinearQueryStatement(GQLParser.AmbientLinearQueryStatementContext ctx) {
        if (processStage.equals(Globals.ProcessStage.SQL_TRANSLATION)) {
            if (ctx.primitiveResultStatement() != null) {

                GQLParser.ReturnStatementBodyContext returnContext = ctx.primitiveResultStatement().returnStatement().returnStatementBody();
                Set<String> varNames = new HashSet<>();
                if (returnContext.ASTERISK() != null) {
                    varNames.add(returnContext.ASTERISK().getText());

                    SQLNode from = ctx.simpleLinearQueryStatement() != null ? sqlNodes.get(ctx.simpleLinearQueryStatement()) : initialRelation == null ? new SQLEmptyNode() : initialRelation;

                    sqlNodes.put(ctx, new SQLProjectNode(varNames, from, new HashMap<>(schemasAndsignatures), null, false));
                    schemasAndsignatures.clear();
                } else {

                    Map<String, String> renameMap = new HashMap<>();
                    boolean renaming = false;
                    for (GQLParser.ReturnItemContext item : returnContext.returnItemList().returnItem()) {

                        if (item.returnItemAlias() != null) {
                            renaming = true;
                            renameMap.put(item.aggregatingValueExpression().getText(), item.returnItemAlias().identifier().getText());
                        }

                        varNames.add(item.aggregatingValueExpression().getText());
                    }

                    SQLNode from = ctx.simpleLinearQueryStatement() != null ? sqlNodes.get(ctx.simpleLinearQueryStatement()) : initialRelation == null ? new SQLEmptyNode() : initialRelation;

                    SQLNode projectNode = new SQLProjectNode(varNames, from, new HashMap<>(schemasAndsignatures), null, false);
                    schemasAndsignatures.clear();

                    if (renaming) {
                        sqlNodes.put(ctx, new SQLRenameNode(projectNode, renameMap));
                    } else {
                        sqlNodes.put(ctx, projectNode);
                    }

                }

            } else if (ctx.nestedQuerySpecification() != null) {

                sqlNodes.put(ctx, sqlNodes.get(ctx.nestedQuerySpecification().procedureBody()));

            } else {
                throw new RuntimeException("Only primitive result statements with simple linear queries and nested query specifications are supported.");
            }

        } else if (processStage.equals(Globals.ProcessStage.REWRITE)) {
            if (ctx.primitiveResultStatement() != null) {

                SQLNode sqlNode = sqlNodes.get(ctx);
                Set<String> returnVars = sqlNode.getReturnVarsForRewriting();
                Set<String> externalReturnVars = sqlNode.getExternalVarsForRewriting();

                if(externalReturnVars == null){ externalReturnVars = new HashSet<>(); }
                externalReturnVars.addAll(returnVars);

                GQLParser.ReturnStatementContext returnStatementCtx = ctx.primitiveResultStatement().returnStatement();

                // add all necessary bindings to return
                for (String varName : externalReturnVars) {

                    if (varName.startsWith(Globals.TEMP_PATH_PREFIX)) {

                        String origVarName = varName.substring(Globals.TEMP_PATH_PREFIX.length());

                        if (!externalReturnVars.contains(Globals.PATH_PREFIX + origVarName)) {

                            String newVarName = returnVars.contains(varName) ? origVarName : "\"" + Globals.EXTERNAL_VAR_VALUE + "\"";
                            this.rewriter.insertAfter(returnStatementCtx.getStop(), ", " + newVarName + " AS " + Globals.PATH_PREFIX + origVarName);
                        }
                        sqlNode.updateVarInSchemaAndSignatures(varName);

                    } else if (varName.startsWith(Globals.TEMP_VAR_PREFIX)) {

                        String origVarName = varName.substring(Globals.TEMP_VAR_PREFIX.length());

                        if (!externalReturnVars.contains(Globals.VAR_PREFIX + origVarName)) {

                            String newVarName = returnVars.contains(varName) ? origVarName : "\"" + Globals.EXTERNAL_VAR_VALUE + "\"";
                            this.rewriter.insertAfter(returnStatementCtx.getStop(), ", " + newVarName + " AS " + Globals.VAR_PREFIX + origVarName);
                        }
                        sqlNode.updateVarInSchemaAndSignatures(varName);

                    } else {

                        String newVarName = returnVars.contains(varName) ? "" : "\"" + Globals.EXTERNAL_VAR_VALUE + "\" AS ";
                        this.rewriter.insertAfter(returnStatementCtx.getStop(), ", " + newVarName + varName);
                    }
                }
            }
        }
    }

    @Override
    public void enterSimpleLinearQueryStatement(GQLParser.SimpleLinearQueryStatementContext ctx) {
        if (processStage.equals(Globals.ProcessStage.SQL_TRANSLATION)) {
            List<GQLParser.SimpleQueryStatementContext> simpleQueryStatements = ctx.simpleQueryStatement();

            GQLParser.SimpleQueryStatementContext from = null;

            for (GQLParser.SimpleQueryStatementContext statement : simpleQueryStatements) {

                if (statement.callQueryStatement() != null) {

                    GQLParser.InlineProcedureCallContext inlineProcedureCallContext = statement.callQueryStatement().callProcedureStatement().procedureCall().inlineProcedureCall();
                    GQLParser.VariableScopeClauseContext varScopeCtx = inlineProcedureCallContext.variableScopeClause();

                    if (varScopeCtx != null && !varScopeCtx.isEmpty() && from != null) {
                        subqueryScopes.put(inlineProcedureCallContext, from);
                    }
                } else {
                    from = statement;
                }
            }

            if (preStatementContext != null) {
                storedPreStatements.add(preStatementContext);
                preStatementContext = null;
            }
        }
    }

    @Override
    public void exitSimpleLinearQueryStatement(GQLParser.SimpleLinearQueryStatementContext ctx) {
        if (processStage.equals(Globals.ProcessStage.SQL_TRANSLATION)) {

            if (!storedPreStatements.isEmpty()) {
                preStatementContext = storedPreStatements.removeLast();
            } else {
                preStatementContext = null;
            }
            sqlNodes.put(ctx, sqlNodes.get(ctx.simpleQueryStatement().getLast()));

        }
    }

    @Override
    public void exitSimpleQueryStatement(GQLParser.SimpleQueryStatementContext statement) {
        if (processStage.equals(Globals.ProcessStage.SQL_TRANSLATION)) {

            if (statement.callQueryStatement() != null) {
                if (preStatementContext == null) {
                    sqlNodes.put(statement, sqlNodes.get(statement.callQueryStatement()));
                } else {
                    sqlNodes.put(statement, new SQLJoin(sqlNodes.get(preStatementContext), sqlNodes.get(statement.callQueryStatement()), null));
                }

                preStatementContext = statement;
            } else {

                GQLParser.PrimitiveQueryStatementContext primitiveQueryStatement = statement.primitiveQueryStatement();

                if (primitiveQueryStatement.matchStatement() != null) {
                    if (preStatementContext == null) {
                        sqlNodes.put(statement, sqlNodes.get(primitiveQueryStatement.matchStatement()));
                    } else {
                        sqlNodes.put(statement, new SQLJoin(sqlNodes.get(preStatementContext), sqlNodes.get(primitiveQueryStatement.matchStatement()), new HashMap<>(schemasAndsignatures)));
                        schemasAndsignatures.clear();
                    }
                    preStatementContext = statement;

                } else if (primitiveQueryStatement.filterStatement() != null) {
                    SQLNode from;
                    if (preStatementContext == null && initialRelation == null) {
                        from = new SQLEmptyNode();
                    } else if (preStatementContext == null) {
                        from = initialRelation;
                    } else {
                        from = sqlNodes.get(preStatementContext);
                    }
                    sqlNodes.put(statement, new SQLSelectNode(from, (SQLSelectCriteriaNode) sqlNodes.get(primitiveQueryStatement.filterStatement())));
                    preStatementContext = statement;

                } else if (primitiveQueryStatement.letStatement() != null) {
                    if (preStatementContext == null) {
                        sqlNodes.put(statement, sqlNodes.get(primitiveQueryStatement.letStatement()));
                    } else {
                        sqlNodes.put(statement, new SQLJoin(sqlNodes.get(preStatementContext), sqlNodes.get(primitiveQueryStatement.letStatement()), null));
                    }
                    preStatementContext = statement;
                } else if(primitiveQueryStatement.orderByAndPageStatement() == null) {
                    throw new RuntimeException("Only match, filter and let statements are supported.");
                }else {
                    sqlNodes.put(statement, sqlNodes.get(preStatementContext));
                    schemasAndsignatures.clear();
                }
            }

        }
    }


    @Override
    public void exitFilterStatement(GQLParser.FilterStatementContext ctx) {
        if (processStage.equals(Globals.ProcessStage.SQL_TRANSLATION)) {
            sqlNodes.put(ctx, new SQLSelectCriteriaNode(ctx.searchCondition().getText(), new HashMap<>(schemasAndsignatures)));
            schemasAndsignatures.clear();
        }
    }

    @Override
    public void exitLetStatement(GQLParser.LetStatementContext ctx) {
        if (processStage.equals(Globals.ProcessStage.SQL_TRANSLATION)) {
            sqlNodes.put(ctx, new SQLRelationNode(ctx.getText(), new HashSet<>(varsInMatchClause), new HashMap<>(schemasAndsignatures)));
            varsInMatchClause.clear();
            schemasAndsignatures.clear();
        }
    }

    @Override
    public void exitMatchStatement(GQLParser.MatchStatementContext ctx) {

        if (processStage.equals(Globals.ProcessStage.SQL_TRANSLATION)) {
            if (ctx.simpleMatchStatement() != null) {
                sqlNodes.put(ctx, sqlNodes.get(ctx.simpleMatchStatement()));
            } else {
                throw new RuntimeException("Optional match statements are not supported.");
            }

        }
    }

    @Override
    public void exitSimpleMatchStatement(GQLParser.SimpleMatchStatementContext ctx) {
        if (processStage.equals(Globals.ProcessStage.SQL_TRANSLATION)) {

            if (initialRelation != null) {
                sqlNodes.put(ctx, new SQLJoin(initialRelation, sqlNodes.get(ctx.graphPatternBindingTable()), new HashMap<>(schemasAndsignatures)));
            } else {
                sqlNodes.put(ctx, new SQLJoin(new SQLEmptyNode(), sqlNodes.get(ctx.graphPatternBindingTable()), new HashMap<>(schemasAndsignatures)));
            }
            schemasAndsignatures.clear();
        }
    }

    @Override
    public void exitGraphPatternBindingTable(GQLParser.GraphPatternBindingTableContext ctx) {
        if (processStage.equals(Globals.ProcessStage.SQL_TRANSLATION)) {

            sqlNodes.put(ctx, sqlNodes.get(ctx.graphPattern()));

            if (ctx.graphPatternYieldClause() != null) {
                throw new RuntimeException("YIELD clauses are not supported.");
            }
        }
    }

    @Override
    public void exitGraphPattern(GQLParser.GraphPatternContext ctx) {
        if (processStage.equals(Globals.ProcessStage.SQL_TRANSLATION)) {

            List<GQLParser.PathPatternContext> pathPatternContexts = ctx.pathPatternList().pathPattern();
            SQLNode from = null;
            for (GQLParser.PathPatternContext pathPatternContext : pathPatternContexts) {

                if (from == null) {
                    from = sqlNodes.get(pathPatternContext);
                } else {
                    from = new SQLJoin(from, sqlNodes.get(pathPatternContext), null);
                }
            }
            sqlNodes.put(ctx, from);
        }
    }

    @Override
    public void exitCallQueryStatement(GQLParser.CallQueryStatementContext ctx) {
        if (processStage.equals(Globals.ProcessStage.SQL_TRANSLATION)) {
            sqlNodes.put(ctx, sqlNodes.get(ctx.callProcedureStatement()));
        }
    }

    @Override
    public void exitCallProcedureStatement(GQLParser.CallProcedureStatementContext ctx) {
        if (processStage.equals(Globals.ProcessStage.SQL_TRANSLATION)) {
            sqlNodes.put(ctx, sqlNodes.get(ctx.procedureCall()));
        }
    }

    @Override
    public void exitProcedureCall(GQLParser.ProcedureCallContext ctx) {
        if (processStage.equals(Globals.ProcessStage.SQL_TRANSLATION)) {
            if (ctx.inlineProcedureCall() != null) {
                sqlNodes.put(ctx, sqlNodes.get(ctx.inlineProcedureCall()));
                initialRelation = null;
            } else {
                throw new RuntimeException("Only inline procedure calls are allowed.");
            }
        }
    }

    @Override
    public void enterInlineProcedureCall(GQLParser.InlineProcedureCallContext ctx) {
        if (processStage.equals(Globals.ProcessStage.SQL_TRANSLATION) && subqueryScopes.containsKey(ctx)) {

            GQLParser.VariableScopeClauseContext varScopeCtx = ctx.variableScopeClause();

            if (varScopeCtx != null && !varScopeCtx.isEmpty()) {


                initialRelation = new SQLProjectNode(Arrays.stream(
                                varScopeCtx.bindingVariableReferenceList().getText().split(",")
                        )
                        .collect(Collectors.toSet()), sqlNodes.get(subqueryScopes.get(ctx)), null, null, true);
            }
        }
    }

    @Override
    public void exitInlineProcedureCall(GQLParser.InlineProcedureCallContext ctx) {
        if (processStage.equals(Globals.ProcessStage.SQL_TRANSLATION)) {
            sqlNodes.put(ctx, sqlNodes.get(ctx.nestedProcedureSpecification()));
        }
    }

    @Override
    public void exitNestedProcedureSpecification(GQLParser.NestedProcedureSpecificationContext ctx) {
        if (processStage.equals(Globals.ProcessStage.SQL_TRANSLATION)) {
            sqlNodes.put(ctx, sqlNodes.get(ctx.procedureSpecification()));
        }
    }

    @Override
    public void exitProcedureSpecification(GQLParser.ProcedureSpecificationContext ctx) {
        if (processStage.equals(Globals.ProcessStage.SQL_TRANSLATION)) {
            sqlNodes.put(ctx, sqlNodes.get(ctx.procedureBody()));
        }
    }

    @Override
    public void exitProcedureBody(GQLParser.ProcedureBodyContext ctx) {
        if (processStage.equals(Globals.ProcessStage.SQL_TRANSLATION)) {
            sqlNodes.put(ctx, sqlNodes.get(ctx.statementBlock()));
        }
    }

    @Override
    public void enterNodePattern(GQLParser.NodePatternContext ctx) {

        if (processStage.equals(Globals.ProcessStage.SQL_TRANSLATION)) { //changed
            GQLParser.ElementPatternFillerContext patternFiller = ctx.elementPatternFiller();
            processPatternFiller(patternFiller);
        }
    }

    @Override
    public void enterEdgePattern(GQLParser.EdgePatternContext ctx) {

        if (processStage.equals(Globals.ProcessStage.SQL_TRANSLATION)) { //changed
            if (ctx.fullEdgePattern() != null) {
                GQLParser.ElementPatternFillerContext patternFiller;
                if (ctx.fullEdgePattern().fullEdgePointingLeft() != null) {
                    patternFiller = ctx.fullEdgePattern().fullEdgePointingLeft().elementPatternFiller();
                } else if (ctx.fullEdgePattern().fullEdgeUndirected() != null) {
                    patternFiller = ctx.fullEdgePattern().fullEdgeUndirected().elementPatternFiller();
                } else if (ctx.fullEdgePattern().fullEdgePointingRight() != null) {
                    patternFiller = ctx.fullEdgePattern().fullEdgePointingRight().elementPatternFiller();
                } else if (ctx.fullEdgePattern().fullEdgeLeftOrUndirected() != null) {
                    patternFiller = ctx.fullEdgePattern().fullEdgeLeftOrUndirected().elementPatternFiller();
                } else if (ctx.fullEdgePattern().fullEdgeUndirectedOrRight() != null) {
                    patternFiller = ctx.fullEdgePattern().fullEdgeUndirectedOrRight().elementPatternFiller();
                } else if (ctx.fullEdgePattern().fullEdgeLeftOrRight() != null) {
                    patternFiller = ctx.fullEdgePattern().fullEdgeLeftOrRight().elementPatternFiller();
                } else {
                    patternFiller = ctx.fullEdgePattern().fullEdgeAnyDirection().elementPatternFiller();
                }
                processPatternFiller(patternFiller);

            }
        }
    }

    @Override
    public void exitPathPattern(GQLParser.PathPatternContext ctx) {

        if (processStage.equals(Globals.ProcessStage.SQL_TRANSLATION)) {  //changed
            // add path variables to pattern expressions
            if (ctx.pathVariableDeclaration() == null) {
                String newVar = Globals.PATH_PREFIX + counter++;
                this.rewriter.insertBefore(ctx.getStart(), newVar + " = ");

                schemasAndsignatures.put(newVar, new ArrayList<>());
                sqlNodes.put(ctx, new SQLRelationNode(newVar, new HashSet<>(varsInMatchClause), new HashMap<>(schemasAndsignatures)));
            } else {
                String varName = ctx.pathVariableDeclaration().pathVariable().getText();

                schemasAndsignatures.put(Globals.TEMP_PATH_PREFIX + varName, new ArrayList<>());
                sqlNodes.put(ctx, new SQLRelationNode(Globals.PATH_PREFIX + varName, new HashSet<>(varsInMatchClause), new HashMap<>(schemasAndsignatures)));
            }

            varsInMatchClause.clear();
            schemasAndsignatures.clear();

        }
    }

    @Override
    public void enterValueExpressionPrimary(GQLParser.ValueExpressionPrimaryContext ctx) {

        if (processStage.equals(Globals.ProcessStage.SQL_TRANSLATION)) {
            // add properties referenced in other clauses than MATCH
            if (ctx.propertyName() != null && ctx.valueExpressionPrimary() != null && ctx.valueExpressionPrimary().bindingVariableReference() != null) {

                String varName = Globals.TEMP_VAR_PREFIX + ctx.valueExpressionPrimary().bindingVariableReference().getText();
                schemasAndsignatures.computeIfAbsent(varName, k -> new ArrayList<>()).add(Globals.PROP_ANNOT_KEY_PREFIX + ctx.propertyName().getText());
            }
        }

    }

    private void processPatternFiller(GQLParser.ElementPatternFillerContext patternFiller) {

        GQLParser.IsLabelExpressionContext labelsCtx = patternFiller.isLabelExpression();
        GQLParser.ElementPatternPredicateContext predicate = patternFiller.elementPatternPredicate();

        if (labelsCtx != null || predicate != null) {

            String varName;
            if (patternFiller.elementVariableDeclaration() != null) {
                varName = patternFiller.elementVariableDeclaration().elementVariable().getText();
                varsInMatchClause.add(varName);
                varName = Globals.TEMP_VAR_PREFIX + varName;
            } else {
                // add missing variables to pattern
                varName = Globals.VAR_PREFIX + Globals.ANONYMOUS_VAR_PREFIX + varCounter++;
                this.rewriter.insertBefore(patternFiller.getStart(), varName);
            }

            List<String> attr = schemasAndsignatures.getOrDefault(varName, new ArrayList<>());

            // add labels to variable schema
            if (labelsCtx != null) {


                List<String> nodeLabels = getLabelName(labelsCtx.labelExpression());
                for (String lbl : nodeLabels) {
                    attr.add(Globals.LBL_ANNOT_KEY_PREFIX + lbl);
                }
            }

            // add properties to variable schema from property specification
            if (predicate != null) {
                if (predicate.elementPropertySpecification() != null) {
                    List<GQLParser.PropertyKeyValuePairContext> propertyKeyValuePairs = predicate.elementPropertySpecification().propertyKeyValuePairList().propertyKeyValuePair();

                    for (GQLParser.PropertyKeyValuePairContext keyValuePair : propertyKeyValuePairs) {
                        attr.add(Globals.PROP_ANNOT_KEY_PREFIX + keyValuePair.propertyName().getText());
                    }
                }

            }
            schemasAndsignatures.put(varName, attr);
        } else if (patternFiller.elementVariableDeclaration() != null) {
            String varName = patternFiller.elementVariableDeclaration().elementVariable().getText();
            varsInMatchClause.add(varName);
        }
    }

    private List<String> getLabelName(GQLParser.LabelExpressionContext ctx) {

        List<String> labelNames = new ArrayList<>();

        if (ctx instanceof GQLParser.LabelExpressionNegationContext) {
            labelNames.addAll(getLabelName(((GQLParser.LabelExpressionNegationContext) ctx).labelExpression()));
        } else if (ctx instanceof GQLParser.LabelExpressionParenthesizedContext) {
            labelNames.addAll(getLabelName(((GQLParser.LabelExpressionParenthesizedContext) ctx).labelExpression()));
        } else if (ctx instanceof GQLParser.LabelExpressionConjunctionContext) {
            List<GQLParser.LabelExpressionContext> lblExpCtxs = ((GQLParser.LabelExpressionConjunctionContext) ctx).labelExpression();
            for (GQLParser.LabelExpressionContext lblExpCtx : lblExpCtxs) {
                labelNames.addAll(getLabelName(lblExpCtx));
            }
        } else if (ctx instanceof GQLParser.LabelExpressionDisjunctionContext) {
            List<GQLParser.LabelExpressionContext> lblExpCtxs = ((GQLParser.LabelExpressionDisjunctionContext) ctx).labelExpression();
            for (GQLParser.LabelExpressionContext lblExpCtx : lblExpCtxs) {
                labelNames.addAll(getLabelName(lblExpCtx));
            }
        } else if (ctx instanceof GQLParser.LabelExpressionNameContext) {
            labelNames.add(((GQLParser.LabelExpressionNameContext) ctx).labelName().getText());
        }
        return labelNames;
    }

    @Override
    public String getRewrittenQuery() {
        return this.rewriter.getText();
    }
}
