/* SPDX-License-Identifier: Apache-2.0 */
import com.m3.text.M3String;
public final class RouteAStringTest { public static void main(String[] args) { if (!M3String.fromString("abc").asString().equals("abc")) throw new AssertionError(); } }
