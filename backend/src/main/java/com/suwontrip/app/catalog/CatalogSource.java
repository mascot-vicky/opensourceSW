package com.suwontrip.app.catalog;

import java.net.URI;
import java.time.LocalDate;

public record CatalogSource(
        URI url,
        String externalId,
        LocalDate observedOn
) {
}
