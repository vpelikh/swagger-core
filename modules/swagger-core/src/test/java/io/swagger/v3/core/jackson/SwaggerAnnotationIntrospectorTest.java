package io.swagger.v3.core.jackson;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import tools.jackson.databind.ObjectMapper;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverterContextImpl;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.Schema;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class SwaggerAnnotationIntrospectorTest {

    private ModelConverterContextImpl context;

    @BeforeMethod
    public void setup() {
        ModelResolver modelResolver = new ModelResolver(new ObjectMapper());
        context = new ModelConverterContextImpl(modelResolver);
    }

    @AfterMethod
    public void tearDown() {
        context = null;
    }

    @Test
    public void testFindSubtypesWithJsonSubTypesOnly() {
        // This test verifies that @JsonSubTypes annotation is properly handled
        // when no @Schema(subTypes) is present (the Jackson 3 regression case)
        final Schema<?> type = context.resolve(new AnnotatedType(Animal.class));

        Assert.assertNotNull(type);

        // With @JsonSubTypes support, findSubtypes should return subtypes
        // and they should be available in the resolved schema's oneOf or discriminator
        Assert.assertNotNull(context.getDefinedModels().get("Dog"));
        Assert.assertNotNull(context.getDefinedModels().get("Cat"));
    }

    @Test
    public void testFindSubtypesWithSchemaSubTypesOnly() {
        final Schema<?> type = context.resolve(new AnnotatedType(Vehicle.class));

        Assert.assertNotNull(type);

        // Verify @Schema(subTypes) is handled
        Assert.assertNotNull(context.getDefinedModels().get("Car"));
        Assert.assertNotNull(context.getDefinedModels().get("Truck"));
    }

    @Test
    public void testSchemaOneOfIsNotIgnoredForJsonSubTypes() {
        // Mirror of https://github.com/swagger-api/swagger-core/issues/4732
        // A type declared with @Schema(oneOf = {...}) must not silently gain extra
        // subtypes (e.g. a third class) beyond those explicitly configured.
        final Schema<?> type = context.resolve(new AnnotatedType(OneOfParent.class));

        Assert.assertNotNull(type);
        Assert.assertNotNull(type.getOneOf());
        Assert.assertEquals(type.getOneOf().size(), 2);
        Assert.assertEquals(type.getOneOf().get(0).get$ref(), "#/components/schemas/Child1");
        Assert.assertEquals(type.getOneOf().get(1).get$ref(), "#/components/schemas/Child2");
    }

    @Test
    public void testJsonSubTypesDrivesDiscriminatorMapping() {
        // Mirror of https://github.com/swagger-api/swagger-core/issues/3411
        // A parent declaring only @JsonSubTypes should resolve its subtypes without
        // needing a redundant @Schema(discriminatorMapping) — findSubtypes() must
        // surface the @JsonSubTypes names so the discriminator mapping can be built.
        final Schema<?> type = context.resolve(new AnnotatedType(Shape.class));

        Assert.assertNotNull(type);

        // Subtypes declared via @JsonSubTypes are resolved into defined models
        Assert.assertNotNull(context.getDefinedModels().get("Circle"));
        Assert.assertNotNull(context.getDefinedModels().get("Square"));

        // Discriminator driven by @JsonTypeInfo.property, and mapping filled from @JsonSubTypes names
        Assert.assertNotNull(type.getDiscriminator());
        Assert.assertEquals(type.getDiscriminator().getPropertyName(), "kind");
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "kind", visible = true)
    @JsonSubTypes({
        @JsonSubTypes.Type(value = Circle.class, name = "Circle"),
        @JsonSubTypes.Type(value = Square.class, name = "Square")
    })
    static class Shape {
        public String kind;
    }

    static class Circle extends Shape {
        public double radius;
    }

    static class Square extends Shape {
        public double side;
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type", visible = true)
    @JsonSubTypes({
        @JsonSubTypes.Type(name = "Dog", value = Dog.class),
        @JsonSubTypes.Type(name = "Cat", value = Cat.class)
    })
    static class Animal {
        public String type;
    }

    static class Dog extends Animal {
        public String breed;
    }

    static class Cat extends Animal {
        public String color;
    }

    @io.swagger.v3.oas.annotations.media.Schema(
        subTypes = {Car.class, Truck.class}
    )
    static class Vehicle {
    }

    static class Car extends Vehicle {
    }

    static class Truck extends Vehicle {
    }

    @io.swagger.v3.oas.annotations.media.Schema(oneOf = {Child1.class, Child2.class})
    static class OneOfParent {
    }

    static class Child1 {
        public String a;
    }

    static class Child2 {
        public String b;
    }

    static class Child3 {
        public String c;
    }
}