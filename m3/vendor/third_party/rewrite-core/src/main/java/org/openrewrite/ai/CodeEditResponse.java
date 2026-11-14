// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/ai/CodeEditResponse.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
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
package org.openrewrite.ai;

import org.openrewrite.internal.lang.Nullable;
import java.util.List;

public final class CodeEditResponse {
    private final List<Choice> choices;
    @Nullable
    private final Error error;


    public static final class Choice {
        private final String text;

        public Choice(final String text) {
            this.text = text;
        }

        public String getText() {
            return this.text;
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof CodeEditResponse.Choice)) return false;
            final CodeEditResponse.Choice other = (CodeEditResponse.Choice) o;
            final Object this$text = this.getText();
            final Object other$text = other.getText();
            if (this$text == null ? other$text != null : !this$text.equals(other$text)) return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $text = this.getText();
            result = result * PRIME + ($text == null ? 43 : $text.hashCode());
            return result;
        }

        @org.openrewrite.internal.lang.NonNull
        @Override
        public String toString() {
            return "CodeEditResponse.Choice(text=" + this.getText() + ")";
        }
    }


    public static final class Error {
        private final String message;

        public Error(final String message) {
            this.message = message;
        }

        public String getMessage() {
            return this.message;
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof CodeEditResponse.Error)) return false;
            final CodeEditResponse.Error other = (CodeEditResponse.Error) o;
            final Object this$message = this.getMessage();
            final Object other$message = other.getMessage();
            if (this$message == null ? other$message != null : !this$message.equals(other$message)) return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $message = this.getMessage();
            result = result * PRIME + ($message == null ? 43 : $message.hashCode());
            return result;
        }

        @org.openrewrite.internal.lang.NonNull
        @Override
        public String toString() {
            return "CodeEditResponse.Error(message=" + this.getMessage() + ")";
        }
    }

    public CodeEditResponse(final List<Choice> choices, @Nullable final Error error) {
        this.choices = choices;
        this.error = error;
    }

    public List<Choice> getChoices() {
        return this.choices;
    }

    @Nullable
    public Error getError() {
        return this.error;
    }

    @Override
    public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
        if (o == this) return true;
        if (!(o instanceof CodeEditResponse)) return false;
        final CodeEditResponse other = (CodeEditResponse) o;
        final Object this$choices = this.getChoices();
        final Object other$choices = other.getChoices();
        if (this$choices == null ? other$choices != null : !this$choices.equals(other$choices)) return false;
        final Object this$error = this.getError();
        final Object other$error = other.getError();
        if (this$error == null ? other$error != null : !this$error.equals(other$error)) return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $choices = this.getChoices();
        result = result * PRIME + ($choices == null ? 43 : $choices.hashCode());
        final Object $error = this.getError();
        result = result * PRIME + ($error == null ? 43 : $error.hashCode());
        return result;
    }

    @org.openrewrite.internal.lang.NonNull
    @Override
    public String toString() {
        return "CodeEditResponse(choices=" + this.getChoices() + ", error=" + this.getError() + ")";
    }
}
