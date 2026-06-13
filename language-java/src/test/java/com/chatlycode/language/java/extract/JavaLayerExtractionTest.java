package com.chatlycode.language.java.extract;

import com.chatlycode.language.spi.SourceFile;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaLayerExtractionTest {

    private final JavaSourceExtractor extractor = new JavaSourceExtractor();

    @Test
    void classifiesControllerAndRepositoryDependency() {
        Path root = Path.of("C:/workspace/sample").toAbsolutePath().normalize();
        SourceFile sourceFile = new SourceFile(
                root,
                root.resolve("src/main/java/com/example/OrderController.java"),
                """
                        package com.example;

                        import com.example.repo.OrderRepository;

                        @RestController
                        public class OrderController {
                            private OrderRepository orderRepository;
                        }
                        """
        );

        var result = extractor.extract(sourceFile);

        assertTrue(result.nodes().stream().anyMatch(node -> node.name().equals("OrderController") && node.decorators().contains("RestController")));
        assertTrue(result.edges().stream().anyMatch(edge -> edge.kind().equals("type_of") && edge.targetId().contains("OrderRepository")));
        assertTrue(result.unresolvedReferences().stream().anyMatch(ref -> ref.referenceName().contains("OrderRepository")));
    }
}
