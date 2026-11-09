// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/text/Find.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
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
package org.openrewrite.text;

import org.openrewrite.*;
import org.openrewrite.binary.Binary;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.Markers;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.quark.Quark;
import org.openrewrite.remote.Remote;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import static java.util.Objects.requireNonNull;

public final class Find extends Recipe {
    @Override
    public String getDisplayName() {
        return "Find text";
    }

    @Override
    public String getDescription() {
        return "Search for text, treating all textual sources as plain text.";
    }

    @Option(displayName = "Find", description = "The text to find. This snippet can be multiline.", example = "blacklist")
    private final String find;
    @Option(displayName = "Regex", description = "If true, `find` will be interpreted as a Regular Expression. Default `false`.", required = false)
    @Nullable
    private final Boolean regex;
    @Option(displayName = "Case sensitive", description = "If `true` the search will be sensitive to case. Default `false`.", required = false)
    @Nullable
    private final Boolean caseSensitive;
    @Option(displayName = "Regex multiline mode", description = "When performing a regex search setting this to `true` allows \"^\" and \"$\" to match the beginning and end of lines, respectively. " + "When performing a regex search when this is `false` \"^\" and \"$\" will match only the beginning and ending of the entire source file, respectively." + "Has no effect when not performing a regex search. Default `false`.", required = false)
    @Nullable
    private final Boolean multiline;
    @Option(displayName = "Regex dot all", description = "When performing a regex search setting this to `true` allows \".\" to match line terminators." + "Has no effect when not performing a regex search. Default `false`.", required = false)
    @Nullable
    private final Boolean dotAll;
    @Option(displayName = "File pattern", description = "A glob expression that can be used to constrain which directories or source files should be searched. " + "Multiple patterns may be specified, separated by a semicolon `;`. " + "If multiple patterns are supplied any of the patterns matching will be interpreted as a match. " + "When not set, all source files are searched. ", required = false, example = "**/*.java")
    @Nullable
    private final String filePattern;

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        TreeVisitor<?, ExecutionContext> visitor = new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                SourceFile sourceFile = (SourceFile) requireNonNull(tree);
                if (sourceFile instanceof Quark || sourceFile instanceof Remote || sourceFile instanceof Binary) {
                    return sourceFile;
                }
                PlainText plainText = PlainTextParser.convert(sourceFile);
                String searchStr = find;
                if (!Boolean.TRUE.equals(regex)) {
                    searchStr = Pattern.quote(searchStr);
                }
                int patternOptions = 0;
                if (!Boolean.TRUE.equals(caseSensitive)) {
                    patternOptions |= Pattern.CASE_INSENSITIVE;
                }
                if (Boolean.TRUE.equals(multiline)) {
                    patternOptions |= Pattern.MULTILINE;
                }
                if (Boolean.TRUE.equals(dotAll)) {
                    patternOptions |= Pattern.DOTALL;
                }
                Pattern pattern = Pattern.compile(searchStr, patternOptions);
                Matcher matcher = pattern.matcher(plainText.getText());
                String rawText = plainText.getText();
                if (!matcher.find()) {
                    return sourceFile;
                }
                matcher.reset();
                List<PlainText.Snippet> snippets = new ArrayList<>();
                int previousEnd = 0;
                while (matcher.find()) {
                    int matchStart = matcher.start();
                    snippets.add(snippet(rawText.substring(previousEnd, matchStart)));
                    snippets.add(SearchResult.found(snippet(rawText.substring(matchStart, matcher.end()))));
                    previousEnd = matcher.end();
                }
                snippets.add(snippet(rawText.substring(previousEnd)));
                return plainText.withText("").withSnippets(snippets);
            }
        };
        //noinspection DuplicatedCode
        if (filePattern != null) {
            //noinspection unchecked
            TreeVisitor<?, ExecutionContext> check = Preconditions.or(Arrays.stream(filePattern.split(";")).map(FindSourceFiles::new).map(Recipe::getVisitor).toArray(TreeVisitor[]::new));
            visitor = Preconditions.check(check, visitor);
        }
        return visitor;
    }

    private static PlainText.Snippet snippet(String text) {
        return new PlainText.Snippet(Tree.randomId(), Markers.EMPTY, text);
    }

    public Find(final String find, @Nullable final Boolean regex, @Nullable final Boolean caseSensitive, @Nullable final Boolean multiline, @Nullable final Boolean dotAll, @Nullable final String filePattern) {
        this.find = find;
        this.regex = regex;
        this.caseSensitive = caseSensitive;
        this.multiline = multiline;
        this.dotAll = dotAll;
        this.filePattern = filePattern;
    }

    public String getFind() {
        return this.find;
    }

    @Nullable
    public Boolean getRegex() {
        return this.regex;
    }

    @Nullable
    public Boolean getCaseSensitive() {
        return this.caseSensitive;
    }

    @Nullable
    public Boolean getMultiline() {
        return this.multiline;
    }

    @Nullable
    public Boolean getDotAll() {
        return this.dotAll;
    }

    @Nullable
    public String getFilePattern() {
        return this.filePattern;
    }

    @org.openrewrite.internal.lang.NonNull
    @Override
    public String toString() {
        return "Find(find=" + this.getFind() + ", regex=" + this.getRegex() + ", caseSensitive=" + this.getCaseSensitive() + ", multiline=" + this.getMultiline() + ", dotAll=" + this.getDotAll() + ", filePattern=" + this.getFilePattern() + ")";
    }

    @Override
    public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
        if (o == this) return true;
        if (!(o instanceof Find)) return false;
        final Find other = (Find) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$regex = this.getRegex();
        final Object other$regex = other.getRegex();
        if (this$regex == null ? other$regex != null : !this$regex.equals(other$regex)) return false;
        final Object this$caseSensitive = this.getCaseSensitive();
        final Object other$caseSensitive = other.getCaseSensitive();
        if (this$caseSensitive == null ? other$caseSensitive != null : !this$caseSensitive.equals(other$caseSensitive)) return false;
        final Object this$multiline = this.getMultiline();
        final Object other$multiline = other.getMultiline();
        if (this$multiline == null ? other$multiline != null : !this$multiline.equals(other$multiline)) return false;
        final Object this$dotAll = this.getDotAll();
        final Object other$dotAll = other.getDotAll();
        if (this$dotAll == null ? other$dotAll != null : !this$dotAll.equals(other$dotAll)) return false;
        final Object this$find = this.getFind();
        final Object other$find = other.getFind();
        if (this$find == null ? other$find != null : !this$find.equals(other$find)) return false;
        final Object this$filePattern = this.getFilePattern();
        final Object other$filePattern = other.getFilePattern();
        if (this$filePattern == null ? other$filePattern != null : !this$filePattern.equals(other$filePattern)) return false;
        return true;
    }

    protected boolean canEqual(@org.openrewrite.internal.lang.Nullable final Object other) {
        return other instanceof Find;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $regex = this.getRegex();
        result = result * PRIME + ($regex == null ? 43 : $regex.hashCode());
        final Object $caseSensitive = this.getCaseSensitive();
        result = result * PRIME + ($caseSensitive == null ? 43 : $caseSensitive.hashCode());
        final Object $multiline = this.getMultiline();
        result = result * PRIME + ($multiline == null ? 43 : $multiline.hashCode());
        final Object $dotAll = this.getDotAll();
        result = result * PRIME + ($dotAll == null ? 43 : $dotAll.hashCode());
        final Object $find = this.getFind();
        result = result * PRIME + ($find == null ? 43 : $find.hashCode());
        final Object $filePattern = this.getFilePattern();
        result = result * PRIME + ($filePattern == null ? 43 : $filePattern.hashCode());
        return result;
    }
}
