package com.suwontrip.app.catalog;

import java.time.LocalDate;

public record CatalogMenu(
        String id,
        String cafeId,
        String name,
        String description,
        Long priceKrw,
        Boolean isDemo,
        Availability availability,
        SaleConfirmation saleConfirmation,
        CatalogSource source
) {
    public enum Availability {
        UNKNOWN, AVAILABLE, UNAVAILABLE, SEASON_ENDED
    }

    public record SaleConfirmation(
            LocalDate confirmedOn,
            String reference
    ) {
    }
}
