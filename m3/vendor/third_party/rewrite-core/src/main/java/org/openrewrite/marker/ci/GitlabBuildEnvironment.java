// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/marker/ci/GitlabBuildEnvironment.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
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
package org.openrewrite.marker.ci;

import org.openrewrite.internal.StringUtils;
import org.openrewrite.marker.GitProvenance;
import java.util.UUID;
import java.util.function.UnaryOperator;
import static java.util.Collections.emptyList;
import static org.openrewrite.Tree.randomId;

public final class GitlabBuildEnvironment implements BuildEnvironment {
    private final UUID id;
    private final String buildId;
    private final String buildUrl;
    private final String host;
    private final String job;
    private final String ciRepositoryUrl;
    private final String ciCommitRefName;
    private final String ciCommitSha;

    public static GitlabBuildEnvironment build(UnaryOperator<String> environment) {
        return new GitlabBuildEnvironment(randomId(), environment.apply("CI_BUILD_ID"), environment.apply("CI_JOB_URL"), environment.apply("CI_SERVER_HOST"), environment.apply("CI_BUILD_NAME"), environment.apply("CI_REPOSITORY_URL"), environment.apply("CI_COMMIT_REF_NAME"), environment.apply("CI_COMMIT_SHA"));
    }

    @Override
    public GitProvenance buildGitProvenance() throws IncompleteGitConfigException {
        if (StringUtils.isBlank(ciRepositoryUrl) || StringUtils.isBlank(ciCommitRefName) || StringUtils.isBlank(ciCommitSha)) {
            throw new IncompleteGitConfigException();
        }
        return new GitProvenance(UUID.randomUUID(), ciRepositoryUrl, ciCommitRefName, ciCommitSha, null, null, emptyList());
    }

    public GitlabBuildEnvironment(final UUID id, final String buildId, final String buildUrl, final String host, final String job, final String ciRepositoryUrl, final String ciCommitRefName, final String ciCommitSha) {
        this.id = id;
        this.buildId = buildId;
        this.buildUrl = buildUrl;
        this.host = host;
        this.job = job;
        this.ciRepositoryUrl = ciRepositoryUrl;
        this.ciCommitRefName = ciCommitRefName;
        this.ciCommitSha = ciCommitSha;
    }

    public UUID getId() {
        return this.id;
    }

    public String getBuildId() {
        return this.buildId;
    }

    public String getBuildUrl() {
        return this.buildUrl;
    }

    public String getHost() {
        return this.host;
    }

    public String getJob() {
        return this.job;
    }

    public String getCiRepositoryUrl() {
        return this.ciRepositoryUrl;
    }

    public String getCiCommitRefName() {
        return this.ciCommitRefName;
    }

    public String getCiCommitSha() {
        return this.ciCommitSha;
    }

    @org.openrewrite.internal.lang.NonNull
    @Override
    public String toString() {
        return "GitlabBuildEnvironment(id=" + this.getId() + ", buildId=" + this.getBuildId() + ", buildUrl=" + this.getBuildUrl() + ", host=" + this.getHost() + ", job=" + this.getJob() + ", ciRepositoryUrl=" + this.getCiRepositoryUrl() + ", ciCommitRefName=" + this.getCiCommitRefName() + ", ciCommitSha=" + this.getCiCommitSha() + ")";
    }

    @Override
    public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
        if (o == this) return true;
        if (!(o instanceof GitlabBuildEnvironment)) return false;
        final GitlabBuildEnvironment other = (GitlabBuildEnvironment) o;
        final Object this$id = this.getId();
        final Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
        final Object this$buildId = this.getBuildId();
        final Object other$buildId = other.getBuildId();
        if (this$buildId == null ? other$buildId != null : !this$buildId.equals(other$buildId)) return false;
        final Object this$buildUrl = this.getBuildUrl();
        final Object other$buildUrl = other.getBuildUrl();
        if (this$buildUrl == null ? other$buildUrl != null : !this$buildUrl.equals(other$buildUrl)) return false;
        final Object this$host = this.getHost();
        final Object other$host = other.getHost();
        if (this$host == null ? other$host != null : !this$host.equals(other$host)) return false;
        final Object this$job = this.getJob();
        final Object other$job = other.getJob();
        if (this$job == null ? other$job != null : !this$job.equals(other$job)) return false;
        final Object this$ciRepositoryUrl = this.getCiRepositoryUrl();
        final Object other$ciRepositoryUrl = other.getCiRepositoryUrl();
        if (this$ciRepositoryUrl == null ? other$ciRepositoryUrl != null : !this$ciRepositoryUrl.equals(other$ciRepositoryUrl)) return false;
        final Object this$ciCommitRefName = this.getCiCommitRefName();
        final Object other$ciCommitRefName = other.getCiCommitRefName();
        if (this$ciCommitRefName == null ? other$ciCommitRefName != null : !this$ciCommitRefName.equals(other$ciCommitRefName)) return false;
        final Object this$ciCommitSha = this.getCiCommitSha();
        final Object other$ciCommitSha = other.getCiCommitSha();
        if (this$ciCommitSha == null ? other$ciCommitSha != null : !this$ciCommitSha.equals(other$ciCommitSha)) return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final Object $buildId = this.getBuildId();
        result = result * PRIME + ($buildId == null ? 43 : $buildId.hashCode());
        final Object $buildUrl = this.getBuildUrl();
        result = result * PRIME + ($buildUrl == null ? 43 : $buildUrl.hashCode());
        final Object $host = this.getHost();
        result = result * PRIME + ($host == null ? 43 : $host.hashCode());
        final Object $job = this.getJob();
        result = result * PRIME + ($job == null ? 43 : $job.hashCode());
        final Object $ciRepositoryUrl = this.getCiRepositoryUrl();
        result = result * PRIME + ($ciRepositoryUrl == null ? 43 : $ciRepositoryUrl.hashCode());
        final Object $ciCommitRefName = this.getCiCommitRefName();
        result = result * PRIME + ($ciCommitRefName == null ? 43 : $ciCommitRefName.hashCode());
        final Object $ciCommitSha = this.getCiCommitSha();
        result = result * PRIME + ($ciCommitSha == null ? 43 : $ciCommitSha.hashCode());
        return result;
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public GitlabBuildEnvironment withId(final UUID id) {
        return this.id == id ? this : new GitlabBuildEnvironment(id, this.buildId, this.buildUrl, this.host, this.job, this.ciRepositoryUrl, this.ciCommitRefName, this.ciCommitSha);
    }
}
