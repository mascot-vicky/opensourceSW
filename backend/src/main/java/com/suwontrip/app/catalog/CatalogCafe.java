package com.suwontrip.app.catalog;

public record CatalogCafe(
        String id,
        String name,
        String roadAddress,
        Location location,
        CatalogSource source
) {
    public record Location(Double latitude, Double longitude) {
    }
}
