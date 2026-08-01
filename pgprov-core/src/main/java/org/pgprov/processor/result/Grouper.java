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
    private final boolean edgeMinimality;

    public Grouper(SQLNode ast, BiFunction<S, SQLNode, R> rowFactory, boolean edgeMinimality) {
        this.sqlNode = ast;
        this.rowFactory = rowFactory;
        this.edgeMinimality = edgeMinimality;
    }

    public Stream<R> process(Stream<S> resultStream) {

        Map<Integer, R> grouped = new LinkedHashMap<>();

        resultStream
                .map(row -> handleRow(row, sqlNode))
                .forEach(row -> {
                    int key = row.hashCode();
                    grouped.merge(
                            key,
                            row,
                            (existing, incoming) -> {
                                existing.mergeProvenance(incoming, this.edgeMinimality);
                                return existing;
                            }
                    );
                });

        return grouped.values().stream();
    }

    private R handleRow(S row, SQLNode sqlNode) {
        Map<String, Object> rowContext = new LinkedHashMap<>();
        rowContext.put("row", row);
        rowContext.put("edgeMinimality", this.edgeMinimality);
        return rowFactory.apply((S) rowContext, sqlNode);
    }
}
