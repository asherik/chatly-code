package com.chatlycode.desktop.graph;

import com.chatlycode.graph.domain.NodeKind;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.text.TextAlignment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class GraphCanvas extends Pane {

    private static final double MIN_ZOOM = 0.22;
    private static final double MAX_ZOOM = 4.0;

    private final Group world = new Group();
    private final Group edgesLayer = new Group();
    private final Group nodesLayer = new Group();
    private final Group labelsLayer = new Group();
    private final Map<String, Point2D> positions = new LinkedHashMap<>();
    private final Map<String, Group> nodeViews = new HashMap<>();
    private final Map<String, Label> labels = new HashMap<>();
    private final List<EdgeView> edgeViews = new ArrayList<>();
    private final ObjectProperty<GraphVertex> selectedVertex = new SimpleObjectProperty<>();
    private GraphProjection projection = GraphProjection.empty();
    private double zoom = 1.0;
    private boolean fitPending;
    private double panStartX;
    private double panStartY;
    private double translateStartX;
    private double translateStartY;

    public GraphCanvas() {
        getStyleClass().add("graph-canvas");
        world.getChildren().addAll(edgesLayer, nodesLayer, labelsLayer);
        getChildren().add(world);
        world.setCache(true);
        setupZoom();
        setupPan();
        widthProperty().addListener((obs, old, value) -> centerIfEmpty());
        heightProperty().addListener((obs, old, value) -> centerIfEmpty());
    }

    public void setProjection(GraphProjection projection) {
        this.projection = projection == null ? GraphProjection.empty() : projection;
        selectedVertex.set(null);
        positions.clear();
        nodeViews.clear();
        labels.clear();
        edgeViews.clear();
        edgesLayer.getChildren().clear();
        nodesLayer.getChildren().clear();
        labelsLayer.getChildren().clear();
        layoutProjection();
        drawEdges();
        drawNodes();
        updateLabelVisibility();
        fitToViewWhenReady();
    }

    public ObjectProperty<GraphVertex> selectedVertexProperty() {
        return selectedVertex;
    }

    public void resetView() {
        fitToViewWhenReady();
    }

    private void setupZoom() {
        setOnScroll(event -> {
            if (projection.vertices().isEmpty()) {
                return;
            }
            double oldZoom = zoom;
            double factor = event.getDeltaY() > 0 ? 1.09 : 0.92;
            zoom = clamp(zoom * factor, MIN_ZOOM, MAX_ZOOM);
            double ratio = zoom / oldZoom;
            double pivotX = event.getX();
            double pivotY = event.getY();
            world.setTranslateX(pivotX - (pivotX - world.getTranslateX()) * ratio);
            world.setTranslateY(pivotY - (pivotY - world.getTranslateY()) * ratio);
            world.setScaleX(zoom);
            world.setScaleY(zoom);
            updateLabelVisibility();
            event.consume();
        });
    }

    private void setupPan() {
        setOnMousePressed(event -> {
            if (event.getButton() != MouseButton.PRIMARY || event.getTarget() != this) {
                return;
            }
            panStartX = event.getSceneX();
            panStartY = event.getSceneY();
            translateStartX = world.getTranslateX();
            translateStartY = world.getTranslateY();
        });
        setOnMouseDragged(event -> {
            if (event.getButton() != MouseButton.PRIMARY || event.getTarget() != this) {
                return;
            }
            world.setTranslateX(translateStartX + event.getSceneX() - panStartX);
            world.setTranslateY(translateStartY + event.getSceneY() - panStartY);
            event.consume();
        });
    }

    private void layoutProjection() {
        int count = projection.vertices().size();
        if (count == 0) {
            return;
        }
        double width = Math.max(getWidth(), 1100);
        double height = Math.max(getHeight(), 720);
        double centerX = width / 2.0;
        double centerY = height / 2.0;
        Map<NodeKind, List<GraphVertex>> byKind = new LinkedHashMap<>();
        for (GraphVertex vertex : projection.vertices()) {
            byKind.computeIfAbsent(vertex.kind(), ignored -> new ArrayList<>()).add(vertex);
        }
        int ringIndex = 0;
        int ringCount = Math.max(1, byKind.size());
        for (List<GraphVertex> ring : byKind.values()) {
            double radius = Math.min(width, height) * (0.14 + 0.34 * (ringIndex + 1) / ringCount);
            for (int index = 0; index < ring.size(); index++) {
                double angle = -Math.PI / 2 + Math.PI * 2 * index / Math.max(1, ring.size());
                double jitter = (index % 5) * 10.0;
                positions.put(ring.get(index).id(), new Point2D(
                        centerX + Math.cos(angle) * (radius + jitter),
                        centerY + Math.sin(angle) * (radius + jitter)
                ));
            }
            ringIndex++;
        }
    }

    private void drawEdges() {
        for (GraphLink link : projection.links()) {
            Point2D source = positions.get(link.sourceId());
            Point2D target = positions.get(link.targetId());
            if (source == null || target == null) {
                continue;
            }
            Line line = new Line(source.getX(), source.getY(), target.getX(), target.getY());
            line.getStyleClass().addAll("graph-edge", "graph-edge-" + cssName(link.kind().name()));
            line.setMouseTransparent(true);
            edgeViews.add(new EdgeView(link, line));
            edgesLayer.getChildren().add(line);
        }
    }

    private void drawNodes() {
        for (GraphVertex vertex : projection.vertices()) {
            Point2D point = positions.get(vertex.id());
            if (point == null) {
                continue;
            }
            Circle circle = new Circle(radius(vertex));
            circle.getStyleClass().addAll("graph-node", "graph-node-" + cssName(vertex.kind().name()));
            if (vertex.hasProblems()) {
                circle.getStyleClass().add("graph-node-problem");
            }
            Group nodeGroup = new Group(circle);
            nodeGroup.setLayoutX(point.getX());
            nodeGroup.setLayoutY(point.getY());
            nodeGroup.setUserData(vertex);
            nodeGroup.setOnMouseClicked(event -> {
                select(vertex);
                event.consume();
            });
            nodeGroup.setOnMouseEntered(event -> highlight(vertex.id()));
            nodeGroup.setOnMouseExited(event -> clearHighlight());
            installNodeDrag(nodeGroup, vertex);
            nodeViews.put(vertex.id(), nodeGroup);
            nodesLayer.getChildren().add(nodeGroup);

            Label label = new Label(vertex.label());
            label.getStyleClass().add("graph-label");
            label.setTextAlignment(TextAlignment.CENTER);
            label.setLayoutX(point.getX() + radius(vertex) + 5);
            label.setLayoutY(point.getY() - 10);
            labels.put(vertex.id(), label);
            labelsLayer.getChildren().add(label);
        }
    }

    private void installNodeDrag(Group nodeGroup, GraphVertex vertex) {
        final double[] start = new double[4];
        nodeGroup.setOnMousePressed(event -> {
            if (event.getButton() != MouseButton.PRIMARY) {
                return;
            }
            start[0] = event.getSceneX();
            start[1] = event.getSceneY();
            start[2] = nodeGroup.getLayoutX();
            start[3] = nodeGroup.getLayoutY();
            event.consume();
        });
        nodeGroup.setOnMouseDragged(event -> {
            if (event.getButton() != MouseButton.PRIMARY) {
                return;
            }
            double nextX = start[2] + (event.getSceneX() - start[0]) / zoom;
            double nextY = start[3] + (event.getSceneY() - start[1]) / zoom;
            nodeGroup.setLayoutX(nextX);
            nodeGroup.setLayoutY(nextY);
            positions.put(vertex.id(), new Point2D(nextX, nextY));
            Label label = labels.get(vertex.id());
            if (label != null) {
                label.setLayoutX(nextX + radius(vertex) + 5);
                label.setLayoutY(nextY - 10);
            }
            updateConnectedEdges(vertex.id());
            event.consume();
        });
    }

    private void select(GraphVertex vertex) {
        selectedVertex.set(vertex);
        for (Map.Entry<String, Group> entry : nodeViews.entrySet()) {
            Node circle = entry.getValue().getChildren().getFirst();
            circle.getStyleClass().remove("graph-node-selected");
            if (entry.getKey().equals(vertex.id())) {
                circle.getStyleClass().add("graph-node-selected");
                entry.getValue().toFront();
            }
        }
        highlight(vertex.id());
    }

    private void highlight(String nodeId) {
        for (Group node : nodeViews.values()) {
            node.setOpacity(0.28);
            node.setScaleX(1.0);
            node.setScaleY(1.0);
        }
        for (EdgeView edgeView : edgeViews) {
            boolean connected = edgeView.link.sourceId().equals(nodeId) || edgeView.link.targetId().equals(nodeId);
            edgeView.line.setOpacity(connected ? 0.9 : 0.12);
            edgeView.line.setStrokeWidth(connected ? 2.2 : 1.0);
            if (connected) {
                Group source = nodeViews.get(edgeView.link.sourceId());
                Group target = nodeViews.get(edgeView.link.targetId());
                if (source != null) {
                    source.setOpacity(1.0);
                }
                if (target != null) {
                    target.setOpacity(1.0);
                }
            }
        }
        Group selected = nodeViews.get(nodeId);
        if (selected != null) {
            selected.setOpacity(1.0);
            selected.setScaleX(1.16);
            selected.setScaleY(1.16);
        }
    }

    private void clearHighlight() {
        GraphVertex selected = selectedVertex.get();
        if (selected != null) {
            highlight(selected.id());
            return;
        }
        for (Group node : nodeViews.values()) {
            node.setOpacity(1.0);
            node.setScaleX(1.0);
            node.setScaleY(1.0);
        }
        for (EdgeView edgeView : edgeViews) {
            edgeView.line.setOpacity(0.42);
            edgeView.line.setStrokeWidth(1.0);
        }
    }

    private void updateConnectedEdges(String nodeId) {
        for (EdgeView edgeView : edgeViews) {
            if (!edgeView.link.sourceId().equals(nodeId) && !edgeView.link.targetId().equals(nodeId)) {
                continue;
            }
            Point2D source = positions.get(edgeView.link.sourceId());
            Point2D target = positions.get(edgeView.link.targetId());
            if (source != null && target != null) {
                edgeView.line.setStartX(source.getX());
                edgeView.line.setStartY(source.getY());
                edgeView.line.setEndX(target.getX());
                edgeView.line.setEndY(target.getY());
            }
        }
    }

    private void updateLabelVisibility() {
        boolean visible = zoom >= 0.55 || projection.vertices().size() <= 120;
        for (Label label : labels.values()) {
            label.setVisible(visible);
            label.setManaged(visible);
        }
    }

    private void centerIfEmpty() {
        if (fitPending && !projection.vertices().isEmpty()) {
            fitToViewWhenReady();
            return;
        }
        if (projection.vertices().isEmpty()) {
            centerWorld();
        }
    }

    private void fitToViewWhenReady() {
        if (getWidth() <= 0 || getHeight() <= 0) {
            fitPending = true;
            Platform.runLater(this::fitToViewWhenReady);
            return;
        }
        fitPending = false;
        if (projection.vertices().isEmpty()) {
            centerWorld();
            return;
        }
        Bounds2D bounds = graphBounds();
        double availableWidth = Math.max(320, getWidth() - 96);
        double availableHeight = Math.max(240, getHeight() - 96);
        double scaleX = availableWidth / Math.max(1, bounds.width());
        double scaleY = availableHeight / Math.max(1, bounds.height());
        zoom = clamp(Math.min(scaleX, scaleY), MIN_ZOOM, 0.72);
        world.setScaleX(zoom);
        world.setScaleY(zoom);
        world.setTranslateX((getWidth() - bounds.width() * zoom) / 2.0 - bounds.minX() * zoom);
        world.setTranslateY((getHeight() - bounds.height() * zoom) / 2.0 - bounds.minY() * zoom);
        updateLabelVisibility();
    }

    private Bounds2D graphBounds() {
        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE;
        double maxY = -Double.MAX_VALUE;
        for (Point2D point : positions.values()) {
            minX = Math.min(minX, point.getX());
            minY = Math.min(minY, point.getY());
            maxX = Math.max(maxX, point.getX());
            maxY = Math.max(maxY, point.getY());
        }
        if (positions.isEmpty()) {
            return new Bounds2D(0, 0, 1, 1);
        }
        return new Bounds2D(minX - 80, minY - 80, maxX + 220, maxY + 80);
    }

    private void centerWorld() {
        world.setTranslateX(Math.max(0, getWidth() * 0.04));
        world.setTranslateY(Math.max(0, getHeight() * 0.04));
    }

    private double radius(GraphVertex vertex) {
        return switch (vertex.kind()) {
            case FILE -> 9;
            case PACKAGE, MODULE, NAMESPACE -> 13;
            case CLASS, INTERFACE, RECORD, ENUM -> 11;
            case METHOD, FUNCTION, CONSTRUCTOR -> 7;
            case COMPONENT, ROUTE -> 10;
            case EXTERNAL_SERVICE -> 12;
            default -> 8;
        };
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private String cssName(String value) {
        return value.toLowerCase().replace('_', '-');
    }

    private record EdgeView(GraphLink link, Line line) {
    }

    private record Bounds2D(double minX, double minY, double maxX, double maxY) {

        double width() {
            return maxX - minX;
        }

        double height() {
            return maxY - minY;
        }
    }
}
