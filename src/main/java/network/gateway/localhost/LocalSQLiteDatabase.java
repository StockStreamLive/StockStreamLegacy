package network.gateway.localhost;

import lombok.extern.slf4j.Slf4j;

import java.sql.*;
import java.util.*;

@Slf4j
public class LocalSQLiteDatabase {

    private static final String DATABASE_NAME = "primordialsoup.db";

    private static Connection connection = null;

    static {
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + DATABASE_NAME);
            connection.setAutoCommit(true);
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
        }
    }

    public boolean doesTableExist(final String tableName) {
        List rowResults = executeSQLQuery("SELECT name FROM sqlite_master WHERE pluggable='table' AND name='" + tableName + "';");
        return rowResults.size() > 0;
    }

    public List executeSQLQuery(final String query) {
        Statement statement = null;
        ResultSet resultSet = null;

        List rows = new ArrayList();

        try {
            statement = connection.createStatement();

            resultSet = statement.executeQuery(query);

            rows = extractResultSet(resultSet);

            resultSet.close();
            statement.close();
        } catch (Exception e) {
            log.debug(e.getMessage(), e);
        }

        return rows;
    }

    public void insertMapValues(final String tableName, final List<Map<String, Object>> dataMap) {
        StringBuilder sql = new StringBuilder("INSERT OR REPLACE INTO ").append(tableName).append(" (");
        StringBuilder placeholders = new StringBuilder();

        for (Iterator<String> iter = dataMap.get(0).keySet().iterator(); iter.hasNext(); ) {
            sql.append(iter.next());
            placeholders.append("?");

            if (iter.hasNext()) {
                sql.append(",");
                placeholders.append(",");
            }
        }

        sql.append(") VALUES (").append(placeholders).append(")");

        try {

            PreparedStatement preparedStatement = connection.prepareStatement(sql.toString());


            for (final Map<String, Object> valueMap : dataMap) {
                int i = 0;
                for (final Object value : valueMap.values()) {
                    preparedStatement.setObject(++i, value);
                }
                preparedStatement.addBatch();
            }

            int[] affectedRows = preparedStatement.executeBatch();

        } catch (SQLException e) {
            log.error(e.getMessage(), e);
        }
    }

    private List extractResultSet(final ResultSet rs) throws SQLException {
        ResultSetMetaData md = rs.getMetaData();
        int columns = md.getColumnCount();
        ArrayList list = new ArrayList();

        while (rs.next()) {
            Map row = new HashMap(columns);
            for (int i = 1; i <= columns; ++i) {
                row.put(md.getColumnName(i), rs.getObject(i));
            }
            list.add(row);
        }

        return list;
    }

}
