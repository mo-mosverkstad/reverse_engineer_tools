package uml4java.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MemberTest {

    @Test
    void fieldMemberProperties() {
        FieldMember f = new FieldMember('-', "String", "name", false);
        assertEquals('-', f.getVisibility());
        assertEquals("String", f.getType());
        assertEquals("name", f.getName());
        assertFalse(f.isStatic());
        assertFalse(f.isMethod());
    }

    @Test
    void fieldMemberToMermaid() {
        FieldMember f = new FieldMember('+', "int", "count", true);
        assertEquals("+int count", f.toMermaid());
    }

    @Test
    void methodMemberWithReturnType() {
        MethodMember m = new MethodMember('+', "String", "getName", "", false);
        assertTrue(m.isMethod());
        assertEquals("", m.getParams());
        assertEquals("+getName() String", m.toMermaid());
    }

    @Test
    void methodMemberWithParams() {
        MethodMember m = new MethodMember('+', "void", "setName", "String name", false);
        assertEquals("+setName(String name) void", m.toMermaid());
    }

    @Test
    void methodMemberStatic() {
        MethodMember m = new MethodMember('+', "int", "getCount", "", true);
        assertEquals("+getCount()$ int", m.toMermaid());
    }

    @Test
    void methodMemberConstructorNoReturnType() {
        MethodMember m = new MethodMember('+', "", "Animal", "String name, int age", false);
        assertEquals("+Animal(String name, int age)", m.toMermaid());
    }
}
