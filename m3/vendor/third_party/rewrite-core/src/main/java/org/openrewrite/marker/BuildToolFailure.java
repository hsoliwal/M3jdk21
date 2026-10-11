// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/marker/BuildToolFailure.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
/*
 * Copyright 2021 the original author or authors.
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

import org.openrewrite.internal.lang.Nullable;
import java.util.UUID;

/**
 * @deprecated Only included for backwards compatibility with old LSTs.
 */
@Deprecated
public final class BuildToolFailure implements Marker {
    private final UUID id;
    /**
     * The name of the build tool that failed, possibly a wrapper.
     */
    private final String type;
    @Nullable
    private final String version;
    /**
     * The command that was executed.
     */
    @Nullable
    private final String command;
    @Nullable
    private final Integer exitCode;

    public BuildToolFailure(final UUID id, final String type, @Nullable final String version, @Nullable final String command, @Nullable final Integer exitCode) {
        this.id = id;
        this.type = type;
        this.version = version;
        this.command = command;
        this.exitCode = exitCode;
    }

    public UUID getId() {
        return this.id;
    }

    /**
     * The name of the build tool that failed, possibly a wrapper.
     */
    public String getType() {
        return this.type;
    }

    @Nullable
    public String getVersion() {
        return this.version;
    }

    /**
     * The command that was executed.
     */
    @Nullable
    public String getCommand() {
        return this.command;
    }

    @Nullable
    public Integer getExitCode() {
        return this.exitCode;
    }

    @org.openrewrite.internal.lang.NonNull
    @Override
    public String toString() {
        return "BuildToolFailure(id=" + this.getId() + ", type=" + this.getType() + ", version=" + this.getVersion() + ", command=" + this.getCommand() + ", exitCode=" + this.getExitCode() + ")";
    }

    @Override
    public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
        if (o == this) return true;
        if (!(o instanceof BuildToolFailure)) return false;
        final BuildToolFailure other = (BuildToolFailure) o;
        final Object this$id = this.getId();
        final Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
        return true;
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
    public BuildToolFailure withId(final UUID id) {
        return this.id == id ? this : new BuildToolFailure(id, this.type, this.version, this.command, this.exitCode);
    }

    /**
     * The name of the build tool that failed, possibly a wrapper.
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public BuildToolFailure withType(final String type) {
        return this.type == type ? this : new BuildToolFailure(this.id, type, this.version, this.command, this.exitCode);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public BuildToolFailure withVersion(@Nullable final String version) {
        return this.version == version ? this : new BuildToolFailure(this.id, this.type, version, this.command, this.exitCode);
    }

    /**
     * The command that was executed.
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public BuildToolFailure withCommand(@Nullable final String command) {
        return this.command == command ? this : new BuildToolFailure(this.id, this.type, this.version, command, this.exitCode);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public BuildToolFailure withExitCode(@Nullable final Integer exitCode) {
        return this.exitCode == exitCode ? this : new BuildToolFailure(this.id, this.type, this.version, this.command, exitCode);
    }
}
