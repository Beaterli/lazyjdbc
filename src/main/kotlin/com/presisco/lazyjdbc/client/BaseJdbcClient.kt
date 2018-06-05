package com.presisco.lazyjdbc.client

import com.presisco.toolbox.StringToolbox
import java.sql.Connection
import java.sql.SQLException
import javax.sql.DataSource

abstract class BaseJdbcClient<T>(
        protected val dataSource: DataSource,
        protected val queryTimeoutSecs: Int = 2,
        protected val rollbackOnBatchFailure: Boolean = true
) {
    val databaseName: String
    val databaseVersion: String
    val wrapper: String

    init {
        val connection = dataSource.connection
        val metaData = connection.metaData
        databaseName = metaData.databaseProductName
        databaseVersion = metaData.databaseProductVersion
        wrapper = when (databaseName.toLowerCase()) {
            "mysql" -> "`"
            "oracle" -> "\""
            else -> ""
        }
    }

    fun getConnection(): Connection {
        val connection = dataSource.connection
        connection.autoCommit = false
        return connection
    }

    fun executeSQL(query: String): Boolean {
        val connection = getConnection()
        val statement = connection.createStatement()
        statement.queryTimeout = queryTimeoutSecs
        return statement.execute(query)
    }

    fun buildInsertSql(tableName: String, columns: Collection<String>)
            = INSERT.replace("TABLENAME", wrapper + tableName + wrapper)
            .replace("COLUMNS", StringToolbox.concat(columns, wrapper, ", "))
            .replace("PLACEHOLDERS", StringToolbox.concat("?", columns.size, ", "))

    fun buildReplaceSql(tableName: String, columns: Collection<String>) = REPLACE.replace("TABLENAME", wrapper + tableName + wrapper)
            .replace("COLUMNS", StringToolbox.concat(columns, wrapper, ", "))
            .replace("PLACEHOLDERS", StringToolbox.concat("?", columns.size, ", "))

    protected fun closeConnection(connection: Connection?) {
        if (connection != null) {
            try {
                connection.close()
            } catch (e: SQLException) {
                throw RuntimeException("Failed to close connection", e)
            }
        }
    }

    abstract fun select(sql: String): List<T>

    abstract fun insert(tableName: String, dataList: List<T>): Set<Int>

    abstract fun replace(tableName: String, dataList: List<T>): Set<Int>

    abstract fun delete(sql: String)

    companion object {
        const val INSERT = "insert into TABLENAME(COLUMNS) values(PLACEHOLDERS)"
        const val REPLACE = "replace into TABLENAME(COLUMNS) values(PLACEHOLDERS)"
    }
}