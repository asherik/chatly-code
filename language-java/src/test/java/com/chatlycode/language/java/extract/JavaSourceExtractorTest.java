package com.chatlycode.language.java.extract;

import com.chatlycode.language.spi.SourceFile;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaSourceExtractorTest {

    private final JavaSourceExtractor extractor = new JavaSourceExtractor();

    @Test
    void extractsJavaTypeAndImports() {
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

        assertEquals(1, result.nodes().size());
        assertEquals("DemoService", result.nodes().getFirst().name());
        assertTrue(result.edges().stream().anyMatch(edge -> edge.toName().equals("java.util.List")));
        assertTrue(result.edges().stream().anyMatch(edge -> edge.toName().equals("spring.stereotype")));
    }
}
