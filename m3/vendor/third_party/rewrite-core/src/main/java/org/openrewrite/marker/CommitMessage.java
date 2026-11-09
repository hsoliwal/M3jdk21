// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/marker/CommitMessage.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
/*
 * Copyright 2024 the original author or authors.
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

import org.openrewrite.Recipe;
import org.openrewrite.Tree;
import java.util.UUID;
import static org.openrewrite.Tree.randomId;

/**
 * A marker that indicates an extra message to be appended to the message of any commit generated from the recipe run.
 */
public final class CommitMessage implements Marker {
    private final UUID id;
    private final String recipeName;
    private final String message;

    public static <T extends Tree> T message(Tree t, Recipe r, String message) {
        return t.withMarkers(t.getMarkers().add(new CommitMessage(randomId(), r.getName(), message)));
    }

    public CommitMessage(final UUID id, final String recipeName, final String message) {
        this.id = id;
        this.recipeName = recipeName;
        this.message = message;
    }

    public UUID getId() {
        return this.id;
    }

    public String getRecipeName() {
        return this.recipeName;
    }

    public String getMessage() {
        return this.message;
    }

    @Override
    public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
        if (o == this) return true;
        if (!(o instanceof CommitMessage)) return false;
        final CommitMessage other = (CommitMessage) o;
        final Object this$id = this.getId();
        final Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
        final Object this$recipeName = this.getRecipeName();
        final Object other$recipeName = other.getRecipeName();
        if (this$recipeName == null ? other$recipeName != null : !this$recipeName.equals(other$recipeName)) return false;
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
        final Object $recipeName = this.getRecipeName();
        result = result * PRIME + ($recipeName == null ? 43 : $recipeName.hashCode());
        final Object $message = this.getMessage();
        result = result * PRIME + ($message == null ? 43 : $message.hashCode());
        return result;
    }

    @org.openrewrite.internal.lang.NonNull
    @Override
    public String toString() {
        return "CommitMessage(id=" + this.getId() + ", recipeName=" + this.getRecipeName() + ", message=" + this.getMessage() + ")";
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public CommitMessage withId(final UUID id) {
        return this.id == id ? this : new CommitMessage(id, this.recipeName, this.message);
    }
}
