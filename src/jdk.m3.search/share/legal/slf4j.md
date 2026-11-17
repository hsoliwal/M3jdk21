## SLF4J API v2.0.12

### Origin

The Java sources under `providers/classes/jdk/internal/search/slf4j` of the module `jdk.m3.search` are the SLF4J
API 2.0.12 (https://www.slf4j.org), the main sources of the published sources jar as vendored in this repository
under `m3/vendor/third_party/slf4j-api-2.0.12`. SLF4J is licensed under the MIT License.

### Changes made by Synexia (2026-11-17)

- 46 sources relocated from `org.slf4j` to the concealed package `jdk.internal.search.slf4j`
  (package names in code and strings), for the use of Apache OpenNLP inside the module; not exported.
- `@SuppressWarnings` added where the JDK build's lint reports a warning, documentation comments fixed where
  its doclint refuses them, and the JDK repository's whitespace rule applied. The compiled classes are
  identical to those of the relocated sources (`javap -p -c`).

### MIT License

<pre>
Copyright (c) 2004-2022 QOS.ch Sarl (Switzerland)
All rights reserved.

Permission is hereby granted, free  of charge, to any person obtaining
a  copy  of this  software  and  associated  documentation files  (the
"Software"), to  deal in  the Software without  restriction, including
without limitation  the rights to  use, copy, modify,  merge, publish,
distribute,  sublicense, and/or sell  copies of  the Software,  and to
permit persons to whom the Software  is furnished to do so, subject to
the following conditions:

The  above  copyright  notice  and  this permission  notice  shall  be
included in all copies or substantial portions of the Software.

THE  SOFTWARE IS  PROVIDED  "AS  IS", WITHOUT  WARRANTY  OF ANY  KIND,
EXPRESS OR  IMPLIED, INCLUDING  BUT NOT LIMITED  TO THE  WARRANTIES OF
MERCHANTABILITY,    FITNESS    FOR    A   PARTICULAR    PURPOSE    AND
NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
OF CONTRACT, TORT OR OTHERWISE,  ARISING FROM, OUT OF OR IN CONNECTION
WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
</pre>
