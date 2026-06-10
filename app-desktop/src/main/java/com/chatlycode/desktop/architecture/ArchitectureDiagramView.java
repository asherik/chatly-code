package com.chatlycode.desktop.architecture;

import com.chatlycode.architecture.domain.ArchitectureContainer;
import com.chatlycode.architecture.domain.ArchitectureRelationship;
import com.chatlycode.architecture.domain.ArchitectureSummary;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ArchitectureDiagramView extends ScrollPane {

    private static final double CARD_WIDTH = 230;
    private static final double CARD_HEIGHT = 96;
    private static final double GAP_X = 48;
    private static final double GAP_Y = 34;
    private static final double SYSTEM_X = 250;
    private static final double SYSTEM_Y = 46;

    private final Pane canvas = new Pane();

    public ArchitectureDiagramView() {
        getStyleClass().add("architecture-scroll");
        setFitToWidth(false);
        setFitToHeight(false);
        setContent(canvas);
        setArchitecture(null);
    }

    public void setArchitecture(ArchitectureSummary architecture) {
        canvas.getChildren().clear();
        if (architecture == null || architecture.containers().isEmpty()) {
            Label empty = new Label("Open a project to see the C4 architecture diagram");
            empty.getStyleClass().add("architecture-empty");
            empty.relocate(24, 24);
            canvas.getChildren().add(empty);
            canvas.setPrefSize(720, 360);
            return;
        }

        int columns = Math.min(3, Math.max(1, architecture.containers().size()));
        int rows = (int) Math.ceil(architecture.containers().size() / (double) columns);
        double systemWidth = columns * CARD_WIDTH + (columns + 1) * GAP_X;
        double systemHeight = rows * CARD_HEIGHT + (rows + 1) * GAP_Y + 48;
        double canvasWidth = SYSTEM_X + systemWidth + 80;
        double canvasHeight = Math.max(420, SYSTEM_Y + systemHeight + 60);

        StackPane system = new StackPane();
        system.getStyleClass().add("architecture-system");
        system.resizeRelocate(SYSTEM_X, SYSTEM_Y, systemWidth, systemHeight);

        Label systemTitle = new Label("Software system");
        systemTitle.getStyleClass().add("architecture-system-title");
        systemTitle.relocate(SYSTEM_X + 18, SYSTEM_Y + 14);

        StackPane user = architectureNode("Developer", "Maintains and evolves", "", "Person");
        user.getStyleClass().add("architecture-person");
        user.resizeRelocate(34, SYSTEM_Y + systemHeight / 2 - CARD_HEIGHT / 2, 170, CARD_HEIGHT);

        canvas.getChildren().addAll(system, systemTitle, user);

        Map<String, StackPane> nodesById = new LinkedHashMap<>();
        for (int index = 0; index < architecture.containers().size(); index++) {
            ArchitectureContainer container = architecture.containers().get(index);
            int column = index % columns;
            int row = index / columns;
            double x = SYSTEM_X + GAP_X + column * (CARD_WIDTH + GAP_X);
            double y = SYSTEM_Y + GAP_Y + 48 + row * (CARD_HEIGHT + GAP_Y);
            StackPane node = architectureNode(container.name(), container.description(), container.technology(), container.tag());
            node.resizeRelocate(x, y, CARD_WIDTH, CARD_HEIGHT);
            nodesById.put(container.id(), node);
        }

        drawRelationship(centerRight(user), new Point2D(SYSTEM_X, SYSTEM_Y + systemHeight / 2), "Inspects");
        for (ArchitectureRelationship relationship : architecture.relationships()) {
            StackPane source = nodesById.get(relationship.sourceId());
            StackPane target = nodesById.get(relationship.targetId());
            if (source != null && target != null) {
                drawRelationship(center(source), center(target), relationship.description());
            }
        }

        canvas.getChildren().addAll(nodesById.values());
        canvas.setPrefSize(canvasWidth, canvasHeight);
    }

    private StackPane architectureNode(String title, String description, String technology, String tag) {
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("architecture-node-title");
        titleLabel.setWrapText(true);
        titleLabel.setMaxWidth(CARD_WIDTH - 24);

        Label descriptionLabel = new Label(description);
        descriptionLabel.getStyleClass().add("architecture-node-description");
        descriptionLabel.setWrapText(true);
        descriptionLabel.setMaxWidth(CARD_WIDTH - 24);

        Label technologyLabel = new Label(technology);
        technologyLabel.getStyleClass().add("architecture-node-technology");
        technologyLabel.setWrapText(true);
        technologyLabel.setMaxWidth(CARD_WIDTH - 24);

        VBox labels = new VBox(4, titleLabel, descriptionLabel, technologyLabel);
        labels.setPadding(new Insets(10, 12, 10, 12));

        StackPane node = new StackPane(labels);
        node.getStyleClass().add("architecture-node");
        if (!tag.isBlank()) {
            node.getStyleClass().add("architecture-node-" + tag.toLowerCase());
        }
        return node;
    }

    private void drawRelationship(Point2D source, Point2D target, String labelText) {
        Line line = new Line(source.getX(), source.getY(), target.getX(), target.getY());
        line.getStyleClass().add("architecture-link");

        double angle = Math.atan2(target.getY() - source.getY(), target.getX() - source.getX());
        double arrowLength = 10;
        double arrowWidth = 5;
        Polygon arrow = new Polygon(
                target.getX(), target.getY(),
                target.getX() - arrowLength * Math.cos(angle) + arrowWidth * Math.sin(angle),
                target.getY() - arrowLength * Math.sin(angle) - arrowWidth * Math.cos(angle),
                target.getX() - arrowLength * Math.cos(angle) - arrowWidth * Math.sin(angle),
                target.getY() - arrowLength * Math.sin(angle) + arrowWidth * Math.cos(angle)
        );
        arrow.getStyleClass().add("architecture-arrow");

        Label label = new Label(labelText);
        label.getStyleClass().add("architecture-link-label");
        label.relocate((source.getX() + target.getX()) / 2 - 34, (source.getY() + target.getY()) / 2 - 16);
        canvas.getChildren().addAll(line, arrow, label);
    }

    private Point2D center(StackPane node) {
        return new Point2D(node.getLayoutX() + node.getWidth() / 2, node.getLayoutY() + node.getHeight() / 2);
    }

    private Point2D centerRight(StackPane node) {
        return new Point2D(node.getLayoutX() + node.getWidth(), node.getLayoutY() + node.getHeight() / 2);
    }
}
