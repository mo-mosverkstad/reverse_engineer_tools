package uml4java.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ClassInfoTest {

    @Test
    void defaultValues() {
        ClassInfo cls = new ClassInfo();
        assertEquals("", cls.getName());
        assertEquals("", cls.getPackageName());
        assertEquals("", cls.getExtendsName());
        assertTrue(cls.getMembers().isEmpty());
        assertTrue(cls.getImplementsNames().isEmpty());
        assertTrue(cls.getBodyRefs().isEmpty());
    }

    @Test
    void settersAndAdders() {
        ClassInfo cls = new ClassInfo();
        cls.setName("Foo");
        cls.setPackageName("com.example");
        cls.setExtendsName("Bar");
        cls.addImplements("Baz");
        cls.addBodyRef("Qux");
        cls.addMember(new FieldMember('-', "String", "x", false));

        assertEquals("Foo", cls.getName());
        assertEquals("com.example", cls.getPackageName());
        assertEquals("Bar", cls.getExtendsName());
        assertEquals(1, cls.getImplementsNames().size());
        assertEquals(1, cls.getBodyRefs().size());
        assertEquals(1, cls.getMembers().size());
    }

    @Test
    void toMermaidFieldsBeforeMethods() {
        ClassInfo cls = new ClassInfo();
        cls.setName("MyClass");
        cls.addMember(new MethodMember('+', "void", "doIt", "", false));
        cls.addMember(new FieldMember('-', "int", "x", false));

        String mermaid = cls.toMermaid("    ");
        int fieldPos = mermaid.indexOf("-int x");
        int methodPos = mermaid.indexOf("+doIt()");
        assertTrue(fieldPos < methodPos, "Fields should appear before methods");
    }

    @Test
    void toMermaidIndent() {
        ClassInfo cls = new ClassInfo();
        cls.setName("A");
        String mermaid = cls.toMermaid("        ");
        assertTrue(mermaid.startsWith("        class A {"));
    }
}
