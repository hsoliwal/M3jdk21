// SPDX-License-Identifier: Apache-2.0
package jdk.internal.mindex;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Frozen formula DAG over canonical M3 coordinates.
 *
 * <p>This is a clean-room JDK-internal projection of the primitive formula-table mechanics
 * reviewed in Synexia's Tweety-inspired precompute. Runtime state is primitive only: formula IDs,
 * operator bytes, atom IDs, a packed operand arena, topological rows, and a lookup directory.
 * Formula/atom spellings are never retained here.</p>
 */
public final class M3FormulaTable {
    public enum Kind {
        ATOM,
        TRUE,
        FALSE,
        NOT,
        AND,
        OR,
        IMPLIES,
        EQUIVALENT
    }

    @FunctionalInterface
    public interface AtomTruthProvider {
        M3Truth truth(long atomId);
    }

    private final long[] formulaIds;
    private final byte[] kinds;
    private final long[] atomIds;
    private final int[] operandOffsets;
    private final long[] operands;
    private final int[] topologicalRows;
    private final long[] slots;

    private M3FormulaTable(
            long[] formulaIds,
            byte[] kinds,
            long[] atomIds,
            int[] operandOffsets,
            long[] operands,
            int[] topologicalRows) {
        this.formulaIds = formulaIds;
        this.kinds = kinds;
        this.atomIds = atomIds;
        this.operandOffsets = operandOffsets;
        this.operands = operands;
        this.topologicalRows = topologicalRows;
        this.slots = buildSlots(formulaIds);
    }

    public static Builder builder() {
        return new Builder();
    }

    public int size() {
        return formulaIds.length;
    }

    public boolean contains(long formulaId) {
        return row(formulaId) >= 0;
    }

    public Kind kind(long formulaId) {
        return Kind.values()[Byte.toUnsignedInt(kinds[checkedRow(formulaId)])];
    }

    public long atom(long formulaId) {
        int row = checkedRow(formulaId);
        if (kinds[row] != (byte) Kind.ATOM.ordinal()) {
            throw new IllegalStateException("formula is not an atom");
        }
        return atomIds[row];
    }

    public long[] operands(long formulaId) {
        int row = checkedRow(formulaId);
        return Arrays.copyOfRange(operands, operandOffsets[row], operandOffsets[row + 1]);
    }

    public long[] formulaIds() {
        return formulaIds.clone();
    }

    public Evaluator evaluator(AtomTruthProvider atoms) {
        return new Evaluator(this, Objects.requireNonNull(atoms, "atoms"));
    }

    public M3Truth evaluate(long formulaId, AtomTruthProvider atoms) {
        return evaluator(atoms).truth(formulaId);
    }

    public long retainedPrimitiveBytes() {
        return (long) (formulaIds.length + atomIds.length + operands.length + slots.length)
                        * Long.BYTES
                + (long) (operandOffsets.length + topologicalRows.length) * Integer.BYTES
                + kinds.length;
    }

    private int checkedRow(long formulaId) {
        int row = row(formulaId);
        if (row < 0) throw new IllegalArgumentException("unknown formula ID");
        return row;
    }

    int row(long formulaId) {
        if (formulaId == 0L || slots.length == 0) return -1;
        int mask = slots.length - 1;
        int slot = mix(formulaId) & mask;
        for (int probe = 0; probe < slots.length; probe++) {
            long encoded = slots[slot];
            if (encoded == 0L) return -1;
            int row = (int) (encoded - 1L);
            if (formulaIds[row] == formulaId) return row;
            slot = (slot + 1) & mask;
        }
        return -1;
    }

    public static final class Evaluator {
        private final M3FormulaTable table;
        private final byte[] states;

