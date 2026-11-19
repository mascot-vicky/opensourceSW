package com.suwontrip.app.catalog;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

final class CatalogTestData {
    static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-18T00:00:00Z"), ZoneId.of("Asia/Seoul"));
    static final String JSON = """
            {
              "schemaVersion": 1,
              "dataVersion": "test-2026-11-18.1",
              "cafes": [{
                "id": "cafe-test-001",
                "name": "가상 테스트 카페",
                "roadAddress": "테스트 전용 주소",
                "location": null,
                "source": {
                  "url": "https://example.invalid/cafe/test-001",
                  "externalId": "test-001",
                  "observedOn": "2026-11-18"
                }
              }],
              "menus": [{
                "id": "menu-test-001",
                "cafeId": "cafe-test-001",
                "name": "가상 테스트 음료",
                "description": null,
                "priceKrw": null,
                "isDemo": true,
                "availability": "UNKNOWN",
                "saleConfirmation": null,
                "source": {
                  "url": "https://example.invalid/cafe/test-001/menu",
                  "externalId": "external-menu-001",
                  "observedOn": "2026-11-18"
                }
              }]
            }
            """;
    private static final JsonMapper MAPPER = new JsonMapper();

    private CatalogTestData() {
    }

    static ObjectNode document() throws IOException {
        return (ObjectNode) MAPPER.readTree(JSON);
    }

    static ObjectNode replace(String pointer, String jsonValue) throws IOException {
        ObjectNode document = document();
        JsonPointer target = JsonPointer.compile(pointer);
        JsonNode parent = document.at(target.head());
        JsonNode value = MAPPER.readTree(jsonValue);
        if (parent instanceof ArrayNode array) {
            array.set(target.last().getMatchingIndex(), value);
        } else {
            ((ObjectNode) parent).set(target.last().getMatchingProperty(), value);
        }
        return document;
    }

    static Path write(Path directory, JsonNode document) throws IOException {
        return Files.writeString(directory.resolve("catalog.json"), document.toString());
    }

    static CatalogLoader loader() {
        return new CatalogLoader(new CatalogValidator(CLOCK));
    }
}
