package motion.soft.XmlLoader.Scripts

import groovy.xml.XmlSlurper
import groovy.xml.slurpersupport.GPathResult
import motion.soft.XmlLoader.Models.TableSchema

class XmlParserGroovy {
    private final String xmlUrl

    XmlParserGroovy(String xmlUrl) {
        this.xmlUrl = xmlUrl
    }

    List<String> getTableNames() {
        def root = parseRoot()
        def shopNode = root.children().find { it.name() == 'shop' }

        if (!shopNode) {
            log.warn("Node <shop> not found in XML root")
            return []
        }

        def targetTables = ['currencies', 'categories', 'offers']

        return shopNode.children()
                .findAll { node -> targetTables.contains(node.name()) }
                .collect { node -> node.name() }
    }

    String getTableDDL(String tableName) {
        def root = parseRoot()
        def tableNode = findNode(root, tableName)

        if (!tableNode) {
            throw new IllegalArgumentException("Table ${tableName} not found in XML")
        }

        def columns = extractColumns(tableNode)
        def colDefs = columns.collect { col ->
            if (tableName == "offers" && col == "vendorCode") {
                return "${col} TEXT UNIQUE NOT NULL"
            } else if (col.equalsIgnoreCase("id")) {
                return "${col} TEXT PRIMARY KEY"
            }
            return "${col} TEXT"
        }

        return "CREATE TABLE IF NOT EXISTS ${tableName} (${colDefs.join(', ')});"
    }

    TableSchema parseTable(String tableName) {
        def root = parseRoot()
        def tableNode = findNode(root, tableName)

        if (!tableNode) {
            throw new IllegalArgumentException("Table ${tableName} not found in XML")
        }

        def allColumns = [] as Set
        def rows = []

        tableNode.children().each { rowObj ->
            def rowData = [:]

            rowObj.attributes().each { key, value ->
                rowData[key] = value?.toString() ?: ""
                allColumns << key
            }

            rowObj.children().each { childNode ->
                def key = childNode.name()
                def value = childNode.text()
                rowData[key] = value
                allColumns << key
            }

            rows << rowData
        }

        return new TableSchema(tableName, allColumns.toList(), rows)
    }

    private GPathResult parseRoot() {
        def url = new URL(xmlUrl)
        def inputStream = url.openStream()

        try {
            def slurper = new XmlSlurper()
            slurper.setFeature("http://apache.org/xml/features/disallow-doctype-decl", false)
            slurper.setFeature("http://xml.org/sax/features/external-general-entities", true)
            slurper.setFeature("http://xml.org/sax/features/external-parameter-entities", true)
            slurper.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
            return slurper.parse(inputStream)
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse XML from ${xmlUrl}: ${e.message}", e)
        } finally {
            inputStream.close()
        }
    }

    private GPathResult findNode(GPathResult root, String name) {
        def shopNode = root.children().find { it.name() == 'shop' }
        if (!shopNode) {
            return null
        }
        return shopNode.children().find { it.name() == name }
    }

    private List<String> extractColumns(GPathResult tableNode) {
        def columns = [] as Set
        def firstRow = tableNode.children().find()

        if (firstRow) {
            firstRow.attributes().keySet().each { columns << it.toString() }
            firstRow.children().each { columns << it.name() }
        }
        return columns.toList()
    }
}