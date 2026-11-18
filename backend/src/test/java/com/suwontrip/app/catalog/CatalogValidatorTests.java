package com.suwontrip.app.catalog;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CatalogValidatorTests {
    @TempDir
    Path directory;

    private final CatalogLoader loader = CatalogTestData.loader();

    @ParameterizedTest(name = "{0} rejects {1}")
    @MethodSource("invalidValues")
    void rejectsInvalidCatalogValuesWithTheirFieldPath(String pointer, String value, String expectedPath) throws Exception {
        Path file = CatalogTestData.write(directory, CatalogTestData.replace(pointer, value));

        assertThatExceptionOfType(CatalogLoadException.class).isThrownBy(() -> loader.load(file))
                .satisfies(error -> assertThat(error.getFieldPath()).isEqualTo(expectedPath));
    }

    static Stream<Arguments> invalidValues() {
        return Stream.of(
                Arguments.of("/schemaVersion", "2", "$.schemaVersion"),
                Arguments.of("/schemaVersion", "null", "$.schemaVersion"),
                Arguments.of("/dataVersion", "\" \"", "$.dataVersion"),
                Arguments.of("/cafes", "[]", "$.cafes"),
                Arguments.of("/menus", "null", "$.menus"),
                Arguments.of("/cafes/0", "null", "$.cafes[0]"),
                Arguments.of("/menus/0", "null", "$.menus[0]"),
                Arguments.of("/cafes/0/id", "\"bad id\"", "$.cafes[0].id"),
                Arguments.of("/cafes/0/name", "\" \"", "$.cafes[0].name"),
                Arguments.of("/cafes/0/roadAddress", "null", "$.cafes[0].roadAddress"),
                Arguments.of("/menus/0/cafeId", "\"missing-cafe\"", "$.menus[0].cafeId"),
                Arguments.of("/menus/0/priceKrw", "-1", "$.menus[0].priceKrw"),
                Arguments.of("/menus/0/isDemo", "null", "$.menus[0].isDemo"),
                Arguments.of("/menus/0/availability", "null", "$.menus[0].availability"),
                Arguments.of("/menus/0/source", "null", "$.menus[0].source"),
                Arguments.of("/menus/0/source/url", "\"/relative/menu\"", "$.menus[0].source.url"),
                Arguments.of("/menus/0/source/url", "\"file:///tmp/menu\"", "$.menus[0].source.url"),
                Arguments.of("/menus/0/source/url", "\"https://user:password@example.invalid/menu\"", "$.menus[0].source.url"),
                Arguments.of("/menus/0/source/externalId", "\"\"", "$.menus[0].source.externalId"),
                Arguments.of("/menus/0/source/observedOn", "null", "$.menus[0].source.observedOn"),
                Arguments.of("/menus/0/source/observedOn", "\"2026-11-19\"", "$.menus[0].source.observedOn"),
                Arguments.of("/cafes/0/location", "{\"latitude\":91,\"longitude\":127}", "$.cafes[0].location.latitude"),
                Arguments.of("/cafes/0/location", "{\"latitude\":37,\"longitude\":-181}", "$.cafes[0].location.longitude"),
                Arguments.of("/cafes/0/location", "{\"latitude\":37}", "$.cafes[0].location.longitude"),
                Arguments.of("/menus/0/availability", "\"AVAILABLE\"", "$.menus[0].saleConfirmation"),
                Arguments.of("/menus/0/availability", "\"UNAVAILABLE\"", "$.menus[0].saleConfirmation"),
                Arguments.of("/menus/0/saleConfirmation", "{\"confirmedOn\":\"2026-11-18\",\"reference\":\"test evidence\"}", "$.menus[0].saleConfirmation")
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"cafes", "menus"})
    void rejectsDuplicateInternalIds(String collection) throws Exception {
        ObjectNode document = CatalogTestData.document();
        ArrayNode entries = (ArrayNode) document.get(collection);
        entries.add(entries.get(0).deepCopy());

        assertThatExceptionOfType(CatalogLoadException.class)
                .isThrownBy(() -> loader.load(CatalogTestData.write(directory, document)))
                .satisfies(error -> assertThat(error.getFieldPath()).isEqualTo("$." + collection + "[1].id"));
    }

    @Test
    void requiresAnExplicitDemoFlagEvenWhenTheFieldIsOmitted() throws Exception {
        ObjectNode document = CatalogTestData.document();
        ((ObjectNode) document.at("/menus/0")).remove("isDemo");

        assertThatExceptionOfType(CatalogLoadException.class)
                .isThrownBy(() -> loader.load(CatalogTestData.write(directory, document)))
                .satisfies(error -> assertThat(error.getFieldPath()).isEqualTo("$.menus[0].isDemo"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"AVAILABLE", "UNAVAILABLE"})
    void preservesConfirmationSeparatelyFromObservationDate(String status) throws Exception {
        ObjectNode document = confirmedDocument(status);

        CatalogMenu menu = loader.load(CatalogTestData.write(directory, document)).menus().getFirst();

        assertThat(menu.availability().name()).isEqualTo(status);
        assertThat(menu.saleConfirmation().confirmedOn()).isEqualTo(LocalDate.of(2026, 11, 17));
        assertThat(menu.source().observedOn()).isEqualTo(LocalDate.of(2026, 11, 18));
    }

    @ParameterizedTest
    @ValueSource(strings = {"confirmedOn", "reference"})
    void rejectsIncompleteSaleConfirmation(String field) throws Exception {
        ObjectNode document = confirmedDocument("AVAILABLE");
        ((ObjectNode) document.at("/menus/0/saleConfirmation")).remove(field);

        assertThatExceptionOfType(CatalogLoadException.class)
                .isThrownBy(() -> loader.load(CatalogTestData.write(directory, document)))
                .satisfies(error -> assertThat(error.getFieldPath()).isEqualTo("$.menus[0].saleConfirmation." + field));
    }

    @Test
    void rejectsFutureSaleConfirmation() throws Exception {
        ObjectNode document = confirmedDocument("AVAILABLE");
        ((ObjectNode) document.at("/menus/0/saleConfirmation")).put("confirmedOn", "2026-11-19");

        assertThatExceptionOfType(CatalogLoadException.class)
                .isThrownBy(() -> loader.load(CatalogTestData.write(directory, document)))
                .satisfies(error -> assertThat(error.getFieldPath()).endsWith("saleConfirmation.confirmedOn"));
    }

    @Test
    void preservesSourceMarkedSeasonEndWithoutInventingAConfirmation() throws Exception {
        Catalog catalog = loader.load(CatalogTestData.write(directory,
                CatalogTestData.replace("/menus/0/availability", "\"SEASON_ENDED\"")));

        assertThat(catalog.menus().getFirst().availability()).isEqualTo(CatalogMenu.Availability.SEASON_ENDED);
        assertThat(catalog.menus().getFirst().saleConfirmation()).isNull();
    }

    @Test
    void usesTheSuppliedClockZoneForCalendarDates() throws Exception {
        Clock clock = Clock.fixed(Instant.parse("2026-11-17T15:01:00Z"), ZoneId.of("Asia/Seoul"));
        CatalogLoader seoulLoader = new CatalogLoader(new CatalogValidator(clock));

        assertThat(seoulLoader.load(CatalogTestData.write(directory, CatalogTestData.document())).cafes()).hasSize(1);
    }

    @Test
    void acceptsCoordinateBoundaries() throws Exception {
        Catalog catalog = loader.load(CatalogTestData.write(directory,
                CatalogTestData.replace("/cafes/0/location", "{\"latitude\":-90,\"longitude\":180}")));

        assertThat(catalog.cafes().getFirst().location()).isEqualTo(new CatalogCafe.Location(-90.0, 180.0));
    }

    @Test
    void rejectsNonFiniteCoordinatesFromProgrammaticInput() throws Exception {
        Catalog valid = loader.load(CatalogTestData.write(directory, CatalogTestData.document()));
        CatalogCafe cafe = valid.cafes().getFirst();
        CatalogCafe invalid = new CatalogCafe(cafe.id(), cafe.name(), cafe.roadAddress(),
                new CatalogCafe.Location(Double.NaN, 127.0), cafe.source());
        Catalog catalog = new Catalog(1, valid.dataVersion(), List.of(invalid), valid.menus());

        assertThatExceptionOfType(CatalogLoadException.class)
                .isThrownBy(() -> new CatalogValidator(CatalogTestData.CLOCK).validate(catalog))
                .satisfies(error -> assertThat(error.getFieldPath()).isEqualTo("$.cafes[0].location.latitude"));
    }

    @Test
    void protectsCatalogListsFromCallerMutation() throws Exception {
        Catalog valid = loader.load(CatalogTestData.write(directory, CatalogTestData.document()));
        List<CatalogCafe> cafes = new ArrayList<>(valid.cafes());
        List<CatalogMenu> menus = new ArrayList<>(valid.menus());
        Catalog catalog = new Catalog(1, valid.dataVersion(), cafes, menus);

        cafes.clear();
        menus.clear();

        assertThat(catalog.cafes()).hasSize(1);
        assertThat(catalog.menus()).hasSize(1);
        assertThatThrownBy(() -> catalog.cafes().clear()).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> catalog.menus().clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    private ObjectNode confirmedDocument(String status) throws Exception {
        ObjectNode document = CatalogTestData.document();
        ObjectNode menu = (ObjectNode) document.at("/menus/0");
        menu.put("availability", status);
        menu.putObject("saleConfirmation")
                .put("confirmedOn", "2026-11-17")
                .put("reference", "가상 테스트 판매 확인 기록");
        return document;
    }
}
