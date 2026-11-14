// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// Modified for M3JDK: attributed package relocation and product module integration.
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.llm.internal;

import java.util.*;
import java.util.regex.*;

/** Deterministic extractive Luhn-style summarizer for cached chunk/node summaries. */
public final class LuhnSummarizer {
    private static final Pattern WORD=Pattern.compile("[\\p{L}\\p{N}_-]+");
    private LuhnSummarizer() {}
    public static List<String> summarize(String text,int maxSentences){if(maxSentences<1)return List.of();String[] sentences=text.split("(?<=[.!?])\\s+|\\R+");Map<String,Integer>freq=new HashMap<>();for(String s:sentences){Matcher m=WORD.matcher(s.toLowerCase(Locale.ROOT));while(m.find())if(m.group().length()>2)freq.merge(m.group(),1,Integer::sum);}record S(int i,long score,String text){}List<S>scores=new ArrayList<>();for(int i=0;i<sentences.length;i++){String s=sentences[i].trim();if(s.isEmpty())continue;long score=0;Matcher m=WORD.matcher(s.toLowerCase(Locale.ROOT));while(m.find())score+=freq.getOrDefault(m.group(),0);scores.add(new S(i,score,s));}scores.sort(Comparator.comparingLong(S::score).reversed().thenComparingInt(S::i));List<S>top=scores.subList(0,Math.min(maxSentences,scores.size())).stream().sorted(Comparator.comparingInt(S::i)).toList();return top.stream().map(S::text).toList();}
}
