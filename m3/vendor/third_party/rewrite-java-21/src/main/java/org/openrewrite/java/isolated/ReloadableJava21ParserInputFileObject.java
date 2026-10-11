// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/java/isolated/ReloadableJava21ParserInputFileObject.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
/*
 * Copyright 2023 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.java.isolated;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaParser;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.NestingKind;
import javax.tools.JavaFileObject;
import java.io.*;
import java.net.URI;
import java.nio.file.Path;

/**
 * So that {@link JavaParser} can ingest source files from {@link InputStream} sources
 * other than a file on disk.
 */
public class ReloadableJava21ParserInputFileObject implements JavaFileObject {
    @Nullable
    private final Path path;
    private final Parser.Input input;
    private final ExecutionContext ctx;

    public ReloadableJava21ParserInputFileObject(Parser.Input input, ExecutionContext ctx) {
        this.input = input;
        this.path = input.getPath();
        this.ctx = ctx;
    }

    @Override
    public URI toUri() {
        if (path == null) {
            //noinspection ConstantConditions
            return null;
        }
        return path.toUri();
    }

    @Override
    public String getName() {
        if (path == null) {
            //noinspection ConstantConditions
            return null;
        }
        return path.toString();
    }

    @Override
    public InputStream openInputStream() {
        return input.getSource(ctx);
    }

    @Override
    public OutputStream openOutputStream() {
        throw new UnsupportedOperationException("Should be no need to write output to this file");
    }

    @Override
    public Reader openReader(boolean ignoreEncodingErrors) {
        return new InputStreamReader(input.getSource(ctx));
    }

    @Override
    public CharSequence getCharContent(boolean ignoreEncodingErrors) {
        return input.getSource(ctx).readFully();
    }

    @Override
    public Writer openWriter() {
        throw new UnsupportedOperationException("Should be no need to write output to this file");
    }

    @Override
    public long getLastModified() {
        return 0;
    }

    @Override
    public boolean delete() {
        return false;
    }

    @Override
    public Kind getKind() {
        return Kind.SOURCE;
    }

    @Override
    public boolean isNameCompatible(String simpleName, Kind kind) {
        String baseName = simpleName + kind.extension;
        return kind.equals(getKind()) && path.getFileName().toString().equals(baseName);
    }

    @Override
    public NestingKind getNestingKind() {
        return NestingKind.TOP_LEVEL;
    }

    @Override
    public Modifier getAccessLevel() {
        return Modifier.PUBLIC;
    }

    @Override
    public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
        if (o == this) return true;
        if (!(o instanceof ReloadableJava21ParserInputFileObject)) return false;
        final ReloadableJava21ParserInputFileObject other = (ReloadableJava21ParserInputFileObject) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$path = this.path;
        final Object other$path = other.path;
        if (this$path == null ? other$path != null : !this$path.equals(other$path)) return false;
        return true;
    }

    protected boolean canEqual(@org.openrewrite.internal.lang.Nullable final Object other) {
        return other instanceof ReloadableJava21ParserInputFileObject;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $path = this.path;
        result = result * PRIME + ($path == null ? 43 : $path.hashCode());
        return result;
    }

    public Parser.Input getInput() {
        return this.input;
    }
}
