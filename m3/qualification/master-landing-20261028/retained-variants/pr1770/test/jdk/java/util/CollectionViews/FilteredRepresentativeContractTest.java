/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: GPL-2.0-only WITH Classpath-exception-2.0
 */
/*
 * @test
 * @summary M3JDK21 backed filtered views use stored visible members and original set identity
 * @run main/othervm FilteredRepresentativeContractTest
 */
import java.util.ArrayList;
import java.util.Collection;
import java.util.CollectionViews;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.Comparator;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

public class FilteredRepresentativeContractTest {
    private record Token(int id, boolean visible) {
        @Override public boolean equals(Object obj) {
            return obj instanceof Token other && id == other.id;
        }
        @Override public int hashCode() { return id; }
    }

    private static long checks;

    private static void check(boolean success, String detail) {
        checks++;
        if (!success) throw new AssertionError("contract #" + checks + ": " + detail);
    }

    private static void storedListOccurrences() {
        Token hidden = new Token(7, false);
        Token shown = new Token(7, true);
        ArrayList<Token> owner = new ArrayList<>(List.of(hidden, shown));
        Collection<Token> view = CollectionViews.filtered(owner, Token::visible);
        check(view.size() == 1, "one physical visible occurrence");
        check(view.contains(new Token(7, false)), "visible stored representative, not probe");
        check(view.remove(new Token(7, false)), "remove visible representative");
        check(owner.size() == 1 && owner.getFirst() == hidden, "hidden equal reference survives");
        check(!view.contains(new Token(7, true)), "hidden only is not visible");
        check(!view.remove(new Token(7, true)), "hidden only removal refused");
        owner.add(shown);
        owner.add(new Token(7, true));
        check(view.remove(new Token(7, false)), "remove first visible duplicate");
        check(owner.size() == 2 && owner.getFirst() == hidden && owner.get(1).visible(),
                "preserve hidden and remaining visible order");

        CopyOnWriteArrayList<Token> cow = new CopyOnWriteArrayList<>(
                List.of(new Token(4, false), new Token(4, true)));
        Collection<Token> cowView = CollectionViews.filtered(cow, Token::visible);
        check(cowView.remove(new Token(4, false)), "copy-on-write index-based removal");
        check(cow.size() == 1 && !cow.getFirst().visible(),
                "COW hidden equal element remains");
    }

    private static void ownerSetIdentityAndEquality() {
        Token hidden = new Token(3, false);
        Token shown = new Token(3, true);
        Set<Token> identity = Collections.newSetFromMap(new IdentityHashMap<>());
        identity.add(hidden);
        identity.add(shown);
        Set<Token> ids = CollectionViews.filtered(identity, Token::visible);
        check(!ids.contains(new Token(3, true)), "identity set rejects external equal");
        check(!ids.remove(new Token(3, true)), "identity set cannot remove external equal");
        check(ids.contains(shown) && !ids.contains(hidden), "stored reference visibility");
        check(ids.remove(shown), "remove actual visible reference");
        check(identity.size() == 1 && identity.contains(hidden), "retain hidden identity");

        Set<Token> hash = new HashSet<>(List.of(hidden));
        Set<Token> values = CollectionViews.filtered(hash, Token::visible);
        check(!values.contains(new Token(3, true)), "hash membership must test hidden stored");
        check(!values.remove(new Token(3, true)), "hidden hash representative stays");
        hash.clear();
        hash.add(shown);
        check(values.contains(new Token(3, false)), "visible stored under hidden probe");
        check(values.remove(new Token(3, false)) && hash.isEmpty(),
                "equals-backed visible removal");

        CopyOnWriteArraySet<Token> cow = new CopyOnWriteArraySet<>(List.of(shown));
        Set<Token> cowView = CollectionViews.filtered(cow, Token::visible);
        check(cowView.remove(new Token(3, false)) && cow.isEmpty(),
                "copy-on-write set fallback without iterator.remove");
    }

    private static void sortedOwnerRemainsAuthoritative() {
        Comparator<Token> order = Comparator.comparingInt(Token::id);
        TreeSet<Token> set = new TreeSet<>(order);
        set.add(new Token(1, true));
        set.add(new Token(2, false));
        var view = CollectionViews.filtered(set, Token::visible);
        check(view.contains(new Token(1, false)), "TreeSet comparator representative");
        check(!view.contains(new Token(2, true)), "TreeSet hidden representative");
        check(view.remove(new Token(1, false)), "TreeSet comparator removal");
        check(set.size() == 1 && set.first().id() == 2,
                "other comparator key unaffected");
    }

    private static void randomDifferential() {
        Random random = new Random(0x7103b19L);
        for (int trial = 0; trial < 20_000; trial++) {
            ArrayList<Token> owner = new ArrayList<>();
            int count = random.nextInt(30);
            for (int i = 0; i < count; i++) {
                owner.add(new Token(random.nextInt(13), random.nextBoolean()));
            }
            ArrayList<Token> expected = new ArrayList<>(owner);
            Collection<Token> view = CollectionViews.filtered(owner, Token::visible);
            Token probe = new Token(random.nextInt(13), random.nextBoolean());
            int first = -1;
            for (int i = 0; i < expected.size(); i++) {
                Token stored = expected.get(i);
                if (Objects.equals(probe, stored) && stored.visible()) {
                    first = i;
                    break;
                }
            }
            boolean shouldExist = first >= 0;
            check(view.contains(probe) == shouldExist, "random stored membership " + trial);
            check(view.remove(probe) == shouldExist, "random removal result " + trial);
            if (shouldExist) expected.remove(first);
            check(owner.size() == expected.size(), "random count " + trial);
            for (int i = 0; i < expected.size(); i++) {
                check(owner.get(i) == expected.get(i), "random physical identity/order " + trial);
            }
        }
    }

    public static void main(String[] args) {
        storedListOccurrences();
        ownerSetIdentityAndEquality();
        sortedOwnerRemainsAuthoritative();
        randomDifferential();
        System.out.println("PASS FilteredRepresentativeContractTest assertions=" + checks);
    }
}
