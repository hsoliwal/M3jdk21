def count = 1
def value = "count=${-> count}"
count = 2
return value.toString()
