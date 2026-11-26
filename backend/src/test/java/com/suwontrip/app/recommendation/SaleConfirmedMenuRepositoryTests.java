package com.suwontrip.app.recommendation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.junit.jupiter.api.AfterEach;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SaleConfirmedMenuRepositoryTests {
    private final EmbeddedDatabase db = new EmbeddedDatabaseBuilder()
            .generateUniqueName(true).setType(EmbeddedDatabaseType.H2).build();
    private final JdbcTemplate jdbc = new JdbcTemplate(db);
    private final SaleConfirmedMenuRepository repository =
            new SaleConfirmedMenuRepository(new NamedParameterJdbcTemplate(jdbc));
    private final LocalDate today = jdbc.queryForObject("SELECT CURRENT_DATE", LocalDate.class);

    @BeforeEach
    void createTables() {
        jdbc.execute("CREATE TABLE catalog_active (singleton_id INT PRIMARY KEY, data_version VARCHAR)");
        jdbc.execute("""
                CREATE TABLE catalog_menu (data_version VARCHAR, menu_id VARCHAR, ordinal INT,
                    is_demo BOOLEAN, availability VARCHAR, confirmed_on DATE, confirmation_reference VARCHAR,
                    PRIMARY KEY (data_version, menu_id))
                """);
        jdbc.update("INSERT INTO catalog_active VALUES (1, 'v2')");
    }

    @AfterEach
    void closeDatabase() {
        db.shutdown();
    }

    @Test
    void selectsOnlyReviewedRealMenusWithSaleEvidenceInCatalogOrder() {
        menu("v2", "first", 1, false, "AVAILABLE", today.minusDays(1), "매장 확인");
        menu("v2", "second", 2, false, "AVAILABLE", today, "매장 주문");
        menu("v2", "unreviewed", 3, false, "AVAILABLE", today, "확인");
        menu("v2", "demo", 4, true, "AVAILABLE", today, "확인");
        menu("v2", "unknown", 5, false, "UNKNOWN", null, null);
        menu("v2", "stopped", 6, false, "UNAVAILABLE", today, "판매 중단");
        menu("v2", "ended", 7, false, "SEASON_ENDED", null, null);

        assertThat(repository.findConfirmedIds("v2", List.of(
                "second", "demo", "unknown", "stopped", "ended", "first", "first")))
                .containsExactly("first", "second");
    }

    @Test
    void ranksBySixEqualMatchesAfterFilteringAndBreaksTiesByCatalogOrder() {
        jdbc.execute("""
                CREATE TABLE traits (data_version VARCHAR, menu_id VARCHAR, menu_type VARCHAR,
                    price_level VARCHAR, sweetness VARCHAR, coffee_based BOOLEAN,
                    temperature VARCHAR, is_signature BOOLEAN)
                """);
        for (String view : List.of("classification", "price", "temperature", "reviewed_traits")) {
            jdbc.execute("CREATE VIEW catalog_menu_" + view + "_v1 AS SELECT * FROM traits");
        }
        var reviewed = List.of("unknown", "second", "first", "excluded", "stopped", "partial");
        for (int i = 0; i < reviewed.size(); i++) {
            String id = reviewed.get(i);
            menu("v2", id, i, false, id.equals("stopped") ? "UNAVAILABLE" : "AVAILABLE", today, "검증 기록");
        }
        jdbc.update("""
                INSERT INTO traits VALUES
                    ('v2','unknown',NULL,NULL,NULL,NULL,NULL,NULL),
                    ('v2','first','beverage','medium','high',false,'cold',true),
                    ('v2','second','beverage','medium','high',false,'both',true),
                    ('v2','partial','dessert','high','low',NULL,'cold',false),
                    ('v2','excluded','beverage','medium','high',false,'cold',true),
                    ('v2','stopped','beverage','medium','high',false,'cold',true)
                """);
        var profile = new PreferenceProfile(PreferenceProfile.MenuType.BEVERAGE, PreferenceProfile.Level.MEDIUM,
                PreferenceProfile.Level.HIGH, false, PreferenceProfile.Temperature.COLD, true);
        assertThat(repository.rank("v2", reviewed, List.of("excluded"), profile))
                .containsExactly(new SaleConfirmedMenuRepository.RankedMenu("second", 6),
                        new SaleConfirmedMenuRepository.RankedMenu("first", 6),
                        new SaleConfirmedMenuRepository.RankedMenu("partial", 1),
                        new SaleConfirmedMenuRepository.RankedMenu("unknown", 0));
        assertThat(repository.rank("v1", reviewed, List.of(), profile)).isEmpty();
        assertThat(repository.rank("v2", reviewed, reviewed, profile)).isEmpty();
        var indifferent = new PreferenceProfile(null, null, null, null, null, null);
        assertThat(repository.rank("v2", List.of("first", "unknown", "second"), List.of(), indifferent))
                .containsExactly(new SaleConfirmedMenuRepository.RankedMenu("unknown", 0),
                        new SaleConfirmedMenuRepository.RankedMenu("second", 0),
                        new SaleConfirmedMenuRepository.RankedMenu("first", 0));
    }

    @Test
    void rejectsIncompleteOrFutureEvidence() {
        menu("v2", "blank", 1, false, "AVAILABLE", today, " \t ");
        menu("v2", "no-date", 2, false, "AVAILABLE", null, "매장 확인");
        menu("v2", "no-reference", 3, false, "AVAILABLE", today, null);
        menu("v2", "future", 4, false, "AVAILABLE", today.plusDays(1), "예정");
        assertThat(repository.findConfirmedIds("v2", List.of("blank", "no-date", "no-reference", "future")))
                .isEmpty();
    }

    @Test
    void keepsReviewBoundToActiveVersionEvenWhenIdsAreReused() {
        menu("v1", "same", 1, false, "AVAILABLE", today, "이전 확인");
        menu("v2", "same", 1, false, "UNKNOWN", null, null);
        assertThat(repository.findConfirmedIds("v1", List.of("same"))).isEmpty();
        assertThat(repository.findConfirmedIds("v2", List.of("same"))).isEmpty();
        jdbc.update("UPDATE catalog_active SET data_version = 'v1'");
        assertThat(repository.findConfirmedIds("v1", List.of("same"))).containsExactly("same");
        assertThat(repository.findConfirmedIds("v1", List.of())).isEmpty();
        jdbc.update("UPDATE catalog_active SET data_version = NULL");
        assertThat(repository.findConfirmedIds("v1", List.of("same"))).isEmpty();
    }

    private void menu(String version, String id, int ordinal, boolean demo, String status,
                      LocalDate confirmedOn, String reference) {
        jdbc.update("INSERT INTO catalog_menu VALUES (?, ?, ?, ?, ?, ?, ?)",
                version, id, ordinal, demo, status, confirmedOn, reference);
    }
}
