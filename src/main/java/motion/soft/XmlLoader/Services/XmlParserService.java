package motion.soft.XmlLoader.Services;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import motion.soft.XmlLoader.Models.TableSchema;


import java.util.List;
import motion.soft.XmlLoader.Scripts.XmlParserGroovy;

@Slf4j
@AllArgsConstructor
public class XmlParserService {

    private final XmlParserGroovy groovyParser;

    public XmlParserService(String xmlUrl) {
        this.groovyParser = new XmlParserGroovy(xmlUrl);
    }

    public List<String> getTableNames() {
        log.info("Fetching table names from XML...");
        List<String> names = groovyParser.getTableNames();
        log.info("Found tables: {}", names);
        return names;
    }

    public String getTableDDL(String tableName) {
        log.debug("Generating DDL for table: {}", tableName);
        String ddl = groovyParser.getTableDDL(tableName);
        log.debug("Generated DDL: {}", ddl);
        return ddl;
    }

    public TableSchema parseTable(String tableName) {
        log.info("Parsing data for table: {}", tableName);
        TableSchema schema = groovyParser.parseTable(tableName);
        log.info("Parsed {} rows for table {}", schema.getRows().size(), tableName);
        return schema;
    }
}