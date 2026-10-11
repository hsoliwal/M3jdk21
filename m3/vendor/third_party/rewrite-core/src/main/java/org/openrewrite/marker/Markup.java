// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/marker/Markup.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
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

import com.fasterxml.jackson.annotation.JsonCreator;
import org.openrewrite.Cursor;
import org.openrewrite.Incubating;
import org.openrewrite.RecipeScheduler;
import org.openrewrite.Tree;
import org.openrewrite.internal.ExceptionUtils;
import org.openrewrite.internal.RecipeRunException;
import org.openrewrite.internal.lang.Nullable;
import java.util.UUID;
import java.util.function.UnaryOperator;
import static org.openrewrite.Tree.randomId;

@Incubating(since = "7.31.0")
public interface Markup extends Marker {
    String getMessage();

    @Nullable
    String getDetail();

    @Override
    default String print(Cursor cursor, UnaryOperator<String> commentWrapper, boolean verbose) {
        if (verbose) {
            return commentWrapper.apply("(" + getDetail() + ")");
        }
        return commentWrapper.apply("(" + getMessage() + ")");
    }

    static <T extends Tree> T error(T t, Throwable throwable) {
        if (ExceptionUtils.containsCircularReferences(throwable)) {
            throwable = new Exception(throwable.getMessage());
            throwable.setStackTrace(throwable.getStackTrace());
        }
        return markup(t, new Markup.Error(randomId(), throwable));
    }

    static <T extends Tree> T warn(T t, Throwable throwable) {
        if (ExceptionUtils.containsCircularReferences(throwable)) {
            throwable = new Exception(throwable.getMessage());
            throwable.setStackTrace(throwable.getStackTrace());
        }
        return markup(t, new Markup.Warn(randomId(), throwable));
    }

    static <T extends Tree> T info(T t, String message) {
        return info(t, message, null);
    }

    static <T extends Tree> T info(T t, String message, @Nullable String detail) {
        return markup(t, new Markup.Info(randomId(), message, detail));
    }

    static <T extends Tree> T debug(T t, String message) {
        return debug(t, message, null);
    }

    static <T extends Tree> T debug(T t, String message, @Nullable String detail) {
        return markup(t, new Markup.Debug(randomId(), message, detail));
    }

    static <T extends Tree> T markup(T t, Markup markup) {
        return t.withMarkers(t.getMarkers().compute(markup, (s1, s2) -> s1 == null ? s2 : s1));
    }


    final class Error implements Markup {
        private final UUID id;
        private final String message;
        private final String detail;

        public Error(UUID id, Throwable exception) {
            this.id = id;
            Throwable cause = exception instanceof RecipeRunException ? exception.getCause() : exception;
            this.message = cause.getMessage();
            this.detail = ExceptionUtils.sanitizeStackTrace(cause, RecipeScheduler.class);
        }

