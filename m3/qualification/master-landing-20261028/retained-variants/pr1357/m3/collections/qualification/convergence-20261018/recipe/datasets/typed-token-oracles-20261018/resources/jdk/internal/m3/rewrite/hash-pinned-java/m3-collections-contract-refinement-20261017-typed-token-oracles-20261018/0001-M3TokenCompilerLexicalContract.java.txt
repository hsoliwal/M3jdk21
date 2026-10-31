/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright 2026 Hitesh Soliwal and contributors
 * @test
 * @summary Actual javac lexical tokens distinguish identifiers from comments and literal content
 * @modules java.base/jdk.internal.mindex jdk.compiler/com.sun.tools.javac.parser jdk.compiler/com.sun.tools.javac.util
 * @run main/othervm M3TokenCompilerLexicalContract
 */
import java.util.ArrayList;
import com.sun.tools.javac.parser.ScannerFactory;
import com.sun.tools.javac.parser.Tokens.TokenKind;
import com.sun.tools.javac.util.Context;
import jdk.internal.mindex.M3TokenRegexPlan;
import jdk.internal.mindex.M3TokenRegexPlan.Input;
import jdk.internal.mindex.M3TokenRegexPlan.Predicate;

public class M3TokenCompilerLexicalContract {
    public static void main(String[] args) {
        String source = "class C { int target; String s=\"target\"; String t=\"\"\"\n target\n\"\"\"; // target\n"
                + "void f(){ target++; int target2=0; \\u0074arget++; } }";
        var scanner = ScannerFactory.instance(new Context()).newScanner(source, true);
        ArrayList<Integer> kinds = new ArrayList<>(), starts = new ArrayList<>(), ends = new ArrayList<>();
        ArrayList<Long> names = new ArrayList<>(), flags = new ArrayList<>();
        for (scanner.nextToken(); scanner.token().kind != TokenKind.EOF; scanner.nextToken()) {
            var token = scanner.token();
            boolean identifier = token.kind == TokenKind.IDENTIFIER;
            kinds.add(token.kind.ordinal());
            names.add(identifier && token.name().contentEquals("target") ? 1L : 0L);
            flags.add(identifier ? 1L : 0L);
            starts.add(token.pos);
            ends.add(token.endPos);
        }
        Object domain = new Object(); // Lexical name IDs only, not attributed compiler symbols.
        Input input = Input.of(domain, kinds.stream().mapToInt(Integer::intValue).toArray(),
                names.stream().mapToLong(Long::longValue).toArray(), flags.stream().mapToLong(Long::longValue).toArray());
        Predicate target = new Predicate(TokenKind.IDENTIFIER.ordinal(), 1L, 1L, 0,
                M3TokenRegexPlan.KIND | M3TokenRegexPlan.SYMBOL);
        M3TokenRegexPlan plan = M3TokenRegexPlan.compile(domain, new Predicate[]{target}, 0);
        int offset = 0, count = 0;
        boolean unicode = false;
        while (offset < input.tokenCount()) {
            var match = plan.find(input.slice(offset, input.tokenCount()));
            if (match == null) break;
            if (match.endToken() != match.startToken() + 1) throw new AssertionError("not one token");
            int at = offset + match.startToken();
            String raw = source.substring(starts.get(at), ends.get(at));
            if (raw.equals("\\u0074arget")) unicode = true;
            else if (!raw.equals("target")) throw new AssertionError("wrong lexical token " + raw);
            offset += match.endToken();
            count++;
        }
        if (count != 3 || !unicode) throw new AssertionError("identifier/comment/literal/Unicode boundary");
        Predicate keyword = new Predicate(TokenKind.INT.ordinal(), 0, 0, 0, M3TokenRegexPlan.KIND);
        Predicate semi = new Predicate(TokenKind.SEMI.ordinal(), 0, 0, 0, M3TokenRegexPlan.KIND);
        var declaration = M3TokenRegexPlan.compile(domain, new Predicate[]{keyword, target, semi}, 0).find(input);
        if (declaration == null || declaration.endToken() - declaration.startToken() != 3) {
            throw new AssertionError("typed declaration sequence");
        }
        System.out.println("M3TokenCompilerLexicalContract PASS targetIdentifiers=3 UnicodeEscape=true commentsAndLiteralContentExcluded=true");
    }
}
