// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/marker/ci/CustomBuildEnvironment.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
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
package org.openrewrite.marker.ci;

import org.openrewrite.internal.StringUtils;
import org.openrewrite.marker.GitProvenance;
import java.util.UUID;
import java.util.function.UnaryOperator;
import static java.util.Collections.emptyList;
import static org.openrewrite.Tree.randomId;

public final class CustomBuildEnvironment implements BuildEnvironment {
    private final UUID id;
    private final String cloneURL;
    private final String ref;
    private final String sha;

    public static CustomBuildEnvironment build(UnaryOperator<String> environment) {
        return new CustomBuildEnvironment(randomId(), environment.apply("CUSTOM_GIT_CLONE_URL"), environment.apply("CUSTOM_GIT_REF"), environment.apply("CUSTOM_GIT_SHA"));
    }

    @Override
    public GitProvenance buildGitProvenance() throws IncompleteGitConfigException {
        if (StringUtils.isBlank(cloneURL) || StringUtils.isBlank(ref) || StringUtils.isBlank(sha)) {
            throw new IncompleteGitConfigException();
        } else {
            return new GitProvenance(UUID.randomUUID(), cloneURL, ref, sha, null, null, emptyList());
        }
    }

    public CustomBuildEnvironment(final UUID id, final String cloneURL, final String ref, final String sha) {
        this.id = id;
        this.cloneURL = cloneURL;
        this.ref = ref;
        this.sha = sha;
    }

    public UUID getId() {
        return this.id;
    }

    public String getCloneURL() {
        return this.cloneURL;
    }

    public String getRef() {
        return this.ref;
    }

    public String getSha() {
        return this.sha;
    }

    @org.openrewrite.internal.lang.NonNull
    @Override
    public String toString() {
        return "CustomBuildEnvironment(id=" + this.getId() + ", cloneURL=" + this.getCloneURL() + ", ref=" + this.getRef() + ", sha=" + this.getSha() + ")";
    }

    @Override
    public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
        if (o == this) return true;
        if (!(o instanceof CustomBuildEnvironment)) return false;
        final CustomBuildEnvironment other = (CustomBuildEnvironment) o;
        final Object this$id = this.getId();
        final Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
        final Object this$cloneURL = this.getCloneURL();
        final Object other$cloneURL = other.getCloneURL();
        if (this$cloneURL == null ? other$cloneURL != null : !this$cloneURL.equals(other$cloneURL)) return false;
        final Object this$ref = this.getRef();
        final Object other$ref = other.getRef();
        if (this$ref == null ? other$ref != null : !this$ref.equals(other$ref)) return false;
        final Object this$sha = this.getSha();
        final Object other$sha = other.getSha();
        if (this$sha == null ? other$sha != null : !this$sha.equals(other$sha)) return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final Object $cloneURL = this.getCloneURL();
        result = result * PRIME + ($cloneURL == null ? 43 : $cloneURL.hashCode());
        final Object $ref = this.getRef();
        result = result * PRIME + ($ref == null ? 43 : $ref.hashCode());
        final Object $sha = this.getSha();
        result = result * PRIME + ($sha == null ? 43 : $sha.hashCode());
        return result;
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public CustomBuildEnvironment withId(final UUID id) {
        return this.id == id ? this : new CustomBuildEnvironment(id, this.cloneURL, this.ref, this.sha);
    }
}
