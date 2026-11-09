# SPDX-License-Identifier: Apache-2.0
# Projects COMPILETIME_STRING_INVENTORY.tsv (long form) into COMPILETIME_STRING_UNITS.tsv:
#   awk -F'\t' -f units_projection.awk COMPILETIME_STRING_INVENTORY.tsv | (read h; echo "$h"; sort)
NR > 1 {
  u = $1 "\t" $2; units[u] = 1; v[u, $3 "\t" $4] = $5
  if ($3 == "b" && $4 ~ /_const$/) b[u] += $5
  if ($3 == "c" && $4 ~ /_const$/) c[u] += $5
  if ($3 == "f" && $4 ~ /^(resource_bundle_key|property_key|property_key_set|env_key)_const$/) f[u] += $5
  if ($3 == "g" && $4 ~ /^search_.*_const$|^equals_ignore_case_arg_const$/) g[u] += $5
}
END {
  print "corpus\tunit\tclasses\tldc_string_sites\tldc_class_string_pairs\tregex_const_sites\tformat_const_sites\tindy_concat_sites\tstring_switch_sites\tannotation_string_values\tenum_constant_names\tkey_const_sites\tneedle_const_sites\tsb_append_const_sites\texception_message_const_sites"
  for (u in units)
    printf "%s\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\n", u, v[u, "all\tclasses"], v[u, "a\tldc_string_sites"], v[u, "a\tldc_class_string_pairs"], b[u], c[u], v[u, "d\tindy_concat_sites"], v[u, "e\tstring_switch_sites"], v[u, "f\tannotation_string_values"], v[u, "f\tenum_constant_names"], f[u], g[u], v[u, "g\tstring_builder_append_const"], v[u, "g\texception_message_const"]
}
