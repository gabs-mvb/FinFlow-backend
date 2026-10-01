package com.finflow

import java.sql.DriverManager
import java.sql.SQLException
import java.util.UUID
import kotlin.test.*

class OnboardingMigrationTest {
    @Test fun `migration preserves owned completed users and never assigns ownerless finances`() {
        DriverManager.getConnection("jdbc:h2:mem:onboarding-${UUID.randomUUID()};MODE=PostgreSQL", "sa", "").use { connection ->
            connection.createStatement().use { sql ->
                sql.execute("create table users(id integer primary key, onboarding_completed boolean not null)")
                sql.execute("create table financial_accounts(id integer primary key, user_id integer)")
                sql.execute("create table financial_profiles(id integer primary key, user_id integer)")
                sql.execute("insert into users values (1, true), (2, true), (3, false)")
                sql.execute("insert into financial_accounts values (1, 1), (2, null)")
                sql.execute("insert into financial_profiles values (1, 1), (2, null)")
                val migration = requireNotNull(javaClass.getResource("/db/migration/V8__progressive_user_onboarding.sql")).readText()
                migration.split(';').filter { it.isNotBlank() }.forEach { sql.execute(it) }
                sql.executeQuery("select user_id, status, current_step from user_onboarding order by user_id").use { rows ->
                    assertTrue(rows.next()); assertEquals("COMPLETED", rows.getString("status"))
                    assertTrue(rows.next()); assertEquals("NOT_STARTED", rows.getString("status")); assertEquals("WELCOME", rows.getString("current_step"))
                    assertTrue(rows.next()); assertEquals("NOT_STARTED", rows.getString("status"))
                }
                sql.executeQuery("select user_id from financial_accounts where id = 2").use { rows -> assertTrue(rows.next()); assertNull(rows.getObject(1)) }
                assertFailsWith<SQLException> { sql.execute("insert into user_onboarding(user_id) values (9999)") }
            }
        }
    }
    @Test fun `legacy global identities are scoped and payday 31 becomes valid`() {
        DriverManager.getConnection("jdbc:h2:mem:scope-${UUID.randomUUID()};MODE=PostgreSQL", "sa", "").use { connection ->
            connection.createStatement().use { sql ->
                sql.execute("create table financial_accounts(user_id integer, institution varchar(120), external_id varchar(160), constraint uk_account_institution_external unique(institution, external_id))")
                sql.execute("create table portfolio_positions(user_id integer, asset_code varchar(48), constraint uk_portfolio_asset_code unique(asset_code))")
                sql.execute("create table allocation_targets(user_id integer, asset_class varchar(40), constraint uk_allocation_target_class unique(asset_class))")
                sql.execute("create table open_finance_consents(user_id integer, provider varchar(80), external_consent_id varchar(180), constraint uk_consent_provider_external unique(provider, external_consent_id))")
                sql.execute("create table financial_profiles(user_id integer, pay_day integer, constraint financial_profiles_pay_day_check check(pay_day between 1 and 28))")
                listOf("obligations", "debts", "financial_goals", "financial_plans").forEach { sql.execute("create table $it(user_id integer)") }
                val migration = requireNotNull(javaClass.getResource("/db/migration/V9__scope_financial_uniqueness_and_payday.sql")).readText()
                migration.split(';').filter { it.isNotBlank() }.forEach { sql.execute(it) }
                sql.execute("insert into financial_accounts values (1, 'Nubank', 'same'), (2, 'Nubank', 'same')")
                sql.execute("insert into portfolio_positions values (1, 'CDB'), (2, 'CDB')")
                sql.execute("insert into allocation_targets values (1, 'CASH'), (2, 'CASH')")
                sql.execute("insert into open_finance_consents values (1, 'provider', 'same'), (2, 'provider', 'same')")
                sql.execute("insert into financial_profiles values (1, 31)")
                assertFailsWith<SQLException> { sql.execute("insert into financial_accounts values (1, 'Nubank', 'same')") }
                assertFailsWith<SQLException> { sql.execute("insert into financial_profiles values (2, 32)") }
            }
        }
    }
}
