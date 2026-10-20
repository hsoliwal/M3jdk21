package com.github.openjson.issues;

import com.github.openjson.JSONArray;
import com.github.openjson.JSONException;
import com.github.openjson.JSONObject;
import com.github.openjson.JSONTokener;
import org.junit.Test;

import static com.github.openjson.JSONTokenerTest.assertIsMaxNestingLevelException;

public class Issue18Test {
    private final static int TOO_DEEP_NESTING = 9999;

    public static String buildNestedDoc(int nesting, String open, String close, String content) {
        StringBuilder sb = new StringBuilder(nesting * (open.length() + close.length()));
        for (int i = 0; i < nesting; ++i) {
            sb.append(open);
            if ((i & 31) == 0) {
                sb.append("\n");
            }
        }
        sb.append("\n").append(content).append("\n");
        for (int i = 0; i < nesting; ++i) {
            sb.append(close);
            if ((i & 31) == 0) {
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    @Test
    public void test1() {
        String tooDeepArray = buildNestedDoc(TOO_DEEP_NESTING, "[ ", "] ", "x");
        JSONTokener jsonTokener = new JSONTokener(tooDeepArray);
        Throwable throwable = null;
        try {
            new JSONArray(jsonTokener);
        } catch (JSONException e) {
            throwable = e;
        }
        assertIsMaxNestingLevelException(throwable);
    }

    @Test
    public void test2() {
        String tooDeepObject = "{" + buildNestedDoc(TOO_DEEP_NESTING, "\"a\": { ", "} ", "y") + "}";
        Throwable throwable = null;
        JSONTokener jsonTokener2 = new JSONTokener(tooDeepObject);
        try {
            new JSONObject(jsonTokener2);
        } catch (JSONException e) {
            throwable = e;
        }
        assertIsMaxNestingLevelException(throwable);
    }
}
