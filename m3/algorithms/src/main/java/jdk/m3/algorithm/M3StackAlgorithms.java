// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.algorithm;

import jdk.m3.algorithm.M3ProgressMonitor;
import java.util.ArrayDeque;
import java.util.Map;

//FLLDCIM: RELOCATE - donor blob dcc4a2474804 at commit 3385d79a2968 (repository, path, URL and digests: m3/algorithms/recipe/families-20261106/PROVENANCE.tsv); recipe m3/algorithms/recipe/families-20261106/relocate.py v1
public final class M3StackAlgorithms {
    private M3StackAlgorithms() {
    }

    public static boolean validBrackets(CharSequence input, M3ProgressMonitor monitor) {
        M3AlgorithmSupport.begin(monitor, "valid-brackets", input.length());
        try {
            ArrayDeque<Character> stack = new ArrayDeque<>();
            Map<Character, Character> closing = Map.of(')', '(', ']', '[', '}', '{');
            for (int index = 0; index < input.length(); index++) {
                M3AlgorithmSupport.tick(monitor);
                char c = input.charAt(index);
                if (c == '(' || c == '[' || c == '{') {
                    stack.push(c);
                } else if (closing.containsKey(c)) {
                    if (stack.isEmpty() || stack.pop() != closing.get(c)) {
                        return false;
                    }
                }
            }
            return stack.isEmpty();
        } finally {
            monitor.done();
        }
    }

    public static int[] nextGreater(int[] values, M3ProgressMonitor monitor) {
        M3AlgorithmSupport.begin(monitor, "next-greater", values.length);
        try {
            int[] result = new int[values.length];
            java.util.Arrays.fill(result, -1);
            ArrayDeque<Integer> stack = new ArrayDeque<>();
            for (int index = 0; index < values.length; index++) {
                M3AlgorithmSupport.tick(monitor);
                while (!stack.isEmpty() && values[stack.peek()] < values[index]) {
                    result[stack.pop()] = values[index];
                }
                stack.push(index);
            }
            return result;
        } finally {
            monitor.done();
        }
    }

    public static int[] dailyTemperatures(int[] values, M3ProgressMonitor monitor) {
        M3AlgorithmSupport.begin(monitor, "daily-temperatures", values.length);
        try {
            int[] result = new int[values.length];
            ArrayDeque<Integer> stack = new ArrayDeque<>();
            for (int index = 0; index < values.length; index++) {
                M3AlgorithmSupport.tick(monitor);
                while (!stack.isEmpty() && values[stack.peek()] < values[index]) {
                    int previous = stack.pop();
                    result[previous] = index - previous;
                }
                stack.push(index);
            }
            return result;
        } finally {
            monitor.done();
        }
    }

    public static long largestRectangleArea(int[] heights, M3ProgressMonitor monitor) {
        M3AlgorithmSupport.begin(monitor, "largest-histogram-rectangle", heights.length + 1L);
        try {
            ArrayDeque<Integer> stack = new ArrayDeque<>();
            long best = 0L;
            for (int index = 0; index <= heights.length; index++) {
                M3AlgorithmSupport.tick(monitor);
                int height = index == heights.length ? 0 : heights[index];
                while (!stack.isEmpty()
                        && heights[stack.peek()] > height) {
                    int top = stack.pop();
                    int left = stack.isEmpty() ? -1 : stack.peek();
                    long width = index - left - 1L;
                    best = Math.max(best, width * heights[top]);
                }
                if (index < heights.length) {
                    stack.push(index);
                }
            }
            return best;
        } finally {
            monitor.done();
        }
    }

    public static long evaluateRpn(String[] tokens, M3ProgressMonitor monitor) {
        M3AlgorithmSupport.begin(monitor, "evaluate-rpn", tokens.length);
        try {
            ArrayDeque<Long> stack = new ArrayDeque<>();
            for (String token : tokens) {
                M3AlgorithmSupport.tick(monitor);
                switch (token) {
                    case "+", "-", "*", "/" -> {
                        long right = stack.pop();
                        long left = stack.pop();
                        stack.push(switch (token) {
                            case "+" -> left + right;
                            case "-" -> left - right;
                            case "*" -> left * right;
                            case "/" -> left / right;
                            default -> throw new AssertionError();
                        });
                    }
                    default -> stack.push(Long.parseLong(token));
                }
            }
            if (stack.size() != 1) {
                throw new IllegalArgumentException("invalid RPN expression");
            }
            return stack.pop();
        } finally {
            monitor.done();
        }
    }
}
