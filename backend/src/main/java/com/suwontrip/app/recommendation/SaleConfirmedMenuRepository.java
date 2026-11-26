package com.suwontrip.app.recommendation;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public class SaleConfirmedMenuRepository {
    private final NamedParameterJdbcTemplate jdbc;

    public SaleConfirmedMenuRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<String> findConfirmedIds(String reviewedVersion, Collection<String> reviewedMenuIds) {
        if (reviewedMenuIds.isEmpty()) {
            return List.of();
        }
        var parameters = new MapSqlParameterSource()
                .addValue("version", reviewedVersion)
                .addValue("ids", reviewedMenuIds);

        return jdbc.queryForList("""
                SELECT m.menu_id
                FROM catalog_active a
                JOIN catalog_menu m ON m.data_version = a.data_version
                WHERE a.singleton_id = 1 AND a.data_version = :version
                  AND m.menu_id IN (:ids)
                  AND NOT m.is_demo AND m.availability = 'AVAILABLE'
                  AND m.confirmed_on <= CURRENT_DATE
                  AND m.confirmation_reference ~ '\\S'
                ORDER BY m.ordinal
                """, parameters, String.class);
    }
}
