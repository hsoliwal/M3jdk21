// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Line layer that runs a pattern over each decoded line as the main stream moves ({@code grep}).
 * Lines split on LF, CR and CRLF like {@code BufferedReader}; memory is bounded by the longest line.
 */
final class M3FileGrepShadow implements M3FileCharShadow {
    private final M3File file;
    private final Matcher matcher;
    private final StringBuilder line = new StringBuilder();
    private final List<M3FileMatch> matches = new ArrayList<>();
    private long number;
    private boolean afterCr;
    private boolean pending;

    M3FileGrepShadow(M3File file, Pattern pattern) {
        this.file = file;
        this.matcher = pattern.matcher(line);
    }

    @Override
    public void update(char[] chars, int offset, int length) {
        for (int i = offset; i < offset + length; i++) {
            char c = chars[i];
            if (c == '\n' && afterCr) {
                afterCr = false;
                continue;
            }
            afterCr = c == '\r';
            if (c == '\n' || c == '\r') {
                endLine();
            } else {
                line.append(c);
                pending = true;
            }
        }
    }

    @Override
    public void finish() {
        if (pending) endLine();
    }

    List<M3FileMatch> matches() {
        return matches;
    }

    private void endLine() {
        number++;
        if (matcher.reset().find()) matches.add(new M3FileMatch(file, number, line.toString()));
        line.setLength(0);
        pending = false;
    }
}
