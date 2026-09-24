package com.finflow

import java.sql.DriverManager
import java.sql.SQLException
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PersonalizedPlanMigrationTest {
    @Test
    fun `V5 preserves existing plans and enforces revision uniqueness`() {
        DriverManager.getConnection("jdbc:h2:mem:migration-${UUID.randomUUID()};MODE=PostgreSQL", "sa", "").use { connection ->
            connection.createStatement().use { sql ->
                sql.execute("create table users(id integer primary key)")
                sql.execute("create table financial_plans(id uuid primary key, warnings varchar(2000) not null)")
                sql.execute("insert into users values (1)")
                val planId = UUID.randomUUID()
                sql.execute("insert into financial_plans values ('$planId', 'Aviso antigo|Outro aviso')")
                val migration = requireNotNull(javaClass.getResource("/db/migration/V5__personalized_plans.sql")).readText()
                migration.split(';').filter { it.isNotBlank() }.forEach { sql.execute(it) }
                sql.executeQuery("select revision, warnings, total_balance_snapshot from financial_plans").use { result ->
                    assertTrue(result.next())
                    assertEquals(0, result.getInt("revision"))
                    assertEquals("Aviso antigo|Outro aviso", result.getString("warnings"))
                    assertNull(result.getBigDecimal("total_balance_snapshot"))
                }
                sql.execute(
                    "insert into financial_plan_revisions values ('${UUID.randomUUID()}', '$planId', 1, 0, current_timestamp, '{}')",
                )
                assertFailsWith<SQLException> {
                    sql.execute(
                        "insert into financial_plan_revisions values ('${UUID.randomUUID()}', '$planId', 1, 0, current_timestamp, '{}')",
                    )
                }
            }
        }
    }
}
