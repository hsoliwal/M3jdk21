// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/marker/DeserializationError.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
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
package org.openrewrite.marker;

import com.fasterxml.jackson.annotation.JsonCreator;
import org.openrewrite.internal.ExceptionUtils;
import java.util.UUID;

public class DeserializationError implements Marker {
    private final UUID id;
    private final String message;
    private final String detail;

    @SuppressWarnings("unused")
    public DeserializationError(UUID id, String message, Throwable cause) {
        this.id = id;
        this.message = message;
        this.detail = ExceptionUtils.sanitizeStackTrace(cause, Object.class);
    }

    @JsonCreator
    DeserializationError(UUID id, String message, String detail) {
        this.id = id;
        this.message = message;
        this.detail = detail;
    }

    @Override
    public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
        if (o == this) return true;
        if (!(o instanceof DeserializationError)) return false;
        final DeserializationError other = (DeserializationError) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$id = this.getId();
        final Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
        return true;
    }

    protected boolean canEqual(@org.openrewrite.internal.lang.Nullable final Object other) {
        return other instanceof DeserializationError;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        return result;
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public DeserializationError withId(final UUID id) {
        return this.id == id ? this : new DeserializationError(id, this.message, this.detail);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public DeserializationError withMessage(final String message) {
        return this.message == message ? this : new DeserializationError(this.id, message, this.detail);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public DeserializationError withDetail(final String detail) {
        return this.detail == detail ? this : new DeserializationError(this.id, this.message, detail);
    }

    public UUID getId() {
        return this.id;
    }

    public String getMessage() {
        return this.message;
    }

    public String getDetail() {
        return this.detail;
    }

    @org.openrewrite.internal.lang.NonNull
    @Override
    public String toString() {
        return "DeserializationError(id=" + this.getId() + ", message=" + this.getMessage() + ", detail=" + this.getDetail() + ")";
    }
}
