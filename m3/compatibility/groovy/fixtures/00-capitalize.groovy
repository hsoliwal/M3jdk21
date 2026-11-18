def samples = ['hello', 'straße', 'mIxEd', '']
return samples.collect { it.capitalize() }.join('|') + ';' + 'ABc'.uncapitalize()
