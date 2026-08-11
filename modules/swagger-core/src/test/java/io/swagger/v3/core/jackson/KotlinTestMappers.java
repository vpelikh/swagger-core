package io.swagger.v3.core.jackson;

import io.swagger.v3.core.util.ObjectMapperFactory;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.module.kotlin.KotlinModule;

/**
 * Builds mappers for the Kotlin accessor-naming reproduction tests. Kotlin cannot type the
 * self-referential generics of {@link ObjectMapper#rebuild()}, so the builder chain lives here.
 */
final class KotlinTestMappers {

    private KotlinTestMappers() {
    }

    /** The same mapper swagger-core uses in production, plus jackson-module-kotlin. */
    static ObjectMapper createJsonWithKotlinModule() {
        return ObjectMapperFactory.createJson().rebuild()
                .addModule(new KotlinModule.Builder().build())
                .build();
    }
}
