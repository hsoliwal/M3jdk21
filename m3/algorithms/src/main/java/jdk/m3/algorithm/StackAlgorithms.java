// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.algorithm;

import jdk.m3.algorithm.ProgressMonitor;
import java.util.ArrayDeque;
import java.util.Map;
import jdk.m3.collections.IntArrayDeque;
import java.util.Arrays;

//FLLDCIM: RELOCATE - donor blob dcc4a2474804 at commit 3385d79a2968 (repository, path, URL and digests: m3/algorithms/recipe/families-20261106/PROVENANCE.tsv); recipe m3/algorithms/recipe/families-20261106/relocate.py v1
public final class StackAlgorithms {
    private StackAlgorithms() {
    }

    public static boolean validBrackets(CharSequence input, ProgressMonitor monitor) {
        AlgorithmSupport.begin(monitor, "valid-brackets", input.length());
        try {
            ArrayDeque<Character> stack = new ArrayDeque<>();
            Map<Character, Character> closing = Map.of(')', '(', ']', '[', '}', '{');
            for (int index = 0; index < input.length(); index++) {
                AlgorithmSupport.tick(monitor);
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

    public static int[] nextGreater(int[] values, ProgressMonitor monitor) {
        AlgorithmSupport.begin(monitor, "next-greater", values.length);
        try {
            int[] result = new int[values.length];
            java.util.Arrays.fill(result, -1);
            ArrayDeque<Integer> stack = new ArrayDeque<>();
            for (int index = 0; index < values.length; index++) {
                AlgorithmSupport.tick(monitor);
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

    public static int[] dailyTemperatures(int[] values, ProgressMonitor monitor) {
        AlgorithmSupport.begin(monitor, "daily-temperatures", values.length);
        try {
            int[] result = new int[values.length];
            ArrayDeque<Integer> stack = new ArrayDeque<>();
            for (int index = 0; index < values.length; index++) {
                AlgorithmSupport.tick(monitor);
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

    public static long largestRectangleArea(int[] heights, ProgressMonitor monitor) {
        AlgorithmSupport.begin(monitor, "largest-histogram-rectangle", heights.length + 1L);
        try {
            ArrayDeque<Integer> stack = new ArrayDeque<>();
            long best = 0L;
            for (int index = 0; index <= heights.length; index++) {
                AlgorithmSupport.tick(monitor);
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

    public static long evaluateRpn(String[] tokens, ProgressMonitor monitor) {
        AlgorithmSupport.begin(monitor, "evaluate-rpn", tokens.length);
        try {
            ArrayDeque<Long> stack = new ArrayDeque<>();
            for (String token : tokens) {
                AlgorithmSupport.tick(monitor);
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

    //FLLDCIM: FOLD - member MonotonicStackElements.nextSmaller from donor blob 0a288bd5c9f2 (donor blob 0a288bd5c9f2 at commit 45da5f035 (repository, path and digests: m3/algorithms/recipe/monotonic-stack-fold/PROVENANCE.tsv)); recipe m3/tooling/jdk-naming/fold_members.py v1, map m3/algorithms/recipe/monotonic-stack-fold/FOLD_MAP.tsv
    /**
     * Next Smaller Element for each position.
     *
     * @param nums
     *            input array
     * @return array where result[i] is next smaller element of nums[i], or -1
     */
    public static int[] nextSmaller(int[] nums) {
        int n = nums.length;
        int[] result = new int[n];
        Arrays.fill(result, -1);
        IntArrayDeque stack = new IntArrayDeque();

        for (int i = 0; i < n; i++) {
            while (!stack.isEmpty() && nums[stack.getFirst()] > nums[i]) {
                result[stack.removeFirst()] = nums[i];
            }
            stack.addFirst(i);
        }
        return result;
    }

    //FLLDCIM: FOLD - member MonotonicStackElements.previousGreater from donor blob 0a288bd5c9f2 (donor blob 0a288bd5c9f2 at commit 45da5f035 (repository, path and digests: m3/algorithms/recipe/monotonic-stack-fold/PROVENANCE.tsv)); recipe m3/tooling/jdk-naming/fold_members.py v1, map m3/algorithms/recipe/monotonic-stack-fold/FOLD_MAP.tsv
    /** Previous Greater Element for each position. */
    public static int[] previousGreater(int[] nums) {
        int n = nums.length;
        int[] result = new int[n];
        Arrays.fill(result, -1);
        IntArrayDeque stack = new IntArrayDeque();

        for (int i = 0; i < n; i++) {
            while (!stack.isEmpty() && nums[stack.getFirst()] <= nums[i]) {
                stack.removeFirst();
            }
            if (!stack.isEmpty()) {
                result[i] = nums[stack.getFirst()];
            }
            stack.addFirst(i);
        }
        return result;
    }

    //FLLDCIM: FOLD - member MonotonicStackElements.previousSmaller from donor blob 0a288bd5c9f2 (donor blob 0a288bd5c9f2 at commit 45da5f035 (repository, path and digests: m3/algorithms/recipe/monotonic-stack-fold/PROVENANCE.tsv)); recipe m3/tooling/jdk-naming/fold_members.py v1, map m3/algorithms/recipe/monotonic-stack-fold/FOLD_MAP.tsv
    /** Previous Smaller Element for each position. */
    public static int[] previousSmaller(int[] nums) {
        int n = nums.length;
        int[] result = new int[n];
        Arrays.fill(result, -1);
        IntArrayDeque stack = new IntArrayDeque();

        for (int i = 0; i < n; i++) {
            while (!stack.isEmpty() && nums[stack.getFirst()] >= nums[i]) {
                stack.removeFirst();
            }
            if (!stack.isEmpty()) {
                result[i] = nums[stack.getFirst()];
            }
            stack.addFirst(i);
        }
        return result;
    }

    //FLLDCIM: FOLD - member MonotonicStackElements.nextGreaterIndex from donor blob 0a288bd5c9f2 (donor blob 0a288bd5c9f2 at commit 45da5f035 (repository, path and digests: m3/algorithms/recipe/monotonic-stack-fold/PROVENANCE.tsv)); recipe m3/tooling/jdk-naming/fold_members.py v1, map m3/algorithms/recipe/monotonic-stack-fold/FOLD_MAP.tsv
    /**
     * Next Greater Element Index for each position. Returns -1 if no greater
     * element exists.
     */
    public static int[] nextGreaterIndex(int[] nums) {
        int n = nums.length;
        int[] result = new int[n];
        Arrays.fill(result, -1);
        IntArrayDeque stack = new IntArrayDeque();

        for (int i = 0; i < n; i++) {
            while (!stack.isEmpty() && nums[stack.getFirst()] < nums[i]) {
                result[stack.removeFirst()] = i;
            }
            stack.addFirst(i);
        }
        return result;
    }

    //FLLDCIM: FOLD - member MonotonicStackElements.previousSmallerIndex from donor blob 0a288bd5c9f2 (donor blob 0a288bd5c9f2 at commit 45da5f035 (repository, path and digests: m3/algorithms/recipe/monotonic-stack-fold/PROVENANCE.tsv)); recipe m3/tooling/jdk-naming/fold_members.py v1, map m3/algorithms/recipe/monotonic-stack-fold/FOLD_MAP.tsv
    /** Previous Smaller Element Index for each position. */
    public static int[] previousSmallerIndex(int[] nums) {
        int n = nums.length;
        int[] result = new int[n];
        Arrays.fill(result, -1);
        IntArrayDeque stack = new IntArrayDeque();

        for (int i = 0; i < n; i++) {
            while (!stack.isEmpty() && nums[stack.getFirst()] >= nums[i]) {
                stack.removeFirst();
            }
            if (!stack.isEmpty()) {
                result[i] = stack.getFirst();
            }
            stack.addFirst(i);
        }
        return result;
    }
    //FLLDCIM: FOLD - member MonotonicStackRectangle.maximalRectangle from donor blob 93e559ad6b46 (donor blob 93e559ad6b46 at commit 45da5f035 (repository, path and digests: m3/algorithms/recipe/monotonic-stack-fold/PROVENANCE.tsv)); recipe m3/tooling/jdk-naming/fold_members.py v1, map m3/algorithms/recipe/monotonic-stack-fold/FOLD_MAP.tsv
    /** Maximal Rectangle in Binary Matrix (LeetCode 85). */
    public static int maximalRectangle(int[][] matrix) {
        if (matrix.length == 0) return 0;

        int m = matrix.length, n = matrix[0].length;
        int[] heights = new int[n];
        int maxArea = 0;

        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                heights[j] = (matrix[i][j] == 1) ? heights[j] + 1 : 0;
            }
            maxArea = Math.max(maxArea, (int) largestRectangleArea(heights, ProgressMonitor.none()));
        }
        return maxArea;
    }
    //FLLDCIM: FOLD - member MonotonicStackSubarray.stockSpan from donor blob ede7785f71a7 (donor blob ede7785f71a7 at commit 45da5f035 (repository, path and digests: m3/algorithms/recipe/monotonic-stack-fold/PROVENANCE.tsv)); recipe m3/tooling/jdk-naming/fold_members.py v1, map m3/algorithms/recipe/monotonic-stack-fold/FOLD_MAP.tsv
    /** Stock Span Problem. How many consecutive days price was <= today's price. */
    public static int[] stockSpan(int[] prices) {
        int n = prices.length;
        int[] result = new int[n];
        IntArrayDeque stack = new IntArrayDeque();

        for (int i = 0; i < n; i++) {
            while (!stack.isEmpty() && prices[stack.getFirst()] <= prices[i]) {
                stack.removeFirst();
            }
            result[i] = stack.isEmpty() ? i + 1 : i - stack.getFirst();
            stack.addFirst(i);
        }
        return result;
    }

    //FLLDCIM: FOLD - member MonotonicStackSubarray.sumSubarrayMins from donor blob ede7785f71a7 (donor blob ede7785f71a7 at commit 45da5f035 (repository, path and digests: m3/algorithms/recipe/monotonic-stack-fold/PROVENANCE.tsv)); recipe m3/tooling/jdk-naming/fold_members.py v1, map m3/algorithms/recipe/monotonic-stack-fold/FOLD_MAP.tsv
    /** Sum of Subarray Minimums (LeetCode 907). */
    public static int sumSubarrayMins(int[] arr) {
        int MOD = 1_000_000_007;
        int n = arr.length;
        long result = 0;

        // Previous smaller and next smaller indices
        int[] prevSmaller = new int[n];
        int[] nextSmaller = new int[n];
        Arrays.fill(prevSmaller, -1);
        Arrays.fill(nextSmaller, n);

        IntArrayDeque stack = new IntArrayDeque();

        for (int i = 0; i < n; i++) {
            while (!stack.isEmpty() && arr[stack.getFirst()] >= arr[i]) {
                stack.removeFirst();
            }
            if (!stack.isEmpty()) {
                prevSmaller[i] = stack.getFirst();
            }
            stack.addFirst(i);
        }

        stack.clear();

        for (int i = n - 1; i >= 0; i--) {
            while (!stack.isEmpty() && arr[stack.getFirst()] > arr[i]) {
                stack.removeFirst();
            }
            if (!stack.isEmpty()) {
                nextSmaller[i] = stack.getFirst();
            }
            stack.addFirst(i);
        }

        for (int i = 0; i < n; i++) {
            long left = i - prevSmaller[i];
            long right = nextSmaller[i] - i;
            result = (result + arr[i] * left * right) % MOD;
        }

        return (int) result;
    }

    //FLLDCIM: FOLD - member MonotonicStackSubarray.sumSubarrayMaxs from donor blob ede7785f71a7 (donor blob ede7785f71a7 at commit 45da5f035 (repository, path and digests: m3/algorithms/recipe/monotonic-stack-fold/PROVENANCE.tsv)); recipe m3/tooling/jdk-naming/fold_members.py v1, map m3/algorithms/recipe/monotonic-stack-fold/FOLD_MAP.tsv
    /** Sum of Subarray Maximums. */
    public static int sumSubarrayMaxs(int[] arr) {
        int MOD = 1_000_000_007;
        int n = arr.length;
        long result = 0;

        int[] prevGreater = new int[n];
        int[] nextGreater = new int[n];
        Arrays.fill(prevGreater, -1);
        Arrays.fill(nextGreater, n);

        IntArrayDeque stack = new IntArrayDeque();

        for (int i = 0; i < n; i++) {
            while (!stack.isEmpty() && arr[stack.getFirst()] <= arr[i]) {
                stack.removeFirst();
            }
            if (!stack.isEmpty()) {
                prevGreater[i] = stack.getFirst();
            }
            stack.addFirst(i);
        }

        stack.clear();

        for (int i = n - 1; i >= 0; i--) {
            while (!stack.isEmpty() && arr[stack.getFirst()] < arr[i]) {
                stack.removeFirst();
            }
            if (!stack.isEmpty()) {
                nextGreater[i] = stack.getFirst();
            }
            stack.addFirst(i);
        }

        for (int i = 0; i < n; i++) {
            long left = i - prevGreater[i];
            long right = nextGreater[i] - i;
            result = (result + (long) arr[i] * left * right) % MOD;
        }

        return (int) result;
    }
    //FLLDCIM: FOLD - member MonotonicStackMisc.longestValidParentheses from donor blob 6f7721ce52b9 (donor blob 6f7721ce52b9 at commit 45da5f035 (repository, path and digests: m3/algorithms/recipe/monotonic-stack-fold/PROVENANCE.tsv)); recipe m3/tooling/jdk-naming/fold_members.py v1, map m3/algorithms/recipe/monotonic-stack-fold/FOLD_MAP.tsv
    /** Longest Valid Parentheses (LeetCode 32). */
    public static int longestValidParentheses(String s) {
        IntArrayDeque stack = new IntArrayDeque();
        stack.addFirst(-1);
        int maxLen = 0;

        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == '(') {
                stack.addFirst(i);
            } else {
                stack.removeFirst();
                if (stack.isEmpty()) {
                    stack.addFirst(i);
                } else {
                    maxLen = Math.max(maxLen, i - stack.getFirst());
                }
            }
        }
        return maxLen;
    }

    //FLLDCIM: FOLD - member MonotonicStackMisc.minRemoveToMakeValid from donor blob 6f7721ce52b9 (donor blob 6f7721ce52b9 at commit 45da5f035 (repository, path and digests: m3/algorithms/recipe/monotonic-stack-fold/PROVENANCE.tsv)); recipe m3/tooling/jdk-naming/fold_members.py v1, map m3/algorithms/recipe/monotonic-stack-fold/FOLD_MAP.tsv
    /**
     * Remove Invalid Parentheses to make string valid. Returns minimum removals
     * needed.
     */
    public static int minRemoveToMakeValid(String s) {
        // Only unmatched-opening cardinality is observed; no element storage is needed.
        int unmatched = 0;
        int removals = 0;

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '(') {
                unmatched++;
            } else if (c == ')') {
                if (unmatched == 0) {
                    removals++;
                } else {
                    unmatched--;
                }
            }
        }
        return removals + unmatched;
    }

}
