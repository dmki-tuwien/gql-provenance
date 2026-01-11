package org.pgprov.graph.model;

public interface Entity {

    String getElementId();

    boolean hasProperty(String propertyName);

    boolean hasLabel(String label);

    Object getProperty(String propertyName);

    Object getAnnotation();

    Object getTypedAnnotation(String typeName);

    Object getPropertyAnnotation(String propertyName);

    Object getPropertyTypedAnnotation(String propertyName, String typeName);

    Object getLabelAnnotation(String labelName);

    Object getLabelTypedAnnotation(String labelName, String typeName);


}
