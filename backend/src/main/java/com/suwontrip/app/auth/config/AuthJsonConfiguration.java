package com.suwontrip.app.auth.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.cfg.CoercionAction;
import com.fasterxml.jackson.databind.cfg.CoercionInputShape;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AuthJsonConfiguration {

    // Spring이 구성하는 기본 ObjectMapper 전체에 적용
    @Bean
    Jackson2ObjectMapperBuilderCustomizer strictRequestJson() {
        return builder -> {

            builder.featuresToEnable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION,
                    DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                    DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES,
                    DeserializationFeature.FAIL_ON_TRAILING_TOKENS);

            builder.postConfigurer(mapper -> mapper.coercionConfigFor(String.class)
                    .setCoercion(CoercionInputShape.Integer, CoercionAction.Fail)
                    .setCoercion(CoercionInputShape.Float, CoercionAction.Fail)
                    .setCoercion(CoercionInputShape.Boolean, CoercionAction.Fail));
        };
    }
}
