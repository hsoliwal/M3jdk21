// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
import javax.swing.text.GapContent;
import javax.swing.text.GapContentLowerBoundTest;
import org.netbeans.lib.editor.util.OffsetGapList;

/** Executes unmodified donor sources on their admitted positive-offset domain. */
public final class NetBeansOffsetOracle {
    private NetBeansOffsetOracle() { }

    private static final class Mark {
        private int raw;
        private Mark(int raw) { this.raw = raw; }
    }

    @SuppressWarnings("serial")
    private static final class Offsets extends OffsetGapList<Mark> {
        private Offsets() { super(2); }
        protected int elementRawOffset(Mark mark) { return mark.raw; }
        protected void setElementRawOffset(Mark mark, int offset) { mark.raw = offset; }
        private void append(int offset) {
            Mark mark = new Mark(offset);
            updateElementOffsetAdd(mark);
            add(mark);
        }
        private int first(int offset) { return findElementIndex(offset); }
        private int actual(int index) { return elementOffset(index); }
    }

    public static void main(String[] arguments) throws Exception {
        int checks = 0;
        long trace = 7;
        for (int size : new int[] {0, 1, 2, 3, 63, 64, 65, 1024, 65536}) {
            for (int run : new int[] {1, 4, 64, 65536}) {
                Offsets donor = new Offsets();
                for (int i = 0; i < size; i++) donor.append(i / run);
                for (int pass = 0; pass < 4; pass++) {
                    if (pass == 1) donor.defaultInsertUpdate(1, 3);
                    if (pass == 2) donor.defaultRemoveUpdate(1, 2);
                    if (pass == 3) donor.defaultRemoveUpdate(0, 2);
                    int[] offsets = new int[size];
                    for (int i = 0; i < size; i++) offsets[i] = donor.actual(i);
                    GapContent target = GapContentLowerBoundTest.fixture(offsets);
                    for (int key : new int[] {1, 2, 3, 4, 7, 64, 65, 1024, 65536, 1000000}) {
                        int expected = donor.first(key);
                        int actual = GapContentLowerBoundTest.boundary(target, key);
                        int linear = 0;
                        while (linear < offsets.length && offsets[linear] < key) linear++;
                        if (actual != expected || actual != linear) throw new AssertionError(size + ":" + key);
                        trace = trace * 31 + actual;
                        checks++;
                    }
                }
            }
        }
        System.out.println("NetBeans oracle checks=" + checks + " trace=" + Long.toHexString(trace));
    }
}
