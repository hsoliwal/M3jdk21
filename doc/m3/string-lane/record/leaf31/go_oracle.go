// SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
// Copyright (c) 2026 Synexia, Hitesh Soliwal and others

// go_oracle.go <work-dir>: Go's strings.EqualFold and unicode.SimpleFold as the oracle of equalsSimpleFold.
// Texts are UTF-16 code units in hex (big endian) as CaseFamilyProof corpus writes them; a surrogate pair becomes its
// rune and a lone surrogate u becomes the private-use rune 0xF0000 + (u - 0xD800), which, like a lone surrogate in
// Java, folds to nothing else (the corpus holds no plane-15 private-use character). Writes:
// SIMPLE_FOLD.txt        every rune r with unicode.SimpleFold(r) != r: hex r TAB hex SimpleFold(r)
// SIMPLE_EQUAL.bin       for every ordered pair (a, b) of pair_texts.txt: '1' if strings.EqualFold(a, b) else '0'
// SIMPLE_EQUAL_EXTRA.bin the same for each line 'a TAB b' of pair_extra.txt
package main

import (
	"bufio"
	"encoding/hex"
	"fmt"
	"os"
	"path/filepath"
	"runtime"
	"strings"
	"unicode"
)

func text(h string) string {
	b, err := hex.DecodeString(h)
	if err != nil {
		panic(err)
	}
	units := make([]rune, len(b)/2)
	for i := range units {
		units[i] = rune(b[2*i])<<8 | rune(b[2*i+1])
	}
	var out []rune
	for i := 0; i < len(units); i++ {
		u := units[i]
		if u >= 0xD800 && u < 0xDC00 && i+1 < len(units) && units[i+1] >= 0xDC00 && units[i+1] < 0xE000 {
			r := 0x10000 + (u-0xD800)<<10 + (units[i+1] - 0xDC00)
			if r >= 0xF0000 && r < 0xF0800 {
				panic("plane-15 private-use character in the corpus")
			}
			out = append(out, r)
			i++
		} else if u >= 0xD800 && u < 0xE000 {
			out = append(out, 0xF0000+(u-0xD800))
		} else {
			out = append(out, u)
		}
	}
	return string(out)
}

func lines(path string) []string {
	data, err := os.ReadFile(path)
	if err != nil {
		panic(err)
	}
	all := strings.Split(string(data), "\n")
	return all[:len(all)-1]
}

func main() {
	work := os.Args[1]
	f, err := os.Create(filepath.Join(work, "SIMPLE_FOLD.txt"))
	if err != nil {
		panic(err)
	}
	w := bufio.NewWriter(f)
	folding := 0
	for r := rune(0); r <= unicode.MaxRune; r++ {
		if s := unicode.SimpleFold(r); s != r {
			fmt.Fprintf(w, "%X\t%X\n", r, s)
			folding++
		}
	}
	w.Flush()
	f.Close()
	var texts []string
	for _, h := range lines(filepath.Join(work, "pair_texts.txt")) {
		texts = append(texts, text(h))
	}
	equal := make([]byte, 0, len(texts)*len(texts))
	for _, a := range texts {
		for _, b := range texts {
			if strings.EqualFold(a, b) {
				equal = append(equal, '1')
			} else {
				equal = append(equal, '0')
			}
		}
	}
	if err := os.WriteFile(filepath.Join(work, "SIMPLE_EQUAL.bin"), equal, 0o644); err != nil {
		panic(err)
	}
	var extra []byte
	for _, line := range lines(filepath.Join(work, "pair_extra.txt")) {
		ab := strings.Split(line, "\t")
		if strings.EqualFold(text(ab[0]), text(ab[1])) {
			extra = append(extra, '1')
		} else {
			extra = append(extra, '0')
		}
	}
	if err := os.WriteFile(filepath.Join(work, "SIMPLE_EQUAL_EXTRA.bin"), extra, 0o644); err != nil {
		panic(err)
	}
	fmt.Println("go", runtime.Version(), "unicode", unicode.Version, "folding_runes", folding, "pairs", len(equal),
		"extra_pairs", len(extra))
}
