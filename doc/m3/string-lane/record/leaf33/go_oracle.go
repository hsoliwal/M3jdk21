// SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
// Copyright (c) 2026 Synexia, Hitesh Soliwal and others

// go_oracle.go <work-dir>: Go's net/url PathEscape and QueryEscape as an oracle of the ENCODE family. Texts are UTF-16
// code units in hex (big endian) as EncodeFamilyProof corpus writes them; a Go string is UTF-8, so a text with a lone
// surrogate has no Go form and its row is '-' TAB '-'. A cell is '=' + the ASCII result. Writes:
// GO_ENC.txt per line of enc_texts.txt: url.PathEscape(s) TAB url.QueryEscape(s)
package main

import (
	"bufio"
	"encoding/hex"
	"fmt"
	"net/url"
	"os"
	"path/filepath"
	"runtime"
	"strings"
	"unicode/utf16"
)

func text(h string) (string, bool) {
	b, err := hex.DecodeString(h)
	if err != nil {
		panic(err)
	}
	units := make([]uint16, len(b)/2)
	for i := range units {
		units[i] = uint16(b[2*i])<<8 | uint16(b[2*i+1])
	}
	for i := 0; i < len(units); i++ {
		u := units[i]
		if u >= 0xD800 && u < 0xDC00 && i+1 < len(units) && units[i+1] >= 0xDC00 && units[i+1] < 0xE000 {
			i++
		} else if u >= 0xD800 && u < 0xE000 {
			return "", false
		}
	}
	return string(utf16.Decode(units)), true
}

func main() {
	work := os.Args[1]
	data, err := os.ReadFile(filepath.Join(work, "enc_texts.txt"))
	if err != nil {
		panic(err)
	}
	all := strings.Split(string(data), "\n")
	all = all[:len(all)-1]
	f, err := os.Create(filepath.Join(work, "GO_ENC.txt"))
	if err != nil {
		panic(err)
	}
	w := bufio.NewWriter(f)
	kept := 0
	for _, h := range all {
		s, ok := text(h)
		if !ok {
			fmt.Fprint(w, "-\t-\n")
			continue
		}
		kept++
		fmt.Fprint(w, "=", url.PathEscape(s), "\t=", url.QueryEscape(s), "\n")
	}
	w.Flush()
	f.Close()
	fmt.Println("go", runtime.Version(), "enc_texts", len(all), "well_formed", kept)
}
