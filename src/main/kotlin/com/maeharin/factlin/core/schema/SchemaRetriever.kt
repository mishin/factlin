package com.maeharin.factlin.core.schema

import com.maeharin.factlin.core.dialect.Dialect
import com.maeharin.factlin.core.dialect.MariadbDialect
import com.maeharin.factlin.core.dialect.PostgresDialect
import com.maeharin.factlin.gradle.FactlinExtension
import java.sql.DatabaseMetaData
import java.sql.DriverManager

class SchemaRetriever(
        val extension: FactlinExtension,
        val dialect: Dialect
) {
    fun retrieve(): List<Table> {
        when(dialect) {
            is PostgresDialect -> {
                Class.forName("org.postgresql.Driver")
            }
            is MariadbDialect -> {
                Class.forName("org.mariadb.jdbc.Driver")
            }
        }

        val conn = DriverManager.getConnection(extension.dbUrl, extension.dbUser, extension.dbPassword)

        val metaData = conn.metaData

        return _getTables(metaData)
                .map { table ->
                    table.copy(columns = _getColumns(metaData, table))
                }
    }

    private fun _getTables(metaData: DatabaseMetaData): List<Table> {
        // todo
        val tableSet = metaData.getTables(null, null, "%", arrayOf("TABLE"))

        val tables = ArrayList<Table>()

        while (tableSet.next()) {
            val table = Table(
                    name = tableSet.getString("TABLE_NAME"),
                    comment = tableSet.getString("REMARKS"),
                    schema = tableSet.getString("TABLE_SCHEM"),
                    catalog = tableSet.getString("TABLE_CAT"),
                    columns = emptyList()
            )

            tables.add(table)
        }

        tableSet.close()

        return tables
    }

    private fun _getColumns(metaData: DatabaseMetaData, table: Table): ArrayList<Column> {
        val primaryKeySet = metaData.getPrimaryKeys(null, null, table.name)
        val primaryKeyNames = ArrayList<String>()
        while(primaryKeySet.next()) {
            primaryKeyNames.add(primaryKeySet.getString("COLUMN_NAME"))
        }
        primaryKeySet.close()

        val colSet = metaData.getColumns(null, null, table.name, "%")
        val columns = ArrayList<Column>()
        while(colSet.next()) {
            val name = colSet.getString("COLUMN_NAME")

            val column = Column(
                    name = name,
                    typeName = colSet.getString("TYPE_NAME"),
                    type = colSet.getInt("DATA_TYPE"),
                    isNullable = colSet.getBoolean("NULLABLE"),
                    defaultValue =  colSet.getString("COLUMN_DEF"),
                    isPrimaryKey = primaryKeyNames.contains(name),
                    comment = colSet.getString("REMARKS")
            )
            columns.add(column)
        }
        colSet.close()

        return columns
    }
}