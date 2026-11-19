package com.suwontrip.app.catalog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public record Catalog(
        Integer schemaVersion,
        String dataVersion,
        List<CatalogCafe> cafes,
        List<CatalogMenu> menus
) {
    public Catalog {
        cafes = cafes == null ? null : Collections.unmodifiableList(new ArrayList<>(cafes));
        menus = menus == null ? null : Collections.unmodifiableList(new ArrayList<>(menus));
    }
}
