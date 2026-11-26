package com.suwontrip.app.recommendation;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Types;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

@Repository
public class SaleConfirmedMenuRepository {
    private static final String CONFIRMED = """
             WHERE m.data_version = :version AND m.menu_id IN (:ids)
               AND EXISTS (SELECT 1 FROM catalog_active a
                           WHERE a.singleton_id = 1 AND a.data_version = m.data_version)
               AND NOT m.is_demo AND m.availability = 'AVAILABLE'
               AND m.confirmed_on <= CURRENT_DATE AND m.confirmation_reference ~ '\\S'
            """;
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
            """ + CONFIRMED + " ORDER BY matches DESC, m.ordinal";

    public record RankedMenu(String menuId, int matches) {}

    private final NamedParameterJdbcTemplate jdbc;

    public SaleConfirmedMenuRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<String> findConfirmedIds(String reviewedVersion, Collection<String> reviewedMenuIds) {
        if (reviewedMenuIds.isEmpty()) {
            return List.of();
        }
        return jdbc.queryForList("SELECT m.menu_id FROM catalog_menu m " + CONFIRMED + " ORDER BY m.ordinal",
                candidates(reviewedVersion, reviewedMenuIds), String.class);
    }

    public List<RankedMenu> rank(String reviewedVersion, Collection<String> reviewedMenuIds,
                                 Collection<String> excludedIds, PreferenceProfile profile) {
        var ids = new HashSet<>(reviewedMenuIds);
        ids.removeAll(excludedIds);
        if (ids.isEmpty()) {
            return List.of();
        }
        var parameters = candidates(reviewedVersion, ids)
                .addValue("menuType", label(profile.menuType()), Types.VARCHAR)
                .addValue("price", label(profile.priceLevel()), Types.VARCHAR)
                .addValue("sweetness", label(profile.sweetness()), Types.VARCHAR)
                .addValue("coffee", profile.menuType() == PreferenceProfile.MenuType.BEVERAGE
                        ? profile.coffeeBased() : null, Types.BOOLEAN)
                .addValue("temperature", label(profile.temperature()), Types.VARCHAR)
                .addValue("signature", profile.signature(), Types.BOOLEAN);
        return jdbc.query(RANKED, parameters, (row, index) ->
                new RankedMenu(row.getString("menu_id"), row.getInt("matches")));
    }

    private static MapSqlParameterSource candidates(String version, Collection<String> ids) {
        return new MapSqlParameterSource().addValue("version", version).addValue("ids", ids);
    }

    private static String label(Enum<?> value) {
        return value == null ? null : value.name().toLowerCase(Locale.ROOT);
    }
}
