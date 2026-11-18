def matches = ('a1 b2' =~ /([a-z])(\d)/)
def pairs = []
while (matches.find()) {
    pairs << (matches.group(1) + matches.group(2))
}
def missing = null
return pairs.join(':') + '|' + (missing?.name == null)
