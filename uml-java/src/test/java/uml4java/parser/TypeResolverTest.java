package uml4java.parser;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uml4java.model.ClassInfo;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TypeResolverTest {
    private List<ClassInfo> classes;
    private TypeResolver resolver;

    @BeforeEach
    void setUp() {
        classes = new ArrayList<>();
        resolver = new TypeResolver(classes);
    }

    @Test
    void primitiveTypes() {
        assertTrue(resolver.isPrimitive("int"));
        assertTrue(resolver.isPrimitive("String"));
        assertTrue(resolver.isPrimitive("List"));
        assertTrue(resolver.isPrimitive("HashMap"));
        assertTrue(resolver.isPrimitive("void"));
    }

    @Test
    void nonPrimitiveTypes() {
        assertFalse(resolver.isPrimitive("Animal"));
        assertFalse(resolver.isPrimitive("MyClass"));
        assertFalse(resolver.isPrimitive(""));
    }

    @Test
    void isKnownClassEmpty() {
        assertFalse(resolver.isKnownClass("Foo"));
    }

    @Test
    void isKnownClassAfterAdding() {
        ClassInfo cls = new ClassInfo();
        cls.setName("Foo");
        classes.add(cls);
        assertTrue(resolver.isKnownClass("Foo"));
        assertFalse(resolver.isKnownClass("Bar"));
    }

    @Test
    void stripGenerics() {
        assertEquals("List", resolver.strip("List<String>"));
        assertEquals("Map", resolver.strip("Map<String, Integer>"));
    }

    @Test
    void stripArrayBrackets() {
        assertEquals("int", resolver.strip("int[]"));
        assertEquals("String", resolver.strip("String[][]"));
    }

    @Test
    void stripPlainType() {
        assertEquals("Foo", resolver.strip("Foo"));
    }

    @Test
    void stripGenericAndArray() {
        assertEquals("List", resolver.strip("List<String>[]"));
    }
}
