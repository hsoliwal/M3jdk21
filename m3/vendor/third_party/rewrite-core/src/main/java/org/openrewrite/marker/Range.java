// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/marker/Range.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
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
package org.openrewrite.marker;

import java.util.UUID;

/**
 * @author Alex Boyko
 */
public final class Range implements Marker {

    public static final class Position {
        private final int offset;
        private final int line;
        private final int column;

        public Position(final int offset, final int line, final int column) {
            this.offset = offset;
            this.line = line;
            this.column = column;
        }

        public int getOffset() {
            return this.offset;
        }

        public int getLine() {
            return this.line;
        }

        public int getColumn() {
            return this.column;
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof Range.Position)) return false;
            final Range.Position other = (Range.Position) o;
            if (this.getOffset() != other.getOffset()) return false;
            if (this.getLine() != other.getLine()) return false;
            if (this.getColumn() != other.getColumn()) return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            result = result * PRIME + this.getOffset();
            result = result * PRIME + this.getLine();
            result = result * PRIME + this.getColumn();
            return result;
        }

        @org.openrewrite.internal.lang.NonNull
        @Override
        public String toString() {
            return "Range.Position(offset=" + this.getOffset() + ", line=" + this.getLine() + ", column=" + this.getColumn() + ")";
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Range.Position withOffset(final int offset) {
            return this.offset == offset ? this : new Range.Position(offset, this.line, this.column);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Range.Position withLine(final int line) {
            return this.line == line ? this : new Range.Position(this.offset, line, this.column);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Range.Position withColumn(final int column) {
            return this.column == column ? this : new Range.Position(this.offset, this.line, column);
        }
    }

    private final UUID id;
    private final Position start;
    private final Position end;

    public int length() {
        return end.offset - start.offset;
    }

    public Range(final UUID id, final Position start, final Position end) {
        this.id = id;
        this.start = start;
        this.end = end;
    }

    public UUID getId() {
        return this.id;
    }

    public Position getStart() {
        return this.start;
    }

    public Position getEnd() {
        return this.end;
    }

    @Override
    public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
        if (o == this) return true;
        if (!(o instanceof Range)) return false;
        final Range other = (Range) o;
        final Object this$id = this.getId();
        final Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
        final Object this$start = this.getStart();
        final Object other$start = other.getStart();
        if (this$start == null ? other$start != null : !this$start.equals(other$start)) return false;
        final Object this$end = this.getEnd();
        final Object other$end = other.getEnd();
        if (this$end == null ? other$end != null : !this$end.equals(other$end)) return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final Object $start = this.getStart();
        result = result * PRIME + ($start == null ? 43 : $start.hashCode());
        final Object $end = this.getEnd();
        result = result * PRIME + ($end == null ? 43 : $end.hashCode());
        return result;
    }

    @org.openrewrite.internal.lang.NonNull
    @Override
    public String toString() {
        return "Range(id=" + this.getId() + ", start=" + this.getStart() + ", end=" + this.getEnd() + ")";
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public Range withId(final UUID id) {
        return this.id == id ? this : new Range(id, this.start, this.end);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public Range withStart(final Position start) {
        return this.start == start ? this : new Range(this.id, start, this.end);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public Range withEnd(final Position end) {
        return this.end == end ? this : new Range(this.id, this.start, end);
    }
}