        private Evaluator(M3FormulaTable table, AtomTruthProvider atoms) {
            this.table = table;
            this.states = new byte[table.size()];
            for (int row : table.topologicalRows) {
                Kind kind = Kind.values()[Byte.toUnsignedInt(table.kinds[row])];
                states[row] =
                        switch (kind) {
                            case ATOM ->
                                    Objects.requireNonNull(
                                                    atoms.truth(table.atomIds[row]), "atom truth")
                                            .bits();
                            case TRUE -> M3Truth.TRUE.bits();
                            case FALSE -> M3Truth.FALSE.bits();
                            case NOT -> negate(stateOf(table.operands[table.operandOffsets[row]]));
                            case AND -> conjunction(row);
                            case OR -> disjunction(row);
                            case IMPLIES -> implication(row);
                            case EQUIVALENT -> equivalence(row);
                        };
            }
        }

        public M3Truth truth(long formulaId) {
            return M3Truth.fromBits(states[table.checkedRow(formulaId)]);
        }

        private byte stateOf(long formulaId) {
            return states[table.checkedRow(formulaId)];
        }

        private byte conjunction(int row) {
            int start = table.operandOffsets[row];
            int end = table.operandOffsets[row + 1];
            boolean positive = true;
            boolean negative = false;
            for (int index = start; index < end; index++) {
                byte state = stateOf(table.operands[index]);
                positive &= (state & 1) != 0;
                negative |= (state & 2) != 0;
            }
            return encode(positive, negative);
        }

        private byte disjunction(int row) {
            int start = table.operandOffsets[row];
            int end = table.operandOffsets[row + 1];
            boolean positive = false;
            boolean negative = true;
            for (int index = start; index < end; index++) {
                byte state = stateOf(table.operands[index]);
                positive |= (state & 1) != 0;
                negative &= (state & 2) != 0;
            }
            return encode(positive, negative);
        }

        private byte implication(int row) {
            int start = table.operandOffsets[row];
            byte left = stateOf(table.operands[start]);
            byte right = stateOf(table.operands[start + 1]);
            return or(negate(left), right);
        }

        private byte equivalence(int row) {
            int start = table.operandOffsets[row];
            byte left = stateOf(table.operands[start]);
            byte right = stateOf(table.operands[start + 1]);
            return and(or(negate(left), right), or(negate(right), left));
        }

        private static byte negate(byte value) {
            return (byte) (((value & 1) << 1) | ((value & 2) >>> 1));
        }

        private static byte and(byte left, byte right) {
            return encode(
                    (left & 1) != 0 && (right & 1) != 0,
                    (left & 2) != 0 || (right & 2) != 0);
        }

        private static byte or(byte left, byte right) {
            return encode(
                    (left & 1) != 0 || (right & 1) != 0,
                    (left & 2) != 0 && (right & 2) != 0);
        }

        private static byte encode(boolean positive, boolean negative) {
            return (byte) ((positive ? 1 : 0) | (negative ? 2 : 0));
        }
    }

    public static final class Builder {
        private final TreeMap<Long, Draft> drafts = new TreeMap<>(Long::compareUnsigned);

        public Builder atom(long formulaId, long atomId) {
            if (atomId == 0L) throw new IllegalArgumentException("atom ID must be nonzero");
            return add(formulaId, Kind.ATOM, atomId, new long[0]);
        }

        public Builder constant(long formulaId, boolean value) {
            return add(formulaId, value ? Kind.TRUE : Kind.FALSE, 0L, new long[0]);
        }

        public Builder not(long formulaId, long operand) {
            return add(formulaId, Kind.NOT, 0L, new long[] {operand});
        }

        public Builder and(long formulaId, long... operands) {
            return add(formulaId, Kind.AND, 0L, operands);
        }

        public Builder or(long formulaId, long... operands) {
            return add(formulaId, Kind.OR, 0L, operands);
        }

        public Builder implies(long formulaId, long antecedent, long consequent) {
            return add(formulaId, Kind.IMPLIES, 0L, new long[] {antecedent, consequent});
        }

        public Builder equivalent(long formulaId, long left, long right) {
            return add(formulaId, Kind.EQUIVALENT, 0L, new long[] {left, right});
        }

