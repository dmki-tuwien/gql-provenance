package org.pgprov.ast;

import org.pgprov.Globals;
import org.pgprov.graph.model.Path;
import org.pgprov.graph.model.Entity;

import java.util.*;

public class SQLRelationNode extends SQLNode {

    private final String relation;
    private final Set<String> columns;
    private final Map<String, List<String>> schemaAndSignatures;

    public SQLRelationNode(String relation, Set<String> columns,
                           Map<String, List<String>> schemaAndSignatures) {

        this.relation = relation;
        this.columns = columns;
        this.schemaAndSignatures = schemaAndSignatures;
    }

    @Override
    public String toString(int indent) {
        return "SQLRelationNode[relation=" + relation
                + ", variables=" + this.columns
                + "]";
    }

    @Override
    public Set<String> getOriginalReturnVars() {
        return columns;
    }

    @Override
    public Set<String> getReturnVarsForRewriting() {
        return schemaAndSignatures.keySet();
    }

    @Override
    public void updateVarInSchemaAndSignatures(String varName) {

        if (schemaAndSignatures.containsKey(varName)) {
            List<String> entry = schemaAndSignatures.remove(varName);

            String newVar = varName;
            if (varName.startsWith(Globals.TEMP_VAR_PREFIX)) {
                newVar = Globals.VAR_PREFIX + varName.substring(Globals.TEMP_VAR_PREFIX.length());
            } else if (varName.startsWith(Globals.TEMP_PATH_PREFIX)) {
                newVar = Globals.PATH_PREFIX + varName.substring(Globals.TEMP_PATH_PREFIX.length());
            }

            schemaAndSignatures.put(newVar, entry);


        }
    }

    public void updateSchemaAndSignatures(Map<String, List<String>> varSchemaAndSignatures) {

        for (String var : varSchemaAndSignatures.keySet()) {
            schemaAndSignatures
                    .computeIfAbsent(var, k -> new ArrayList<>())
                    .addAll(varSchemaAndSignatures.get(var));
        }
    }

    @Override
    public Set<Set<String>> calculateWhyProv(Map<String, Object> row) {

        Set<String> whyProvenance = new HashSet<>();
        if (row.containsKey(relation) && !(row.get(relation).equals(Globals.EXTERNAL_VAR_VALUE))) {

            Path path = (Path) row.get(relation);

            if (path != null) {
                // construct the path induced graph
                for (Entity entity : path) {
                    whyProvenance.add((String) entity.getAnnotation());
                }
            }

                for (String key : schemaAndSignatures.keySet()) {

                if (!key.startsWith(Globals.PATH_PREFIX) && row.containsKey(key) && !(row.get(key).equals(Globals.EXTERNAL_VAR_VALUE))) {

                    if (row.get(key) instanceof List<?> list) {
                        for (Object entity : list) {
                            List<String> varSchema = schemaAndSignatures.get(key);
                            appendEntityAnnotations(varSchema, (Entity) entity, whyProvenance);
                        }
                    } else {
                        Entity entity = (Entity) row.get(key);
                        List<String> varSchema = schemaAndSignatures.get(key);
                        appendEntityAnnotations(varSchema, entity, whyProvenance);
                    }

                }
            }
        }

        Set<Set<String>> whyProv = new HashSet<>();
        if (!whyProvenance.isEmpty()) {
            whyProv.add(whyProvenance);
        }
        return whyProv;
    }

    @Override
    public String calculateHowProv(Map<String, Object> row) {

        StringBuilder provenance = new StringBuilder();

        Object relValue = row.get(relation);
        if (relValue != null && !relValue.equals(Globals.EXTERNAL_VAR_VALUE)) {

            Map<String, Set<String>> varsAnnotations = new HashMap<>();

            for (String key : schemaAndSignatures.keySet()) {
                if (key.startsWith(Globals.PATH_PREFIX)) {
                    continue;
                }

                Object value = row.get(key);
                if (value != null && !value.equals(Globals.EXTERNAL_VAR_VALUE)) {


                    if (value instanceof List<?> list) {
                        for (Object entity : list) {

                            Set<String> varSchemaAnn = new HashSet<>();
                            List<String> varSchema = schemaAndSignatures.get(key);
                            appendEntityAnnotations(varSchema, (Entity) entity, varSchemaAnn);

                            String entityAnn = (String) ((Entity) entity).getAnnotation();

                            varsAnnotations
                                    .computeIfAbsent(entityAnn, k -> new HashSet<>())
                                    .addAll(varSchemaAnn);
                        }
                    } else {
                        Entity entity = (Entity) value;
                        Set<String> varSchemaAnn = new HashSet<>();
                        List<String> varSchema = schemaAndSignatures.get(key);
                        appendEntityAnnotations(varSchema, entity, varSchemaAnn);

                        String entityAnn = (String) entity.getAnnotation();

                        varsAnnotations
                                .computeIfAbsent(entityAnn, k -> new HashSet<>())
                                .addAll(varSchemaAnn);
                    }


                }
            }

            Path path = (Path) row.get(relation);

            if (path != null) {

                boolean first = true;
                // construct the path induced graph
                for (Entity entity : path) {
                    String entityAnn = (String) entity.getAnnotation();

                    if (!first) {
                        provenance.append(" x ");
                    }
                    first = false;

                    Set<String> varSchemaAnns = varsAnnotations.get(entityAnn);
                    if (varSchemaAnns != null && !varSchemaAnns.isEmpty()) {
                        provenance.append("(").append(entityAnn).append("+").append(String.join("+", varSchemaAnns)).append(")");
                    } else {
                        provenance.append(entityAnn);
                    }

                }
            }
        }

        return provenance.toString();
    }

    private Set<String> appendEntityAnnotations(List<String> varSchema, Entity entity, Set<String> whyProvenance) {
        // construct the varschema for entity

        if(varSchema != null) {
            for (String attr : varSchema) {
                String origAttr = extractOrigAttr(attr);
                if (attr.startsWith(Globals.PROP_ANNOT_KEY_PREFIX)) {
                    String ann = (String) entity.getPropertyAnnotation(origAttr);
                    if (ann != null) {
                        whyProvenance.add(ann);
                    }
                } else if (attr.startsWith(Globals.LBL_ANNOT_KEY_PREFIX)) {
                    String ann = (String) entity.getLabelAnnotation(origAttr);
                    if (ann != null) {
                        whyProvenance.add(ann);
                    }
                }
            }
        }
        return whyProvenance;
    }

    private String extractOrigAttr(String attr) {
        int idx = -1;
        for (int i = 0; i < 3; i++) {
            idx = attr.indexOf('_', idx + 1);
        }

        return attr.substring(idx + 1);
    }
}
