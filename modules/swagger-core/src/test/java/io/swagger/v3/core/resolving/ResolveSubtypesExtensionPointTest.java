package io.swagger.v3.core.resolving;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverterContext;
import io.swagger.v3.core.converter.ModelConverterContextImpl;
import io.swagger.v3.core.jackson.ModelResolver;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.media.Schema;
import org.testng.annotations.Test;
import tools.jackson.databind.BeanDescription;
import tools.jackson.databind.ObjectMapper;

import java.util.concurrent.atomic.AtomicInteger;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * {@code ModelResolver.resolveSubtypes(...)} is an extension point: subclasses can override it to
 * control subtype resolution, matching the pattern suggested in swagger-api/swagger-core#5030.
 */
public class ResolveSubtypesExtensionPointTest {

    @Test
    public void subclassCanOverrideResolveSubtypes() {
        final AtomicInteger calls = new AtomicInteger();
        final ModelResolver resolver = new ModelResolver(Json.mapper()) {
            @Override
            protected boolean resolveSubtypes(Schema model, BeanDescription bean,
                                              ModelConverterContext context, JsonView jsonViewAnnotation) {
                calls.incrementAndGet();
                return false;
            }
        };

        final ModelConverterContextImpl context = new ModelConverterContextImpl(resolver);
        context.resolve(new AnnotatedType(Pet.class));

        assertEquals(calls.get(), 1,
                "the overridden resolveSubtypes must be invoked exactly once for the resolved type");
        assertTrue(calls.get() > 0, "the overridden resolveSubtypes must be reachable");
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "petType")
    @JsonSubTypes({@JsonSubTypes.Type(value = Dog.class, name = "dog"),
                   @JsonSubTypes.Type(value = Cat.class, name = "cat")})
    static abstract class Pet {
        public String petType;
    }

    static class Dog extends Pet {
    }

    static class Cat extends Pet {
    }
}
