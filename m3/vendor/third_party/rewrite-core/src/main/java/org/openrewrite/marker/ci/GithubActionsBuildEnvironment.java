// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/marker/ci/GithubActionsBuildEnvironment.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
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

public final class GithubActionsBuildEnvironment implements BuildEnvironment {
    private final UUID id;
    private final String buildNumber;
    private final String buildId;
    private final String host;
    private final String job;
    private final String apiURL; //https://api.github.com
    private final String repository; //e.g octocat/Hello-World
    private final String ghRef; //// refs/pull/<pr_number>/merge or refs/heads/<branch_name>
    private final String sha;
    private final String headRef;

    public static GithubActionsBuildEnvironment build(UnaryOperator<String> environment) {
        return new GithubActionsBuildEnvironment(randomId(), environment.apply("GITHUB_RUN_NUMBER"), environment.apply("GITHUB_RUN_ID"), environment.apply("GITHUB_SERVER_URL"), environment.apply("GITHUB_ACTION"), environment.apply("GITHUB_API_URL"), environment.apply("GITHUB_REPOSITORY"), environment.apply("GITHUB_REF"), environment.apply("GITHUB_SHA"), environment.apply("GITHUB_HEAD_REF"));
    }

    @Override
    public GitProvenance buildGitProvenance() throws IncompleteGitConfigException {
        String host = getApiURL().replaceFirst("api\\.", "");
        String gitRef = getGhRef();
        if (gitRef.startsWith("refs/pull")) {
            gitRef = getHeadRef();
        } else {
            gitRef = gitRef.replaceFirst("refs/heads/", "");
        }
        if (StringUtils.isBlank(ghRef) || StringUtils.isBlank(host) || StringUtils.isBlank(repository) || StringUtils.isBlank(sha)) {
            throw new IncompleteGitConfigException(String.format("Invalid GitHub environment with host: %s, branch: %s, " + "repository: %s, sha: %s", host, ghRef, repository, sha));
        }
        return new GitProvenance(UUID.randomUUID(), host + "/" + getRepository() + ".git", gitRef, getSha(), null, null, emptyList());
    }

    public GithubActionsBuildEnvironment(final UUID id, final String buildNumber, final String buildId, final String host, final String job, final String apiURL, final String repository, final String ghRef, final String sha, final String headRef) {
        this.id = id;
        this.buildNumber = buildNumber;
        this.buildId = buildId;
        this.host = host;
        this.job = job;
        this.apiURL = apiURL;
        this.repository = repository;
        this.ghRef = ghRef;
        this.sha = sha;
        this.headRef = headRef;
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

    public String getHost() {
        return this.host;
    }

    public String getJob() {
        return this.job;
    }

    public String getApiURL() {
        return this.apiURL;
    }

    public String getRepository() {
        return this.repository;
    }

    public String getGhRef() {
        return this.ghRef;
    }

    public String getSha() {
        return this.sha;
    }

    public String getHeadRef() {
        return this.headRef;
    }

    @org.openrewrite.internal.lang.NonNull
    @Override
    public String toString() {
        return "GithubActionsBuildEnvironment(id=" + this.getId() + ", buildNumber=" + this.getBuildNumber() + ", buildId=" + this.getBuildId() + ", host=" + this.getHost() + ", job=" + this.getJob() + ", apiURL=" + this.getApiURL() + ", repository=" + this.getRepository() + ", ghRef=" + this.getGhRef() + ", sha=" + this.getSha() + ", headRef=" + this.getHeadRef() + ")";
    }

    @Override
    public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
        if (o == this) return true;
        if (!(o instanceof GithubActionsBuildEnvironment)) return false;
        final GithubActionsBuildEnvironment other = (GithubActionsBuildEnvironment) o;
        final Object this$id = this.getId();
        final Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
        final Object this$buildNumber = this.getBuildNumber();
        final Object other$buildNumber = other.getBuildNumber();
        if (this$buildNumber == null ? other$buildNumber != null : !this$buildNumber.equals(other$buildNumber)) return false;
        final Object this$buildId = this.getBuildId();
        final Object other$buildId = other.getBuildId();
        if (this$buildId == null ? other$buildId != null : !this$buildId.equals(other$buildId)) return false;
        final Object this$host = this.getHost();
        final Object other$host = other.getHost();
        if (this$host == null ? other$host != null : !this$host.equals(other$host)) return false;
        final Object this$job = this.getJob();
        final Object other$job = other.getJob();
        if (this$job == null ? other$job != null : !this$job.equals(other$job)) return false;
        final Object this$apiURL = this.getApiURL();
        final Object other$apiURL = other.getApiURL();
        if (this$apiURL == null ? other$apiURL != null : !this$apiURL.equals(other$apiURL)) return false;
        final Object this$repository = this.getRepository();
        final Object other$repository = other.getRepository();
        if (this$repository == null ? other$repository != null : !this$repository.equals(other$repository)) return false;
        final Object this$ghRef = this.getGhRef();
        final Object other$ghRef = other.getGhRef();
        if (this$ghRef == null ? other$ghRef != null : !this$ghRef.equals(other$ghRef)) return false;
        final Object this$sha = this.getSha();
        final Object other$sha = other.getSha();
        if (this$sha == null ? other$sha != null : !this$sha.equals(other$sha)) return false;
        final Object this$headRef = this.getHeadRef();
        final Object other$headRef = other.getHeadRef();
        if (this$headRef == null ? other$headRef != null : !this$headRef.equals(other$headRef)) return false;
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
        final Object $host = this.getHost();
        result = result * PRIME + ($host == null ? 43 : $host.hashCode());
        final Object $job = this.getJob();
        result = result * PRIME + ($job == null ? 43 : $job.hashCode());
        final Object $apiURL = this.getApiURL();
        result = result * PRIME + ($apiURL == null ? 43 : $apiURL.hashCode());
        final Object $repository = this.getRepository();
        result = result * PRIME + ($repository == null ? 43 : $repository.hashCode());
        final Object $ghRef = this.getGhRef();
        result = result * PRIME + ($ghRef == null ? 43 : $ghRef.hashCode());
        final Object $sha = this.getSha();
        result = result * PRIME + ($sha == null ? 43 : $sha.hashCode());
        final Object $headRef = this.getHeadRef();
        result = result * PRIME + ($headRef == null ? 43 : $headRef.hashCode());
        return result;
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public GithubActionsBuildEnvironment withId(final UUID id) {
        return this.id == id ? this : new GithubActionsBuildEnvironment(id, this.buildNumber, this.buildId, this.host, this.job, this.apiURL, this.repository, this.ghRef, this.sha, this.headRef);
    }
}
