package org.alessio29.savagebot.internal.builders;

import java.util.List;

public class TableData {
    private final String title;
    private final String[] headers;
    private final List<String[]> rows;

    public TableData(String title, String[] headers, List<String[]> rows) {
        this.title = title;
        this.headers = headers;
        this.rows = rows;
    }

    public TableData(String[] headers, List<String[]> rows) {
        this(null, headers, rows);
    }

    public String getTitle() {
        return title;
    }

    public String[] getHeaders() {
        return headers;
    }

    public List<String[]> getRows() {
        return rows;
    }
}
