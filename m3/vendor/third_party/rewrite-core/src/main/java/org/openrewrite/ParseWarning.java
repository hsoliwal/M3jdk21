// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/ParseWarning.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
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
package org.openrewrite;

import org.openrewrite.internal.ExceptionUtils;
import org.openrewrite.marker.Marker;
import java.util.UUID;
import static org.openrewrite.Tree.randomId;

public final class ParseWarning implements Marker {
    private final UUID id;
    private final String message;

    public static ParseWarning build(Parser parser, Throwable t) {
        return new ParseWarning(randomId(), ExceptionUtils.sanitizeStackTrace(t, parser.getClass()));
    }

    public ParseWarning(final UUID id, final String message) {
        this.id = id;
        this.message = message;
    }

    public UUID getId() {
        return this.id;
    }

    public String getMessage() {
        return this.message;
    }

    @Override
    public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
        if (o == this) return true;
        if (!(o instanceof ParseWarning)) return false;
        final ParseWarning other = (ParseWarning) o;
        final Object this$id = this.getId();
        final Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
        final Object this$message = this.getMessage();
        final Object other$message = other.getMessage();
        if (this$message == null ? other$message != null : !this$message.equals(other$message)) return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final Object $message = this.getMessage();
        result = result * PRIME + ($message == null ? 43 : $message.hashCode());
        return result;
    }

    @org.openrewrite.internal.lang.NonNull
    @Override
    public String toString() {
        return "ParseWarning(id=" + this.getId() + ", message=" + this.getMessage() + ")";
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public ParseWarning withId(final UUID id) {
        return this.id == id ? this : new ParseWarning(id, this.message);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public ParseWarning withMessage(final String message) {
        return this.message == message ? this : new ParseWarning(this.id, message);
    }
}