        @JsonCreator
        public Error(UUID id, String message, String detail) {
            this.id = id;
            this.message = message;
            this.detail = detail;
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
            return "Markup.Error(id=" + this.getId() + ", message=" + this.getMessage() + ", detail=" + this.getDetail() + ")";
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof Markup.Error)) return false;
            final Markup.Error other = (Markup.Error) o;
            final Object this$message = this.getMessage();
            final Object other$message = other.getMessage();
            if (this$message == null ? other$message != null : !this$message.equals(other$message)) return false;
            final Object this$detail = this.getDetail();
            final Object other$detail = other.getDetail();
            if (this$detail == null ? other$detail != null : !this$detail.equals(other$detail)) return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $message = this.getMessage();
            result = result * PRIME + ($message == null ? 43 : $message.hashCode());
            final Object $detail = this.getDetail();
            result = result * PRIME + ($detail == null ? 43 : $detail.hashCode());
            return result;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Markup.Error withId(final UUID id) {
            return this.id == id ? this : new Markup.Error(id, this.message, this.detail);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Markup.Error withMessage(final String message) {
            return this.message == message ? this : new Markup.Error(this.id, message, this.detail);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Markup.Error withDetail(final String detail) {
            return this.detail == detail ? this : new Markup.Error(this.id, this.message, detail);
        }
    }


    final class Warn implements Markup {
        private final UUID id;
        private final String message;
        private final String detail;

        public Warn(UUID id, Throwable exception) {
            this.id = id;
            Throwable cause = exception instanceof RecipeRunException ? exception.getCause() : exception;
            this.message = cause.getMessage();
            this.detail = ExceptionUtils.sanitizeStackTrace(cause, RecipeScheduler.class);
        }

        @JsonCreator
        public Warn(UUID id, String message, String detail) {
            this.id = id;
            this.message = message;
            this.detail = detail;
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
            return "Markup.Warn(id=" + this.getId() + ", message=" + this.getMessage() + ", detail=" + this.getDetail() + ")";
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof Markup.Warn)) return false;
            final Markup.Warn other = (Markup.Warn) o;
            final Object this$message = this.getMessage();
            final Object other$message = other.getMessage();
            if (this$message == null ? other$message != null : !this$message.equals(other$message)) return false;
            final Object this$detail = this.getDetail();
            final Object other$detail = other.getDetail();
            if (this$detail == null ? other$detail != null : !this$detail.equals(other$detail)) return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $message = this.getMessage();
            result = result * PRIME + ($message == null ? 43 : $message.hashCode());
            final Object $detail = this.getDetail();
            result = result * PRIME + ($detail == null ? 43 : $detail.hashCode());
            return result;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Markup.Warn withId(final UUID id) {
            return this.id == id ? this : new Markup.Warn(id, this.message, this.detail);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Markup.Warn withMessage(final String message) {
            return this.message == message ? this : new Markup.Warn(this.id, message, this.detail);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Markup.Warn withDetail(final String detail) {
            return this.detail == detail ? this : new Markup.Warn(this.id, this.message, detail);
        }
    }


    final class Info implements Markup {
        private final UUID id;
        private final String message;
        @Nullable
        private final String detail;

        @Override
        public String getMessage() {
            return message;
        }

        @Nullable
        @Override
        public String getDetail() {
            return detail;
        }

        public Info(final UUID id, final String message, @Nullable final String detail) {
            this.id = id;
            this.message = message;
            this.detail = detail;
        }

        public UUID getId() {
            return this.id;
        }

        @org.openrewrite.internal.lang.NonNull
        @Override
        public String toString() {
            return "Markup.Info(id=" + this.getId() + ", message=" + this.getMessage() + ", detail=" + this.getDetail() + ")";
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof Markup.Info)) return false;
            final Markup.Info other = (Markup.Info) o;
            final Object this$message = this.getMessage();
            final Object other$message = other.getMessage();
            if (this$message == null ? other$message != null : !this$message.equals(other$message)) return false;
            final Object this$detail = this.getDetail();
            final Object other$detail = other.getDetail();
            if (this$detail == null ? other$detail != null : !this$detail.equals(other$detail)) return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $message = this.getMessage();
            result = result * PRIME + ($message == null ? 43 : $message.hashCode());
            final Object $detail = this.getDetail();
            result = result * PRIME + ($detail == null ? 43 : $detail.hashCode());
            return result;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Markup.Info withId(final UUID id) {
            return this.id == id ? this : new Markup.Info(id, this.message, this.detail);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Markup.Info withMessage(final String message) {
            return this.message == message ? this : new Markup.Info(this.id, message, this.detail);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Markup.Info withDetail(@Nullable final String detail) {
            return this.detail == detail ? this : new Markup.Info(this.id, this.message, detail);
        }
    }


    final class Debug implements Markup {
        private final UUID id;
        private final String message;
        @Nullable
        private final String detail;

        @Override
        public String getMessage() {
            return message;
        }

        @Nullable
        @Override
        public String getDetail() {
            return detail;
        }

        public Debug(final UUID id, final String message, @Nullable final String detail) {
            this.id = id;
            this.message = message;
            this.detail = detail;
        }

        public UUID getId() {
            return this.id;
        }

        @org.openrewrite.internal.lang.NonNull
        @Override
        public String toString() {
            return "Markup.Debug(id=" + this.getId() + ", message=" + this.getMessage() + ", detail=" + this.getDetail() + ")";
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof Markup.Debug)) return false;
            final Markup.Debug other = (Markup.Debug) o;
            final Object this$message = this.getMessage();
            final Object other$message = other.getMessage();
            if (this$message == null ? other$message != null : !this$message.equals(other$message)) return false;
            final Object this$detail = this.getDetail();
            final Object other$detail = other.getDetail();
            if (this$detail == null ? other$detail != null : !this$detail.equals(other$detail)) return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $message = this.getMessage();
            result = result * PRIME + ($message == null ? 43 : $message.hashCode());
            final Object $detail = this.getDetail();
            result = result * PRIME + ($detail == null ? 43 : $detail.hashCode());
            return result;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Markup.Debug withId(final UUID id) {
            return this.id == id ? this : new Markup.Debug(id, this.message, this.detail);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Markup.Debug withMessage(final String message) {
            return this.message == message ? this : new Markup.Debug(this.id, message, this.detail);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Markup.Debug withDetail(@Nullable final String detail) {
            return this.detail == detail ? this : new Markup.Debug(this.id, this.message, detail);
        }
    }
}
