/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

/**
 * Model differential between the Nebula and the Swing side (pattern M3P-CORE-1): for every concept row of
 * {@code MEMBERS.tsv}, builds the family model of both packages with the same configuration, replays one
 * scripted operation sequence, and compares {@code snapshot()} and the display-list hash after every step.
 * <p>
 * usage: {@code java -cp <nebula model classes>;<swing model classes> M3WebDifferential.java <MEMBERS.tsv> <out.tsv>}
 * </p>
 */
public final class M3WebDifferential {
	static final String NEB = "org.eclipse.nebula.widgets.m3web.model.", SW = "com.m3.swing.web.model.";
	static final String[] OPS = { "load", "layout", "keys", "pointer", "rtl", "dark", "variant", "tick", "scroll", "logical", "filter" };

	public static void main(String[] args) throws Exception {
		List<String> rows = Files.readAllLines(Path.of(args[0]), StandardCharsets.UTF_8);
		StringBuilder out = new StringBuilder("concept\tfamily\tmode\tsteps\tmismatches\tfirst_mismatch\n");
		int total = 0, bad = 0;
		for (String row : rows.subList(1, rows.size())) {
			String[] f = row.split("\t", -1);
			int[] r = compare(f);
			total++;
			bad += r[1] > 0 ? 1 : 0;
			out.append(f[0]).append('\t').append(f[1]).append('\t').append(f[2]).append('\t').append(r[0]).append('\t').append(r[1])
					.append('\t').append(r[1] > 0 ? OPS[r[2]] : "").append('\n');
		}
		Files.writeString(Path.of(args[1]), out.toString(), StandardCharsets.UTF_8);
		System.out.println("M3_WEB_DIFFERENTIAL configs=" + total + " mismatching=" + bad);
	}

	static int[] compare(String[] f) throws Exception {
		Object a = build(NEB, f), b = build(SW, f);
		int steps = 0, mismatches = 0, first = -1;
		for (int op = 0; op < OPS.length; op++) {
			apply(a, op, f[6]);
			apply(b, op, f[6]);
			steps++;
			if (!Arrays.equals(snapshot(a), snapshot(b)) || paintHash(a, NEB) != paintHash(b, SW) || !Arrays.equals(core(a), core(b))) {
				mismatches++;
				first = first < 0 ? op : first;
			}
		}
		return new int[] { steps, mismatches, Math.max(0, first) };
	}

	static Object build(String pkg, String[] f) throws Exception {
		String fam = f[1].charAt(0) + f[1].substring(1).toLowerCase();
		Class<?> c = Class.forName(pkg + "M3" + fam + "Model");
		Class<?> states = Class.forName(pkg + "M3WebStates");
		int role = (int) states.getMethod("role", String.class).invoke(null, f[3]);
		int mode = c.getField(f[2]).getInt(null), shape = states.getField(f[4]).getInt(null);
		return c.getConstructor(String.class, int.class, int.class, int.class, int.class).newInstance(f[0], role, mode, shape, Integer.parseInt(f[5]));
	}

	static void apply(Object m, int op, String sample) throws Exception {
		switch (OPS[op]) {
		case "load": call(m, "load", sample.equals("-") ? "x" : sample); break;
		case "layout": call(m, "layout", 360, 260); break;
		case "keys": keys(m); break;
		case "pointer": for (int k = 1; k <= 6; k++) { call(m, "pointer", k, 20, 12, 0); } break;
		case "rtl": call(m, "setRtl", true); call(m, "layout", 300, 220); break;
		case "dark": call(m, "setDark", true); break;
		case "variant": call(m, "setVariant", 3); call(m, "setSize", 4); break;
		case "tick": for (int i = 0; i < 20; i++) { call(m, "tick", 16); } break;
		case "scroll": call(m, "scrollTo", 70L); break;
		case "logical": call(m, "setLogicalCount", 100_000); call(m, "scrollTo", 400_000L); break;
		default: call(m, "setFilter", "1");
		}
	}

	static void keys(Object m) throws Exception {
		call(m, "setState", -1, 2, true);
		for (int k = 1; k <= 15; k++) {
			call(m, "key", k, k == 15 ? (int) '3' : 0, 0);
		}
	}

	/** Viewport core answers: drained damage box and the scroll bar projection (SWT facades vs Swing port core). */
	static int[] core(Object m) throws Exception {
		int[] d = new int[4], t = new int[2];
		boolean dirty = (boolean) m.getClass().getMethod("takeDamage", int[].class).invoke(m, (Object) d);
		m.getClass().getMethod("thumb", int.class, int[].class).invoke(m, 200, t);
		return new int[] { dirty ? 1 : 0, d[0], d[1], d[2], d[3], t[0], t[1] };
	}

	static int[] snapshot(Object m) throws Exception {
		return (int[]) m.getClass().getMethod("snapshot").invoke(m);
	}

	static int paintHash(Object m, String pkg) throws Exception {
		Class<?> pc = Class.forName(pkg + "M3WebPaint");
		Object p = pc.getConstructor().newInstance();
		m.getClass().getMethod("paint", pc).invoke(m, p);
		return (int) pc.getMethod("hash").invoke(p);
	}

	static void call(Object m, String name, Object... args) throws Exception {
		for (Method x : m.getClass().getMethods()) {
			if (x.getName().equals(name) && x.getParameterCount() == args.length) {
				x.invoke(m, args);
				return;
			}
		}
		throw new NoSuchMethodException(name);
	}
}
