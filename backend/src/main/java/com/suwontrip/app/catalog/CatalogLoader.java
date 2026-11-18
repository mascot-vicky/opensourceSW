package com.suwontrip.app.catalog;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.cfg.CoercionAction;
import com.fasterxml.jackson.databind.cfg.CoercionInputShape;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.type.LogicalType;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.Objects;

public final class CatalogLoader {
    private final ObjectReader reader;
    private final CatalogValidator validator;

    public CatalogLoader() {
        this(new CatalogValidator(Clock.system(ZoneId.of("Asia/Seoul"))));
    }

    public CatalogLoader(CatalogValidator validator) {
        this.validator = Objects.requireNonNull(validator, "validator");
        JsonFactory factory = JsonFactory.builder()
                .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
                .build();
        reader = JsonMapper.builder(factory)
                .addModule(new JavaTimeModule().addDeserializer(LocalDate.class, new CatalogDateDeserializer()))
                .disable(MapperFeature.ALLOW_COERCION_OF_SCALARS)
                .disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT)
                .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .enable(DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS)
                .withCoercionConfig(LogicalType.Integer, config -> config
                        .setAcceptBlankAsEmpty(false)
                        .setCoercion(CoercionInputShape.EmptyString, CoercionAction.Fail))
                .withCoercionConfig(LogicalType.Textual, config -> config
                        .setCoercion(CoercionInputShape.Integer, CoercionAction.Fail)
                        .setCoercion(CoercionInputShape.Float, CoercionAction.Fail)
                        .setCoercion(CoercionInputShape.Boolean, CoercionAction.Fail))
                .build()
                .readerFor(Catalog.class);
    }

    public Catalog load(Path path) {
        Objects.requireNonNull(path, "path");
        try {
            return load(Files.readAllBytes(path));
        } catch (IOException exception) {
            throw new CatalogLoadException("$", "cannot read catalog file: " + path, exception);
        }
    }

    public Catalog load(byte[] document) {
        Objects.requireNonNull(document, "document");
        try {
            Catalog catalog = reader.readValue(document);
            validator.validate(catalog);
            return catalog;
        } catch (JsonMappingException exception) {
            throw new CatalogLoadException(fieldPath(exception), "invalid JSON field or value", exception);
        } catch (JsonProcessingException exception) {
            throw new CatalogLoadException("$", "invalid JSON document", exception);
        } catch (IOException exception) {
            throw new CatalogLoadException("$", "cannot parse catalog document", exception);
        }
    }

    private String fieldPath(JsonMappingException exception) {
        StringBuilder path = new StringBuilder("$");
        for (JsonMappingException.Reference reference : exception.getPath()) {
            if (reference.getFieldName() != null) {
                path.append('.').append(reference.getFieldName());
            } else if (reference.getIndex() >= 0) {
                path.append('[').append(reference.getIndex()).append(']');
            }
        }
        return path.toString();
    }

    private static final class CatalogDateDeserializer extends StdScalarDeserializer<LocalDate> {
        private CatalogDateDeserializer() {
            super(LocalDate.class);
        }

        @Override
        public LocalDate deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            if (!parser.hasToken(JsonToken.VALUE_STRING)) {
                return (LocalDate) context.handleUnexpectedToken(LocalDate.class, parser);
            }
            String value = parser.getText();
            if (!value.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}")) {
                return (LocalDate) context.handleWeirdStringValue(LocalDate.class, value, "expected uuuu-MM-dd");
            }
            try {
                return LocalDate.parse(value);
            } catch (DateTimeParseException exception) {
                return (LocalDate) context.handleWeirdStringValue(LocalDate.class, value, "invalid calendar date");
            }
        }
    }
}
