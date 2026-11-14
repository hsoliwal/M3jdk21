// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/ParseExceptionResult.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite;

import org.openrewrite.internal.ExceptionUtils;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.Marker;
import java.util.UUID;
import static org.openrewrite.Tree.randomId;

public final class ParseExceptionResult implements Marker {
    private final UUID id;
    private final String parserType;
    private final String exceptionType;
    private final String message;
    /**
     * The type of tree element that was being parsed when the failure occurred.
     */
    @Nullable
    private final String treeType;

    public static ParseExceptionResult build(Class<? extends Parser> parserClass, Throwable t, @Nullable String message) {
        String simpleName = t.getClass().getSimpleName();
        return new ParseExceptionResult(randomId(), parserClass.getSimpleName(), !StringUtils.isBlank(simpleName) ? simpleName : t.getClass().getName(), (message != null ? message : "") + ExceptionUtils.sanitizeStackTrace(t, parserClass), null);
    }

    public static ParseExceptionResult build(Parser parser, Throwable t, @Nullable String message) {
        return build(parser.getClass(), t, message);
    }

    public static ParseExceptionResult build(Parser parser, Throwable t) {
        return build(parser.getClass(), t, null);
    }

    public ParseExceptionResult(final UUID id, final String parserType, final String exceptionType, final String message, @Nullable final String treeType) {
        this.id = id;
        this.parserType = parserType;
        this.exceptionType = exceptionType;
        this.message = message;
        this.treeType = treeType;
    }

    public UUID getId() {
        return this.id;
    }

    public String getParserType() {
        return this.parserType;
    }

    public String getExceptionType() {
        return this.exceptionType;
    }

    public String getMessage() {
        return this.message;
    }

    /**
     * The type of tree element that was being parsed when the failure occurred.
     */
    @Nullable
    public String getTreeType() {
        return this.treeType;
    }

    @Override
    public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
        if (o == this) return true;
        if (!(o instanceof ParseExceptionResult)) return false;
        final ParseExceptionResult other = (ParseExceptionResult) o;
        final Object this$id = this.getId();
        final Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
        final Object this$parserType = this.getParserType();
        final Object other$parserType = other.getParserType();
        if (this$parserType == null ? other$parserType != null : !this$parserType.equals(other$parserType)) return false;
        final Object this$exceptionType = this.getExceptionType();
        final Object other$exceptionType = other.getExceptionType();
        if (this$exceptionType == null ? other$exceptionType != null : !this$exceptionType.equals(other$exceptionType)) return false;
        final Object this$message = this.getMessage();
        final Object other$message = other.getMessage();
        if (this$message == null ? other$message != null : !this$message.equals(other$message)) return false;
        final Object this$treeType = this.getTreeType();
        final Object other$treeType = other.getTreeType();
        if (this$treeType == null ? other$treeType != null : !this$treeType.equals(other$treeType)) return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final Object $parserType = this.getParserType();
        result = result * PRIME + ($parserType == null ? 43 : $parserType.hashCode());
        final Object $exceptionType = this.getExceptionType();
        result = result * PRIME + ($exceptionType == null ? 43 : $exceptionType.hashCode());
        final Object $message = this.getMessage();
        result = result * PRIME + ($message == null ? 43 : $message.hashCode());
        final Object $treeType = this.getTreeType();
        result = result * PRIME + ($treeType == null ? 43 : $treeType.hashCode());
        return result;
    }

    @org.openrewrite.internal.lang.NonNull
    @Override
    public String toString() {
        return "ParseExceptionResult(id=" + this.getId() + ", parserType=" + this.getParserType() + ", exceptionType=" + this.getExceptionType() + ", message=" + this.getMessage() + ", treeType=" + this.getTreeType() + ")";
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public ParseExceptionResult withId(final UUID id) {
        return this.id == id ? this : new ParseExceptionResult(id, this.parserType, this.exceptionType, this.message, this.treeType);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public ParseExceptionResult withParserType(final String parserType) {
        return this.parserType == parserType ? this : new ParseExceptionResult(this.id, parserType, this.exceptionType, this.message, this.treeType);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public ParseExceptionResult withExceptionType(final String exceptionType) {
        return this.exceptionType == exceptionType ? this : new ParseExceptionResult(this.id, this.parserType, exceptionType, this.message, this.treeType);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public ParseExceptionResult withMessage(final String message) {
        return this.message == message ? this : new ParseExceptionResult(this.id, this.parserType, this.exceptionType, message, this.treeType);
    }

    /**
     * The type of tree element that was being parsed when the failure occurred.
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public ParseExceptionResult withTreeType(@Nullable final String treeType) {
        return this.treeType == treeType ? this : new ParseExceptionResult(this.id, this.parserType, this.exceptionType, this.message, treeType);
    }
}
