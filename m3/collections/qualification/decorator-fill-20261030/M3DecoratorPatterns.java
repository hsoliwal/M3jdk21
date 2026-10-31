/*
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 */

import java.util.Map;
import java.util.regex.Pattern;

/**
 * DECORATOR_FILL pattern atoms: the regular expressions the classifier matches method bodies
 * with, and the WRAP table of Commons wrapper factories to their JDK owners. Data only;
 * {@code M3DecoratorFill} reads them.
 */
final class M3DecoratorPatterns {
    private M3DecoratorPatterns() {
    }

    static final Pattern METHOD = Pattern.compile(
            "(?m)^    (public|private) (.+) \\{\\R        throw new " +
                    "UnsupportedOperationException\\(M3_SCAFFOLD_MESSAGE\\);\\R    }\\R");

    /**
     * WRAP table: Commons wrapper factory -> owner expression; %s is the delegated call, %b the
     * backing.
     */
    static final Map<String, String> WRAP = Map.ofEntries(
            Map.entry("UnmodifiableIterator.unmodifiableIterator:iterator",
                    "java.util.Collections.unmodifiableCollection(%b).iterator()"),
            Map.entry("UnmodifiableIterator.unmodifiableIterator:descendingIterator",
                    "java.util.Collections.unmodifiableNavigableSet(%b).descendingIterator()"),
            Map.entry("UnmodifiableListIterator.unmodifiableListIterator:listIterator",
                    "java.util.Collections.unmodifiableList(%b).%c"),
            Map.entry("UnmodifiableSortedSet.unmodifiableSortedSet",
                    "java.util.Collections.unmodifiableSortedSet(%s)"),
            Map.entry("UnmodifiableNavigableSet.unmodifiableNavigableSet",
                    "java.util.Collections.unmodifiableNavigableSet(%s)"),
            Map.entry("UnmodifiableSet.unmodifiableSet",
                    "java.util.Collections.unmodifiableSet(%s)"),
            Map.entry("UnmodifiableList.unmodifiableList",
                    "java.util.Collections.unmodifiableList(%s)"),
            // v1.6: the Unmodifiable map family
            Map.entry("UnmodifiableCollection.unmodifiableCollection",
                    "java.util.Collections.unmodifiableCollection(%s)"),
            Map.entry("UnmodifiableEntrySet.unmodifiableEntrySet:entrySet",
                    "java.util.Collections.unmodifiableMap(%b).entrySet()"),
            Map.entry("UnmodifiableSortedMap.unmodifiableSortedMap",
                    "java.util.Collections.unmodifiableSortedMap(%s)"),
            Map.entry("UnmodifiableMap.unmodifiableMap",
                    "java.util.Collections.unmodifiableMap(%s)"));

    static final String LIT = "\"(?:[^\"\\\\]|\\\\.)*\"";

    static final Pattern LITERAL = Pattern.compile(LIT);

    static final Pattern INVERSE_CACHE = Pattern.compile(
            "^if \\(inverse == null\\) \\{ inverse = new (\\w+)<>\\(decorated\\(\\)\\.(\\w+)"
            + "\\(\\)\\); inverse\\.inverse = this; \\} return inverse;$");

    /**
     * LANES_CONSTRUCTOR statement shapes s1..s4 (normalized Commons constructor text, super(..)
     * chain flattened).
     */
    static final Pattern CTOR_STMT = Pattern.compile(
            "this\\.(\\w+) = Objects\\.requireNonNull\\((\\w+), (" + LIT + ")\\);"
            + "|this\\.(\\w+) = (\\w+);|for \\(final (\\w+) (\\w+) : (\\w+)\\) \\{ " +
                    "validate\\(\\7\\); \\}|(\\w+)\\.forEach\\(this::validate\\);");

    /**
     * GUARD_DELEGATE guard forms: elem, entry, each, each-entry; group 9 is the rest of the body.
     */
    static final Pattern VALIDATE_GUARD = Pattern.compile(
            "^(?:validate\\((\\w+)\\);|validate\\((\\w+), (\\w+)\\);"
            + "|for \\(final (\\w+) (\\w+) : (\\w+)\\) \\{ validate\\(\\5\\); \\}"
            + "|for \\(final Map\\.Entry<\\? extends K, \\? extends V> (\\w+) : " +
                    "(\\w+)\\.entrySet\\(\\)\\) \\{ "
            + "validate\\(\\7\\.getKey\\(\\), \\7\\.getValue\\(\\)\\); \\}) (.+)$");

    /**
     * LANE_CHECK statement of a lane member: if ([L != null && ]!L.test(p)) { throw new
     * IllegalArgumentException(MSG); }.
     */
    static final Pattern LANE_CHECK = Pattern.compile(
            "if \\((?:(\\w+) != null && )?!(\\w+)\\.test\\((\\w+)\\)\\) \\{ "
            + "throw new IllegalArgumentException\\(((?:" + LIT + "|\\w+)(?: \\+ (?:" + LIT
                    + "|\\w+))*)\\); \\}");

    static final Pattern CHECKED_ENTRY_SET = Pattern.compile(
            "^if \\(isSetValueChecking\\(\\)\\) \\{ return new (\\w+)"
            + "\\(decorated\\(\\)\\.entrySet\\(\\), this\\); \\} return " +
                    "decorated\\(\\)\\.entrySet\\(\\);$");

    static final String GENERATED_BLOCK = "(?s)/\\*\n \\* GENERATED by .*? \\*/";

    static final Pattern TRANSFORM_ARG =
            Pattern.compile("transform(Key|Value|Map)?\\((\\w+)\\)");
}
