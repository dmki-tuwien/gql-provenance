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
    private final Set<String> schemasAndsignatures = new HashSet<>();
    private final Set<String> labelsSignature = new HashSet<>();
    private final Set<String> varsInMatchClause = new HashSet<>();
    private boolean enterNextStatement = false;
    private GQLParser.PathFactorContext repetitivePathFactorContext = null;

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
    public void enterNextStatement(GQLParser.NextStatementContext ctx) {
        if (processStage.equals(Globals.ProcessStage.SQL_TRANSLATION)) {
            if (ctx.yieldClause() != null) {
                throw new RuntimeException("YIELD clause is not supported.");
            }
            enterNextStatement = true;
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

                    sqlNodes.put(ctx, new SQLProjectNode(varNames, from, new HashSet<>(schemasAndsignatures)));
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

                    SQLNode projectNode = new SQLProjectNode(varNames, from, new HashSet<>(schemasAndsignatures));
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

        }
        else if (processStage.equals(Globals.ProcessStage.REWRITE_WHY_PROVENANCE)) {
            if (ctx.primitiveResultStatement() != null) {

                SQLNode sqlNode = sqlNodes.get(ctx);

                Set<String> provenanceEncodings = null;

                if(sqlNode instanceof SQLRenameNode node){
                    provenanceEncodings = new HashSet<>(node.getWhyProvenanceEncodings());
                }else if(sqlNode instanceof SQLProjectNode node){
                    provenanceEncodings = new HashSet<>(node.getWhyProvenanceEncodings());
                }

                GQLParser.ReturnStatementContext returnStatementCtx = ctx.primitiveResultStatement().returnStatement();

                if(provenanceEncodings != null){
                    for (String entry : provenanceEncodings) {

                        if (entry.startsWith(Globals.TEMP_PATH_PREFIX)) {

                            String origVarName = entry.substring(Globals.TEMP_PATH_PREFIX.length());

                            if(origVarName.startsWith(Globals.PATH_PREFIX)){
                                this.rewriter.insertAfter(returnStatementCtx.getStop(), ", [x IN nodes("+origVarName+") | "+Globals.ID_FUNCTION+"(x)] + [r IN relationships("+origVarName+") | "+Globals.ID_FUNCTION+"(r)] AS "+ origVarName);
                            }else{
                                this.rewriter.insertAfter(returnStatementCtx.getStop(), ", [x IN nodes("+origVarName+") | "+Globals.ID_FUNCTION+"(x)] + [r IN relationships("+origVarName+") | "+Globals.ID_FUNCTION+"(r)] AS "+ Globals.PATH_PREFIX + origVarName);
                            }
                            getSQLAST().updateWhyProvenanceEncodingVariable(entry, sqlNode);
                         }else if (entry.startsWith(Globals.TEMP_VAR_LIST_PREFIX)) {

                            String origVarName = entry.substring(Globals.TEMP_VAR_LIST_PREFIX.length());
                            if(origVarName.contains(Globals.PROP_ANNOT_KEY_PREFIX)){
                                String[] varNameParts = origVarName.split(Globals.PROP_ANNOT_KEY_PREFIX);
                                this.rewriter.insertAfter(returnStatementCtx.getStop(), ", [x IN "+varNameParts[0]+" | CASE WHEN x."+varNameParts[1]+" IS NULL THEN \""+Globals.EXTERNAL_VAR_VALUE+"\" ELSE "
                                        +Globals.ID_FUNCTION+"(x)+\"." + varNameParts[1] + "\" END] AS " + Globals.VAR_PREFIX + origVarName);
                            }else if(origVarName.contains(Globals.LBL_ANNOT_KEY_PREFIX)){
                                String mainName = origVarName.substring(+Globals.NODE_ANNOT_PREFIX.length());
                                String[] varNameParts = mainName.split(Globals.LBL_ANNOT_KEY_PREFIX);

                                if(origVarName.startsWith(Globals.NODE_ANNOT_PREFIX)) {
                                    this.rewriter.insertAfter(returnStatementCtx.getStop(), ", [x IN " + varNameParts[0] + " | CASE WHEN \"" + varNameParts[1] + "\" IN labels(x) THEN "
                                            + Globals.ID_FUNCTION + "(x)+\":" + varNameParts[1] + "\" ELSE \"" + Globals.EXTERNAL_VAR_VALUE + "\" END] AS " + Globals.VAR_PREFIX + mainName);
                                }else{
                                    this.rewriter.insertAfter(returnStatementCtx.getStop(), ", [x IN " + varNameParts[0] + " | CASE WHEN \"" + varNameParts[1] + "\"=type(x) THEN "
                                            + Globals.ID_FUNCTION + "(x)+\":" + varNameParts[1] + "\" ELSE \"" + Globals.EXTERNAL_VAR_VALUE + "\" END] AS " + Globals.VAR_PREFIX + mainName);
                                }
                            }else{
                                this.rewriter.insertAfter(returnStatementCtx.getStop(), ", [x IN "+origVarName+" | "+Globals.ID_FUNCTION+"(x)] AS "+  Globals.VAR_PREFIX + origVarName);
                            }
                            getSQLAST().updateWhyProvenanceEncodingVariable(entry, sqlNode);

                        }
                        else if (entry.startsWith(Globals.TEMP_VAR_PREFIX)) {

                            String origVarName = entry.substring(Globals.TEMP_VAR_PREFIX.length());
                            if(origVarName.contains(Globals.PROP_ANNOT_KEY_PREFIX)){
                                String[] varNameParts = origVarName.split(Globals.PROP_ANNOT_KEY_PREFIX);
                                this.rewriter.insertAfter(returnStatementCtx.getStop(), ", CASE WHEN "+varNameParts[0]+"."+varNameParts[1] +" IS NULL THEN \""
                                        + Globals.EXTERNAL_VAR_VALUE + "\" ELSE "
                                        +Globals.ID_FUNCTION+"(" + varNameParts[0] + ")+\"." + varNameParts[1] + "\" END AS " + Globals.VAR_PREFIX + origVarName);

                            }else if(origVarName.contains(Globals.LBL_ANNOT_KEY_PREFIX)){
                                String mainName = origVarName.substring(+Globals.NODE_ANNOT_PREFIX.length());
                                String[] varNameParts = mainName.split(Globals.LBL_ANNOT_KEY_PREFIX);
                                if(origVarName.startsWith(Globals.NODE_ANNOT_PREFIX)) {
                                    this.rewriter.insertAfter(returnStatementCtx.getStop(), ", CASE WHEN \"" + varNameParts[1] + "\" IN labels(" + varNameParts[0] + ") THEN " + Globals.ID_FUNCTION + "(" + varNameParts[0] + ")+\":" + varNameParts[1] + "\" ELSE \""
                                            + Globals.EXTERNAL_VAR_VALUE + "\" END AS " + Globals.VAR_PREFIX + mainName);
                                }else{
                                    this.rewriter.insertAfter(returnStatementCtx.getStop(), ", CASE WHEN \"" + varNameParts[1] + "\"=type(" + varNameParts[0] + ") THEN " + Globals.ID_FUNCTION + "(" + varNameParts[0] + ")+\":" + varNameParts[1] + "\" ELSE \""
                                            + Globals.EXTERNAL_VAR_VALUE + "\" END AS " + Globals.VAR_PREFIX + mainName);
                                }
                            }else{
                                this.rewriter.insertAfter(returnStatementCtx.getStop(), ", "+Globals.ID_FUNCTION+"(" + origVarName +") AS "+  Globals.VAR_PREFIX + origVarName);
                            }
                            getSQLAST().updateWhyProvenanceEncodingVariable(entry, sqlNode);

                        }else{

                            if (entry.startsWith(Globals.PATH_PREFIX)) {
                                this.rewriter.insertAfter(returnStatementCtx.getStop(), ", "+ entry);
                            }
                            else {
                                if(entry.contains(Globals.NODE_ANNOT_PREFIX) ){
                                    String[] varNameParts = entry.split(Globals.NODE_ANNOT_PREFIX);
                                    entry = varNameParts[0]+varNameParts[1];
                                }else if(entry.contains(Globals.EDGE_ANNOT_PREFIX) ){
                                    String[] varNameParts = entry.split(Globals.EDGE_ANNOT_PREFIX);
                                    entry = varNameParts[0]+varNameParts[1];
                                }
                                this.rewriter.insertAfter(returnStatementCtx.getStop(), ", "+  entry);
                            }
                        }
                    }
                }

                Set<String> externalProvenanceEncodings = null;

                if(sqlNode instanceof SQLRenameNode node){
                    externalProvenanceEncodings = new HashSet<>(node.getExternalProvenanceEncodings());
                }else if(sqlNode instanceof SQLProjectNode node){
                    externalProvenanceEncodings = new HashSet<>(node.getExternalProvenanceEncodings());
                }

                if(externalProvenanceEncodings != null) {
                    for (String entry : externalProvenanceEncodings) {

                        if (entry.startsWith(Globals.TEMP_PATH_PREFIX) && (!provenanceEncodings.contains(entry))) {

                            String origVarName = entry.substring(Globals.TEMP_PATH_PREFIX.length());

                            if(!provenanceEncodings.contains(origVarName)){
                                if(origVarName.startsWith(Globals.PATH_PREFIX)) {
                                    this.rewriter.insertAfter(returnStatementCtx.getStop(), ", \"" + Globals.EXTERNAL_VAR_VALUE + "\" AS " + origVarName);
                                }else{
                                    this.rewriter.insertAfter(returnStatementCtx.getStop(), ", \"" + Globals.EXTERNAL_VAR_VALUE + "\" AS " + Globals.PATH_PREFIX +origVarName);
                                }
                            }
                        }else if (entry.startsWith(Globals.TEMP_VAR_LIST_PREFIX)) {

                            String origVarName = entry.substring(Globals.TEMP_VAR_LIST_PREFIX.length());
                            String mainName = origVarName;
                            if(origVarName.contains(Globals.NODE_ANNOT_PREFIX) || origVarName.contains(Globals.EDGE_ANNOT_PREFIX) ){
                                mainName = origVarName.substring(Globals.EDGE_ANNOT_PREFIX.length());
                            }

                            if((!provenanceEncodings.contains(entry)) && (!provenanceEncodings.contains( Globals.VAR_PREFIX + origVarName))) {
                                if (origVarName.contains(Globals.PROP_ANNOT_KEY_PREFIX)|| origVarName.contains(Globals.LBL_ANNOT_KEY_PREFIX)) {
                                    this.rewriter.insertAfter(returnStatementCtx.getStop(), ", \"" + Globals.EXTERNAL_VAR_VALUE + "\" AS " + Globals.VAR_PREFIX + mainName);
                                }
                            }

                        }
                        else if (entry.startsWith(Globals.TEMP_VAR_PREFIX)) {

                            String origVarName = entry.substring(Globals.TEMP_VAR_PREFIX.length());
                            String mainName = origVarName;
                            if(origVarName.contains(Globals.NODE_ANNOT_PREFIX) || origVarName.contains(Globals.EDGE_ANNOT_PREFIX) ){
                                mainName = origVarName.substring(Globals.EDGE_ANNOT_PREFIX.length());
                            }

                            if((!provenanceEncodings.contains(entry)) && (!provenanceEncodings.contains( Globals.VAR_PREFIX + origVarName))) {
                                if (origVarName.contains(Globals.PROP_ANNOT_KEY_PREFIX)|| origVarName.contains(Globals.LBL_ANNOT_KEY_PREFIX)) {
                                    this.rewriter.insertAfter(returnStatementCtx.getStop(), ", \"" + Globals.EXTERNAL_VAR_VALUE + "\" AS " + Globals.VAR_PREFIX + mainName);
                                }
                            }
                        }else{

                            if (entry.startsWith(Globals.PATH_PREFIX) && !provenanceEncodings.contains(entry)) {
                                this.rewriter.insertAfter(returnStatementCtx.getStop(), ", \"" + Globals.EXTERNAL_VAR_VALUE + "\" AS " + entry);
                            }else {
                                String origVarName = entry.substring(Globals.VAR_PREFIX.length());
                                String mainName = origVarName;
                                if(origVarName.contains(Globals.NODE_ANNOT_PREFIX) || origVarName.contains(Globals.EDGE_ANNOT_PREFIX) ){
                                    mainName = origVarName.substring(Globals.NODE_ANNOT_PREFIX.length());
                                }

                                if((!provenanceEncodings.contains(entry))) {
                                    if (origVarName.contains(Globals.PROP_ANNOT_KEY_PREFIX)|| origVarName.contains(Globals.LBL_ANNOT_KEY_PREFIX)) {
                                        this.rewriter.insertAfter(returnStatementCtx.getStop(), ", \"" + Globals.EXTERNAL_VAR_VALUE + "\" AS " + Globals.VAR_PREFIX + mainName);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }else if(processStage.equals(Globals.ProcessStage.REWRITE_WHERE_PROVENANCE)){
            if (ctx.primitiveResultStatement() != null) {

                SQLNode sqlNode = sqlNodes.get(ctx);

                Map<String, String> provenanceEncodings = null;

                if(sqlNode instanceof SQLRenameNode node){
                    provenanceEncodings = new HashMap<>(node.getWhereProvenanceEncodings());
                }else if(sqlNode instanceof SQLProjectNode node){
                    provenanceEncodings = new HashMap<>(node.getWhereProvenanceEncodings());
                }

                GQLParser.ReturnStatementContext returnStatementCtx = ctx.primitiveResultStatement().returnStatement();


                if(provenanceEncodings != null){
                    for (Map.Entry<String, String> entry : provenanceEncodings.entrySet()) {

                        if (entry.getValue().startsWith(Globals.TEMP_PATH_PREFIX)) {

                            String origVarName = entry.getValue().substring(Globals.TEMP_PATH_PREFIX.length());

                            if(origVarName.startsWith(Globals.PATH_PREFIX)){
                                this.rewriter.insertAfter(returnStatementCtx.getStop(), ", [x IN nodes("+origVarName+") | "+Globals.ID_FUNCTION+"(x)] + [r IN relationships("+origVarName+") | "+Globals.ID_FUNCTION+"(r)] AS "+ Globals.VAR_PREFIX + entry.getKey());
                            }else{
                                this.rewriter.insertAfter(returnStatementCtx.getStop(), ", [x IN nodes("+origVarName+") | "+Globals.ID_FUNCTION+"(x)] + [r IN relationships("+origVarName+") | "+Globals.ID_FUNCTION+"(r)] AS "+ Globals.VAR_PREFIX + entry.getKey());
                            }
                            getSQLAST().updateWhyProvenanceEncodingVariable(entry.getKey(), sqlNode);
                        }else if (entry.getValue().startsWith(Globals.TEMP_VAR_LIST_PREFIX)) {

                            String origVarName = entry.getValue().substring(Globals.TEMP_VAR_LIST_PREFIX.length());
                            String name = origVarName;
                            if(origVarName.contains(Globals.PROP_ANNOT_KEY_PREFIX)){
                                String[] varNameParts = origVarName.split(Globals.PROP_ANNOT_KEY_PREFIX);
                                this.rewriter.insertAfter(returnStatementCtx.getStop(), ", [x IN "+varNameParts[0]+" | CASE WHEN x."+varNameParts[1]+" IS NULL THEN \""+Globals.EXTERNAL_VAR_VALUE+"\" ELSE "
                                        +Globals.ID_FUNCTION+"(x)+\"." + varNameParts[1] + "\" END] AS "  + Globals.VAR_PREFIX + origVarName);
                            }else if(origVarName.contains(Globals.LBL_ANNOT_KEY_PREFIX)){
                                String mainName = origVarName.substring(+Globals.NODE_ANNOT_PREFIX.length());
                                String[] varNameParts = mainName.split(Globals.LBL_ANNOT_KEY_PREFIX);
                                if(origVarName.startsWith(Globals.NODE_ANNOT_PREFIX)){
                                    this.rewriter.insertAfter(returnStatementCtx.getStop(), ", [x IN "+varNameParts[0]+" | CASE WHEN \""+varNameParts[1]+"\" IN labels(x)) THEN "
                                            +Globals.ID_FUNCTION+"(x)+\":" + varNameParts[1] + "\" ELSE \""+Globals.EXTERNAL_VAR_VALUE+"\" END] AS "+  Globals.VAR_PREFIX + mainName);
                                }else{
                                    this.rewriter.insertAfter(returnStatementCtx.getStop(), ", [x IN "+varNameParts[0]+" | CASE WHEN \""+varNameParts[1]+"\"=type(x) THEN "
                                            +Globals.ID_FUNCTION+"(x)+\":" + varNameParts[1] + "\" ELSE \""+Globals.EXTERNAL_VAR_VALUE+"\" END] AS "+  Globals.VAR_PREFIX + mainName);
                                }
                                name = mainName;

                            }else{
                                this.rewriter.insertAfter(returnStatementCtx.getStop(), ", [x IN "+origVarName+" | "+Globals.ID_FUNCTION+"(x)] AS "+  Globals.VAR_PREFIX + origVarName);
                            }

                            getSQLAST().updateWhyProvenanceEncodingVariable(name, sqlNode);
                        }
                        else if (entry.getValue().startsWith(Globals.TEMP_VAR_PREFIX)) {

                            String origVarName = entry.getValue().substring(Globals.TEMP_VAR_PREFIX.length());
                            String name = origVarName;
                            if(origVarName.contains(Globals.PROP_ANNOT_KEY_PREFIX)){
                                String[] varNameParts = origVarName.split(Globals.PROP_ANNOT_KEY_PREFIX);
                                String newName = entry.getKey();
                                if(entry.getKey().contains(".")){
                                    String[] propNameParts = entry.getKey().split("\\.");
                                    newName = propNameParts[0]+"_"+propNameParts[1];
                                }

                                this.rewriter.insertAfter(returnStatementCtx.getStop(), ", CASE WHEN "+varNameParts[0]+"."+varNameParts[1] +" IS NULL THEN \""
                                        + Globals.EXTERNAL_VAR_VALUE + "\" ELSE "
                                        +Globals.ID_FUNCTION+"(" + varNameParts[0] + ")+\"." + varNameParts[1] + "\" END AS " + Globals.VAR_PREFIX + newName);
                                name = newName;

                            }else if(origVarName.contains(Globals.LBL_ANNOT_KEY_PREFIX)){
                                String mainName = origVarName.substring(Globals.NODE_ANNOT_PREFIX.length());
                                String[] varNameParts = mainName.split(Globals.LBL_ANNOT_KEY_PREFIX);
                                String newName = entry.getKey();
                                if(entry.getKey().contains(":")){
                                    String[] propNameParts = entry.getKey().split(":");
                                    newName = propNameParts[0]+"_"+propNameParts[1];
                                }

                                if(origVarName.startsWith(Globals.NODE_ANNOT_PREFIX)) {
                                    this.rewriter.insertAfter(returnStatementCtx.getStop(), ", CASE WHEN \"" + varNameParts[1] + "\" IN labels(" + varNameParts[0] + ") THEN " + Globals.ID_FUNCTION + "(" + varNameParts[0] + ")+\":" + varNameParts[1] + "\" ELSE \""
                                            + Globals.EXTERNAL_VAR_VALUE + "\" END AS " + Globals.VAR_PREFIX + newName);
                                }else{
                                    this.rewriter.insertAfter(returnStatementCtx.getStop(), ", CASE WHEN \"" + varNameParts[1] + "\"=type(" + varNameParts[0] + ") THEN " + Globals.ID_FUNCTION + "(" + varNameParts[0] + ")+\":" + varNameParts[1] + "\" ELSE \""
                                            + Globals.EXTERNAL_VAR_VALUE + "\" END AS " + Globals.VAR_PREFIX + newName);
                                }
                                name= newName;
                            }else{

                                this.rewriter.insertAfter(returnStatementCtx.getStop(), ", "+Globals.ID_FUNCTION+"(" + origVarName +") AS "+  Globals.VAR_PREFIX + entry.getKey());
                                name = entry.getKey();
                            }

                            getSQLAST().updateWhereProvenanceEncodingVariable(name, sqlNode);

                        }else{

                            if (entry.getValue().startsWith(Globals.PATH_PREFIX)) {
                                this.rewriter.insertAfter(returnStatementCtx.getStop(), ", "+ entry);
                            }else {

                                String name = entry.getValue();
                                if(name.contains(Globals.NODE_ANNOT_PREFIX) ){
                                    String[] varNameParts = name.split(Globals.NODE_ANNOT_PREFIX);
                                    name = varNameParts[0]+varNameParts[1];
                                }else if(name.contains(Globals.EDGE_ANNOT_PREFIX) ){
                                    String[] varNameParts = name.split(Globals.EDGE_ANNOT_PREFIX);
                                    name = varNameParts[0]+varNameParts[1];
                                }

                                if(name.contains(Globals.PROP_ANNOT_KEY_PREFIX)) {
                                    String[] varNames = name.split(Globals.PROP_ANNOT_KEY_PREFIX);
                                    this.rewriter.insertAfter(returnStatementCtx.getStop(), ", " + Globals.VAR_PREFIX + varNames[1]);
                                }else if(name.contains(Globals.LBL_ANNOT_KEY_PREFIX)){
                                    String[] varNames = name.split(Globals.LBL_ANNOT_KEY_PREFIX);
                                    this.rewriter.insertAfter(returnStatementCtx.getStop(), ", " + Globals.VAR_PREFIX + varNames[1]);
                                }else{
                                    this.rewriter.insertAfter(returnStatementCtx.getStop(), ", " + name);
                                }
                            }
                        }
                    }
                }

//                Set<String> externalProvenanceEncodings = null;
//
//                if(sqlNode instanceof SQLRenameNode node){
//                    externalProvenanceEncodings = new HashSet<>(node.getExternalProvenanceEncodings());
//                }else if(sqlNode instanceof SQLProjectNode node){
//                    externalProvenanceEncodings = new HashSet<>(node.getExternalProvenanceEncodings());
//                }

//                if(externalProvenanceEncodings != null) {
//                    for (String entry : externalProvenanceEncodings) {
//
//                        if (entry.startsWith(Globals.TEMP_PATH_PREFIX) && (!provenanceEncodings.contains(entry))) {
//
//                            String origVarName = entry.substring(Globals.TEMP_PATH_PREFIX.length());
//
//                            if(!provenanceEncodings.contains(origVarName)){
//                                if(origVarName.startsWith(Globals.PATH_PREFIX)) {
//                                    this.rewriter.insertAfter(returnStatementCtx.getStop(), ", \"" + Globals.EXTERNAL_VAR_VALUE + "\" AS " + origVarName);
//                                }else{
//                                    this.rewriter.insertAfter(returnStatementCtx.getStop(), ", \"" + Globals.EXTERNAL_VAR_VALUE + "\" AS " + Globals.PATH_PREFIX +origVarName);
//                                }
//                            }
//                        }
//                        else if (entry.startsWith(Globals.TEMP_VAR_PREFIX)) {
//
//                            String origVarName = entry.substring(Globals.TEMP_VAR_PREFIX.length());
//                            if((!provenanceEncodings.contains(entry)) && (!provenanceEncodings.contains( Globals.VAR_PREFIX + origVarName))) {
//                                if (origVarName.contains(Globals.PROP_ANNOT_KEY_PREFIX)|| origVarName.contains(Globals.LBL_ANNOT_KEY_PREFIX)) {
//                                    this.rewriter.insertAfter(returnStatementCtx.getStop(), ", \"" + Globals.EXTERNAL_VAR_VALUE + "\" AS " + Globals.VAR_PREFIX + origVarName);
//                                }
//                            }
//                        }else{
//
//                            if (entry.startsWith(Globals.PATH_PREFIX) && !provenanceEncodings.contains(entry)) {
//                                this.rewriter.insertAfter(returnStatementCtx.getStop(), ", \"" + Globals.EXTERNAL_VAR_VALUE + "\" AS " + entry);
//                            }else {
//                                String origVarName = entry.substring(Globals.VAR_PREFIX.length());
//
//                                if((!provenanceEncodings.contains(entry))) {
//                                    if (origVarName.contains(Globals.PROP_ANNOT_KEY_PREFIX)|| origVarName.contains(Globals.LBL_ANNOT_KEY_PREFIX)) {
//                                        this.rewriter.insertAfter(returnStatementCtx.getStop(), ", \"" + Globals.EXTERNAL_VAR_VALUE + "\" AS " + Globals.VAR_PREFIX + origVarName);
//                                    }
//                                }
//                            }
//                        }
//                    }
//                }
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
                    sqlNodes.put(statement, new SQLJoin(sqlNodes.get(preStatementContext), sqlNodes.get(statement.callQueryStatement()), null, false));
                }

                preStatementContext = statement;
            } else {

                GQLParser.PrimitiveQueryStatementContext primitiveQueryStatement = statement.primitiveQueryStatement();

                if (primitiveQueryStatement.matchStatement() != null) {
                    if (preStatementContext == null) {
                        sqlNodes.put(statement, sqlNodes.get(primitiveQueryStatement.matchStatement()));
                    } else {
                        sqlNodes.put(statement, new SQLJoin(sqlNodes.get(preStatementContext), sqlNodes.get(primitiveQueryStatement.matchStatement()), new HashSet<>(schemasAndsignatures), false));
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
                        sqlNodes.put(statement, new SQLJoin(sqlNodes.get(preStatementContext), sqlNodes.get(primitiveQueryStatement.letStatement()), null, false));
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
            sqlNodes.put(ctx, new SQLSelectCriteriaNode(ctx.searchCondition().getText(), new HashSet<>(schemasAndsignatures), new HashSet<>(labelsSignature)));
            schemasAndsignatures.clear();
        }
    }

    @Override
    public void exitLetStatement(GQLParser.LetStatementContext ctx) {
        if (processStage.equals(Globals.ProcessStage.SQL_TRANSLATION)) {
            sqlNodes.put(ctx, new SQLRelationNode(ctx.getText(), new HashSet<>(varsInMatchClause), new HashSet<>(schemasAndsignatures), null));
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
                boolean subQueryInnerJoin = true;
                if(initialRelation instanceof SQLEmptyNode || enterNextStatement) {
                    subQueryInnerJoin = false;
                    enterNextStatement = false;
                }
                sqlNodes.put(ctx, new SQLJoin(initialRelation, sqlNodes.get(ctx.graphPatternBindingTable()), new HashSet<>(schemasAndsignatures), subQueryInnerJoin));
            } else {
                sqlNodes.put(ctx, sqlNodes.get(ctx.graphPatternBindingTable()));
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
            int i=0;
            for (GQLParser.PathPatternContext pathPatternContext : pathPatternContexts) {
                i+=1;
                if (from == null) {
                    Set<String> schema = null;
                    if(i==pathPatternContexts.size()) {
                        schema = new HashSet<>(schemasAndsignatures);
                        schemasAndsignatures.clear();
                    }
                    from = new SQLJoin(new SQLEmptyNode(), sqlNodes.get(pathPatternContext), schema, false);
                } else {
                    Set<String> schema = null;
                    if(i==pathPatternContexts.size()) {
                        schema = new HashSet<>(schemasAndsignatures);
                        schemasAndsignatures.clear();
                    }
                    from = new SQLJoin(from, sqlNodes.get(pathPatternContext), schema, false);
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
                        .collect(Collectors.toSet()), sqlNodes.get(subqueryScopes.get(ctx)), null);
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
            processPatternFiller(patternFiller, Globals.NODE_ANNOT_PREFIX);
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
                processPatternFiller(patternFiller, Globals.EDGE_ANNOT_PREFIX);

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
                String tempVar = Globals.TEMP_PATH_PREFIX + newVar;

                schemasAndsignatures.add(tempVar);
                labelsSignature.add(tempVar);
                sqlNodes.put(ctx, new SQLRelationNode(newVar, new HashSet<>(varsInMatchClause), new HashSet<>(schemasAndsignatures), new HashSet<>(labelsSignature)));
            } else {
                String varName = ctx.pathVariableDeclaration().pathVariable().getText();

                schemasAndsignatures.add(Globals.TEMP_PATH_PREFIX + varName);
                labelsSignature.add(Globals.TEMP_PATH_PREFIX + varName);
                sqlNodes.put(ctx, new SQLRelationNode(Globals.PATH_PREFIX + varName, new HashSet<>(varsInMatchClause), new HashSet<>(schemasAndsignatures), new HashSet<>(labelsSignature)));
            }

            varsInMatchClause.clear();
            schemasAndsignatures.clear();
            labelsSignature.clear();
        }
    }

    @Override
    public void enterPfQuantifiedPathPrimary(GQLParser.PfQuantifiedPathPrimaryContext ctx) {

        if(repetitivePathFactorContext==null) repetitivePathFactorContext = ctx;
    }

    @Override
    public void exitPfQuantifiedPathPrimary(GQLParser.PfQuantifiedPathPrimaryContext ctx) {

        if(repetitivePathFactorContext==ctx) repetitivePathFactorContext = null;
    }

    @Override
    public void enterValueExpressionPrimary(GQLParser.ValueExpressionPrimaryContext ctx) {

        if (processStage.equals(Globals.ProcessStage.SQL_TRANSLATION)) {
            // add properties referenced in other clauses than MATCH
            if (ctx.propertyName() != null && ctx.valueExpressionPrimary() != null && ctx.valueExpressionPrimary().bindingVariableReference() != null) {

                String varName = Globals.TEMP_VAR_PREFIX + ctx.valueExpressionPrimary().bindingVariableReference().getText();
                schemasAndsignatures.add(varName+Globals.PROP_ANNOT_KEY_PREFIX + ctx.propertyName().getText());
            }
        }
    }

    private void processPatternFiller(GQLParser.ElementPatternFillerContext patternFiller, String patternType) {

        GQLParser.IsLabelExpressionContext labelsCtx = patternFiller.isLabelExpression();
        GQLParser.ElementPatternPredicateContext predicate = patternFiller.elementPatternPredicate();

        if (labelsCtx != null || predicate != null) {

            String varName;
            String prefix = repetitivePathFactorContext != null? Globals.TEMP_VAR_LIST_PREFIX: Globals.TEMP_VAR_PREFIX;
            if (patternFiller.elementVariableDeclaration() != null) {
                varName = patternFiller.elementVariableDeclaration().elementVariable().getText();
                varsInMatchClause.add(varName);

            } else {
                // add missing variables to pattern
                varName = Globals.ANONYMOUS_VAR_PREFIX + varCounter++;
                this.rewriter.insertBefore(patternFiller.getStart(), varName);
            }
//            schemasAndsignatures.add(prefix + varName);

            // add labels to variable schema
            if (labelsCtx != null) {
                List<String> nodeLabels = getLabelName(labelsCtx.labelExpression());
                for (String lbl : nodeLabels) {
                    labelsSignature.add(prefix+  patternType+ varName+Globals.LBL_ANNOT_KEY_PREFIX + lbl);
                }
            }

            varName = prefix + varName;

            // add properties to variable schema from property specification
            if (predicate != null) {
                if (predicate.elementPropertySpecification() != null) {
                    List<GQLParser.PropertyKeyValuePairContext> propertyKeyValuePairs = predicate.elementPropertySpecification().propertyKeyValuePairList().propertyKeyValuePair();

                    for (GQLParser.PropertyKeyValuePairContext keyValuePair : propertyKeyValuePairs) {
                        schemasAndsignatures.add(varName+Globals.PROP_ANNOT_KEY_PREFIX + keyValuePair.propertyName().getText());
                    }
                }

            }
        } else if (patternFiller.elementVariableDeclaration() != null) {
            String varName = patternFiller.elementVariableDeclaration().elementVariable().getText();
//            String prefix = repetitivePathFactorContext != null? Globals.TEMP_VAR_LIST_PREFIX : Globals.TEMP_VAR_PREFIX ;
            varsInMatchClause.add(varName);
//            schemasAndsignatures.add( prefix + varName);
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

        System.out.println(this.rewriter.getText());
        return this.rewriter.getText();
    }
}
