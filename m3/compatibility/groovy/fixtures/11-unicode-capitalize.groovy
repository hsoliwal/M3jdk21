def samples = ['\u0131stanbul', '\u00dfeta', '\uFB00oo']
return samples.collect { it.capitalize() }.join('|')
