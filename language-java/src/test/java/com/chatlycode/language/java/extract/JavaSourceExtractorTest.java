package com.chatlycode.language.java.extract;

import com.chatlycode.language.spi.SourceFile;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaSourceExtractorTest {

    private final JavaSourceExtractor extractor = new JavaSourceExtractor();

    @Test
    void extractsJavaTypeImportsAndDecorators() {
        Path root = Path.of("C:/workspace/sample").toAbsolutePath().normalize();
        SourceFile sourceFile = new SourceFile(
                root,
                root.resolve("src/main/java/com/example/DemoService.java"),
                """
                        package com.example;

                        import java.util.List;

                        @Service
                        public class DemoService {
                        }
                        """
        );

        var result = extractor.extract(sourceFile);

        assertTrue(result.nodes().stream().anyMatch(node -> node.kind().equals("class") && node.name().equals("DemoService")));
        assertTrue(result.nodes().stream().anyMatch(node -> node.kind().equals("import") && node.name().equals("java.util.List")));
        assertTrue(result.nodes().stream()
                .filter(node -> node.name().equals("DemoService"))
                .anyMatch(node -> node.decorators().contains("Service")));
        assertTrue(result.edges().stream().anyMatch(edge -> edge.kind().equals("imports") && edge.targetId().equals("java.util.List")));
    }
}
