package motion.soft.XmlLoader.Services;

import lombok.extern.slf4j.Slf4j;
import motion.soft.XmlLoader.Models.TableSchema;
import motion.soft.XmlLoader.Utils.DbHelper;

import java.sql.*;
import java.util.*;

@Slf4j
public class DbService {
    private final Connection connection;
    private final DbHelper helper;
    private final Map<String, TableSchema> xmlSchemaCache;

    public DbService(Connection connection) {
        this.connection = connection;
        this.helper = new DbHelper(connection);
        this.xmlSchemaCache = new HashMap<>();
    }

    public void cacheXmlSchema(TableSchema schema) {
        this.xmlSchemaCache.put(schema.getName(), schema);
    }

    public void update(TableSchema schema) throws SQLException {
        String tableName = schema.getName();
        log.info("Starting update for table: {}", tableName);

        cacheXmlSchema(schema);

        if (!helper.tableExists(tableName)) {
            log.info("Table {} does not exist. Creating...", tableName);
            helper.executeUpdate(generateCreateTableSql(schema));
        } else {
            syncSchema(tableName, schema.getColumns());
        }

        loadData(tableName, schema.getColumns(), schema.getRows());
        log.info("Update completed for table: {}", tableName);
    }

    public ArrayList<String> getColumnNames(String tableName) throws SQLException {
        log.debug("Fetching column names for table: {}", tableName);
        List<String> columns = helper.getExistingColumns(tableName);
        return new ArrayList<>(columns);
    }

    public boolean isColumnId(String tableName, String columnName) throws SQLException {
        log.debug("Checking if column {}.{} is an ID", tableName, columnName);

        DatabaseMetaData meta = connection.getMetaData();
        try (ResultSet rsPk = meta.getPrimaryKeys(null, null, tableName)) {
            while (rsPk.next()) {
                if (rsPk.getString("COLUMN_NAME").equalsIgnoreCase(columnName)) {
                    log.debug("Column {} is a Primary Key", columnName);
                    return true;
                }
            }
        }
       
        try (ResultSet rsIdx = meta.getIndexInfo(null, null, tableName, true, false)) {
            while (rsIdx.next()) {
                String idxColumnName = rsIdx.getString("COLUMN_NAME");
                if (idxColumnName != null && idxColumnName.equalsIgnoreCase(columnName)) {
                    log.debug("Column {} has a Unique Index", columnName);
                    return true;
                }
            }
        }

        return false;
    }

    public String getDDLChange(String tableName) throws SQLException {
        log.info("Analyzing schema changes for table: {}", tableName);

        TableSchema xmlSchema = xmlSchemaCache.get(tableName);
        if (xmlSchema == null) {
            throw new IllegalStateException("XML schema for table '" + tableName + "' not found in cache. Call update() or cacheXmlSchema() first.");
        }

        List<String> dbColumns = helper.getExistingColumns(tableName);
        List<String> xmlColumns = xmlSchema.getColumns();

        List<String> newColumns = new ArrayList<>();
        List<String> missingColumns = new ArrayList<>();

        for (String col : xmlColumns) {
            if (!dbColumns.contains(col)) {
                newColumns.add(col);
            }
        }

        for (String col : dbColumns) {
            if (!xmlColumns.contains(col)) {
                missingColumns.add(col);
            }
        }

        if (!missingColumns.isEmpty()) {
            String msg = String.format("Structure mismatch detected for table '%s'. Columns missing in XML but present in DB: %s. Aborting to prevent data loss.",
                    tableName, String.join(", ", missingColumns));
            log.error(msg);
            throw new SQLException(msg);
        }

        if (newColumns.isEmpty()) {
            log.info("No schema changes detected for table {}", tableName);
            return "";
        }

       
        StringBuilder sqlScript = new StringBuilder();
        for (String col : newColumns) {
           
            String definition = col + " TEXT";
            if ("offers".equals(tableName) && "vendorCode".equals(col)) {
                definition += " UNIQUE NOT NULL";
            } else if ("id".equalsIgnoreCase(col)) {
                log.warn("Adding 'id' column as plain TEXT. Primary Key constraint should be added manually if needed.");
            }

            String alterSql = String.format("ALTER TABLE %s ADD COLUMN %s;", tableName, definition);
            sqlScript.append(alterSql).append("\n");
            log.info("Generated DDL change: {}", alterSql);
        }

        return sqlScript.toString().trim();
    }



    private void syncSchema(String tableName, List<String> expectedColumns) throws SQLException {
        String ddlChanges = getDDLChange(tableName);
        if (!ddlChanges.isEmpty()) {
            log.info("Applying schema changes for table {}: \n{}", tableName, ddlChanges);
            String[] statements = ddlChanges.split(";");
            for (String stmt : statements) {
                if (!stmt.trim().isEmpty()) {
                    helper.executeUpdate(stmt.trim() + ";");
                }
            }
        }
    }

    private void loadData(String tableName, List<String> columns, List<Map<String, String>> rows) throws SQLException {
        if (rows.isEmpty()) return;

        String conflictColumn = determineConflictColumn(tableName, columns);

        StringBuilder sql = new StringBuilder();
        sql.append("INSERT INTO ").append(tableName).append(" (");
        sql.append(String.join(", ", columns));
        sql.append(") VALUES (");
        sql.append(String.join(", ", Collections.nCopies(columns.size(), "?")));
        sql.append(")");

        if (conflictColumn != null) {
            sql.append(" ON CONFLICT (").append(conflictColumn).append(") DO UPDATE SET ");
            List<String> updates = new ArrayList<>();
            for (String col : columns) {
                if (!col.equals(conflictColumn)) {
                    updates.add(col + " = EXCLUDED." + col);
                }
            }
            if (!updates.isEmpty()) {
                sql.append(String.join(", ", updates));
            } else {
               
                sql.append(conflictColumn + " = EXCLUDED." + conflictColumn);
            }
        }

        log.debug("Prepared UPSERT statement: {}", sql);

        try (PreparedStatement ps = connection.prepareStatement(sql.toString())) {
            for (Map<String, String> row : rows) {
                for (int i = 0; i < columns.size(); i++) {
                    String col = columns.get(i);
                    ps.setString(i + 1, row.getOrDefault(col, null));
                }
                ps.addBatch();
            }
            int[] results = ps.executeBatch();
            log.info("Loaded {} rows into {}", results.length, tableName);
        }
    }

    private String determineConflictColumn(String tableName, List<String> columns) {
        if ("offers".equals(tableName) && columns.contains("vendorCode")) {
            return "vendorCode";
        }

        if (columns.contains("id")) {
            return "id";
        }

        for (String col : columns) {
            try {
                if (isColumnId(tableName, col)) {
                    return col;
                }
            } catch (SQLException e) {
                log.warn("Error checking column uniqueness for " + col, e);
            }
        }
        return null;
    }

    private String generateCreateTableSql(TableSchema schema) {
        StringBuilder sql = new StringBuilder();
        sql.append("CREATE TABLE ").append(schema.getName()).append(" (");
        List<String> defs = new ArrayList<>();
        for (String col : schema.getColumns()) {
            String def = col + " TEXT";
            if ("offers".equals(schema.getName()) && "vendorCode".equals(col)) {
                def += " UNIQUE NOT NULL";
            } else if ("id".equalsIgnoreCase(col)) {
                def += " PRIMARY KEY";
            }
            defs.add(def);
        }
        sql.append(String.join(", ", defs));
        sql.append(");");
        return sql.toString();
    }
}