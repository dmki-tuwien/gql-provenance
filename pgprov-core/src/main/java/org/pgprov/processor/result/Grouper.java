package org.pgprov.processor.result;

import org.pgprov.ast.SQLNode;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Stream;

public class Grouper<S, T, R extends ResultRow<S, T>> {

    // S : Input Row Type
    // T : Provenance Data Type
    // R : Provenance Result Row Type
    private final SQLNode sqlNode;
    private final BiFunction<S, SQLNode, R> rowFactory;

    public Grouper(SQLNode ast, BiFunction<S, SQLNode, R> rowFactory) {
        this.sqlNode = ast;
        this.rowFactory = rowFactory;
    }

    public Stream<R> process(Stream<S> resultStream) {

        Map<Integer, R> grouped = new LinkedHashMap<>();

        resultStream
                .map(row -> rowFactory.apply(row, sqlNode))
                .forEach(row -> {
                    int key = row.hashCode();
                    grouped.merge(
                            key,
                            row,
                            (existing, incoming) -> {
                                existing.mergeProvenance(incoming);
                                return existing;
                            }
                    );
                });

        return grouped.values().stream();
    }
}
