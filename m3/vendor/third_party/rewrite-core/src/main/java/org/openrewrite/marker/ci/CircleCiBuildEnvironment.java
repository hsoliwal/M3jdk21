// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/marker/ci/CircleCiBuildEnvironment.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
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
import static org.openrewrite.marker.OperatingSystemProvenance.hostname;

public final class CircleCiBuildEnvironment implements BuildEnvironment {
    private final UUID id;
    private final String buildId;
    private final String buildUrl;
    private final String host;
    private final String job;
    private final String branch;
    private final String repositoryURL;
    private final String sha1;
    private final String tag;

    public static CircleCiBuildEnvironment build(UnaryOperator<String> environment) {
        return new CircleCiBuildEnvironment(randomId(), environment.apply("CIRCLE_BUILD_NUM"), environment.apply("CIRCLE_BUILD_URL"), hostname(), environment.apply("CIRCLE_JOB"), environment.apply("CIRCLE_BRANCH"), environment.apply("CIRCLE_REPOSITORY_URL"), environment.apply("CIRCLE_SHA1"), environment.apply("CIRCLE_TAG"));
    }

    @Override
    public GitProvenance buildGitProvenance() throws IncompleteGitConfigException {
        if (StringUtils.isBlank(repositoryURL) || StringUtils.isBlank(sha1) || (StringUtils.isBlank(branch) && StringUtils.isBlank(tag))) {
            throw new IncompleteGitConfigException();
        }
        return new GitProvenance(UUID.randomUUID(), repositoryURL, StringUtils.isBlank(branch) ? tag : branch, sha1, null, null, emptyList());
    }

    public CircleCiBuildEnvironment(final UUID id, final String buildId, final String buildUrl, final String host, final String job, final String branch, final String repositoryURL, final String sha1, final String tag) {
        this.id = id;
        this.buildId = buildId;
        this.buildUrl = buildUrl;
        this.host = host;
        this.job = job;
        this.branch = branch;
        this.repositoryURL = repositoryURL;
        this.sha1 = sha1;
        this.tag = tag;
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

    public String getBranch() {
        return this.branch;
    }

    public String getRepositoryURL() {
        return this.repositoryURL;
    }

    public String getSha1() {
        return this.sha1;
    }

    public String getTag() {
        return this.tag;
    }

    @org.openrewrite.internal.lang.NonNull
    @Override
    public String toString() {
        return "CircleCiBuildEnvironment(id=" + this.getId() + ", buildId=" + this.getBuildId() + ", buildUrl=" + this.getBuildUrl() + ", host=" + this.getHost() + ", job=" + this.getJob() + ", branch=" + this.getBranch() + ", repositoryURL=" + this.getRepositoryURL() + ", sha1=" + this.getSha1() + ", tag=" + this.getTag() + ")";
    }

    @Override
    public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
        if (o == this) return true;
        if (!(o instanceof CircleCiBuildEnvironment)) return false;
        final CircleCiBuildEnvironment other = (CircleCiBuildEnvironment) o;
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
        final Object this$branch = this.getBranch();
        final Object other$branch = other.getBranch();
        if (this$branch == null ? other$branch != null : !this$branch.equals(other$branch)) return false;
        final Object this$repositoryURL = this.getRepositoryURL();
        final Object other$repositoryURL = other.getRepositoryURL();
        if (this$repositoryURL == null ? other$repositoryURL != null : !this$repositoryURL.equals(other$repositoryURL)) return false;
        final Object this$sha1 = this.getSha1();
        final Object other$sha1 = other.getSha1();
        if (this$sha1 == null ? other$sha1 != null : !this$sha1.equals(other$sha1)) return false;
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
        final Object $repositoryURL = this.getRepositoryURL();
        result = result * PRIME + ($repositoryURL == null ? 43 : $repositoryURL.hashCode());
        final Object $sha1 = this.getSha1();
        result = result * PRIME + ($sha1 == null ? 43 : $sha1.hashCode());
        final Object $tag = this.getTag();
        result = result * PRIME + ($tag == null ? 43 : $tag.hashCode());
        return result;
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public CircleCiBuildEnvironment withId(final UUID id) {
        return this.id == id ? this : new CircleCiBuildEnvironment(id, this.buildId, this.buildUrl, this.host, this.job, this.branch, this.repositoryURL, this.sha1, this.tag);
    }
}
