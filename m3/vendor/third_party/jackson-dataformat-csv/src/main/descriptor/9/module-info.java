// NOTICE: declaration generated from the published META-INF/versions/9/module-info.class (class version 53); the published sources jar
// carries no module-info.java (upstream generates the descriptor in its build). No other content.
module com.fasterxml.jackson.dataformat.csv {
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    exports com.fasterxml.jackson.dataformat.csv;
    provides com.fasterxml.jackson.core.JsonFactory with com.fasterxml.jackson.dataformat.csv.CsvFactory;
    provides com.fasterxml.jackson.core.ObjectCodec with com.fasterxml.jackson.dataformat.csv.CsvMapper;
}
