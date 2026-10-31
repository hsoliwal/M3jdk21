// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/marker/ci/BitbucketBuildEnvironment.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
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

public final class BitbucketBuildEnvironment implements BuildEnvironment {
    private final UUID id;
    private final String httpOrigin;
    private final String branch;
    private final String sha;

    public static BitbucketBuildEnvironment build(UnaryOperator<String> environment) {
        return new BitbucketBuildEnvironment(randomId(), environment.apply("BITBUCKET_GIT_HTTP_ORIGIN"), environment.apply("BITBUCKET_BRANCH"), environment.apply("BITBUCKET_COMMIT"));
    }

    @Override
    public GitProvenance buildGitProvenance() throws IncompleteGitConfigException {
        if (StringUtils.isBlank(httpOrigin) || StringUtils.isBlank(branch) || StringUtils.isBlank(sha)) {
            throw new IncompleteGitConfigException();
        } else {
            return new GitProvenance(UUID.randomUUID(), httpOrigin, branch, sha, null, null, emptyList());
        }
    }

    public BitbucketBuildEnvironment(final UUID id, final String httpOrigin, final String branch, final String sha) {
        this.id = id;
        this.httpOrigin = httpOrigin;
        this.branch = branch;
        this.sha = sha;
    }

    public UUID getId() {
        return this.id;
    }

    public String getHttpOrigin() {
        return this.httpOrigin;
    }

    public String getBranch() {
        return this.branch;
    }

    public String getSha() {
        return this.sha;
    }

    @org.openrewrite.internal.lang.NonNull
    @Override
    public String toString() {
        return "BitbucketBuildEnvironment(id=" + this.getId() + ", httpOrigin=" + this.getHttpOrigin() + ", branch=" + this.getBranch() + ", sha=" + this.getSha() + ")";
    }

    @Override
    public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
        if (o == this) return true;
        if (!(o instanceof BitbucketBuildEnvironment)) return false;
        final BitbucketBuildEnvironment other = (BitbucketBuildEnvironment) o;
        final Object this$id = this.getId();
        final Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
        final Object this$httpOrigin = this.getHttpOrigin();
        final Object other$httpOrigin = other.getHttpOrigin();
        if (this$httpOrigin == null ? other$httpOrigin != null : !this$httpOrigin.equals(other$httpOrigin)) return false;
        final Object this$branch = this.getBranch();
        final Object other$branch = other.getBranch();
        if (this$branch == null ? other$branch != null : !this$branch.equals(other$branch)) return false;
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
        final Object $httpOrigin = this.getHttpOrigin();
        result = result * PRIME + ($httpOrigin == null ? 43 : $httpOrigin.hashCode());
        final Object $branch = this.getBranch();
        result = result * PRIME + ($branch == null ? 43 : $branch.hashCode());
        final Object $sha = this.getSha();
        result = result * PRIME + ($sha == null ? 43 : $sha.hashCode());
        return result;
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public BitbucketBuildEnvironment withId(final UUID id) {
        return this.id == id ? this : new BitbucketBuildEnvironment(id, this.httpOrigin, this.branch, this.sha);
    }
}
