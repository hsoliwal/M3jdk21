/*
 * Copyright 2026 Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.jackson;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Caller-owned prepared Jackson binding. The existing immutable ObjectReader
 * and ObjectWriter retain the selected type and immutable databind configuration;
 * no global cache or compiler objects are retained. Their JsonFactory and other
 * external collaborators remain shared according to Jackson's normal contract.
 * Finish configuring the mapper and factory before preparing a binding, and do
 * not mutate shared collaborators while using it. Custom serializers keep their
 * upstream thread safety and external-state obligations.
 */
public final class M3Binding<T> {
    private final JavaType type;
    private final ObjectReader reader;
    private final ObjectWriter writer;

    M3Binding(JavaType type, ObjectReader reader, ObjectWriter writer) {
        this.type = type;
        this.reader = reader;
        this.writer = writer;
    }

    public JavaType type() { return type; }
    public ObjectReader reader() { return reader; }
    public ObjectWriter writer() { return writer; }

    public T readValue(M3File source) throws IOException {
        try (var input = Objects.requireNonNull(source, "source").reader()) {
            return reader.readValue(input);
        }
    }

    public T readValue(String source) throws IOException {
        return reader.readValue(Objects.requireNonNull(source, "source"));
    }

    public T readValue(Path source) throws IOException {
        try (var input = Files.newInputStream(Objects.requireNonNull(source, "source"))) {
            return reader.readValue(input);
        }
    }

    public String writeValueAsString(T value) throws IOException {
        return writer.writeValueAsString(value);
    }

    public void writeValue(Path destination, T value) throws IOException {
        try (var output = Files.newOutputStream(Objects.requireNonNull(destination, "destination"))) {
            writer.writeValue(output, value);
        }
    }
}
