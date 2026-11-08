// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.algorithm;

import jdk.m3.algorithm.ProgressMonitor;
import java.util.ArrayList;
import java.util.List;

//FLLDCIM: RELOCATE - donor blob a947a2c59855 at commit 3385d79a2968 (repository, path, URL and digests: m3/algorithms/recipe/families-20261106/PROVENANCE.tsv); recipe m3/algorithms/recipe/families-20261106/relocate.py v1
public final class StringAlgorithms {
    private StringAlgorithms() {
    }

    public static int[] prefixFunction(CharSequence pattern, ProgressMonitor monitor) {
        AlgorithmSupport.begin(monitor, "kmp-prefix-function", pattern.length());
        try {
            int[] prefix = new int[pattern.length()];
            for (int index = 1; index < pattern.length(); index++) {
                AlgorithmSupport.tick(monitor);
                int candidate = prefix[index - 1];
                while (candidate > 0 && pattern.charAt(index) != pattern.charAt(candidate)) {
                    monitor.checkCanceled();
                    candidate = prefix[candidate - 1];
                }
                if (pattern.charAt(index) == pattern.charAt(candidate)) {
                    candidate++;
                }
                prefix[index] = candidate;
            }
            return prefix;
        } finally {
            monitor.done();
        }
    }

    public static List<Integer> kmpSearch(
            CharSequence text,
            CharSequence pattern,
            ProgressMonitor monitor) {
        if (pattern.length() == 0) {
            return java.util.stream.IntStream.rangeClosed(0, text.length()).boxed().toList();
        }
        int[] prefix = prefixFunction(pattern, monitor);
        AlgorithmSupport.begin(monitor, "kmp-search", text.length());
        try {
            List<Integer> matches = new ArrayList<>();
            int matched = 0;
            for (int index = 0; index < text.length(); index++) {
                AlgorithmSupport.tick(monitor);
                while (matched > 0 && text.charAt(index) != pattern.charAt(matched)) {
                    matched = prefix[matched - 1];
                }
                if (text.charAt(index) == pattern.charAt(matched)) {
                    matched++;
                }
                if (matched == pattern.length()) {
                    matches.add(index - pattern.length() + 1);
                    matched = prefix[matched - 1];
                }
            }
            return List.copyOf(matches);
        } finally {
            monitor.done();
        }
    }

    public static int[] zFunction(CharSequence value, ProgressMonitor monitor) {
        AlgorithmSupport.begin(monitor, "z-function", value.length());
        try {
            int[] z = new int[value.length()];
            int left = 0;
            int right = 0;
            for (int index = 1; index < value.length(); index++) {
                AlgorithmSupport.tick(monitor);
                if (index <= right) {
                    z[index] = Math.min(right - index + 1, z[index - left]);
                }
                while (index + z[index] < value.length()
                        && value.charAt(z[index]) == value.charAt(index + z[index])) {
                    monitor.checkCanceled();
                    z[index]++;
                }
                if (index + z[index] - 1 > right) {
                    left = index;
                    right = index + z[index] - 1;
                }
            }
            return z;
        } finally {
            monitor.done();
        }
    }

    public static String longestPalindrome(String value, ProgressMonitor monitor) {
        if (value.isEmpty()) {
            return "";
        }
        AlgorithmSupport.begin(monitor, "manacher-longest-palindrome", value.length());
        try {
            int length = value.length();
            int[] odd = new int[length];
            int left = 0;
            int right = -1;
            for (int index = 0; index < length; index++) {
                AlgorithmSupport.tick(monitor);
                int radius = index > right
                        ? 1
                        : Math.min(odd[left + right - index], right - index + 1);
                while (index - radius >= 0
                        && index + radius < length
                        && value.charAt(index - radius) == value.charAt(index + radius)) {
                    monitor.checkCanceled();
                    radius++;
                }
                odd[index] = radius;
                if (index + radius - 1 > right) {
                    left = index - radius + 1;
                    right = index + radius - 1;
                }
            }

            int[] even = new int[length];
            left = 0;
            right = -1;
            for (int index = 0; index < length; index++) {
                monitor.checkCanceled();
                int radius = index > right
                        ? 0
                        : Math.min(even[left + right - index + 1], right - index + 1);
                while (index - radius - 1 >= 0
                        && index + radius < length
                        && value.charAt(index - radius - 1) == value.charAt(index + radius)) {
                    radius++;
                }
                even[index] = radius;
                if (index + radius - 1 > right) {
                    left = index - radius;
                    right = index + radius - 1;
                }
            }

            int bestStart = 0;
            int bestLength = 1;
            for (int index = 0; index < length; index++) {
                int oddLength = odd[index] * 2 - 1;
                if (oddLength > bestLength) {
                    bestLength = oddLength;
                    bestStart = index - odd[index] + 1;
                }
                int evenLength = even[index] * 2;
                if (evenLength > bestLength) {
                    bestLength = evenLength;
                    bestStart = index - even[index];
                }
            }
            return value.substring(bestStart, bestStart + bestLength);
        } finally {
            monitor.done();
        }
    }

    public static boolean wildcardMatch(
            CharSequence text,
            CharSequence pattern,
            ProgressMonitor monitor) {
        AlgorithmSupport.begin(monitor, "wildcard-greedy-match", text.length() + pattern.length());
        try {
            int textIndex = 0;
            int patternIndex = 0;
            int star = -1;
            int match = 0;
            while (textIndex < text.length()) {
                AlgorithmSupport.tick(monitor);
                if (patternIndex < pattern.length()
                        && (pattern.charAt(patternIndex) == '?'
                                || pattern.charAt(patternIndex) == text.charAt(textIndex))) {
                    textIndex++;
                    patternIndex++;
                } else if (patternIndex < pattern.length()
                        && pattern.charAt(patternIndex) == '*') {
                    star = patternIndex++;
                    match = textIndex;
                } else if (star >= 0) {
                    patternIndex = star + 1;
                    textIndex = ++match;
                } else {
                    return false;
                }
            }
            while (patternIndex < pattern.length() && pattern.charAt(patternIndex) == '*') {
                monitor.checkCanceled();
                patternIndex++;
            }
            return patternIndex == pattern.length();
        } finally {
            monitor.done();
        }
    }
}
