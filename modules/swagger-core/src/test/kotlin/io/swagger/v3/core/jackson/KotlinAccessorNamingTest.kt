package io.swagger.v3.core.jackson

import io.swagger.v3.core.converter.AnnotatedType
import io.swagger.v3.core.converter.ModelConverterContextImpl
import io.swagger.v3.core.util.ObjectMapperFactory
import io.swagger.v3.oas.models.media.Schema
import org.testng.Assert.assertTrue
import org.testng.annotations.Test
import tools.jackson.databind.PropertyNamingStrategies
import tools.jackson.databind.annotation.JsonNaming

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class SimpleUser(val uid: Long, val isMaster: Boolean)

/**
 * Reproduces the "Why it matters" scenario from PR #147: a Kotlin data class with a boolean
 * `isFoo` property. The PR claims that Jackson's accessor naming strips the `is` prefix to
 * `foo` while jackson-module-kotlin contributes an `isFoo` creator property, so the schema
 * ends up carrying BOTH names (`[uid, isMaster, master]`), and that the fix in
 * `AbstractModelConverter` (only override accessor naming when the caller hasn't) resolves it.
 *
 * ## What we tried to reproduce the split
 *
 * All runs use the full production mapper path (`ObjectMapperFactory.createJson()`, which sets
 * the permissive `DefaultAccessorNamingStrategy.Provider().withFirstCharAcceptance(true, true)`)
 * and `@JsonNaming(SnakeCaseStrategy)` on the class. Results:
 *
 * 1. Plain swagger mapper, no jackson-module-kotlin (`testKotlinIsPrefixProperty`):
 *    properties = `[uid, master]`. The accessor naming strips `isMaster` to `master`, and with
 *    no creator-parameter discovery the schema only carries the accessor-derived name. No split,
 *    but note the `is` prefix is also absent entirely (`is_master` / `isMaster` are not present).
 * 2. Plus jackson-module-kotlin 3.2.1 registered on the mapper
 *    (`testKotlinIsPrefixPropertyWithJmk`): properties = `[uid, is_master]`. jmk names the
 *    creator parameter `isMaster` via `KotlinNamesAnnotationIntrospector.findImplicitPropertyName`,
 *    SnakeCaseStrategy maps it to `is_master`, and the accessor-derived `master` is NOT emitted.
 *    A single clean property, no split.
 *
 * ## Conclusion
 *
 * The `[uid, isMaster, master]` split described in PR #147 does NOT reproduce on the current
 * stack (Jackson 3.2.1 + jackson-module-kotlin 3.2.1). jmk already names the boolean property
 * correctly, so the accessor-naming behavior in `AbstractModelConverter` is not what drives the
 * Kotlin scenario. The PR's fix is still justified on its own merits (restoring caller-supplied
 * `AccessorNamingStrategy`, see `CallerAccessorNamingTest`), but its "Why it matters" Kotlin
 * rationale is not supported by reproduction.
 */
class KotlinAccessorNamingTest {

    @Test
    fun testKotlinIsPrefixProperty() {
        val modelResolver = ModelResolver(ObjectMapperFactory.createJson())
        val context = ModelConverterContextImpl(modelResolver)

        val schema: Schema<*> = modelResolver.resolve(AnnotatedType(SimpleUser::class.java), context, null)

        println("Kotlin SimpleUser properties: " + schema.properties.keys)
        println("Kotlin SimpleUser is_master schema: " + schema.properties["is_master"])
        println("Kotlin SimpleUser master schema: " + schema.properties["master"])
        // Without jmk the creator-parameter name is never discovered: the schema only has the
        // accessor-derived `master`. This documents the pre-existing gap, not the PR's split.
        assertTrue(!schema.properties.containsKey("is_master") && !schema.properties.containsKey("isMaster"),
            "expected no is-prefixed property without jmk, got ${schema.properties.keys}")
    }

    @Test
    fun testKotlinIsPrefixPropertyWithJmk() {
        val mapper = KotlinTestMappers.createJsonWithKotlinModule()
        val modelResolver = ModelResolver(mapper)
        val context = ModelConverterContextImpl(modelResolver)

        val schema: Schema<*> = modelResolver.resolve(AnnotatedType(SimpleUser::class.java), context, null)

        println("Kotlin SimpleUser with jmk properties: " + schema.properties.keys)
        println("Kotlin SimpleUser with jmk is_master schema: " + schema.properties["is_master"])
        println("Kotlin SimpleUser with jmk master schema: " + schema.properties["master"])
        // With jmk the schema is a single clean property; the PR's claimed
        // [uid, isMaster, master] split does not occur.
        assertTrue(schema.properties.containsKey("is_master") || schema.properties.containsKey("isMaster"),
            "expected a single is-prefixed property with jmk, got ${schema.properties.keys}")
        assertTrue(!schema.properties.containsKey("master"),
            "expected no accessor-derived 'master' duplicate with jmk, got ${schema.properties.keys}")
    }
}
