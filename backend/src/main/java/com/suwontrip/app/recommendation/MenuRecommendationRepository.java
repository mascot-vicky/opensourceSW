package com.suwontrip.app.recommendation;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Types;
import java.util.Collection;
import java.util.Locale;
import java.util.Optional;

@Repository
public class MenuRecommendationRepository {
    private static final String RANKED = """
            SELECT m.menu_id,
                   CASE WHEN c.menu_type = :menuType THEN 1 ELSE 0 END
                 + CASE WHEN p.price_level = :price THEN 1 ELSE 0 END
                 + CASE WHEN r.sweetness = :sweetness THEN 1 ELSE 0 END
                 + CASE WHEN c.coffee_based = :coffee THEN 1 ELSE 0 END
                 + CASE WHEN t.temperature = :temperature OR
                             (t.temperature = 'both' AND CAST(:temperature AS VARCHAR) IN ('hot', 'cold'))
                        THEN 1 ELSE 0 END
                 + CASE WHEN r.is_signature = :signature THEN 1 ELSE 0 END AS matches
            FROM catalog_menu m
            JOIN catalog_menu_classification_v1 c USING (data_version, menu_id)
            JOIN catalog_menu_price_v1 p USING (data_version, menu_id)
            JOIN catalog_menu_temperature_v1 t USING (data_version, menu_id)
            JOIN catalog_menu_reviewed_traits_v1 r USING (data_version, menu_id)
            JOIN catalog_active a ON a.singleton_id = 1 AND a.data_version = m.data_version
            WHERE m.data_version = :version AND NOT m.is_demo
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public MenuRecommendationRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<String> first(String version, Collection<String> excludedIds, PreferenceProfile profile) {
        var parameters = new MapSqlParameterSource("version", version)
                .addValue("menuType", label(profile.menuType()), Types.VARCHAR)
                .addValue("price", label(profile.priceLevel()), Types.VARCHAR)
                .addValue("sweetness", label(profile.sweetness()), Types.VARCHAR)
                .addValue("coffee", profile.menuType() == PreferenceProfile.MenuType.BEVERAGE
                        ? profile.coffeeBased() : null, Types.BOOLEAN)
                .addValue("temperature", label(profile.temperature()), Types.VARCHAR)
                .addValue("signature", profile.signature(), Types.BOOLEAN);
        String sql = RANKED;
        if (!excludedIds.isEmpty()) {
            sql += " AND m.menu_id NOT IN (:excludedIds)";
            parameters.addValue("excludedIds", excludedIds);
        }
        return jdbc.query(sql + " ORDER BY matches DESC, m.ordinal LIMIT 1", parameters,
                        (row, index) -> row.getString("menu_id"))
                .stream().findFirst();
    }

    private static String label(Enum<?> value) {
        return value == null ? null : value.name().toLowerCase(Locale.ROOT);
    }
}
