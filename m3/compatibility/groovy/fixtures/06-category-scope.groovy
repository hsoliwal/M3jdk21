class LocalCategory { static String decorated(String self) { 'category:' + self } }
return use(LocalCategory) { 'hello'.decorated() }
