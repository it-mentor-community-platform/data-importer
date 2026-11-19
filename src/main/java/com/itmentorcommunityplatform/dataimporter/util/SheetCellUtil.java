package com.itmentorcommunityplatform.dataimporter.util;

import java.util.List;

public final class SheetCellUtil {

    private SheetCellUtil() {
    }

    public static String readTrimmedCell(List<Object> row, int columnIndex) {
        return row.size() > columnIndex
                ? String.valueOf(row.get(columnIndex)).trim()
                : "";
    }
}