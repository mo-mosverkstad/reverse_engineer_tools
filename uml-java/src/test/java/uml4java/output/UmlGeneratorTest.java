package uml4java.output;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import uml4java.model.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class UmlGeneratorTest {

    @TempDir
    Path tempDir;

    private String generate(List<ClassInfo> classes, List<Relationship> rels) throws IOException {
        Path output = tempDir.resolve("test.md");
        UmlGenerator.generate(classes, rels, output.toString());
        return Files.readString(output);
    }

    @Test
    void emptyDiagram() throws IOException {
        String content = generate(Collections.emptyList(), Collections.emptyList());
        assertTrue(content.contains("classDiagram"));
        assertTrue(content.contains("```mermaid"));
    }

    @Test
    void classWithNamespace() throws IOException {
        ClassInfo cls = new ClassInfo();
        cls.setName("Foo");
        cls.setPackageName("com.example");

        String content = generate(List.of(cls), Collections.emptyList());
        assertTrue(content.contains("namespace com.example {"));
        assertTrue(content.contains("class Foo {"));
    }

    @Test
    void classWithoutPackageNoNamespace() throws IOException {
        ClassInfo cls = new ClassInfo();
        cls.setName("Bar");

        String content = generate(List.of(cls), Collections.emptyList());
        assertFalse(content.contains("namespace"));
        assertTrue(content.contains("class Bar {"));
    }

    @Test
    void multiplePackagesGrouped() throws IOException {
        ClassInfo a = new ClassInfo();
        a.setName("A");
        a.setPackageName("pkg1");

        ClassInfo b = new ClassInfo();
        b.setName("B");
        b.setPackageName("pkg2");

        ClassInfo c = new ClassInfo();
        c.setName("C");
        c.setPackageName("pkg1");

        String content = generate(Arrays.asList(a, b, c), Collections.emptyList());
        assertTrue(content.contains("namespace pkg1 {"));
        assertTrue(content.contains("namespace pkg2 {"));
        // A and C should both be inside pkg1, B inside pkg2
        int pkg1Start = content.indexOf("namespace pkg1 {");
        int pkg2Start = content.indexOf("namespace pkg2 {");
        // pkg1 block ends where pkg2 block starts
        String pkg1Block = content.substring(pkg1Start, pkg2Start);
        assertTrue(pkg1Block.contains("class A {"));
        assertTrue(pkg1Block.contains("class C {"));
        assertFalse(pkg1Block.contains("class B {"));
    }

    @Test
    void relationshipsRendered() throws IOException {
        ClassInfo a = new ClassInfo();
        a.setName("A");
        ClassInfo b = new ClassInfo();
        b.setName("B");

        Relationship r = new Relationship("A", "B", Relationship.Type.INHERITANCE, null);
        String content = generate(Arrays.asList(a, b), List.of(r));
        assertTrue(content.contains("A --|> B"));
    }

    @Test
    void membersRenderedInsideClass() throws IOException {
        ClassInfo cls = new ClassInfo();
        cls.setName("Foo");
        cls.addMember(new FieldMember('-', "int", "x", false));
        cls.addMember(new MethodMember('+', "int", "getX", "", false));

        String content = generate(List.of(cls), Collections.emptyList());
        assertTrue(content.contains("-int x"));
        assertTrue(content.contains("+getX() int"));
    }
}
