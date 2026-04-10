package uml4java.parser;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import uml4java.model.ClassInfo;
import uml4java.model.Member;
import uml4java.model.MethodMember;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class JavaParserTest {
    private JavaParser parser;

    private static String getResourcePath(String name) {
        return new File("src/test/resources/" + name).getAbsolutePath();
    }

    @BeforeEach
    void setUp() {
        parser = new JavaParser();
    }

    // --- Basic class parsing ---

    @Test
    void parseBasicClass() {
        ClassInfo cls = parser.parseJavaFile(getResourcePath("Animal.java"));
        assertEquals("Animal", cls.getName());
        assertEquals("sample", cls.getPackageName());
        assertEquals("", cls.getExtendsName());
        assertTrue(cls.getImplementsNames().isEmpty());
    }

    @Test
    void parseFields() {
        ClassInfo cls = parser.parseJavaFile(getResourcePath("Animal.java"));
        List<Member> fields = cls.getMembers().stream()
                .filter(m -> !m.isMethod()).collect(Collectors.toList());
        assertEquals(2, fields.size());

        Member nameField = fields.stream().filter(m -> m.getName().equals("name")).findFirst().orElse(null);
        assertNotNull(nameField);
        assertEquals('-', nameField.getVisibility());
        assertEquals("String", nameField.getType());

        Member ageField = fields.stream().filter(m -> m.getName().equals("age")).findFirst().orElse(null);
        assertNotNull(ageField);
        assertEquals('#', ageField.getVisibility());
        assertEquals("int", ageField.getType());
    }

    @Test
    void parseMethods() {
        ClassInfo cls = parser.parseJavaFile(getResourcePath("Animal.java"));
        List<Member> methods = cls.getMembers().stream()
                .filter(Member::isMethod).collect(Collectors.toList());

        assertTrue(methods.size() >= 3, "Should have constructor + getName + setName");

        Member constructor = methods.stream().filter(m -> m.getName().equals("Animal")).findFirst().orElse(null);
        assertNotNull(constructor);
        assertEquals("", constructor.getType());

        Member getter = methods.stream().filter(m -> m.getName().equals("getName")).findFirst().orElse(null);
        assertNotNull(getter);
        assertEquals("String", getter.getType());
    }

    // --- Inheritance and interfaces ---

    @Test
    void parseExtends() {
        ClassInfo cls = parser.parseJavaFile(getResourcePath("Dog.java"));
        assertEquals("Dog", cls.getName());
        assertEquals("Animal", cls.getExtendsName());
    }

    @Test
    void parseImplements() {
        ClassInfo cls = parser.parseJavaFile(getResourcePath("Cat.java"));
        assertEquals("Cat", cls.getName());
        assertEquals("Animal", cls.getExtendsName());
        assertTrue(cls.getImplementsNames().contains("Runnable"));
        assertTrue(cls.getImplementsNames().contains("Comparable<Cat>"));
    }

    // --- Static members ---

    @Test
    void parseStaticField() {
        ClassInfo cls = parser.parseJavaFile(getResourcePath("Cat.java"));
        Member countField = cls.getMembers().stream()
                .filter(m -> !m.isMethod() && m.getName().equals("count")).findFirst().orElse(null);
        assertNotNull(countField);
        assertTrue(countField.isStatic());
    }

    @Test
    void parseStaticMethod() {
        ClassInfo cls = parser.parseJavaFile(getResourcePath("Cat.java"));
        Member getCount = cls.getMembers().stream()
                .filter(m -> m.isMethod() && m.getName().equals("getCount")).findFirst().orElse(null);
        assertNotNull(getCount);
        assertTrue(getCount.isStatic());
    }

    // --- Javadoc / comment handling ---

    @Test
    void javadocWithClassKeywordDoesNotConfuseParser() {
        ClassInfo cls = parser.parseJavaFile(getResourcePath("Owner.java"));
        assertEquals("Owner", cls.getName());
    }

    @Test
    void blockCommentWithClassKeywordSkipped() {
        ClassInfo cls = parser.parseJavaFile(getResourcePath("Owner.java"));
        assertEquals("Owner", cls.getName());
        // Should still parse fields correctly after block comment
        Member nameField = cls.getMembers().stream()
                .filter(m -> !m.isMethod() && m.getName().equals("name")).findFirst().orElse(null);
        assertNotNull(nameField);
    }

    // --- Body references ---

    @Test
    void bodyRefsDetected() {
        ClassInfo cls = parser.parseJavaFile(getResourcePath("Owner.java"));
        assertTrue(cls.getBodyRefs().contains("Dog"), "Should detect 'new Dog(...)' in method body");
    }

    // --- Package declaration ---

    @Test
    void packageParsed() {
        ClassInfo cls = parser.parseJavaFile(getResourcePath("Animal.java"));
        assertEquals("sample", cls.getPackageName());
    }

    @Test
    void noPackageDeclaration() {
        ClassInfo cls = parser.parseJavaFile(getResourcePath("NoPackage.java"));
        assertEquals("NoPackage", cls.getName());
        assertEquals("", cls.getPackageName());
    }

    // --- Unresolved types ---

    @Test
    void unresolvedTypesTracked() {
        parser.parseJavaFile(getResourcePath("Dog.java"));
        assertTrue(parser.getUnresolved().contains("Animal"));
    }

    // --- addClass and scanDirectory ---

    @Test
    void addClassIgnoresEmpty() {
        ClassInfo empty = new ClassInfo();
        parser.addClass(empty);
        assertTrue(parser.getClasses().isEmpty());
    }

    @Test
    void addClassRegistersNonEmpty() {
        ClassInfo cls = new ClassInfo();
        cls.setName("Foo");
        parser.addClass(cls);
        assertEquals(1, parser.getClasses().size());
    }

    @Test
    void scanDirectory(@TempDir Path tempDir) throws IOException {
        File javaFile = tempDir.resolve("Hello.java").toFile();
        try (FileWriter fw = new FileWriter(javaFile)) {
            fw.write("package test;\npublic class Hello {\n    public void greet() {}\n}\n");
        }
        // Also create a non-java file that should be ignored
        File txtFile = tempDir.resolve("notes.txt").toFile();
        try (FileWriter fw = new FileWriter(txtFile)) {
            fw.write("not a java file");
        }

        parser.scanDirectory(tempDir.toString());
        assertEquals(1, parser.getClasses().size());
        assertEquals("Hello", parser.getClasses().get(0).getName());
    }

    // --- Edge case: nonexistent file ---

    @Test
    void parseNonexistentFileReturnsEmptyClass() {
        ClassInfo cls = parser.parseJavaFile("/nonexistent/Fake.java");
        assertEquals("", cls.getName());
    }
}
