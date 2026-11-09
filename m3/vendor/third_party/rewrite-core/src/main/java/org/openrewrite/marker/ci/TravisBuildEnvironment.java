// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/marker/ci/TravisBuildEnvironment.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
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

import org.openrewrite.marker.GitProvenance;
import java.util.UUID;
import java.util.function.UnaryOperator;
import static org.openrewrite.Tree.randomId;
import static org.openrewrite.marker.OperatingSystemProvenance.hostname;

public final class TravisBuildEnvironment implements BuildEnvironment {
    private final UUID id;
    private final String buildNumber;
    private final String buildId;
    private final String buildUrl;
    private final String host;
    private final String job;
    private final String branch;
    private final String commit;
    private final String repoSlug;
    private final String tag;

    public static TravisBuildEnvironment build(UnaryOperator<String> environment) {
        return new TravisBuildEnvironment(randomId(), environment.apply("TRAVIS_BUILD_NUMBER"), environment.apply("TRAVIS_BUILD_ID"), environment.apply("TRAVIS_BUILD_WEB_URL"), hostname(), environment.apply("TRAVIS_REPO_SLUG"), environment.apply("TRAVIS_BRANCH"), environment.apply("TRAVIS_COMMIT"), environment.apply("TRAVIS_REPO_SLUG"), environment.apply("TRAVIS_TAG"));
    }

    @Override
    public GitProvenance buildGitProvenance() throws IncompleteGitConfigException {
        //travis generates the .config directory and it is not possible to obtain the clone URL from any env
        throw new IncompleteGitConfigException();
    }

    public TravisBuildEnvironment(final UUID id, final String buildNumber, final String buildId, final String buildUrl, final String host, final String job, final String branch, final String commit, final String repoSlug, final String tag) {
        this.id = id;
        this.buildNumber = buildNumber;
        this.buildId = buildId;
        this.buildUrl = buildUrl;
        this.host = host;
        this.job = job;
        this.branch = branch;
        this.commit = commit;
        this.repoSlug = repoSlug;
        this.tag = tag;
    }

    public UUID getId() {
        return this.id;
    }

    public String getBuildNumber() {
        return this.buildNumber;
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

    public String getBranch() {
        return this.branch;
    }

    public String getCommit() {
        return this.commit;
    }

    public String getRepoSlug() {
        return this.repoSlug;
    }

    public String getTag() {
        return this.tag;
    }

    @org.openrewrite.internal.lang.NonNull
    @Override
    public String toString() {
        return "TravisBuildEnvironment(id=" + this.getId() + ", buildNumber=" + this.getBuildNumber() + ", buildId=" + this.getBuildId() + ", buildUrl=" + this.getBuildUrl() + ", host=" + this.getHost() + ", job=" + this.getJob() + ", branch=" + this.getBranch() + ", commit=" + this.getCommit() + ", repoSlug=" + this.getRepoSlug() + ", tag=" + this.getTag() + ")";
    }

    @Override
    public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
        if (o == this) return true;
        if (!(o instanceof TravisBuildEnvironment)) return false;
        final TravisBuildEnvironment other = (TravisBuildEnvironment) o;
        final Object this$id = this.getId();
        final Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
        final Object this$buildNumber = this.getBuildNumber();
        final Object other$buildNumber = other.getBuildNumber();
        if (this$buildNumber == null ? other$buildNumber != null : !this$buildNumber.equals(other$buildNumber)) return false;
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
        final Object this$branch = this.getBranch();
        final Object other$branch = other.getBranch();
        if (this$branch == null ? other$branch != null : !this$branch.equals(other$branch)) return false;
        final Object this$commit = this.getCommit();
        final Object other$commit = other.getCommit();
        if (this$commit == null ? other$commit != null : !this$commit.equals(other$commit)) return false;
        final Object this$repoSlug = this.getRepoSlug();
        final Object other$repoSlug = other.getRepoSlug();
        if (this$repoSlug == null ? other$repoSlug != null : !this$repoSlug.equals(other$repoSlug)) return false;
        final Object this$tag = this.getTag();
        final Object other$tag = other.getTag();
        if (this$tag == null ? other$tag != null : !this$tag.equals(other$tag)) return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final Object $buildNumber = this.getBuildNumber();
        result = result * PRIME + ($buildNumber == null ? 43 : $buildNumber.hashCode());
        final Object $buildId = this.getBuildId();
        result = result * PRIME + ($buildId == null ? 43 : $buildId.hashCode());
        final Object $buildUrl = this.getBuildUrl();
        result = result * PRIME + ($buildUrl == null ? 43 : $buildUrl.hashCode());
        final Object $host = this.getHost();
        result = result * PRIME + ($host == null ? 43 : $host.hashCode());
        final Object $job = this.getJob();
        result = result * PRIME + ($job == null ? 43 : $job.hashCode());
        final Object $branch = this.getBranch();
        result = result * PRIME + ($branch == null ? 43 : $branch.hashCode());
        final Object $commit = this.getCommit();
        result = result * PRIME + ($commit == null ? 43 : $commit.hashCode());
        final Object $repoSlug = this.getRepoSlug();
        result = result * PRIME + ($repoSlug == null ? 43 : $repoSlug.hashCode());
        final Object $tag = this.getTag();
        result = result * PRIME + ($tag == null ? 43 : $tag.hashCode());
        return result;
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public TravisBuildEnvironment withId(final UUID id) {
        return this.id == id ? this : new TravisBuildEnvironment(id, this.buildNumber, this.buildId, this.buildUrl, this.host, this.job, this.branch, this.commit, this.repoSlug, this.tag);
    }
}
