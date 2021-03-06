/*
 * Copyright 2016-2018 shardingsphere.io.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * </p>
 */

package io.shardingsphere.core.executor;

import io.shardingsphere.core.constant.SQLType;
import io.shardingsphere.core.event.ShardingEventType;
import io.shardingsphere.core.executor.prepared.MemoryStrictlyPreparedStatementExecutor;
import io.shardingsphere.core.executor.prepared.PreparedStatementExecutor;
import io.shardingsphere.core.executor.prepared.PreparedStatementExecuteUnit;
import io.shardingsphere.core.rewrite.SQLBuilder;
import io.shardingsphere.core.routing.RouteUnit;
import org.junit.Test;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public final class PreparedStatementExecutorTest extends AbstractBaseExecutorTest {
    
    private static final String DQL_SQL = "SELECT * FROM table_x";
    
    private static final String DML_SQL = "DELETE FROM table_x";
    
    @SuppressWarnings("unchecked")
    @Test
    public void assertNoStatement() throws SQLException {
        PreparedStatementExecutor actual = new MemoryStrictlyPreparedStatementExecutor(SQLType.DQL, getExecuteTemplate(), Collections.<PreparedStatementExecuteUnit>emptyList());
        assertFalse(actual.execute());
        assertThat(actual.executeUpdate(), is(0));
        assertThat(actual.executeQuery().size(), is(0));
    }
    
    @Test
    public void assertExecuteQueryForSinglePreparedStatementSuccess() throws SQLException {
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);
        Connection connection = mock(Connection.class);
        DatabaseMetaData databaseMetaData = mock(DatabaseMetaData.class);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(preparedStatement.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(databaseMetaData);
        PreparedStatementExecutor actual = new MemoryStrictlyPreparedStatementExecutor(
                SQLType.DQL, getExecuteTemplate(), createPreparedStatementExecuteUnits(DQL_SQL, preparedStatement, "ds_0"));
        assertThat(actual.executeQuery(), is(Collections.singletonList(resultSet)));
        verify(preparedStatement).executeQuery();
        verify(getEventCaller(), times(2)).verifyDataSource("ds_0");
        verify(getEventCaller(), times(2)).verifySQL(DQL_SQL);
        verify(getEventCaller(), times(2)).verifyParameters(Collections.emptyList());
        verify(getEventCaller()).verifyEventExecutionType(ShardingEventType.BEFORE_EXECUTE);
        verify(getEventCaller()).verifyEventExecutionType(ShardingEventType.EXECUTE_SUCCESS);
        verify(getEventCaller(), times(0)).verifyException(null);
    }
    
    @Test
    public void assertExecuteQueryForMultiplePreparedStatementsSuccess() throws SQLException {
        PreparedStatement preparedStatement1 = mock(PreparedStatement.class);
        PreparedStatement preparedStatement2 = mock(PreparedStatement.class);
        ResultSet resultSet1 = mock(ResultSet.class);
        ResultSet resultSet2 = mock(ResultSet.class);
        Connection connection = mock(Connection.class);
        DatabaseMetaData databaseMetaData = mock(DatabaseMetaData.class);
        when(preparedStatement1.executeQuery()).thenReturn(resultSet1);
        when(preparedStatement2.executeQuery()).thenReturn(resultSet2);
        when(preparedStatement1.getConnection()).thenReturn(connection);
        when(preparedStatement2.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(databaseMetaData);
        PreparedStatementExecutor actual = new MemoryStrictlyPreparedStatementExecutor(
                SQLType.DQL, getExecuteTemplate(), createPreparedStatementExecuteUnits(DQL_SQL, preparedStatement1, "ds_0", preparedStatement2, "ds_1"));
        List<ResultSet> actualResultSets = actual.executeQuery();
        assertThat(actualResultSets, hasItem(resultSet1));
        assertThat(actualResultSets, hasItem(resultSet2));
        verify(preparedStatement1).executeQuery();
        verify(preparedStatement2).executeQuery();
        verify(getEventCaller(), times(2)).verifyDataSource("ds_0");
        verify(getEventCaller(), times(2)).verifyDataSource("ds_1");
        verify(getEventCaller(), times(4)).verifySQL(DQL_SQL);
        verify(getEventCaller(), times(4)).verifyParameters(Collections.emptyList());
        verify(getEventCaller(), times(2)).verifyEventExecutionType(ShardingEventType.BEFORE_EXECUTE);
        verify(getEventCaller(), times(2)).verifyEventExecutionType(ShardingEventType.EXECUTE_SUCCESS);
        verify(getEventCaller(), times(0)).verifyException(null);
    }
    
    @Test
    public void assertExecuteQueryForSinglePreparedStatementFailure() throws SQLException {
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        Connection connection = mock(Connection.class);
        DatabaseMetaData databaseMetaData = mock(DatabaseMetaData.class);
        SQLException exp = new SQLException();
        when(preparedStatement.executeQuery()).thenThrow(exp);
        when(preparedStatement.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(databaseMetaData);
        PreparedStatementExecutor actual = new MemoryStrictlyPreparedStatementExecutor(
                SQLType.DQL, getExecuteTemplate(), createPreparedStatementExecuteUnits(DQL_SQL, preparedStatement, "ds_0"));
        assertThat(actual.executeQuery(), is(Collections.singletonList((ResultSet) null)));
        verify(preparedStatement).executeQuery();
        verify(getEventCaller(), times(2)).verifyDataSource("ds_0");
        verify(getEventCaller(), times(2)).verifySQL(DQL_SQL);
        verify(getEventCaller(), times(2)).verifyParameters(Collections.emptyList());
        verify(getEventCaller()).verifyEventExecutionType(ShardingEventType.BEFORE_EXECUTE);
        verify(getEventCaller()).verifyEventExecutionType(ShardingEventType.EXECUTE_FAILURE);
        verify(getEventCaller()).verifyException(exp);
    }
    
    @Test
    public void assertExecuteQueryForMultiplePreparedStatementsFailure() throws SQLException {
        PreparedStatement preparedStatement1 = mock(PreparedStatement.class);
        PreparedStatement preparedStatement2 = mock(PreparedStatement.class);
        Connection connection = mock(Connection.class);
        DatabaseMetaData databaseMetaData = mock(DatabaseMetaData.class);
        SQLException exp = new SQLException();
        when(preparedStatement1.executeQuery()).thenThrow(exp);
        when(preparedStatement2.executeQuery()).thenThrow(exp);
        when(preparedStatement1.getConnection()).thenReturn(connection);
        when(preparedStatement2.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(databaseMetaData);
        PreparedStatementExecutor actual = new MemoryStrictlyPreparedStatementExecutor(
                SQLType.DQL, getExecuteTemplate(), createPreparedStatementExecuteUnits(DQL_SQL, preparedStatement1, "ds_0", preparedStatement2, "ds_1"));
        List<ResultSet> actualResultSets = actual.executeQuery();
        assertThat(actualResultSets, is(Arrays.asList((ResultSet) null, null)));
        verify(preparedStatement1).executeQuery();
        verify(preparedStatement2).executeQuery();
        verify(getEventCaller(), times(2)).verifyDataSource("ds_0");
        verify(getEventCaller(), times(2)).verifyDataSource("ds_1");
        verify(getEventCaller(), times(4)).verifySQL(DQL_SQL);
        verify(getEventCaller(), times(4)).verifyParameters(Collections.emptyList());
        verify(getEventCaller(), times(2)).verifyEventExecutionType(ShardingEventType.BEFORE_EXECUTE);
        verify(getEventCaller(), times(2)).verifyEventExecutionType(ShardingEventType.EXECUTE_FAILURE);
        verify(getEventCaller(), times(2)).verifyException(exp);
    }
    
    @Test
    public void assertExecuteUpdateForSinglePreparedStatementSuccess() throws SQLException {
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        Connection connection = mock(Connection.class);
        DatabaseMetaData databaseMetaData = mock(DatabaseMetaData.class);
        when(preparedStatement.executeUpdate()).thenReturn(10);
        when(preparedStatement.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(databaseMetaData);
        PreparedStatementExecutor actual = new MemoryStrictlyPreparedStatementExecutor(
                SQLType.DML, getExecuteTemplate(), createPreparedStatementExecuteUnits(DML_SQL, preparedStatement, "ds_0"));
        assertThat(actual.executeUpdate(), is(10));
        verify(preparedStatement).executeUpdate();
        verify(getEventCaller(), times(2)).verifyDataSource("ds_0");
        verify(getEventCaller(), times(2)).verifySQL(DML_SQL);
        verify(getEventCaller(), times(2)).verifyParameters(Collections.emptyList());
        verify(getEventCaller()).verifyEventExecutionType(ShardingEventType.BEFORE_EXECUTE);
        verify(getEventCaller()).verifyEventExecutionType(ShardingEventType.EXECUTE_SUCCESS);
        verify(getEventCaller(), times(0)).verifyException(null);
    }
    
    @Test
    public void assertExecuteUpdateForMultiplePreparedStatementsSuccess() throws SQLException {
        PreparedStatement preparedStatement1 = mock(PreparedStatement.class);
        PreparedStatement preparedStatement2 = mock(PreparedStatement.class);
        Connection connection = mock(Connection.class);
        DatabaseMetaData databaseMetaData = mock(DatabaseMetaData.class);
        when(preparedStatement1.executeUpdate()).thenReturn(10);
        when(preparedStatement2.executeUpdate()).thenReturn(20);
        when(preparedStatement1.getConnection()).thenReturn(connection);
        when(preparedStatement2.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(databaseMetaData);
        PreparedStatementExecutor actual = new MemoryStrictlyPreparedStatementExecutor(
                SQLType.DML, getExecuteTemplate(), createPreparedStatementExecuteUnits(DML_SQL, preparedStatement1, "ds_0", preparedStatement2, "ds_1"));
        assertThat(actual.executeUpdate(), is(30));
        verify(preparedStatement1).executeUpdate();
        verify(preparedStatement2).executeUpdate();
        verify(getEventCaller(), times(2)).verifyDataSource("ds_0");
        verify(getEventCaller(), times(2)).verifyDataSource("ds_1");
        verify(getEventCaller(), times(4)).verifySQL(DML_SQL);
        verify(getEventCaller(), times(4)).verifyParameters(Collections.emptyList());
        verify(getEventCaller(), times(2)).verifyEventExecutionType(ShardingEventType.BEFORE_EXECUTE);
        verify(getEventCaller(), times(2)).verifyEventExecutionType(ShardingEventType.EXECUTE_SUCCESS);
        verify(getEventCaller(), times(0)).verifyException(null);
    }
    
    @Test
    public void assertExecuteUpdateForSinglePreparedStatementFailure() throws SQLException {
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        Connection connection = mock(Connection.class);
        DatabaseMetaData databaseMetaData = mock(DatabaseMetaData.class);
        SQLException exp = new SQLException();
        when(preparedStatement.executeUpdate()).thenThrow(exp);
        when(preparedStatement.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(databaseMetaData);
        PreparedStatementExecutor actual = new MemoryStrictlyPreparedStatementExecutor(
                SQLType.DML, getExecuteTemplate(), createPreparedStatementExecuteUnits(DML_SQL, preparedStatement, "ds_0"));
        assertThat(actual.executeUpdate(), is(0));
        verify(preparedStatement).executeUpdate();
        verify(getEventCaller(), times(2)).verifyDataSource("ds_0");
        verify(getEventCaller(), times(2)).verifySQL(DML_SQL);
        verify(getEventCaller(), times(2)).verifyParameters(Collections.emptyList());
        verify(getEventCaller()).verifyEventExecutionType(ShardingEventType.BEFORE_EXECUTE);
        verify(getEventCaller()).verifyEventExecutionType(ShardingEventType.EXECUTE_FAILURE);
        verify(getEventCaller()).verifyException(exp);
    }
    
    @Test
    public void assertExecuteUpdateForMultiplePreparedStatementsFailure() throws SQLException {
        PreparedStatement preparedStatement1 = mock(PreparedStatement.class);
        PreparedStatement preparedStatement2 = mock(PreparedStatement.class);
        Connection connection = mock(Connection.class);
        DatabaseMetaData databaseMetaData = mock(DatabaseMetaData.class);
        SQLException exp = new SQLException();
        when(preparedStatement1.executeUpdate()).thenThrow(exp);
        when(preparedStatement2.executeUpdate()).thenThrow(exp);
        when(preparedStatement1.getConnection()).thenReturn(connection);
        when(preparedStatement2.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(databaseMetaData);
        PreparedStatementExecutor actual = new MemoryStrictlyPreparedStatementExecutor(
                SQLType.DML, getExecuteTemplate(), createPreparedStatementExecuteUnits(DML_SQL, preparedStatement1, "ds_0", preparedStatement2, "ds_1"));
        assertThat(actual.executeUpdate(), is(0));
        verify(preparedStatement1).executeUpdate();
        verify(preparedStatement2).executeUpdate();
        verify(getEventCaller(), times(2)).verifyDataSource("ds_0");
        verify(getEventCaller(), times(2)).verifyDataSource("ds_1");
        verify(getEventCaller(), times(4)).verifySQL(DML_SQL);
        verify(getEventCaller(), times(4)).verifyParameters(Collections.emptyList());
        verify(getEventCaller(), times(2)).verifyEventExecutionType(ShardingEventType.BEFORE_EXECUTE);
        verify(getEventCaller(), times(2)).verifyEventExecutionType(ShardingEventType.EXECUTE_FAILURE);
        verify(getEventCaller(), times(2)).verifyException(exp);
    }
    
    @Test
    public void assertExecuteForSinglePreparedStatementSuccessWithDML() throws SQLException {
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        Connection connection = mock(Connection.class);
        DatabaseMetaData databaseMetaData = mock(DatabaseMetaData.class);
        when(preparedStatement.execute()).thenReturn(false);
        when(preparedStatement.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(databaseMetaData);
        PreparedStatementExecutor actual = new MemoryStrictlyPreparedStatementExecutor(
                SQLType.DML, getExecuteTemplate(), createPreparedStatementExecuteUnits(DML_SQL, preparedStatement, "ds_0"));
        assertFalse(actual.execute());
        verify(preparedStatement).execute();
        verify(getEventCaller(), times(2)).verifyDataSource("ds_0");
        verify(getEventCaller(), times(2)).verifySQL(DML_SQL);
        verify(getEventCaller(), times(2)).verifyParameters(Collections.emptyList());
        verify(getEventCaller()).verifyEventExecutionType(ShardingEventType.BEFORE_EXECUTE);
        verify(getEventCaller()).verifyEventExecutionType(ShardingEventType.EXECUTE_SUCCESS);
        verify(getEventCaller(), times(0)).verifyException(null);
    }
    
    @Test
    public void assertExecuteForMultiplePreparedStatementsSuccessWithDML() throws SQLException {
        PreparedStatement preparedStatement1 = mock(PreparedStatement.class);
        PreparedStatement preparedStatement2 = mock(PreparedStatement.class);
        Connection connection = mock(Connection.class);
        DatabaseMetaData databaseMetaData = mock(DatabaseMetaData.class);
        when(preparedStatement1.execute()).thenReturn(false);
        when(preparedStatement2.execute()).thenReturn(false);
        when(preparedStatement1.getConnection()).thenReturn(connection);
        when(preparedStatement2.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(databaseMetaData);
        PreparedStatementExecutor actual = new MemoryStrictlyPreparedStatementExecutor(
                SQLType.DML, getExecuteTemplate(), createPreparedStatementExecuteUnits(DML_SQL, preparedStatement1, "ds_0", preparedStatement2, "ds_1"));
        assertFalse(actual.execute());
        verify(preparedStatement1).execute();
        verify(preparedStatement2).execute();
        verify(getEventCaller(), times(2)).verifyDataSource("ds_0");
        verify(getEventCaller(), times(2)).verifyDataSource("ds_1");
        verify(getEventCaller(), times(4)).verifySQL(DML_SQL);
        verify(getEventCaller(), times(4)).verifyParameters(Collections.emptyList());
        verify(getEventCaller(), times(2)).verifyEventExecutionType(ShardingEventType.BEFORE_EXECUTE);
        verify(getEventCaller(), times(2)).verifyEventExecutionType(ShardingEventType.EXECUTE_SUCCESS);
        verify(getEventCaller(), times(0)).verifyException(null);
    }
    
    @Test
    public void assertExecuteForSinglePreparedStatementFailureWithDML() throws SQLException {
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        Connection connection = mock(Connection.class);
        DatabaseMetaData databaseMetaData = mock(DatabaseMetaData.class);
        SQLException exp = new SQLException();
        when(preparedStatement.execute()).thenThrow(exp);
        when(preparedStatement.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(databaseMetaData);
        PreparedStatementExecutor actual = new MemoryStrictlyPreparedStatementExecutor(
                SQLType.DML, getExecuteTemplate(), createPreparedStatementExecuteUnits(DML_SQL, preparedStatement, "ds_0"));
        assertFalse(actual.execute());
        verify(preparedStatement).execute();
        verify(getEventCaller(), times(2)).verifyDataSource("ds_0");
        verify(getEventCaller(), times(2)).verifySQL(DML_SQL);
        verify(getEventCaller(), times(2)).verifyParameters(Collections.emptyList());
        verify(getEventCaller()).verifyEventExecutionType(ShardingEventType.BEFORE_EXECUTE);
        verify(getEventCaller()).verifyEventExecutionType(ShardingEventType.EXECUTE_FAILURE);
        verify(getEventCaller()).verifyException(exp);
    }
    
    @Test
    public void assertExecuteForMultiplePreparedStatementsFailureWithDML() throws SQLException {
        PreparedStatement preparedStatement1 = mock(PreparedStatement.class);
        PreparedStatement preparedStatement2 = mock(PreparedStatement.class);
        Connection connection = mock(Connection.class);
        DatabaseMetaData databaseMetaData = mock(DatabaseMetaData.class);
        SQLException exp = new SQLException();
        when(preparedStatement1.execute()).thenThrow(exp);
        when(preparedStatement2.execute()).thenThrow(exp);
        when(preparedStatement1.getConnection()).thenReturn(connection);
        when(preparedStatement2.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(databaseMetaData);
        PreparedStatementExecutor actual = new MemoryStrictlyPreparedStatementExecutor(
                SQLType.DML, getExecuteTemplate(), createPreparedStatementExecuteUnits(DML_SQL, preparedStatement1, "ds_0", preparedStatement2, "ds_1"));
        assertFalse(actual.execute());
        verify(preparedStatement1).execute();
        verify(preparedStatement2).execute();
        verify(getEventCaller(), times(2)).verifyDataSource("ds_0");
        verify(getEventCaller(), times(2)).verifyDataSource("ds_1");
        verify(getEventCaller(), times(4)).verifySQL(DML_SQL);
        verify(getEventCaller(), times(4)).verifyParameters(Collections.emptyList());
        verify(getEventCaller(), times(2)).verifyEventExecutionType(ShardingEventType.BEFORE_EXECUTE);
        verify(getEventCaller(), times(2)).verifyEventExecutionType(ShardingEventType.EXECUTE_FAILURE);
        verify(getEventCaller(), times(2)).verifyException(exp);
    }
    
    @Test
    public void assertExecuteForSinglePreparedStatementWithDQL() throws SQLException {
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        Connection connection = mock(Connection.class);
        DatabaseMetaData databaseMetaData = mock(DatabaseMetaData.class);
        when(preparedStatement.execute()).thenReturn(true);
        when(preparedStatement.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(databaseMetaData);
        PreparedStatementExecutor actual = new MemoryStrictlyPreparedStatementExecutor(
                SQLType.DQL, getExecuteTemplate(), createPreparedStatementExecuteUnits(DQL_SQL, preparedStatement, "ds_0"));
        assertTrue(actual.execute());
        verify(preparedStatement).execute();
        verify(getEventCaller(), times(2)).verifyDataSource("ds_0");
        verify(getEventCaller(), times(2)).verifySQL(DQL_SQL);
        verify(getEventCaller(), times(2)).verifyParameters(Collections.emptyList());
        verify(getEventCaller()).verifyEventExecutionType(ShardingEventType.BEFORE_EXECUTE);
        verify(getEventCaller()).verifyEventExecutionType(ShardingEventType.EXECUTE_SUCCESS);
        verify(getEventCaller(), times(0)).verifyException(null);
    }
    
    @Test
    public void assertExecuteForMultiplePreparedStatements() throws SQLException {
        PreparedStatement preparedStatement1 = mock(PreparedStatement.class);
        PreparedStatement preparedStatement2 = mock(PreparedStatement.class);
        Connection connection = mock(Connection.class);
        DatabaseMetaData databaseMetaData = mock(DatabaseMetaData.class);
        when(preparedStatement1.execute()).thenReturn(true);
        when(preparedStatement2.execute()).thenReturn(true);
        when(preparedStatement1.getConnection()).thenReturn(connection);
        when(preparedStatement2.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(databaseMetaData);
        PreparedStatementExecutor actual = new MemoryStrictlyPreparedStatementExecutor(
                SQLType.DQL, getExecuteTemplate(), createPreparedStatementExecuteUnits(DQL_SQL, preparedStatement1, "ds_0", preparedStatement2, "ds_1"));
        assertTrue(actual.execute());
        verify(preparedStatement1).execute();
        verify(preparedStatement2).execute();
        verify(getEventCaller(), times(2)).verifyDataSource("ds_0");
        verify(getEventCaller(), times(2)).verifyDataSource("ds_1");
        verify(getEventCaller(), times(4)).verifySQL(DQL_SQL);
        verify(getEventCaller(), times(4)).verifyParameters(Collections.emptyList());
        verify(getEventCaller(), times(2)).verifyEventExecutionType(ShardingEventType.BEFORE_EXECUTE);
        verify(getEventCaller(), times(2)).verifyEventExecutionType(ShardingEventType.EXECUTE_SUCCESS);
        verify(getEventCaller(), times(0)).verifyException(null);
    }
    
    private Collection<PreparedStatementExecuteUnit> createPreparedStatementExecuteUnits(final String sql, final PreparedStatement preparedStatement, final String dataSource) {
        Collection<PreparedStatementExecuteUnit> result = new LinkedList<>();
        SQLBuilder sqlBuilder = new SQLBuilder();
        sqlBuilder.appendLiterals(sql);
        result.add(new PreparedStatementExecuteUnit(new RouteUnit(dataSource, sqlBuilder.toSQL(null, Collections.<String, String>emptyMap(), null, null)), preparedStatement));
        return result;
    }
    
    private Collection<PreparedStatementExecuteUnit> createPreparedStatementExecuteUnits(
            final String sql, final PreparedStatement preparedStatement1, final String dataSource1, final PreparedStatement preparedStatement2, final String dataSource2) {
        Collection<PreparedStatementExecuteUnit> result = new LinkedList<>();
        result.addAll(createPreparedStatementExecuteUnits(sql, preparedStatement1, dataSource1));
        result.addAll(createPreparedStatementExecuteUnits(sql, preparedStatement2, dataSource2));
        return result;
    }
}
