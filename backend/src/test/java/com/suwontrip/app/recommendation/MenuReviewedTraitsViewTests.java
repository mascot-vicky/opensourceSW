package com.suwontrip.app.recommendation;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class MenuReviewedTraitsViewTests {
    private static final String VERSION = "haenggung-2026-11-18.2";
    private static final String SHA256 = "77101fe31346de40225d23adca5a485f74cb4861e98a9a8e9d1cf7474ab4eb0d";
    private static final String VIEW = "catalog_menu_reviewed_traits_v1";

    @Test
    void readsOnlyReviewedTraitsForTheActiveSnapshot() {
        var db = new EmbeddedDatabaseBuilder().generateUniqueName(true).setType(EmbeddedDatabaseType.H2).build();
        try {
            var jdbc = new JdbcTemplate(db);
            jdbc.execute("CREATE TABLE catalog_active (singleton_id INT, data_version VARCHAR)");
            jdbc.execute("CREATE TABLE catalog_dataset (data_version VARCHAR, source_sha256 CHAR(64))");
            jdbc.execute("CREATE TABLE catalog_menu (data_version VARCHAR, menu_id VARCHAR)");
            jdbc.update("INSERT INTO catalog_active VALUES (1, ?)", VERSION);
            for (String version : new String[]{VERSION, "next-version"}) {
                jdbc.update("INSERT INTO catalog_dataset VALUES (?, ?)", version, SHA256);
                jdbc.update("""
                        INSERT INTO catalog_menu VALUES
                        (?, 'menu-05d2d92d9ec90b33ce0d'), (?, 'menu-e7b85380c59130b82467'),
                        (?, 'menu-35414df2a5df57ae8f39')
                        """, version, version, version);
            }
            new ResourceDatabasePopulator(new ClassPathResource(
                    "db/migration/R__catalog_menu_reviewed_traits_v1.sql")).execute(db);

            var reviewed = jdbc.queryForList("SELECT * FROM " + VIEW + " ORDER BY menu_id");
            assertThat(reviewed).extracting(row -> row.get("SWEETNESS"), row -> row.get("IS_SIGNATURE"),
                            row -> row.get("SWEETNESS_EVIDENCE"), row -> row.get("SIGNATURE_EVIDENCE"))
                    .containsExactly(tuple("low", null, "단맛이 적고", null), tuple(null, null, null, null),
                            tuple("high", true, "진한 달콤함과 프랑스산 게랑드 소금이 조화", "시그니처 카라멜"));

            jdbc.update("UPDATE catalog_dataset SET source_sha256 = ? WHERE data_version = ?", "0".repeat(64), VERSION);
            assertUnreviewed(jdbc, VERSION);
            jdbc.update("UPDATE catalog_active SET data_version = 'next-version'");
            assertUnreviewed(jdbc, "next-version");
        } finally {
            db.shutdown();
        }
    }

    private static void assertUnreviewed(JdbcTemplate jdbc, String version) {
        assertThat(jdbc.queryForList("SELECT data_version FROM " + VIEW, String.class))
                .containsExactly(version, version, version);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM " + VIEW
                + " WHERE COALESCE(sweetness_evidence, signature_evidence, sweetness) IS NOT NULL"
                + " OR is_signature IS NOT NULL", Integer.class)).isZero();
    }
}
