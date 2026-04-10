package uml4java;

import uml4java.analysis.ClassDiscoverer;
import uml4java.analysis.RelationshipDetector;
import uml4java.model.ClassInfo;
import uml4java.output.UmlGenerator;
import uml4java.parser.JavaParser;

import java.io.File;
import java.util.*;

public class Uml4Java {
    public static void main(String[] args) {
        if (args.length < 6) {
            System.err.println("Usage: java uml4java.Uml4Java --type class --input <java_file_or_dir> [<file2> ...] --output <md_file>");
            System.exit(1);
        }

        String outputFile = null;
        int inputStart = -1;
        for (int i = 0; i < args.length; i++) {
            if ("--output".equals(args[i]) && i + 1 < args.length) {
                outputFile = args[++i];
            } else if ("--input".equals(args[i])) {
                inputStart = i + 1;
            }
        }

        if (inputStart < 0 || outputFile == null) {
            System.err.println("Error: Missing required arguments");
            System.exit(1);
        }

        int inputEnd = args.length;
        for (int i = inputStart; i < args.length; i++) {
            if (args[i].startsWith("--")) { inputEnd = i; break; }
        }

        JavaParser parser = new JavaParser();
        Set<String> searchDirs = new LinkedHashSet<>();

        for (int i = inputStart; i < inputEnd; i++) {
            File f = new File(args[i]);
            if (f.isDirectory()) {
                parser.scanDirectory(f.getPath());
            } else if (f.getName().endsWith(".java")) {
                ClassInfo cls = parser.parseJavaFile(f.getPath());
                if (!cls.getName().isEmpty()) {
                    parser.addClass(cls);
                    searchDirs.add(f.getAbsoluteFile().getParent() + File.separator);
                }
            }
        }

        ClassDiscoverer discoverer = new ClassDiscoverer(
                parser.getClasses(), parser.getUnresolved(), parser, parser.getResolver());
        for (String dir : searchDirs) {
            discoverer.discover(dir);
        }

        RelationshipDetector detector = new RelationshipDetector(parser.getResolver());
        detector.detect(parser.getClasses());

        UmlGenerator.generate(parser.getClasses(), detector.getRels(), outputFile);

        System.out.printf("UML diagram generated: %s (%d classes, %d relationships)%n",
                outputFile, parser.getClasses().size(), detector.getRels().size());
    }
}
