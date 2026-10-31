package com.synexia.mindex.experiment;

import java.util.Objects;
import java.util.function.IntFunction;
import java.util.function.IntSupplier;

/** A stable token namespace; existing IDs must never be rebound to different text. */
public interface TokenSpace {
    Object namespace();
    int size();
    String token(int id);

    /** Adapts an existing vocabulary without reflection, copying, or renumbering. */
    static TokenSpace adapt(Object namespace, IntSupplier size, IntFunction<String> token) {
        Objects.requireNonNull(namespace, "namespace");
        Objects.requireNonNull(size, "size");
        Objects.requireNonNull(token, "token");
        return new TokenSpace() {
            public Object namespace() { return namespace; }
            public int size() { return size.getAsInt(); }
            public String token(int id) { return token.apply(id); }
        };
    }
}
