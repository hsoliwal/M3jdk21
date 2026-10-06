# A3 Synexia delivery work recipe

Named recipe:

`com.m3.rewrite.A3SynexiaDeliveryWork`

Java crate:

`a3-synexia-delivery-work`

Text crate:

`a3-synexia-delivery-work-text`

The recipe adds the target-specific A3 work bridge and direct dependency on the existing
`m3-synexia-import` model.

It does not introduce a second manifest parser, change `m3/vendor/synexia`, or write OpenJDK
product source.
