package com.suwontrip.app.catalog;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CatalogLoaderTests {
    @TempDir
    Path directory;

    private final CatalogLoader loader = CatalogTestData.loader();

    @Test
    void loadsVersionedCatalogWithoutTurningUnknownValuesIntoFacts() throws Exception {
        Catalog catalog = loader.load(CatalogTestData.write(directory, CatalogTestData.document()));

        assertThat(catalog.schemaVersion()).isEqualTo(1);
        assertThat(catalog.dataVersion()).isEqualTo("test-2026-11-18.1");
        assertThat(catalog.cafes().getFirst().location()).isNull();
        CatalogMenu menu = catalog.menus().getFirst();
        assertThat(menu.name()).isEqualTo("가상 테스트 음료");
        assertThat(menu.description()).isNull();
        assertThat(menu.priceKrw()).isNull();
        assertThat(menu.isDemo()).isTrue();
        assertThat(menu.availability()).isEqualTo(CatalogMenu.Availability.UNKNOWN);
        assertThat(menu.saleConfirmation()).isNull();
        assertThat(menu.source().externalId()).isEqualTo("external-menu-001");
        assertThat(menu.source().observedOn()).isEqualTo(LocalDate.of(2026, 11, 18));
    }

    @Test
    void acceptsOmittedOptionalFieldsWithoutDefaults() throws Exception {
        ObjectNode document = CatalogTestData.document();
        ((ObjectNode) document.at("/cafes/0")).remove("location");
        ((ObjectNode) document.at("/menus/0")).remove(List.of("description", "priceKrw", "saleConfirmation"));

        Catalog catalog = loader.load(CatalogTestData.write(directory, document));

        assertThat(catalog.cafes().getFirst().location()).isNull();
        assertThat(catalog.menus().getFirst().priceKrw()).isNull();
        assertThat(catalog.menus().getFirst().description()).isNull();
        assertThat(catalog.menus().getFirst().saleConfirmation()).isNull();
    }

    @Test
    void distinguishesExplicitZeroFromUnknownPrice() throws Exception {
        Catalog catalog = loader.load(CatalogTestData.write(directory, CatalogTestData.replace("/menus/0/priceKrw", "0")));

        assertThat(catalog.menus().getFirst().priceKrw()).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {" ", "\t", "\n", " \t\n"})
    void rejectsBlankPriceStringsInsteadOfTreatingThemAsUnknown(String value) throws Exception {
        ObjectNode document = CatalogTestData.document();
        ((ObjectNode) document.at("/menus/0")).put("priceKrw", value);

        assertThatExceptionOfType(CatalogLoadException.class)
                .isThrownBy(() -> loader.load(CatalogTestData.write(directory, document)))
                .satisfies(error -> assertThat(error.getFieldPath()).isEqualTo("$.menus[0].priceKrw"));
    }

    @Test
    void keepsSameNamedMenusAtDifferentCafesSeparate() throws Exception {
        ObjectNode document = twoCafeDocument();

        Catalog catalog = loader.load(CatalogTestData.write(directory, document));

        assertThat(catalog.cafes()).extracting(CatalogCafe::id).containsExactly("cafe-test-001", "cafe-test-002");
        assertThat(catalog.menus()).extracting(CatalogMenu::cafeId).containsExactly("cafe-test-001", "cafe-test-002");
        assertThat(catalog.menus()).extracting(CatalogMenu::id).containsExactly("menu-test-001", "menu-test-002");
        assertThat(catalog.menus()).extracting(CatalogMenu::priceKrw).containsExactly(6500L, 7000L);
        assertThat(catalog.menus()).extracting(menu -> menu.source().externalId())
                .containsExactly("external-menu-001", "external-menu-001");
    }

    @Test
    void rejectsTheWholeCatalogWhenALaterMenuReferencesAMissingCafe() throws Exception {
        ObjectNode document = twoCafeDocument();
        ((ObjectNode) document.at("/menus/1")).put("cafeId", "missing-cafe");

        assertThatExceptionOfType(CatalogLoadException.class)
                .isThrownBy(() -> loader.load(CatalogTestData.write(directory, document)))
                .satisfies(error -> assertThat(error.getFieldPath()).isEqualTo("$.menus[1].cafeId"));
    }

    @Test
    void acceptsARealLeapDayWithoutChangingIt() throws Exception {
        Catalog catalog = loader.load(CatalogTestData.write(directory,
                CatalogTestData.replace("/menus/0/source/observedOn", "\"2024-02-29\"")));

        assertThat(catalog.menus().getFirst().source().observedOn()).isEqualTo(LocalDate.of(2024, 2, 29));
    }

    @ParameterizedTest(name = "{0} rejects {1}")
    @MethodSource("wrongTypes")
    void rejectsCoercionsAndInvalidTypedValues(String pointer, String value, String expectedPath) throws Exception {
        Path file = CatalogTestData.write(directory, CatalogTestData.replace(pointer, value));

        assertThatExceptionOfType(CatalogLoadException.class).isThrownBy(() -> loader.load(file))
                .satisfies(error -> assertThat(error.getFieldPath()).isEqualTo(expectedPath));
    }

    static Stream<Arguments> wrongTypes() {
        return Stream.of(
                Arguments.of("/schemaVersion", "\"1\"", "$.schemaVersion"),
                Arguments.of("/schemaVersion", "1.5", "$.schemaVersion"),
                Arguments.of("/menus/0/priceKrw", "\"6500\"", "$.menus[0].priceKrw"),
                Arguments.of("/menus/0/priceKrw", "6500.5", "$.menus[0].priceKrw"),
                Arguments.of("/menus/0/priceKrw", "\"\"", "$.menus[0].priceKrw"),
                Arguments.of("/menus/0/priceKrw", "9223372036854775808", "$.menus[0].priceKrw"),
                Arguments.of("/menus/0/isDemo", "\"true\"", "$.menus[0].isDemo"),
                Arguments.of("/menus/0/isDemo", "1", "$.menus[0].isDemo"),
                Arguments.of("/menus/0/name", "123", "$.menus[0].name"),
                Arguments.of("/menus/0/description", "false", "$.menus[0].description"),
                Arguments.of("/menus/0/availability", "0", "$.menus[0].availability"),
                Arguments.of("/menus/0/availability", "\"OPEN\"", "$.menus[0].availability"),
                Arguments.of("/menus/0/source/observedOn", "\"2026-02-30\"", "$.menus[0].source.observedOn"),
                Arguments.of("/menus/0/source/observedOn", "\"2026-1-1\"", "$.menus[0].source.observedOn"),
                Arguments.of("/menus/0/source/observedOn", "[2026,11,18]", "$.menus[0].source.observedOn"),
                Arguments.of("/menus/0/source/observedOn", "1794960000000", "$.menus[0].source.observedOn"),
                Arguments.of("/cafes/0/location", "{\"latitude\":\"37.2\",\"longitude\":127}", "$.cafes[0].location.latitude"),
                Arguments.of("/cafes", "{}", "$.cafes"),
                Arguments.of("/dataVersion", "42", "$.dataVersion")
        );
    }

    @Test
    void rejectsUnknownNestedFieldsInsteadOfLosingInformation() throws Exception {
        ObjectNode document = CatalogTestData.document();
        ((ObjectNode) document.at("/menus/0")).put("prcieKrw", 6500);

        assertThatExceptionOfType(CatalogLoadException.class)
                .isThrownBy(() -> loader.load(CatalogTestData.write(directory, document)))
                .satisfies(error -> assertThat(error.getFieldPath()).isEqualTo("$.menus[0].prcieKrw"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "{", "[]", "null", "{\"schemaVersion\":1,\"schemaVersion\":2}"})
    void rejectsInvalidOrEmptyDocuments(String json) throws Exception {
        Path file = Files.writeString(directory.resolve("invalid.json"), json);

        assertThatThrownBy(() -> loader.load(file)).isInstanceOf(CatalogLoadException.class);
    }

    @Test
    void rejectsDuplicateNestedKeys() throws Exception {
        String json = CatalogTestData.JSON.replace("\"priceKrw\": null", "\"priceKrw\": 6500, \"priceKrw\": 0");
        Path file = Files.writeString(directory.resolve("duplicate.json"), json);

        assertThatThrownBy(() -> loader.load(file)).isInstanceOf(CatalogLoadException.class);
    }

    @Test
    void rejectsASecondDocumentAfterTheCatalog() throws Exception {
        Path file = Files.writeString(directory.resolve("trailing.json"), CatalogTestData.JSON + "{}");

        assertThatThrownBy(() -> loader.load(file)).isInstanceOf(CatalogLoadException.class);
    }

    @Test
    void reportsUnreadableFilesWithoutReturningAnEmptyCatalog() {
        assertThatExceptionOfType(CatalogLoadException.class)
                .isThrownBy(() -> loader.load(directory.resolve("missing.json")))
                .withCauseInstanceOf(IOException.class)
                .withMessageContaining("cannot read catalog file");
    }

    @Test
    void canLoadAgainAfterARejectedFile() throws Exception {
        Path file = Files.writeString(directory.resolve("catalog.json"), "{");
        assertThatThrownBy(() -> loader.load(file)).isInstanceOf(CatalogLoadException.class);
        Files.writeString(file, CatalogTestData.JSON);

        assertThat(loader.load(file).menus()).hasSize(1);
    }

    private ObjectNode twoCafeDocument() throws IOException {
        ObjectNode document = CatalogTestData.document();
        ObjectNode secondCafe = ((ObjectNode) document.at("/cafes/0")).deepCopy();
        secondCafe.put("id", "cafe-test-002").put("name", "가상 테스트 카페 2").put("roadAddress", "테스트 전용 주소 2");
        ((ObjectNode) secondCafe.get("source"))
                .put("url", "https://example.invalid/cafe/test-002").put("externalId", "test-002");
        ((ArrayNode) document.get("cafes")).add(secondCafe);
        ObjectNode firstMenu = (ObjectNode) document.at("/menus/0");
        firstMenu.put("priceKrw", 6500);
        ObjectNode secondMenu = firstMenu.deepCopy();
        secondMenu.put("id", "menu-test-002").put("cafeId", "cafe-test-002").put("priceKrw", 7000);
        ((ObjectNode) secondMenu.get("source")).put("url", "https://example.invalid/cafe/test-002/menu");
        ((ArrayNode) document.get("menus")).add(secondMenu);
        return document;
    }
}
