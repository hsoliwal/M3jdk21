<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Clear-operation diagnostics
These are fresh-JVM, synthetic warm-clear diagnostics, not JMH or an end-to-end
application benchmark. The summary statistic is the median of three process
medians. Ranges expose process variation; no significance claim is made.
No structural storage was added. At extent 65,536 the retained owner/storage/array
closures were 862,696 bytes (list), 2,231,816 (map), and 1,947,280 (set), identical
before and after. These figures exclude caller payloads and probe fixtures.

## Complete 18-workload sweep
| Owner | Extent / live | Baseline ns: median [range] | Candidate ns: median [range] | Candidate / baseline |
| --- | --- | --- | --- | --- |
| list | 8 / 8 | 11.266 [11.031–16.820] | 11.344 [11.266–16.586] | 1.007 |
| list | 64 / 64 | 44.828 [29.344–45.227] | 29.422 [28.789–48.352] | 0.656 |
| list | 65536 / 1 | 32072.250 [30640.250–35434.750] | 42.750 [42.500–47.500] | 0.001 |
| list | 65536 / 64 | 29603.500 [19714.250–38842.500] | 325.500 [308.000–363.000] | 0.011 |
| list | 65536 / 4096 | 46183.250 [28529.500–69254.750] | 31762.000 [29443.500–46105.500] | 0.688 |
| list | 65536 / 65536 | 51631.250 [46213.000–58884.500] | 46228.250 [46210.750–54740.750] | 0.895 |
| map | 8 / 8 | 16.820 [16.516–23.938] | 16.898 [16.586–24.094] | 1.005 |
| map | 64 / 64 | 81.531 [66.742–91.859] | 87.242 [57.430–95.844] | 1.070 |
| map | 65536 / 1 | 110783.500 [102704.000–117165.500] | 5748.500 [5340.250–6264.250] | 0.052 |
| map | 65536 / 64 | 106189.250 [105303.000–122788.750] | 6797.750 [5981.250–8572.750] | 0.064 |
| map | 65536 / 4096 | 120608.000 [98412.750–142936.000] | 101279.500 [100966.500–167051.750] | 0.840 |
| map | 65536 / 65536 | 109304.000 [108555.250–117030.000] | 103730.500 [102526.250–114994.750] | 0.949 |
| set | 8 / 8 | 19.953 [14.477–27.227] | 16.117 [13.773–21.359] | 0.808 |
| set | 64 / 64 | 55.711 [52.500–56.484] | 57.898 [37.945–60.164] | 1.039 |
| set | 65536 / 1 | 34796.500 [34017.750–39348.250] | 6544.750 [6296.750–6564.750] | 0.188 |
| set | 65536 / 64 | 40402.250 [29443.500–40695.000] | 6935.250 [5756.000–8607.750] | 0.172 |
| set | 65536 / 4096 | 41346.000 [40597.500–51295.750] | 53639.250 [47955.750–75088.500] | 1.297 |
| set | 65536 / 65536 | 53120.750 [52299.750–83871.250] | 56000.000 [55431.750–59825.750] | 1.054 |

## Focused same-CPU follow-up
| Owner | Extent / live | Baseline ns: median [range] | Candidate ns: median [range] | Candidate / baseline |
| --- | --- | --- | --- | --- |
| map | 64 / 64 | 90.445 [89.430–91.148] | 90.523 [76.047–90.680] | 1.001 |
| map | 65536 / 64 | 70964.750 [55827.250–75919.750] | 5956.250 [5928.750–7606.000] | 0.084 |
| map | 65536 / 4096 | 71132.500 [66273.000–72612.250] | 50529.500 [49886.250–76492.750] | 0.710 |
| set | 64 / 64 | 58.133 [43.656–58.367] | 50.313 [37.008–53.594] | 0.865 |
| set | 65536 / 64 | 35980.750 [33744.750–36100.750] | 6622.250 [5811.000–6805.000] | 0.184 |
| set | 65536 / 4096 | 37630.750 [33917.500–37963.750] | 25057.000 [24956.750–46193.250] | 0.666 |

Sparse cases improve in both series, without adding retained storage. The complete
sweep also contains slower dense cases; for example set extent 65,536/live 4,096
is slower there but faster in the same-CPU follow-up. This is reported as timing
variance, not hidden or converted into a universal win. The switch retains the
original dense body and changes only the very sparse path (at most 1/64 capacity).

Most ThreadMXBean measurements were zero bytes per clear. One complete-sweep set
process reported 0.149, and several focused processes reported 0.005, including
baseline processes. The raw observations are retained; the source introduces no
allocation expression or retained field in clear, but the diagnostic is not a
proof of zero allocations under every JVM configuration. Peak RSS includes JVM,
compiler, agent and fixture and is not the collection's retained footprint.

Initial v1 diagnostics were stopped and their candidate superseded. The v2 run
used shorter warmup. Both remain historical evidence only. v3 and focused use
the delivered production postimages. The focused probe adds a warmup property;
its default is the same 0.5 seconds as v3. No production code changed between
those two series. Full workload JMH, owning product suites, ARM/small-device
measurements, native memory and fleet-wide acceptance remain open.
