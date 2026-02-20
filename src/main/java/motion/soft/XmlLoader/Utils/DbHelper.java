package motion.soft.XmlLoader.Utils;

import lombok.AllArgsConstructor;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
public class DbHelper {
    private final Connection connection;

    public boolean tableExists(String tableName) throws SQLException {
        DatabaseMetaData meta = connection.getMetaData();
        try (ResultSet rs = meta.getTables(null, null, tableName, new String[]{"TABLE"})) {
            return rs.next();
        }
    }

    public void executeUpdate(String sql) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        }
    }

    public List<String> getExistingColumns(String tableName) throws SQLException {
        List<String> cols = new ArrayList<>();
        DatabaseMetaData meta = connection.getMetaData();

        try (ResultSet rs = meta.getColumns(null, null, tableName, "%")) {
            while (rs.next()) {
                cols.add(rs.getString("COLUMN_NAME"));
            }
        }
        return cols;
    }
}
