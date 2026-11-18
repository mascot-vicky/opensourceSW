package com.suwontrip.app.catalog;

import java.net.URI;
import java.time.Clock;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

public final class CatalogValidator {
    private static final Pattern ID_PATTERN = Pattern.compile("[A-Za-z0-9][A-Za-z0-9_-]{0,63}");
    private final Clock clock;

    public CatalogValidator(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public void validate(Catalog catalog) {
        require(catalog != null, "$", "catalog is required");
        require(Integer.valueOf(1).equals(catalog.schemaVersion()), "$.schemaVersion", "supported version is 1");
        requireText(catalog.dataVersion(), "$.dataVersion");
        requireEntries(catalog.cafes(), "$.cafes");
        requireEntries(catalog.menus(), "$.menus");
        LocalDate today = LocalDate.now(clock);
        Set<String> cafeIds = new HashSet<>();
        for (int i = 0; i < catalog.cafes().size(); i++) {
            String path = "$.cafes[" + i + "]";
            CatalogCafe cafe = catalog.cafes().get(i);
            require(cafe != null, path, "cafe must not be null");
            requireId(cafe.id(), path + ".id");
            require(cafeIds.add(cafe.id()), path + ".id", "duplicate cafe id");
            requireText(cafe.name(), path + ".name");
            requireText(cafe.roadAddress(), path + ".roadAddress");
            validateLocation(cafe.location(), path + ".location");
            validateSource(cafe.source(), path + ".source", today);
        }
        Set<String> menuIds = new HashSet<>();
        for (int i = 0; i < catalog.menus().size(); i++) {
            String path = "$.menus[" + i + "]";
            CatalogMenu menu = catalog.menus().get(i);
            require(menu != null, path, "menu must not be null");
            requireId(menu.id(), path + ".id");
            require(menuIds.add(menu.id()), path + ".id", "duplicate menu id");
            requireId(menu.cafeId(), path + ".cafeId");
            require(cafeIds.contains(menu.cafeId()), path + ".cafeId", "referenced cafe does not exist");
            requireText(menu.name(), path + ".name");
            require(menu.priceKrw() == null || menu.priceKrw() >= 0, path + ".priceKrw", "price must be null or nonnegative");
            require(menu.isDemo() != null, path + ".isDemo", "explicit demo flag is required");
            validateSource(menu.source(), path + ".source", today);
            validateAvailability(menu, path, today);
        }
    }

    private void validateLocation(CatalogCafe.Location location, String path) {
        if (location == null) {
            return;
        }
        require(location.latitude() != null && Double.isFinite(location.latitude())
                && location.latitude() >= -90 && location.latitude() <= 90,
                path + ".latitude", "latitude must be a finite number between -90 and 90");
        require(location.longitude() != null && Double.isFinite(location.longitude())
                && location.longitude() >= -180 && location.longitude() <= 180,
                path + ".longitude", "longitude must be a finite number between -180 and 180");
    }

    private void validateSource(CatalogSource source, String path, LocalDate today) {
        require(source != null, path, "source is required");
        URI url = source.url();
        require(url != null && ("https".equalsIgnoreCase(url.getScheme()) || "http".equalsIgnoreCase(url.getScheme()))
                && url.getHost() != null && url.getUserInfo() == null,
                path + ".url", "source must have an absolute HTTP(S) URL without credentials");
        requireText(source.externalId(), path + ".externalId");
        requireDate(source.observedOn(), path + ".observedOn", today);
    }

    private void validateAvailability(CatalogMenu menu, String path, LocalDate today) {
        require(menu.availability() != null, path + ".availability", "availability is required");
        CatalogMenu.SaleConfirmation confirmation = menu.saleConfirmation();
        if (menu.availability() == CatalogMenu.Availability.UNKNOWN) {
            require(confirmation == null, path + ".saleConfirmation", "unknown availability must not carry a confirmation");
        } else if (menu.availability() == CatalogMenu.Availability.AVAILABLE
                || menu.availability() == CatalogMenu.Availability.UNAVAILABLE) {
            require(confirmation != null, path + ".saleConfirmation", "confirmed availability requires evidence");
        }
        if (confirmation != null) {
            requireDate(confirmation.confirmedOn(), path + ".saleConfirmation.confirmedOn", today);
            requireText(confirmation.reference(), path + ".saleConfirmation.reference");
        }
    }

    private void requireDate(LocalDate date, String path, LocalDate today) {
        require(date != null && !date.isAfter(today), path, "date is required and must not be in the future");
    }

    private void requireId(String id, String path) {
        require(id != null && ID_PATTERN.matcher(id).matches(), path,
                "id must contain 1-64 ASCII letters, digits, hyphens or underscores and start with a letter or digit");
    }

    private void requireText(String value, String path) {
        require(value != null && !value.isBlank(), path, "nonblank text is required");
    }

    private void requireEntries(List<?> entries, String path) {
        require(entries != null && !entries.isEmpty(), path, "nonempty array is required");
    }

    private void require(boolean condition, String path, String message) {
        if (!condition) {
            throw new CatalogLoadException(path, message);
        }
    }
}
