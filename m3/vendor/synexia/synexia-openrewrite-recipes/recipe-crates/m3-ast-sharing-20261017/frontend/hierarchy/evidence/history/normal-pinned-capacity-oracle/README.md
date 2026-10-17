# Corrected capacity oracle

The original test expected transient admission when the only resident entry was actively leased. The existing TieredSwapCache NORMAL policy correctly throws CapacityException in this case. Both mapped tests now require that refusal and assert the held root remains resident, valid, and prevents close until release. No production cache policy was changed.
