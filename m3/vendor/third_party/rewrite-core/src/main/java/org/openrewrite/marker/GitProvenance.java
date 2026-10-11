// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/marker/GitProvenance.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
/*
 * Copyright 2020 the original author or authors.
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

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.treewalk.WorkingTreeOptions;
import org.openrewrite.Incubating;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.ci.BuildEnvironment;
import org.openrewrite.marker.ci.IncompleteGitConfigException;
import org.openrewrite.marker.ci.JenkinsBuildEnvironment;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.*;
import static java.util.Collections.emptyList;
import static org.openrewrite.Tree.randomId;

public final class GitProvenance implements Marker {
    private final UUID id;
    @Nullable
    private final String origin;
    @Nullable
    private final String branch;
    @Nullable
    private final String change;
    @Nullable
    private final AutoCRLF autocrlf;
    @Nullable
    private final EOL eol;
    @Nullable
    @Incubating(since = "8.9.0")
    private final List<Committer> committers;

    /**
     * Extract the organization name, including sub-organizations for git hosting services which support such a concept,
     * from the origin URL. Needs to be supplied with the
     *
     * @param baseUrl the portion of the URL which precedes the organization
     * @return the portion of the git origin URL which corresponds to the organization the git repository is organized under
     */
    @Nullable
    public String getOrganizationName(String baseUrl) {
        if (origin == null) {
            return null;
        }
        int schemeEndIndex = baseUrl.indexOf("://");
        if (schemeEndIndex != -1) {
            baseUrl = baseUrl.substring(schemeEndIndex + 3);
        }
        if (baseUrl.startsWith("git@")) {
            baseUrl = baseUrl.substring(4);
        }
        String remainder = origin.substring(origin.indexOf(baseUrl) + baseUrl.length());
        if (remainder.startsWith("/")) {
            remainder = remainder.substring(1);
        }
        return remainder.substring(0, remainder.lastIndexOf('/'));
    }

    /**
     * There is too much variability in how different git hosting services arrange their organizations to reliably
     * determine the organization component of the URL without additional information. The version of this method
     * which accepts a "baseUrl" parameter should be used instead
     */
    @Deprecated
    @Nullable
    public String getOrganizationName() {
        if (origin == null) {
            return null;
        }
        try {
            String path = new URIish(origin).getPath();
            // Strip off any trailing repository name
            path = path.substring(0, path.lastIndexOf('/'));
            // Strip off any leading sub organization names
            return path.substring(path.lastIndexOf('/') + 1);
        } catch (URISyntaxException e) {
            return null;
        }
    }

    @Nullable
    public String getRepositoryName() {
        if (origin == null) {
            return null;
        }
        try {
            String path = new URIish(origin).getPath();
            return path.substring(path.lastIndexOf('/') + 1).replaceAll("\\.git$", "");
        } catch (URISyntaxException e) {
            return null;
        }
    }

    /**
     * @param projectDir The project directory.
     * @return A marker containing git provenance information.
     * @deprecated Use {@link #fromProjectDirectory(Path, BuildEnvironment) instead}.
     */
    @Nullable
    @Deprecated
    public static GitProvenance fromProjectDirectory(Path projectDir) {
        return fromProjectDirectory(projectDir, null);
    }

    /**
     * @param projectDir  The project directory.
     * @param environment In detached head scenarios, the branch is best
     *                    determined from a {@link BuildEnvironment} marker if possible.
     * @return A marker containing git provenance information.
     */
    @Nullable
    public static GitProvenance fromProjectDirectory(Path projectDir, @Nullable BuildEnvironment environment) {
        if (environment != null) {
            if (environment instanceof JenkinsBuildEnvironment) {
                JenkinsBuildEnvironment jenkinsBuildEnvironment = (JenkinsBuildEnvironment) environment;
                try (Repository repository = new RepositoryBuilder().findGitDir(projectDir.toFile()).build()) {
                    String branch = jenkinsBuildEnvironment.getLocalBranch() != null ? jenkinsBuildEnvironment.getLocalBranch() : localBranchName(repository, jenkinsBuildEnvironment.getBranch());
                    return fromGitConfig(repository, branch, getChangeset(repository));
                } catch (IllegalArgumentException | GitAPIException e) {
                    // Silently ignore if the project directory is not a git repository
                    printRequireGitDirOrWorkTreeException(e);
                    return null;
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            } else {
                File gitDir = new RepositoryBuilder().findGitDir(projectDir.toFile()).getGitDir();
                if (gitDir != null && gitDir.exists()) {
                    //it has been cloned with --depth > 0
                    return fromGitConfig(projectDir);
                } else {
                    //there is not .git config
                    try {
                        return environment.buildGitProvenance();
                    } catch (IncompleteGitConfigException e) {
                        return fromGitConfig(projectDir);
                    }
                }
            }
        } else {
            return fromGitConfig(projectDir);
        }
    }

    private static void printRequireGitDirOrWorkTreeException(Exception e) {
        if (!"requireGitDirOrWorkTree".equals(e.getStackTrace()[0].getMethodName())) {
            e.printStackTrace();
        }
    }

    @Nullable
    private static GitProvenance fromGitConfig(Path projectDir) {
        String branch = null;
        try (Repository repository = new RepositoryBuilder().findGitDir(projectDir.toFile()).build()) {
            String changeset = getChangeset(repository);
            if (!repository.getBranch().equals(changeset)) {
                branch = repository.getBranch();
            }
            return fromGitConfig(repository, branch, changeset);
        } catch (IllegalArgumentException e) {
            printRequireGitDirOrWorkTreeException(e);
            return null;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static GitProvenance fromGitConfig(Repository repository, @Nullable String branch, @Nullable String changeset) {
        if (branch == null) {
            branch = resolveBranchFromGitConfig(repository);
        }
        return new GitProvenance(randomId(), getOrigin(repository), branch, changeset, getAutocrlf(repository), getEOF(repository), getCommitters(repository));
    }

    @Nullable
    static String resolveBranchFromGitConfig(Repository repository) {
        String branch;
        try {
            try (Git git = Git.open(repository.getDirectory())) {
                ObjectId commit = repository.resolve(Constants.HEAD);
                Map<ObjectId, String> branchesByCommit = git.nameRev().addPrefix("refs/heads/").add(commit).call();
                if (branchesByCommit.containsKey(commit)) {
                    // detached head but _not_ a shallow clone
                    branch = branchesByCommit.get(commit);
                } else {
                    // detached head and _also_ a shallow clone
                    branchesByCommit = git.nameRev().add(commit).call();
                    branch = localBranchName(repository, branchesByCommit.get(commit));
                }
                if (branch != null) {
                    if (branch.contains("^")) {
                        branch = branch.substring(0, branch.indexOf('^'));
                    } else if (branch.contains("~")) {
                        branch = branch.substring(0, branch.indexOf('~'));
                    }
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (IllegalArgumentException | GitAPIException e) {
            // Silently ignore if the project directory is not a git repository
            if (!"requireGitDirOrWorkTree".equals(e.getStackTrace()[0].getMethodName())) {
                e.printStackTrace();
            }
            return null;
        }
        return branch;
    }

    @Nullable
    private static String localBranchName(Repository repository, @Nullable String remoteBranch) throws IOException, GitAPIException {
        if (remoteBranch == null) {
            return null;
        } else if (remoteBranch.startsWith("remotes/")) {
            // Remote branch names retrieved from git are prefixed with "remotes/"
            remoteBranch = remoteBranch.substring(8);
        }
        String branch = null;
        try (Git git = Git.open(repository.getDirectory())) {
            List<RemoteConfig> remotes = git.remoteList().call();
            for (RemoteConfig remote : remotes) {
                if (remoteBranch.startsWith(remote.getName()) && (branch == null || branch.length() > remoteBranch.length() - remote.getName().length() - 1)) {
                    branch = remoteBranch.substring(remote.getName().length() + 1); // +1 for the forward slash
                }
            }
        } catch (GitAPIException ignored) {
        }
        return branch;
    }

    @Nullable
    private static String getOrigin(Repository repository) {
        Config storedConfig = repository.getConfig();
        String url = storedConfig.getString("remote", "origin", "url");
        if (url == null) {
            return null;
        }
        if (url.startsWith("https://") || url.startsWith("http://")) {
            url = hideSensitiveInformation(url);
        }
        return url;
    }

    @Nullable
    private static AutoCRLF getAutocrlf(Repository repository) {
        WorkingTreeOptions opt = repository.getConfig().get(WorkingTreeOptions.KEY);
        switch (opt.getAutoCRLF()) {
        case FALSE: 
            return AutoCRLF.False;
        case TRUE: 
            return AutoCRLF.True;
        case INPUT: 
            return AutoCRLF.Input;
        default: 
            return null;
        }
    }

    @Nullable
    private static EOL getEOF(Repository repository) {
        WorkingTreeOptions opt = repository.getConfig().get(WorkingTreeOptions.KEY);
        switch (opt.getEOL()) {
        case CRLF: 
            return EOL.CRLF;
        case LF: 
            return EOL.LF;
        case NATIVE: 
            return EOL.Native;
        default: 
            return null;
        }
    }

    private static List<Committer> getCommitters(Repository repository) {
        try (Git git = Git.open(repository.getDirectory())) {
            ObjectId head = repository.readOrigHead();
            if (head == null) {
                Ref headRef = repository.getRefDatabase().findRef("HEAD");
                if (headRef == null || headRef.getObjectId() == null) {
                    return emptyList();
                }
                head = headRef.getObjectId();
            }
            Map<String, Committer> committers = new TreeMap<>();
            for (RevCommit commit : git.log().add(head).call()) {
                PersonIdent who = commit.getAuthorIdent();
                Committer committer = committers.computeIfAbsent(who.getEmailAddress(), email -> new Committer(who.getName(), email, new TreeMap<>()));
                committer.getCommitsByDay().compute(ZonedDateTime.ofInstant(who.getWhen().toInstant(), who.getTimeZone().toZoneId()).toLocalDate(), (day, count) -> count == null ? 1 : count + 1);
            }
            return new ArrayList<>(committers.values());
        } catch (IOException | GitAPIException e) {
            return emptyList();
        }
    }

    @Nullable
    private static String getChangeset(Repository repository) throws IOException {
        ObjectId head = repository.resolve(Constants.HEAD);
        if (head == null) {
            return null;
        }
        return head.getName();
    }

    private static String hideSensitiveInformation(String url) {
        try {
            String credentials = URI.create(url).toURL().getUserInfo();
            if (credentials != null) {
                return url.replaceFirst(credentials, credentials.replaceFirst(":.*", ""));
            }
        } catch (Exception e) {
            throw new IllegalStateException("Unable to remove credentials from repository URL. {0}", e);
        }
        return url;
    }


    public enum AutoCRLF {
        False, True, Input;
    }


    public enum EOL {
        CRLF, LF, Native;
    }


    public static final class Committer {
        private final String name;
        private final String email;
        private final NavigableMap<LocalDate, Integer> commitsByDay;

        public Committer(final String name, final String email, final NavigableMap<LocalDate, Integer> commitsByDay) {
            this.name = name;
            this.email = email;
            this.commitsByDay = commitsByDay;
        }

        public String getName() {
            return this.name;
        }

        public String getEmail() {
            return this.email;
        }

        public NavigableMap<LocalDate, Integer> getCommitsByDay() {
            return this.commitsByDay;
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof GitProvenance.Committer)) return false;
            final GitProvenance.Committer other = (GitProvenance.Committer) o;
            final Object this$name = this.getName();
            final Object other$name = other.getName();
            if (this$name == null ? other$name != null : !this$name.equals(other$name)) return false;
            final Object this$email = this.getEmail();
            final Object other$email = other.getEmail();
            if (this$email == null ? other$email != null : !this$email.equals(other$email)) return false;
            final Object this$commitsByDay = this.getCommitsByDay();
            final Object other$commitsByDay = other.getCommitsByDay();
            if (this$commitsByDay == null ? other$commitsByDay != null : !this$commitsByDay.equals(other$commitsByDay)) return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $name = this.getName();
            result = result * PRIME + ($name == null ? 43 : $name.hashCode());
            final Object $email = this.getEmail();
            result = result * PRIME + ($email == null ? 43 : $email.hashCode());
            final Object $commitsByDay = this.getCommitsByDay();
            result = result * PRIME + ($commitsByDay == null ? 43 : $commitsByDay.hashCode());
            return result;
        }

        @org.openrewrite.internal.lang.NonNull
        @Override
        public String toString() {
            return "GitProvenance.Committer(name=" + this.getName() + ", email=" + this.getEmail() + ", commitsByDay=" + this.getCommitsByDay() + ")";
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public GitProvenance.Committer withName(final String name) {
            return this.name == name ? this : new GitProvenance.Committer(name, this.email, this.commitsByDay);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public GitProvenance.Committer withEmail(final String email) {
            return this.email == email ? this : new GitProvenance.Committer(this.name, email, this.commitsByDay);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public GitProvenance.Committer withCommitsByDay(final NavigableMap<LocalDate, Integer> commitsByDay) {
            return this.commitsByDay == commitsByDay ? this : new GitProvenance.Committer(this.name, this.email, commitsByDay);
        }
    }

    public GitProvenance(final UUID id, @Nullable final String origin, @Nullable final String branch, @Nullable final String change, @Nullable final AutoCRLF autocrlf, @Nullable final EOL eol, @Nullable final List<Committer> committers) {
        this.id = id;
        this.origin = origin;
        this.branch = branch;
        this.change = change;
        this.autocrlf = autocrlf;
        this.eol = eol;
        this.committers = committers;
    }

    public UUID getId() {
        return this.id;
    }

    @Nullable
    public String getOrigin() {
        return this.origin;
    }

    @Nullable
    public String getBranch() {
        return this.branch;
    }

    @Nullable
    public String getChange() {
        return this.change;
    }

    @Nullable
    public AutoCRLF getAutocrlf() {
        return this.autocrlf;
    }

    @Nullable
    public EOL getEol() {
        return this.eol;
    }

    @Nullable
    public List<Committer> getCommitters() {
        return this.committers;
    }

    @Override
    public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
        if (o == this) return true;
        if (!(o instanceof GitProvenance)) return false;
        final GitProvenance other = (GitProvenance) o;
        final Object this$id = this.getId();
        final Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
        final Object this$origin = this.getOrigin();
        final Object other$origin = other.getOrigin();
        if (this$origin == null ? other$origin != null : !this$origin.equals(other$origin)) return false;
        final Object this$branch = this.getBranch();
        final Object other$branch = other.getBranch();
        if (this$branch == null ? other$branch != null : !this$branch.equals(other$branch)) return false;
        final Object this$change = this.getChange();
        final Object other$change = other.getChange();
        if (this$change == null ? other$change != null : !this$change.equals(other$change)) return false;
        final Object this$autocrlf = this.getAutocrlf();
        final Object other$autocrlf = other.getAutocrlf();
        if (this$autocrlf == null ? other$autocrlf != null : !this$autocrlf.equals(other$autocrlf)) return false;
        final Object this$eol = this.getEol();
        final Object other$eol = other.getEol();
        if (this$eol == null ? other$eol != null : !this$eol.equals(other$eol)) return false;
        final Object this$committers = this.getCommitters();
        final Object other$committers = other.getCommitters();
        if (this$committers == null ? other$committers != null : !this$committers.equals(other$committers)) return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final Object $origin = this.getOrigin();
        result = result * PRIME + ($origin == null ? 43 : $origin.hashCode());
        final Object $branch = this.getBranch();
        result = result * PRIME + ($branch == null ? 43 : $branch.hashCode());
        final Object $change = this.getChange();
        result = result * PRIME + ($change == null ? 43 : $change.hashCode());
        final Object $autocrlf = this.getAutocrlf();
        result = result * PRIME + ($autocrlf == null ? 43 : $autocrlf.hashCode());
        final Object $eol = this.getEol();
        result = result * PRIME + ($eol == null ? 43 : $eol.hashCode());
        final Object $committers = this.getCommitters();
        result = result * PRIME + ($committers == null ? 43 : $committers.hashCode());
        return result;
    }

    @org.openrewrite.internal.lang.NonNull
    @Override
    public String toString() {
        return "GitProvenance(id=" + this.getId() + ", origin=" + this.getOrigin() + ", branch=" + this.getBranch() + ", change=" + this.getChange() + ", autocrlf=" + this.getAutocrlf() + ", eol=" + this.getEol() + ", committers=" + this.getCommitters() + ")";
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public GitProvenance withId(final UUID id) {
        return this.id == id ? this : new GitProvenance(id, this.origin, this.branch, this.change, this.autocrlf, this.eol, this.committers);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public GitProvenance withOrigin(@Nullable final String origin) {
        return this.origin == origin ? this : new GitProvenance(this.id, origin, this.branch, this.change, this.autocrlf, this.eol, this.committers);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public GitProvenance withBranch(@Nullable final String branch) {
        return this.branch == branch ? this : new GitProvenance(this.id, this.origin, branch, this.change, this.autocrlf, this.eol, this.committers);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public GitProvenance withChange(@Nullable final String change) {
        return this.change == change ? this : new GitProvenance(this.id, this.origin, this.branch, change, this.autocrlf, this.eol, this.committers);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public GitProvenance withAutocrlf(@Nullable final AutoCRLF autocrlf) {
        return this.autocrlf == autocrlf ? this : new GitProvenance(this.id, this.origin, this.branch, this.change, autocrlf, this.eol, this.committers);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public GitProvenance withEol(@Nullable final EOL eol) {
        return this.eol == eol ? this : new GitProvenance(this.id, this.origin, this.branch, this.change, this.autocrlf, eol, this.committers);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public GitProvenance withCommitters(@Nullable final List<Committer> committers) {
        return this.committers == committers ? this : new GitProvenance(this.id, this.origin, this.branch, this.change, this.autocrlf, this.eol, committers);
    }
}