        private Builder add(long id, Kind kind, long atom, long[] operands) {
            if (id == 0L) throw new IllegalArgumentException("formula ID must be nonzero");
            Objects.requireNonNull(kind, "kind");
            long[] copied = Objects.requireNonNull(operands, "operands").clone();
            for (long operand : copied) {
                if (operand == 0L) throw new IllegalArgumentException("operand ID must be nonzero");
            }
            Draft draft = new Draft(kind, atom, copied);
            if (drafts.putIfAbsent(id, draft) != null) {
                throw new IllegalArgumentException("duplicate formula ID");
            }
            return this;
        }

        public M3FormulaTable build() {
            int count = drafts.size();
            long[] ids = new long[count];
            byte[] kinds = new byte[count];
            long[] atoms = new long[count];
            int[] offsets = new int[count + 1];

            HashMap<Long, Integer> rowById = new HashMap<>();
            int row = 0;
            int operandCount = 0;
            for (var entry : drafts.entrySet()) {
                ids[row] = entry.getKey();
                kinds[row] = (byte) entry.getValue().kind.ordinal();
                atoms[row] = entry.getValue().atom;
                offsets[row] = operandCount;
                operandCount = Math.addExact(operandCount, entry.getValue().operands.length);
                rowById.put(entry.getKey(), row);
                row++;
            }
            offsets[count] = operandCount;

            long[] operands = new long[operandCount];
            int[] indegree = new int[count];
            @SuppressWarnings("unchecked")
            ArrayList<Integer>[] dependents = new ArrayList[count];
            for (int i = 0; i < count; i++) dependents[i] = new ArrayList<>();

            row = 0;
            int cursor = 0;
            for (Draft draft : drafts.values()) {
                validateArity(draft);
                for (long operand : draft.operands) {
                    Integer dependency = rowById.get(operand);
                    if (dependency == null) {
                        throw new IllegalArgumentException("formula references unknown operand");
                    }
                    operands[cursor++] = operand;
                    indegree[row]++;
                    dependents[dependency].add(row);
                }
                row++;
            }

            ArrayDeque<Integer> queue = new ArrayDeque<>();
            for (int i = 0; i < count; i++) if (indegree[i] == 0) queue.addLast(i);
            int[] topological = new int[count];
            int written = 0;
            while (!queue.isEmpty()) {
                int ready = queue.removeFirst();
                topological[written++] = ready;
                for (int dependent : dependents[ready]) {
                    if (--indegree[dependent] == 0) queue.addLast(dependent);
                }
            }
            if (written != count) throw new IllegalArgumentException("formula graph contains cycle");

            return new M3FormulaTable(ids, kinds, atoms, offsets, operands, topological);
        }

        private static void validateArity(Draft draft) {
            int arity = draft.operands.length;
            switch (draft.kind) {
                case ATOM, TRUE, FALSE -> {
                    if (arity != 0) throw new IllegalArgumentException("constant/atom arity");
                }
                case NOT -> {
                    if (arity != 1) throw new IllegalArgumentException("NOT arity");
                }
                case IMPLIES, EQUIVALENT -> {
                    if (arity != 2) throw new IllegalArgumentException("binary formula arity");
                }
                case AND, OR -> {
                    // Empty AND/OR are legal identities: TRUE/FALSE respectively.
                }
            }
        }
    }

    private record Draft(Kind kind, long atom, long[] operands) {}

    private static long[] buildSlots(long[] ids) {
        if (ids.length == 0) return new long[0];
        int capacity = 2;
        while (capacity < Math.multiplyExact(ids.length, 2)) {
            capacity = Math.multiplyExact(capacity, 2);
        }
        long[] slots = new long[capacity];
        int mask = capacity - 1;
        for (int row = 0; row < ids.length; row++) {
            int slot = mix(ids[row]) & mask;
            while (slots[slot] != 0L) slot = (slot + 1) & mask;
            slots[slot] = row + 1L;
        }
        return slots;
    }

    static int mix(long value) {
        long z = value;
        z = (z ^ (z >>> 33)) * 0xff51afd7ed558ccdL;
        z = (z ^ (z >>> 33)) * 0xc4ceb9fe1a85ec53L;
        z ^= z >>> 33;
        return (int) (z ^ (z >>> 32));
    }
}
