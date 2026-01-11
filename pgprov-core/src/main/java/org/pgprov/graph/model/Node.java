package org.pgprov.graph.model;

import org.pgprov.Globals;

import java.util.Map;
import java.util.Set;

public class Node implements Entity {

    String elementId;
    Set<String> labels;
    Map<String, Object> properties;


    public Node(String elementId, Set<String> labels, Map<String, Object> properties) {
        this.elementId = elementId;
        this.labels = labels;
        this.properties = properties;
    }

    @Override
    public String getElementId() {
        return elementId;
    }

    @Override
    public boolean hasProperty(String propertyName) {
        return properties.containsKey(propertyName);
    }

    @Override
    public boolean hasLabel(String label) {
        return labels.contains(label);
    }

    @Override
    public Object getProperty(String propertyName) {
        return properties.get(propertyName);
    }

    @Override
    public Object getAnnotation() {
        String key = Globals.NODE_ANNOT_KEY;

        if (properties.containsKey(key)) {
            return properties.get(key);
        }
        throw new RuntimeException("Provenance annotations not found");
    }

    @Override
    public Object getTypedAnnotation(String typeName) {

        String key = Globals.EDGE_ANNOT_KEY + typeName;

        if (properties.containsKey(key))
            return properties.get(key);
        throw new RuntimeException(typeName + " typed provenance annotations not found");
    }

    @Override
    public Object getPropertyAnnotation(String propertyName) {

        String key = Globals.PROP_ANNOT_KEY_PREFIX + propertyName;

        if (properties.containsKey(key)) {
            return properties.get(key);
        } else if (properties.containsKey(propertyName)) {

            throw new RuntimeException("Provenance annotations not found");
        }
        return null;
    }

    @Override
    public Object getPropertyTypedAnnotation(String propertyName, String typeName) {

        String key = Globals.PROP_ANNOT_KEY_PREFIX + propertyName + "_" + typeName;

        if (properties.containsKey(key)) {
            return properties.get(key);
        } else if (properties.containsKey(propertyName)) {
            throw new RuntimeException(typeName + " typed provenance annotations not found");
        }
        return null;
    }

    @Override
    public Object getLabelAnnotation(String labelName) {

        String key = Globals.LBL_ANNOT_KEY_PREFIX + labelName;

        if (properties.containsKey(key)) {
            return properties.get(key);
        } else if (labels.contains(labelName)) {
            throw new RuntimeException("Provenance annotations not found");
        }
        return null;
    }

    @Override
    public Object getLabelTypedAnnotation(String labelName, String typeName) {

        String key = Globals.LBL_ANNOT_KEY_PREFIX + labelName + "_" + typeName;

        if (properties.containsKey(key)) {
            return properties.get(key);
        } else if (labels.contains(labelName)) {
            throw new RuntimeException(typeName + " typed provenance annotations not found");
        }
        return null;
    }
}
