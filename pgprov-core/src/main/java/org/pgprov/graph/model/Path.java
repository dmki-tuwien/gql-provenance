package org.pgprov.graph.model;

import java.util.Iterator;
import java.util.List;

public class Path implements Iterable<Entity> {
    private final List<Entity> elements;

    public Path(List<Entity> elements) {
        this.elements = List.copyOf(elements); // defensive copy
    }

    @Override
    public Iterator<Entity> iterator() {
        return elements.iterator();
    }

    public int length() {
        return elements.size();
    }

    public Entity get(int index) {
        return elements.get(index);
    }

    public List<Entity> asList() {
        return elements;
    }
}
