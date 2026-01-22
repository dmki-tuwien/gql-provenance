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

        for (Map.Entry<String, List<String>> entry : varSchemaAndSignatures.entrySet()) {
            schemaAndSignatures
                    .computeIfAbsent(entry.getKey(), k -> new ArrayList<>())
                    .addAll(entry.getValue());
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

            for (Map.Entry<String, List<String>> schemaSet : schemaAndSignatures.entrySet()) {
                String key = schemaSet.getKey();
                if(key.startsWith(Globals.PATH_PREFIX) && !row.containsKey(key)) continue;

                Object value = row.get(key);
                if (value == null ||value.equals(Globals.EXTERNAL_VAR_VALUE)) {
                    continue;
                }

                List<String> varSchema = schemaSet.getValue();
                if (row.get(key) instanceof List<?> list) {
                    for (Object entity : list) {
                        appendEntityAnnotations(varSchema, (Entity) entity, whyProvenance);
                    }
                } else if(value instanceof Entity entity) {
                    appendEntityAnnotations(varSchema, entity, whyProvenance);
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
        if (relValue == null){
            return provenance.toString();
        }else if(relValue.equals(Globals.EXTERNAL_VAR_VALUE)){
            return provenance.toString();
        }

        Map<String, Set<String>> varsAnnotations = new HashMap<>();

        for (Map.Entry<String, List<String>> entry : schemaAndSignatures.entrySet()) {

            System.out.println("My schema"+ schemaAndSignatures);
            String key = entry.getKey();

            if (key.startsWith(Globals.PATH_PREFIX)) {
                continue;
            }

            Object value = row.get(key);
            if (value == null ||value.equals(Globals.EXTERNAL_VAR_VALUE)) {
                continue;
            }

            List<String> varSchema = entry.getValue();

            if (value instanceof List<?> list) {
                for (Object entity : list) {
                    collectAnnotations(varSchema, (Entity)entity, varsAnnotations);
                }
            } else {
                collectAnnotations(varSchema, (Entity) value, varsAnnotations);
            }

            System.out.println("Var annotations"+ varsAnnotations);
            Path path = (Path) relValue;
            boolean first = true;
            // construct the path induced graph
            for (Entity entity : path) {

                if (!first) {
                    provenance.append(" x ");
                }
                first = false;

                String entityAnn = (String) entity.getAnnotation();

                Set<String> varSchemaAnns = varsAnnotations.get(entityAnn);
                if (varSchemaAnns != null && !varSchemaAnns.isEmpty()) {
                    provenance.append("(").append(entityAnn).append("+").append(String.join("+", varSchemaAnns)).append(")");
                } else {
                    provenance.append(entityAnn);
                }
            }
        }

        return provenance.toString();
    }

    private void collectAnnotations(List<String> varSchema, Entity entity, Map<String, Set<String>> varsAnnotations) {
        // construct the varschema for entity

        if (varSchema == null || varSchema.isEmpty() || entity == null) return;

        String entityAnn = (String) entity.getAnnotation();
        Set<String> target = varsAnnotations
                .computeIfAbsent(entityAnn, k -> new HashSet<>());
        appendEntityAnnotations(varSchema, entity, target);
    }

    private void appendEntityAnnotations(List<String> varSchema, Entity entity, Set<String> whyProvenance) {
        // construct the varschema for entity

        if (varSchema == null || varSchema.isEmpty() || entity == null) return;

        for (String attr : varSchema) {
            String origAttr = extractOrigAttr(attr);
            String ann = null;

            if (attr.startsWith(Globals.PROP_ANNOT_KEY_PREFIX)) {
                ann = (String) entity.getPropertyAnnotation(origAttr);
            } else if (attr.startsWith(Globals.LBL_ANNOT_KEY_PREFIX)) {
                ann = (String) entity.getLabelAnnotation(origAttr);
            }

            if(ann!=null){
                whyProvenance.add(ann);
            }
        }

    }

    private String extractOrigAttr(String attr) {
        int idx = -1;
        for (int i = 0; i < 3; i++) {
            idx = attr.indexOf('_', idx + 1);
        }

        return attr.substring(idx + 1);
    }
}
