package com.suwontrip.app.catalog.config;

import com.suwontrip.app.catalog.Catalog;
import com.suwontrip.app.catalog.CatalogLoadException;
import com.suwontrip.app.catalog.CatalogMenu;
import com.suwontrip.app.SuwonTripApplication;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.LazyInitializationBeanFactoryPostProcessor;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class CatalogConfigurationTests {
    private static final String VALID_JSON = """
            {
              "schemaVersion": 1,
              "dataVersion": "configuration-test-v1",
              "cafes": [
                {
                  "id": "cafe-test-001",
                  "name": "가상 설정 테스트 카페",
                  "roadAddress": "테스트 전용 주소",
                  "source": {
                    "url": "https://example.invalid/cafe/001",
                    "externalId": "external-cafe-001",
                    "observedOn": "2020-01-01"
                  }
                }
              ],
              "menus": [
                {
                  "id": "menu-test-001",
                  "cafeId": "cafe-test-001",
                  "name": "가상 설정 테스트 음료",
                  "isDemo": true,
                  "availability": "UNKNOWN",
                  "source": {
                    "url": "https://example.invalid/menu/001",
                    "externalId": "external-menu-001",
                    "observedOn": "2020-01-01"
                  }
                }
              ]
            }
            """;

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(CatalogConfiguration.class);

    @TempDir
    Path directory;

    @Test
    void startsWithoutCatalogWhenActivationIsOmitted() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(Catalog.class);
        });
    }

    @Test
    void ignoresTheCatalogPathWhenDisabled() {
        runner.withPropertyValues(
                "app.catalog.enabled=false",
                "app.catalog.path=" + directory.resolve("missing.json")
        ).run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(Catalog.class);
        });
    }

    @Test
    void loadsOneImmutableCatalogDuringStartup() throws Exception {
        Path file = Files.writeString(directory.resolve("메뉴 catalog.json"), VALID_JSON);

        enabled(file).run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(Catalog.class);

            Catalog catalog = context.getBean(Catalog.class);
            assertThat(catalog.cafes()).hasSize(1);
            assertThat(catalog.menus()).hasSize(1);
            assertThat(catalog.menus().getFirst().availability())
                    .isEqualTo(CatalogMenu.Availability.UNKNOWN);
            assertThat(catalog.menus().getFirst().priceKrw()).isNull();

            Files.writeString(file, "invalid replacement");

            assertThat(context.getBean(Catalog.class)).isSameAs(catalog);
            assertThat(catalog.menus().getFirst().name()).isEqualTo("가상 설정 테스트 음료");
        });
    }

    @Test
    void rejectsAMissingPathDuringStartup() {
        runner.withPropertyValues("app.catalog.enabled=true").run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure())
                    .hasRootCauseInstanceOf(IllegalStateException.class)
                    .hasStackTraceContaining("app.catalog.path is required");
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "\t"})
    void rejectsABlankPathDuringStartup(String path) {
        runner.withPropertyValues(
                "app.catalog.enabled=true",
                "app.catalog.path=" + path
        ).run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure())
                    .hasStackTraceContaining("app.catalog.path is required");
        });
    }

    @Test
    void rejectsAMissingFileDuringStartup() {
        enabled(directory.resolve("missing.json")).run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure())
                    .hasStackTraceContaining("cannot read catalog file");
        });
    }

    @Test
    void rejectsADirectoryInsteadOfAFile() {
        enabled(directory).run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure())
                    .hasStackTraceContaining("cannot read catalog file");
        });
    }

    @Test
    void rejectsAnInvalidFilesystemPath() {
        runner.withPropertyValues(
                "app.catalog.enabled=true",
                "app.catalog.path=invalid\u0000path"
        ).run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure())
                    .hasStackTraceContaining("app.catalog.path is not a valid filesystem path");
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"{", "null", "{}"})
    void rejectsInvalidCatalogDocumentsDuringStartup(String json) throws Exception {
        Path file = Files.writeString(directory.resolve("invalid.json"), json);

        enabled(file).run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure())
                    .hasStackTraceContaining(CatalogLoadException.class.getName());
        });
    }

    @Test
    void rejectsCatalogReferenceErrorsDuringStartup() throws Exception {
        String json = VALID_JSON.replace(
                "\"cafeId\": \"cafe-test-001\"",
                "\"cafeId\": \"missing-cafe\""
        );
        Path file = Files.writeString(directory.resolve("invalid-reference.json"), json);

        enabled(file).run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure())
                    .hasStackTraceContaining("$.menus[0].cafeId");
        });
    }

    @Test
    void validatesAtStartupEvenWhenBeansAreGloballyLazy() throws Exception {
        Path file = Files.writeString(directory.resolve("invalid-lazy.json"), "{");

        enabled(file)
                .withInitializer(context -> context.addBeanFactoryPostProcessor(
                        new LazyInitializationBeanFactoryPostProcessor()
                ))
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void discoversTheConfigurationThroughApplicationPackageScanning() throws Exception {
        Path file = Files.writeString(directory.resolve("application-catalog.json"), VALID_JSON);

        new WebApplicationContextRunner()
                .withInitializer(new ConfigDataApplicationContextInitializer())
                .withUserConfiguration(SuwonTripApplication.class)
                .withPropertyValues(
                        "spring.datasource.url=jdbc:h2:mem:catalog-startup;DB_CLOSE_DELAY=-1",
                        "spring.datasource.username=sa",
                        "spring.datasource.password=",
                        "app.catalog.enabled=true",
                        "app.catalog.path=" + file
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(Catalog.class);
                    assertThat(context.getBean(Catalog.class).menus()).hasSize(1);
                });
    }

    private ApplicationContextRunner enabled(Path file) {
        return runner.withPropertyValues(
                "app.catalog.enabled=true",
                "app.catalog.path=" + file
        );
    }
}
