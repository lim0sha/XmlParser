package motion.soft.XmlLoader.Models;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.*;

@Getter
@AllArgsConstructor
public final class TableSchema {
    private final String name;
    private final List<String> columns;
    private final List<Map<String, String>> rows;

    public List<String> getNewColumns(List<String> existingColumns) {
        List<String> newCols = new ArrayList<>();
        for (String col : columns) {
            if (!existingColumns.contains(col)) {
                newCols.add(col);
            }
        }
        return newCols;
    }
}