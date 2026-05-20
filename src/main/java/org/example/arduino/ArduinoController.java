package org.example.arduino;

import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseEvent;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import org.example.arduino.model.BreadboardLayout;
import org.example.arduino.model.Component;
import org.example.arduino.model.Circuit;
import org.example.arduino.model.LED;
import org.example.arduino.model.Battery;
import org.example.arduino.model.Button;
import org.example.arduino.model.Resistor;
import org.example.arduino.model.Wire;
import org.example.arduino.model.WireAnchor;
import org.example.arduino.model.ArduinoUNO;
import org.example.arduino.model.Timer;
import org.example.arduino.service.FirebaseAuthService;
import org.example.arduino.service.FirebaseService;
import org.example.arduino.util.CircuitAnalyzer;
import org.example.arduino.util.CircuitPhysics;
import org.example.arduino.util.PowerRailSimulator;
import org.example.arduino.util.WireSignals;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

public class ArduinoController implements Initializable {
    @FXML
    private javafx.scene.layout.StackPane boardHost;
    @FXML
    private javafx.scene.layout.StackPane boardStack;
    @FXML
    private Canvas breadboardCanvas;
    @FXML
    private Pane componentPane;
    @FXML
    private Pane wirePane;
    @FXML
    private javafx.scene.control.Button btnLED;
    @FXML
    private javafx.scene.control.Button btnButton;
    @FXML
    private javafx.scene.control.Button btnResistor;
    @FXML
    private javafx.scene.control.Button btnArduino;
    @FXML
    private javafx.scene.control.Button btnTimer;
    @FXML
    private javafx.scene.control.Button btnBattery;
    @FXML
    private javafx.scene.control.Button btnConnect;
    @FXML
    private javafx.scene.control.Button btnDelete;
    @FXML
    private javafx.scene.control.Button btnSave;
    @FXML
    private javafx.scene.control.Button btnLoad;
    @FXML
    private javafx.scene.control.Button btnClear;
    @FXML
    private javafx.scene.control.Button btnDeleteCircuit;
    @FXML
    private javafx.scene.control.Button btnSimulate;
    @FXML
    private javafx.scene.control.Button btnSaveLocal;
    @FXML
    private javafx.scene.control.Button btnLoadLocal;
    @FXML
    private javafx.scene.control.Button btnExportPng;
    @FXML
    private javafx.scene.control.Button btnLogin;
    @FXML
    private javafx.scene.control.Button btnRegister;
    @FXML
    private javafx.scene.control.Button btnLogout;
    @FXML
    private TextField loginEmailField;
    @FXML
    private PasswordField loginPasswordField;
    @FXML
    private TextField circuitNameField;
    @FXML
    private ListView<String> circuitList;
    @FXML
    private Label userLabel;
    @FXML
    private Label statusLabel;
    @FXML
    private javafx.scene.layout.HBox authBox;
    @FXML
    private javafx.scene.layout.HBox circuitBox;

    private GraphicsContext gc;
    private Circuit currentCircuit;
    private List<Component> components;
    private List<Wire> wires;
    private String selectedComponentType;
    private Component selectedComponent;
    private Component sourceComponent;
    private WireAnchor sourceAnchor;
    private boolean isConnecting;
    private boolean isDeleting;
    private boolean isSimulating;
    private String currentUserId;
    private String currentIdToken;
    private String currentUserEmail;
    private FirebaseService firebaseService;
    private FirebaseAuthService firebaseAuthService;
    private volatile boolean authInProgress;
    private AnimationTimer simulationTimer;
    private final List<javafx.scene.shape.Shape> previewShapes = new ArrayList<>();
    private final List<Component> selectedComponents = new ArrayList<>();
    private final Map<Component, double[]> dragStartPositions = new IdentityHashMap<>();
    private boolean isDraggingComponents;
    private boolean suppressClickAfterDrag;
    private Component dragLeadComponent;
    private double dragStartSceneX;
    private double dragStartSceneY;
    private double dragStartLeadX;
    private double dragStartLeadY;
    private javafx.scene.shape.Rectangle selectionRect;
    private boolean isSelectingArea;
    private double selectionStartX;
    private double selectionStartY;

    private static final double BOARD_GRID_LEFT = BreadboardLayout.BOARD_GRID_LEFT;
    private static final double BOARD_GRID_TOP = BreadboardLayout.BOARD_GRID_TOP;
    private static final double BOARD_CELL = BreadboardLayout.BOARD_CELL;
    private static final int BOARD_COLS = BreadboardLayout.BOARD_COLS;
    private static final int BOARD_ROWS = BreadboardLayout.BOARD_ROWS;
    private static final double BOARD_HOLE_MIN_X = BreadboardLayout.BOARD_HOLE_MIN_X;
    private static final double BOARD_HOLE_MIN_Y = BreadboardLayout.BOARD_HOLE_MIN_Y;
    private static final double BOARD_HOLE_MAX_X = BreadboardLayout.BOARD_HOLE_MAX_X;
    private static final double BOARD_HOLE_MAX_Y = BreadboardLayout.BOARD_HOLE_MAX_Y;

    private static final double BOARD_VIEW_WIDTH = BreadboardLayout.BOARD_VIEW_WIDTH;
    private static final double BOARD_VIEW_HEIGHT = BreadboardLayout.BOARD_VIEW_HEIGHT;
    /** Минимальный зазор между компонентами (размещение и перемещение). */
    private static final double PLACEMENT_GAP = 10;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        gc = breadboardCanvas.getGraphicsContext2D();
        components = new ArrayList<>();
        wires = new ArrayList<>();
        currentCircuit = new Circuit("Новая схема");
        firebaseService = new FirebaseService();
        firebaseAuthService = new FirebaseAuthService();
        isConnecting = false;
        isDeleting = false;
        isSimulating = false;
        selectedComponent = null;
        sourceComponent = null;
        sourceAnchor = null;
        selectedComponents.clear();
        isDraggingComponents = false;
        suppressClickAfterDrag = false;
        currentUserId = null;
        currentIdToken = null;
        currentUserEmail = null;
        
        breadboardCanvas.setWidth(BOARD_VIEW_WIDTH);
        breadboardCanvas.setHeight(BOARD_VIEW_HEIGHT);
        componentPane.setMinSize(BOARD_VIEW_WIDTH, BOARD_VIEW_HEIGHT);
        componentPane.setMaxSize(BOARD_VIEW_WIDTH, BOARD_VIEW_HEIGHT);
        componentPane.setPrefSize(BOARD_VIEW_WIDTH, BOARD_VIEW_HEIGHT);
        wirePane.setMinSize(BOARD_VIEW_WIDTH, BOARD_VIEW_HEIGHT);
        wirePane.setMaxSize(BOARD_VIEW_WIDTH, BOARD_VIEW_HEIGHT);
        wirePane.setPrefSize(BOARD_VIEW_WIDTH, BOARD_VIEW_HEIGHT);
        if (boardStack != null) {
            boardStack.setMinSize(BOARD_VIEW_WIDTH, BOARD_VIEW_HEIGHT);
            boardStack.setMaxSize(BOARD_VIEW_WIDTH, BOARD_VIEW_HEIGHT);
            boardStack.setPrefSize(BOARD_VIEW_WIDTH, BOARD_VIEW_HEIGHT);
        }
        breadboardCanvas.widthProperty().addListener((obs, oldVal, newVal) -> drawBreadboard());
        breadboardCanvas.heightProperty().addListener((obs, oldVal, newVal) -> drawBreadboard());
        drawBreadboard();
        setupComponentPane();
        wirePane.toFront();
        componentPane.toFront();

        Platform.runLater(() -> {
            if (boardHost == null || boardHost.getScene() == null) {
                return;
            }
            Node root = boardHost.getScene().getRoot();
            if (root instanceof BorderPane borderPane) {
                borderPane.heightProperty().addListener((o, a, b) -> fitBoardToWindow());
                borderPane.widthProperty().addListener((o, a, b) -> fitBoardToWindow());
            }
            fitBoardToWindow();
            Platform.runLater(this::fitBoardToWindow);
        });

        if (circuitList != null && circuitNameField != null) {
            circuitList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null && !newVal.isEmpty()) {
                    circuitNameField.setText(newVal);
                }
            });
        }
        
        updateStatus("Выберите компонент и разместите на макетной плате");
        updateAuthUi();
    }

    /** Подгоняет масштаб макетной платы под доступное место в окне (шины +/− не обрезаются). */
    private void fitBoardToWindow() {
        if (boardHost == null || boardStack == null || boardHost.getScene() == null) {
            return;
        }
        Node root = boardHost.getScene().getRoot();
        if (!(root instanceof BorderPane borderPane)) {
            return;
        }

        double sceneH = borderPane.getHeight();
        double sceneW = borderPane.getWidth();
        if (sceneH <= 0 || sceneW <= 0) {
            return;
        }

        double topH = borderPane.getTop() != null ? borderPane.getTop().getBoundsInParent().getHeight() : 88;
        double leftW = borderPane.getLeft() != null ? borderPane.getLeft().getBoundsInParent().getWidth() : 220;
        double framePad = 20;
        double centerPad = 8;

        double availH = sceneH - topH - centerPad - framePad;
        double availW = sceneW - leftW - centerPad - framePad;
        if (availH <= 0 || availW <= 0) {
            return;
        }

        double scale = Math.min(availH / BOARD_VIEW_HEIGHT, availW / BOARD_VIEW_WIDTH);
        scale = Math.min(scale, 1.0);
        if (scale >= 0.98) {
            scale = 1.0;
        } else {
            scale = Math.max(0.82, scale);
        }

        boardStack.setScaleX(scale);
        boardStack.setScaleY(scale);

        double displayW = BOARD_VIEW_WIDTH * scale;
        double displayH = BOARD_VIEW_HEIGHT * scale;
        boardHost.setMinSize(displayW, displayH);
        boardHost.setPrefSize(displayW, displayH);
        boardHost.setMaxSize(displayW, displayH);
    }

    private boolean isAuthenticated() {
        return currentUserId != null && !currentUserId.isEmpty()
            && currentIdToken != null && !currentIdToken.isEmpty();
    }

    private void updateAuthUi() {
        boolean auth = isAuthenticated();
        if (userLabel != null) {
            if (auth) {
                userLabel.setText("Пользователь: " + (currentUserEmail != null ? currentUserEmail : "вошли"));
            } else {
                userLabel.setText("Пользователь: гость (вход не выполнен)");
            }
        }
        
        // Управление видимостью контейнеров
        if (authBox != null) {
            authBox.setVisible(!auth);
            authBox.setManaged(!auth);
        }
        if (circuitBox != null) {
            circuitBox.setVisible(auth);
            circuitBox.setManaged(auth);
        }
        
        if (loginEmailField != null) {
            loginEmailField.setVisible(!auth);
            loginEmailField.setManaged(!auth);
            if (auth) {
                loginEmailField.clear();
            }
        }
        if (loginPasswordField != null) {
            loginPasswordField.setVisible(!auth);
            loginPasswordField.setManaged(!auth);
            if (auth) {
                loginPasswordField.clear();
            }
        }
        if (btnLogin != null) {
            btnLogin.setVisible(!auth);
            btnLogin.setManaged(!auth);
        }
        if (btnRegister != null) {
            btnRegister.setVisible(!auth);
            btnRegister.setManaged(!auth);
        }
        if (btnLogout != null) {
            btnLogout.setVisible(auth);
            btnLogout.setManaged(auth);
        }
        if (circuitNameField != null) {
            circuitNameField.setVisible(auth);
            circuitNameField.setManaged(auth);
        }
        if (btnSave != null) {
            btnSave.setDisable(!auth);
            btnSave.setVisible(auth);
            btnSave.setManaged(auth);
        }
        if (btnLoad != null) {
            btnLoad.setVisible(auth);
            btnLoad.setManaged(auth);
        }
        if (btnClear != null) {
            btnClear.setVisible(true);
            btnClear.setManaged(true);
        }
        if (btnDeleteCircuit != null) {
            btnDeleteCircuit.setVisible(auth);
            btnDeleteCircuit.setManaged(auth);
        }
    }

    private void drawBreadboard() {
        double width = breadboardCanvas.getWidth();
        double height = breadboardCanvas.getHeight();

        gc.setFill(Color.web("#F5F0E1"));
        gc.fillRect(0, 0, width, height);

        gc.setFill(Color.web("#E8DFC8"));
        gc.fillRoundRect(12, 12, width - 24, height - 24, 14, 14);

        gc.setStroke(Color.web("#B8956A"));
        gc.setLineWidth(2);
        gc.strokeRoundRect(12, 12, width - 24, height - 24, 14, 14);

        double startX = BOARD_GRID_LEFT;
        double startY = BOARD_GRID_TOP;
        double cellSize = BOARD_CELL;
        int cols = BOARD_COLS;
        int rows = BOARD_ROWS;
        double gridW = cols * cellSize;
        double gridH = rows * cellSize;

        gc.setFill(Color.web("#FFFCF5"));
        gc.fillRoundRect(startX - 4, startY - 4, gridW + 8, gridH + 8, 6, 6);

        gc.setStroke(Color.web("#E0D5C0"));
        gc.setLineWidth(0.5);
        for (int i = 0; i <= cols; i++) {
            double x = startX + i * cellSize;
            gc.strokeLine(x, startY, x, startY + gridH);
        }
        for (int j = 0; j <= rows; j++) {
            double y = startY + j * cellSize;
            gc.strokeLine(startX, y, startX + gridW, y);
        }

        for (int i = 0; i < cols; i++) {
            for (int j = 0; j < rows; j++) {
                double x = BOARD_HOLE_MIN_X + i * cellSize;
                double y = BOARD_HOLE_MIN_Y + j * cellSize;
                gc.setFill(Color.web("#C8BBA0"));
                gc.fillOval(x - 3.5, y - 3.5, 7, 7);
                gc.setStroke(Color.web("#A69378"));
                gc.setLineWidth(0.8);
                gc.strokeOval(x - 3.5, y - 3.5, 7, 7);
            }
        }

        double railLabelW = BreadboardLayout.RAIL_LABEL_WIDTH;
        double railLeft = BreadboardLayout.RAIL_HOLES_MIN_X;
        double railWidth = gridW - railLabelW;

        double plusTop = BreadboardLayout.RAIL_PLUS_CENTER_Y - BreadboardLayout.RAIL_HEIGHT / 2;
        double minusTop = BreadboardLayout.RAIL_MINUS_CENTER_Y - BreadboardLayout.RAIL_HEIGHT / 2;

        gc.setFill(Color.web("#C0392B"));
        gc.fillRoundRect(BOARD_GRID_LEFT, plusTop, railLabelW - 4, BreadboardLayout.RAIL_HEIGHT, 6, 6);
        gc.setFill(Color.web("#2980B9"));
        gc.fillRoundRect(BOARD_GRID_LEFT, minusTop, railLabelW - 4, BreadboardLayout.RAIL_HEIGHT, 6, 6);

        gc.setFill(Color.rgb(231, 76, 60, 0.25));
        gc.fillRoundRect(railLeft, plusTop, railWidth, BreadboardLayout.RAIL_HEIGHT, 8, 8);
        gc.setStroke(Color.web("#C0392B"));
        gc.setLineWidth(2.5);
        gc.strokeRoundRect(railLeft, plusTop, railWidth, BreadboardLayout.RAIL_HEIGHT, 8, 8);

        gc.setFill(Color.rgb(52, 152, 219, 0.25));
        gc.fillRoundRect(railLeft, minusTop, railWidth, BreadboardLayout.RAIL_HEIGHT, 8, 8);
        gc.setStroke(Color.web("#2980B9"));
        gc.strokeRoundRect(railLeft, minusTop, railWidth, BreadboardLayout.RAIL_HEIGHT, 8, 8);

        for (int i = 0; i < cols; i++) {
            double hx = BOARD_HOLE_MIN_X + i * cellSize;
            if (hx >= railLeft) {
                gc.setFill(Color.web("#E74C3C"));
                gc.fillOval(hx - 5, BreadboardLayout.RAIL_PLUS_CENTER_Y - 5, 10, 10);
                gc.setFill(Color.web("#3498DB"));
                gc.fillOval(hx - 5, BreadboardLayout.RAIL_MINUS_CENTER_Y - 5, 10, 10);
            }
        }

        gc.setFont(javafx.scene.text.Font.font("Segoe UI", javafx.scene.text.FontWeight.BOLD, 13));
        gc.setFill(Color.WHITE);
        gc.fillText("+5V", BOARD_GRID_LEFT + 8, BreadboardLayout.RAIL_PLUS_CENTER_Y + 5);
        gc.fillText("GND", BOARD_GRID_LEFT + 8, BreadboardLayout.RAIL_MINUS_CENTER_Y + 5);

        gc.setFill(Color.web("#7F8C8D"));
        gc.setFont(javafx.scene.text.Font.font("Segoe UI", 11));
        gc.fillText("Макетная плата — кликайте по отверстиям", 20, 28);
    }

    private void setupComponentPane() {
        hidePreview();
        if (selectionRect != null) {
            selectionRect.setWidth(0);
            selectionRect.setHeight(0);
        }
        componentPane.getChildren().clear();
        wirePane.getChildren().clear();
        
        // Сначала рисуем провода
        for (Wire wire : wires) {
            wire.updateLine();
            javafx.scene.shape.Line line = wire.getLine();
            if (line != null) {
                line.setMouseTransparent(!isDeleting);
                line.setPickOnBounds(true);
                wirePane.getChildren().add(line);
            }
        }
        
        for (Component comp : components) {
            Node shape = comp.getShape();
            if (shape != null) {
                if (shape instanceof javafx.scene.shape.Circle) {
                    javafx.scene.shape.Circle circle = (javafx.scene.shape.Circle) shape;
                    circle.setCenterX(comp.getX());
                    circle.setCenterY(comp.getY());
                } else if (shape instanceof javafx.scene.shape.Rectangle rect && !(comp instanceof Resistor)) {
                    if (comp instanceof Button) {
                        rect.setX(comp.getX() - 28);
                        rect.setY(comp.getY() - 14);
                    } else {
                        rect.setX(comp.getX() - 25);
                        rect.setY(comp.getY() - 12);
                    }
                } else if (comp instanceof Resistor || comp instanceof ArduinoUNO || comp instanceof Timer || comp instanceof Battery) {
                    comp.setX(comp.getX());
                }
                applyComponentEffects(comp);
                shape.setVisible(true);
                shape.setMouseTransparent(false);
                shape.setPickOnBounds(true);
                if (shape instanceof javafx.scene.Group) {
                    shape.setPickOnBounds(false);
                }
                shape.setOnMousePressed(null);
                shape.setOnMouseDragged(null);
                shape.setOnMouseReleased(null);
                shape.setOnMouseClicked(null);
                shape.setOnMousePressed(e -> {
                    e.consume();
                    if (isConnecting || isDeleting || selectedComponentType != null) {
                        return;
                    }
                    prepareSelectionForDrag(comp, e);
                });
                shape.setOnMouseDragged(e -> {
                    e.consume();
                    if (isConnecting || isDeleting || selectedComponentType != null) {
                        return;
                    }
                    dragSelectedComponents(e);
                });
                shape.setOnMouseReleased(e -> {
                    e.consume();
                    isDraggingComponents = false;
                    dragLeadComponent = null;
                    dragStartPositions.clear();
                    hidePreview();
                });
                shape.setOnMouseClicked(e -> {
                    e.consume();
                    if (suppressClickAfterDrag) {
                        suppressClickAfterDrag = false;
                        return;
                    }
                    handleComponentInteraction(comp, e);
                });
                componentPane.getChildren().add(shape);
            }
        }

        if (isConnecting && sourceAnchor != null) {
            javafx.scene.shape.Circle marker = new javafx.scene.shape.Circle(
                sourceAnchor.getX(), sourceAnchor.getY(), 11);
            marker.setFill(Color.rgb(142, 68, 173, 0.25));
            marker.setStroke(Color.web("#8E44AD"));
            marker.setStrokeWidth(2.5);
            marker.getStrokeDashArray().setAll(5.0, 4.0);
            marker.setMouseTransparent(true);
            wirePane.getChildren().add(marker);
        }

        componentPane.setVisible(true);
        componentPane.setMouseTransparent(false);
        componentPane.toFront();
    }

    private void applyComponentEffects(Component comp) {
        if (!(comp instanceof LED)) {
            return;
        }
        Node shapeNode = comp.getShape();
        if (!(shapeNode instanceof javafx.scene.shape.Circle)) {
            return;
        }
        javafx.scene.shape.Circle circle = (javafx.scene.shape.Circle) shapeNode;
        LED led = (LED) comp;
        if (led.isBurned()) {
            circle.setEffect(null);
        } else if (led.isOn()) {
            javafx.scene.effect.DropShadow glow = new javafx.scene.effect.DropShadow(18, Color.web("#F39C12", 0.85));
            glow.setSpread(0.35);
            circle.setEffect(glow);
        } else {
            circle.setEffect(null);
        }
    }

    private double paneX(MouseEvent event) {
        if (event == null || componentPane == null) {
            return 0;
        }
        return componentPane.sceneToLocal(event.getSceneX(), event.getSceneY()).getX();
    }

    private double paneY(MouseEvent event) {
        if (event == null || componentPane == null) {
            return 0;
        }
        return componentPane.sceneToLocal(event.getSceneX(), event.getSceneY()).getY();
    }

    private WireAnchor connectionAnchor(Component component, Component other, MouseEvent event) {
        double px = event != null ? paneX(event) : (other != null ? other.getX() : component.getX());
        double py = event != null ? paneY(event) : component.getY();
        if (component instanceof Resistor resistor) {
            int pin = event != null ? resistor.pinAt(px, py) : 0;
            if (pin == 0) {
                double refX = other != null ? other.getX() : px;
                pin = Math.abs(refX - resistor.getLeg1X()) <= Math.abs(refX - resistor.getLeg2X()) ? 1 : 2;
            }
            return WireAnchor.component(resistor, pin);
        }
        if (component instanceof Battery battery) {
            int pin = event != null ? battery.pinAt(px, py) : 0;
            if (pin == 0 || pin < 0) {
                double refX = other != null ? other.getX() : px;
                pin = refX < battery.getX() ? 1 : 2;
            }
            return WireAnchor.component(battery, pin);
        }
        if (component instanceof ArduinoUNO) {
            int pin;
            if (other != null) {
                pin = other.getX() < component.getX() ? 2 : 1;
            } else {
                pin = px < component.getX() ? 1 : 2;
            }
            return WireAnchor.component(component, pin);
        }
        return WireAnchor.componentCenter(component);
    }

    @FXML
    private void onBreadboardMouseMoved(MouseEvent event) {
        if (selectedComponentType != null && !isConnecting && !isDeleting) {
            double[] bounds = getPlacementBoundsForType(selectedComponentType);
            double[] snapped = snapBoardPoint(event.getX(), event.getY(), bounds);
            boolean valid = canPlaceComponentAt(snapped[0], snapped[1], selectedComponentType);
            showComponentPreview(snapped[0], snapped[1], selectedComponentType, valid);
        } else if (!isDraggingComponents) {
            hidePreview();
        }
    }

    // ОТЧЁТ ПМ02 РИС.3 — НАЧАЛО СКРИНА (рамочное выделение на макетной плате)
    @FXML
    private void onBreadboardMousePressed(MouseEvent event) {
        if (event == null || event.isPrimaryButtonDown() == false) return;
        if (selectedComponentType != null || isConnecting || isDeleting) return;
        Component clicked = findComponentAt(event.getX(), event.getY());
        if (clicked != null) return;
        isSelectingArea = true;
        selectionStartX = event.getX();
        selectionStartY = event.getY();
        if (selectionRect == null) {
            selectionRect = new javafx.scene.shape.Rectangle();
            selectionRect.setFill(javafx.scene.paint.Color.rgb(52, 152, 219, 0.18));
            selectionRect.setStroke(javafx.scene.paint.Color.rgb(41, 128, 185, 0.95));
            selectionRect.getStrokeDashArray().setAll(6.0, 4.0);
            selectionRect.setMouseTransparent(true);
        }
        selectionRect.setX(selectionStartX);
        selectionRect.setY(selectionStartY);
        selectionRect.setWidth(0);
        selectionRect.setHeight(0);
        if (!componentPane.getChildren().contains(selectionRect)) {
            componentPane.getChildren().add(selectionRect);
        }
        event.consume();
    }

    @FXML
    private void onBreadboardMouseDragged(MouseEvent event) {
        if (!isSelectingArea || selectionRect == null) return;
        double currentX = event.getX();
        double currentY = event.getY();
        double minX = Math.min(selectionStartX, currentX);
        double minY = Math.min(selectionStartY, currentY);
        double width = Math.abs(currentX - selectionStartX);
        double height = Math.abs(currentY - selectionStartY);
        selectionRect.setX(minX);
        selectionRect.setY(minY);
        selectionRect.setWidth(width);
        selectionRect.setHeight(height);
        if (width > 2 || height > 2) {
            suppressClickAfterDrag = true;
        }
        event.consume();
    }

    @FXML
    private void onBreadboardMouseReleased(MouseEvent event) {
        if (!isSelectingArea) return;
        isSelectingArea = false;
        if (selectionRect == null) return;

        double width = selectionRect.getWidth();
        double height = selectionRect.getHeight();
        if (width >= 6 && height >= 6) {
            double minX = selectionRect.getX();
            double maxX = minX + width;
            double minY = selectionRect.getY();
            double maxY = minY + height;

            if (event == null || !event.isControlDown()) {
                clearComponentSelection();
            }
            int selectedCountBefore = selectedComponents.size();
            for (Component component : components) {
                double x = component.getX();
                double y = component.getY();
                if (x >= minX && x <= maxX && y >= minY && y <= maxY) {
                    component.setSelected(true);
                }
            }
            refreshSelectionCache();
            setupComponentPane();
            int selectedNow = selectedComponents.size();
            if (selectedNow > 0) {
                if (selectedNow > selectedCountBefore) {
                    updateStatus("Выделено компонентов: " + selectedNow + ". Перетяните любой из них для группового перемещения.");
                } else {
                    updateStatus("Выделено компонентов: " + selectedNow);
                }
            } else {
                updateStatus("В области выделения нет компонентов");
            }
        }
        componentPane.getChildren().remove(selectionRect);
        selectionRect.setWidth(0);
        selectionRect.setHeight(0);
        event.consume();
    }
    // ОТЧЁТ ПМ02 РИС.3 — КОНЕЦ СКРИНА (рамочное выделение)

    @FXML
    private void onBreadboardClick(MouseEvent event) {
        if (suppressClickAfterDrag) {
            suppressClickAfterDrag = false;
            return;
        }
        double x = event.getX();
        double y = event.getY();
        double snappedX = Math.round((x - BOARD_HOLE_MIN_X) / BOARD_CELL) * BOARD_CELL + BOARD_HOLE_MIN_X;
        double snappedY = Math.round((y - BOARD_HOLE_MIN_Y) / BOARD_CELL) * BOARD_CELL + BOARD_HOLE_MIN_Y;
        double[] bounds = selectedComponentType != null ? getPlacementBoundsForType(selectedComponentType) : new double[] { BOARD_HOLE_MIN_X, BOARD_HOLE_MAX_X, BOARD_HOLE_MIN_Y, BOARD_HOLE_MAX_Y };
        snappedX = Math.max(bounds[0], Math.min(bounds[1], snappedX));
        snappedY = Math.max(bounds[2], Math.min(bounds[3], snappedY));
        if (isDeleting) {
            Wire clickedWire = findWireAt(x, y);
            if (clickedWire != null) {
                deleteWire(clickedWire);
                return;
            }
        }
        if (isConnecting) {
            WireAnchor rail = BreadboardLayout.railAt(x, y);
            if (rail != null) {
                handleWireConnection(rail);
                return;
            }
        }

        Component clicked = findComponentAt(x, y);
        if (clicked != null) {
            handleComponentInteraction(clicked, event);
            return;
        }
        if (selectedComponentType == null && !isConnecting && !isDeleting && !event.isControlDown()) {
            clearComponentSelection();
            setupComponentPane();
        }
        
        if (isSimulating && selectedComponentType != null) {
            onSimulateClick();
            updateStatus("Симуляция выключена. Разместите компонент на плату.");
        }
        
        if (selectedComponentType != null) {
            double[] placeBounds = getPlacementBoundsForType(selectedComponentType);
            if (snappedX < placeBounds[0] || snappedX > placeBounds[1] || snappedY < placeBounds[2] || snappedY > placeBounds[3]) {
                updateStatus("Компоненты можно размещать только на макетной плате!");
                hidePreview();
                return;
            }
        }
        if (selectedComponentType != null && !isConnecting && !isDeleting) {
            if (!canPlaceComponentAt(snappedX, snappedY, selectedComponentType)) {
                updateStatus("Слишком близко к другому компоненту. Выберите другое место.");
                showComponentPreview(snappedX, snappedY, selectedComponentType, false);
                return;
            }
        }
        if (selectedComponentType != null && !isConnecting && !isDeleting) {
            final Component newComponent;
            switch (selectedComponentType) {
                case "LED":
                    newComponent = new LED(snappedX, snappedY);
                    break;
                case "Button":
                    newComponent = new Button(snappedX, snappedY);
                    break;
                case "Resistor":
                    newComponent = new Resistor(snappedX, snappedY);
                    break;
                case "ArduinoUNO":
                    newComponent = new ArduinoUNO(snappedX, snappedY);
                    break;
                case "Timer":
                    newComponent = new Timer(snappedX, snappedY);
                    break;
                case "Battery":
                    newComponent = new Battery(snappedX, snappedY);
                    break;
                default:
                    newComponent = null;
                    break;
            }
            
            if (newComponent != null) {
                components.add(newComponent);
                currentCircuit.addComponent(newComponent);
                Node shape = newComponent.getShape();
                if (shape != null) {
                    shape.setVisible(true);
                    shape.setMouseTransparent(false);
                    shape.setPickOnBounds(true);
                    if (shape instanceof javafx.scene.Group) {
                        shape.setPickOnBounds(false);
                    }
                    if (shape instanceof javafx.scene.shape.Circle) {
                        javafx.scene.shape.Circle circle = (javafx.scene.shape.Circle) shape;
                        circle.setCenterX(snappedX);
                        circle.setCenterY(snappedY);
                    } else if (shape instanceof javafx.scene.shape.Rectangle) {
                        javafx.scene.shape.Rectangle rect = (javafx.scene.shape.Rectangle) shape;
                        if (newComponent instanceof ArduinoUNO) {
                            rect.setX(snappedX - 50);
                            rect.setY(snappedY - 25);
                        } else if (newComponent instanceof Timer) {
                            rect.setX(snappedX - 35);
                            rect.setY(snappedY - 15);
                        } else if (newComponent instanceof Resistor) {
                            rect.setX(snappedX - 30);
                            rect.setY(snappedY - 10);
                        } else {
                            rect.setX(snappedX - 25);
                            rect.setY(snappedY - 12);
                        }
                    }
                    final Component finalComp = newComponent;
                    shape.setOnMouseClicked(e -> {
                        e.consume();
                        handleComponentInteraction(finalComp, e);
                    });
                    
                    componentPane.getChildren().add(shape);
                }
                setupComponentPane();
                updateStatus("✓ Компонент " + newComponent.getType() + " добавлен на (" + 
                    String.format("%.0f", snappedX) + ", " + String.format("%.0f", snappedY) + ")! Кликните по нему для взаимодействия.");
                selectedComponentType = null;
                resetButtonStyles();
                hidePreview();
            }
        } else if (selectedComponentType == null) {
            updateStatus("Сначала выберите компонент из панели слева");
        }
    }

    // ОТЧЁТ ПМ02 РИС.3 — НАЧАЛО СКРИНА (кэш выделения, клик/Ctrl, групповое перетаскивание)
    private void clearComponentSelection() {
        for (Component component : components) {
            component.setSelected(false);
        }
        selectedComponents.clear();
        selectedComponent = null;
    }

    private void refreshSelectionCache() {
        selectedComponents.clear();
        for (Component component : components) {
            if (component.isSelected()) {
                selectedComponents.add(component);
            }
        }
        selectedComponent = selectedComponents.isEmpty() ? null : selectedComponents.get(0);
    }

    private void updateSelectionByClick(Component clicked, MouseEvent event) {
        if (clicked == null) return;
        boolean ctrlDown = event != null && event.isControlDown();
        if (ctrlDown) {
            clicked.setSelected(!clicked.isSelected());
        } else {
            boolean nextSelected = !clicked.isSelected();
            clearComponentSelection();
            if (nextSelected) {
                clicked.setSelected(true);
            }
        }
        refreshSelectionCache();
    }

    private void prepareSelectionForDrag(Component clicked, MouseEvent event) {
        if (clicked == null || event == null) {
            return;
        }
        boolean ctrlDown = event.isControlDown();
        if (!clicked.isSelected()) {
            if (!ctrlDown) {
                clearComponentSelection();
            }
            clicked.setSelected(true);
        }
        refreshSelectionCache();
        isDraggingComponents = !selectedComponents.isEmpty();
        dragLeadComponent = clicked;
        dragStartSceneX = event.getSceneX();
        dragStartSceneY = event.getSceneY();
        dragStartLeadX = clicked.getX();
        dragStartLeadY = clicked.getY();
        dragStartPositions.clear();
        for (Component component : selectedComponents) {
            dragStartPositions.put(component, new double[] { component.getX(), component.getY() });
        }
    }

    private void dragSelectedComponents(MouseEvent event) {
        if (!isDraggingComponents || selectedComponents.isEmpty() || event == null || dragLeadComponent == null) {
            return;
        }
        double totalDx = event.getSceneX() - dragStartSceneX;
        double totalDy = event.getSceneY() - dragStartSceneY;
        if (Math.abs(totalDx) < 0.01 && Math.abs(totalDy) < 0.01) {
            return;
        }

        double[] leadBounds = getPlacementBoundsForComponent(dragLeadComponent);
        double[] snappedLead = snapBoardPoint(dragStartLeadX + totalDx, dragStartLeadY + totalDy, leadBounds);
        double dx = snappedLead[0] - dragStartLeadX;
        double dy = snappedLead[1] - dragStartLeadY;

        double minDx = Double.NEGATIVE_INFINITY;
        double maxDx = Double.POSITIVE_INFINITY;
        double minDy = Double.NEGATIVE_INFINITY;
        double maxDy = Double.POSITIVE_INFINITY;
        for (Component component : selectedComponents) {
            double[] start = dragStartPositions.get(component);
            if (start == null) {
                continue;
            }
            double[] bounds = getPlacementBoundsForComponent(component);
            minDx = Math.max(minDx, bounds[0] - start[0]);
            maxDx = Math.min(maxDx, bounds[1] - start[0]);
            minDy = Math.max(minDy, bounds[2] - start[1]);
            maxDy = Math.min(maxDy, bounds[3] - start[1]);
        }
        dx = Math.max(minDx, Math.min(maxDx, dx));
        dy = Math.max(minDy, Math.min(maxDy, dy));

        if (!canMoveSelectionTo(dx, dy)) {
            showMovePreview(dx, dy, false);
            return;
        }

        hidePreview();
        for (Component component : selectedComponents) {
            double[] start = dragStartPositions.get(component);
            if (start == null) {
                continue;
            }
            component.setX(start[0] + dx);
            component.setY(start[1] + dy);
            updateComponentShapePosition(component);
        }
        for (Wire wire : wires) {
            wire.updateLine();
        }
        suppressClickAfterDrag = true;
    }

    private void updateComponentShapePosition(Component comp) {
        Node shape = comp.getShape();
        if (shape == null) {
            return;
        }
        if (shape instanceof javafx.scene.shape.Circle circle) {
            circle.setCenterX(comp.getX());
            circle.setCenterY(comp.getY());
        } else if (shape instanceof javafx.scene.shape.Rectangle rect && !(comp instanceof Resistor)) {
            if (comp instanceof Button) {
                rect.setX(comp.getX() - 28);
                rect.setY(comp.getY() - 14);
            } else {
                rect.setX(comp.getX() - 25);
                rect.setY(comp.getY() - 12);
            }
        } else if (comp instanceof Resistor || comp instanceof ArduinoUNO || comp instanceof Timer || comp instanceof Battery) {
            comp.setX(comp.getX());
        }
    }
    // ОТЧЁТ ПМ02 РИС.3 — КОНЕЦ СКРИНА (кэш выделения, клик/Ctrl, групповое перетаскивание)

    private double[] getPlacementBoundsForComponent(Component component) {
        if (component == null) {
            return new double[] { BOARD_HOLE_MIN_X, BOARD_HOLE_MAX_X, BOARD_HOLE_MIN_Y, BOARD_HOLE_MAX_Y };
        }
        if (component instanceof LED) return getPlacementBoundsForType("LED");
        if (component instanceof Button) return getPlacementBoundsForType("Button");
        if (component instanceof Resistor) return getPlacementBoundsForType("Resistor");
        if (component instanceof ArduinoUNO) return getPlacementBoundsForType("ArduinoUNO");
        if (component instanceof Timer) return getPlacementBoundsForType("Timer");
        if (component instanceof Battery) return getPlacementBoundsForType("Battery");
        return getPlacementBoundsForType(null);
    }

    private boolean canMoveSelectionTo(double dx, double dy) {
        for (Component moving : selectedComponents) {
            double[] start = dragStartPositions.get(moving);
            if (start == null) {
                continue;
            }
            if (!canPlaceComponentAt(start[0] + dx, start[1] + dy, moving, selectedComponents)) {
                return false;
            }
        }
        return true;
    }

    private double[] snapBoardPoint(double x, double y, double[] bounds) {
        double snappedX = Math.round((x - BOARD_HOLE_MIN_X) / BOARD_CELL) * BOARD_CELL + BOARD_HOLE_MIN_X;
        double snappedY = Math.round((y - BOARD_HOLE_MIN_Y) / BOARD_CELL) * BOARD_CELL + BOARD_HOLE_MIN_Y;
        snappedX = Math.max(bounds[0], Math.min(bounds[1], snappedX));
        snappedY = Math.max(bounds[2], Math.min(bounds[3], snappedY));
        return new double[] { snappedX, snappedY };
    }

    private boolean canPlaceComponentAt(double x, double y, String type) {
        return canPlaceComponentAt(x, y, type, List.of());
    }

    private boolean canPlaceComponentAt(double x, double y, Component component, List<Component> ignore) {
        if (component == null) {
            return false;
        }
        double[] bounds = getPlacementBoundsForComponent(component);
        if (x < bounds[0] || x > bounds[1] || y < bounds[2] || y > bounds[3]) {
            return false;
        }
        return !collidesWithOthers(x, y, footprintOf(component), ignore);
    }

    private boolean canPlaceComponentAt(double x, double y, String type, List<Component> ignore) {
        double[] bounds = getPlacementBoundsForType(type);
        if (x < bounds[0] || x > bounds[1] || y < bounds[2] || y > bounds[3]) {
            return false;
        }
        return !collidesWithOthers(x, y, footprintForType(type), ignore);
    }

    private boolean collidesWithOthers(double x, double y, Component.Footprint footprint, List<Component> ignore) {
        for (Component other : components) {
            if (ignore != null && ignore.contains(other)) {
                continue;
            }
            if (footprintsOverlap(x, y, footprint, other.getX(), other.getY(), footprintOf(other), PLACEMENT_GAP)) {
                return true;
            }
        }
        return false;
    }

    private boolean footprintsOverlap(
            double x1, double y1, Component.Footprint first,
            double x2, double y2, Component.Footprint second,
            double gap) {
        return Math.abs(x1 - x2) < first.halfWidth() + second.halfWidth() + gap
            && Math.abs(y1 - y2) < first.halfHeight() + second.halfHeight() + gap;
    }

    private Component.Footprint footprintOf(Component component) {
        return component != null ? component.getFootprint() : new Component.Footprint(25, 12);
    }

    private Component.Footprint footprintForType(String type) {
        if (type == null) {
            return new Component.Footprint(25, 12);
        }
        return switch (type) {
            case "LED" -> new Component.Footprint(18, 18);
            case "Button" -> new Component.Footprint(28, 14);
            case "Resistor" -> new Component.Footprint(34, 10);
            case "ArduinoUNO" -> new Component.Footprint(52, 28);
            case "Timer" -> new Component.Footprint(36, 16);
            case "Battery" -> new Component.Footprint(38, 16);
            default -> new Component.Footprint(25, 12);
        };
    }

    private String componentTypeKey(Component component) {
        if (component instanceof LED) {
            return "LED";
        }
        if (component instanceof Button) {
            return "Button";
        }
        if (component instanceof Resistor) {
            return "Resistor";
        }
        if (component instanceof ArduinoUNO) {
            return "ArduinoUNO";
        }
        if (component instanceof Timer) {
            return "Timer";
        }
        if (component instanceof Battery) {
            return "Battery";
        }
        return null;
    }

    private javafx.scene.shape.Shape createPreviewShape(String type, double x, double y) {
        if (type == null) {
            return null;
        }
        return switch (type) {
            case "LED" -> new javafx.scene.shape.Circle(x, y, 16);
            case "Button" -> {
                javafx.scene.shape.Rectangle rect = new javafx.scene.shape.Rectangle(x - 28, y - 14, 56, 28);
                rect.setArcWidth(14);
                rect.setArcHeight(14);
                yield rect;
            }
            case "Resistor" -> new javafx.scene.shape.Rectangle(x - 34, y - 10, 68, 20);
            case "ArduinoUNO" -> new javafx.scene.shape.Rectangle(x - 52, y - 28, 104, 56);
            case "Timer" -> new javafx.scene.shape.Rectangle(x - 36, y - 16, 72, 32);
            case "Battery" -> new javafx.scene.shape.Rectangle(x - 38, y - 16, 76, 32);
            default -> null;
        };
    }

    private void stylePreviewShape(javafx.scene.shape.Shape shape, boolean valid) {
        if (valid) {
            shape.setFill(Color.rgb(46, 204, 113, 0.35));
            shape.setStroke(Color.web("#27AE60"));
        } else {
            shape.setFill(Color.rgb(231, 76, 60, 0.4));
            shape.setStroke(Color.web("#E74C3C"));
        }
        shape.setStrokeWidth(2.5);
        if (shape instanceof javafx.scene.shape.Rectangle rect) {
            rect.getStrokeDashArray().setAll(5.0, 5.0);
        } else if (shape instanceof javafx.scene.shape.Circle circle) {
            circle.getStrokeDashArray().setAll(5.0, 5.0);
        }
        shape.setMouseTransparent(true);
    }

    private void showComponentPreview(double x, double y, String type, boolean valid) {
        hidePreview();
        javafx.scene.shape.Shape shape = createPreviewShape(type, x, y);
        if (shape == null) {
            return;
        }
        stylePreviewShape(shape, valid);
        previewShapes.add(shape);
        componentPane.getChildren().add(shape);
    }

    private void showMovePreview(double dx, double dy, boolean valid) {
        hidePreview();
        for (Component component : selectedComponents) {
            double[] start = dragStartPositions.get(component);
            if (start == null) {
                continue;
            }
            String type = componentTypeKey(component);
            javafx.scene.shape.Shape shape = createPreviewShape(type, start[0] + dx, start[1] + dy);
            if (shape == null) {
                continue;
            }
            stylePreviewShape(shape, valid);
            previewShapes.add(shape);
            componentPane.getChildren().add(shape);
        }
    }

    private void showPreview(double x, double y) {
        if (selectedComponentType == null) {
            hidePreview();
            return;
        }
        showComponentPreview(x, y, selectedComponentType, true);
    }

    private void hidePreview() {
        for (javafx.scene.shape.Shape shape : previewShapes) {
            componentPane.getChildren().remove(shape);
        }
        previewShapes.clear();
    }
    
    private void handleComponentInteraction(Component clicked, MouseEvent event) {
        if (clicked == null) return;
        if (isDeleting) {
            deleteComponent(clicked);
            return;
        }
        
        if (isConnecting) {
            WireAnchor anchor = resolveAnchor(clicked, event);
            handleWireConnection(anchor);
        } else {
            if (event != null && event.isControlDown()) {
                updateSelectionByClick(clicked, event);
                setupComponentPane();
                updateStatus("Компонент " + clicked.getType() + " добавлен/убран из множественного выделения");
                return;
            }
            boolean wasSelected = clicked.isSelected();
            if (clicked instanceof Button) {
                Button button = (Button) clicked;
                button.toggle();
                updateStatus("🔘 Кнопка " + (button.isPressed() ? "НАЖАТА" : "отпущена"));
                if (event != null && event.getClickCount() >= 2 && wasSelected && !button.isPressed()) {
                    HelpWindow.showHelp("Button");
                }
            } else if (clicked instanceof LED) {
                LED led = (LED) clicked;
                String status = led.isBurned() ? "ПЕРЕГОРЕЛ (серый)" : (led.isOn() ? "горит" : "не горит");
                updateStatus("💡 LED " + status);
                if (event != null && event.getClickCount() >= 2 && wasSelected) {
                    HelpWindow.showHelp("LED");
                }
            } else if (clicked instanceof Resistor) {
                Resistor resistor = (Resistor) clicked;
                double previewOhms = CircuitAnalyzer.parallelGroupOhms(resistor, components, wires);
                double iMa = CircuitPhysics.currentMilliAmps(
                    CircuitPhysics.SUPPLY_V, previewOhms, CircuitPhysics.LED_VF);
                String parallelNote = Math.abs(previewOhms - resistor.getResistance()) > 0.5
                    ? String.format(", в цепи R≈%.0f Ом", previewOhms) : "";
                updateStatus("⚡ Резистор " + String.format("%.0f", resistor.getResistance()) + " Ом"
                    + parallelNote + " (" + resistor.getColorCodeText() + "). " + CircuitPhysics.shortStatus(iMa));
                if (event != null && event.getClickCount() >= 2 && wasSelected) {
                    ResistorConfigWindow.showConfig(resistor, components, wires);
                }
            } else if (clicked instanceof ArduinoUNO) {
                ArduinoUNO arduino = (ArduinoUNO) clicked;
                arduino.toggle();
                updateStatus(arduino.getStatusText());
                if (event != null && event.getClickCount() >= 2 && wasSelected) {
                    ArduinoConfigWindow.showConfig(arduino, components, wires);
                }
            } else if (clicked instanceof Timer) {
                Timer timer = (Timer) clicked;
                timer.toggle();
                if (timer.isActive() && !timer.isRunning() && isSimulating) {
                    timer.start();
                } else if (!timer.isActive()) {
                    timer.stop();
                }
                updateStatus(timer.getStatusText());
                if (event != null && event.getClickCount() >= 2 && wasSelected) {
                    TimerConfigWindow.showConfig(timer);
                }
            } else if (clicked instanceof Battery) {
                Battery battery = (Battery) clicked;
                updateStatus(String.format("🔋 Батарейка %.1f В — двойной клик для настройки", battery.getVoltage()));
                if (event != null && event.getClickCount() >= 2 && wasSelected) {
                    BatteryConfigWindow.showConfig(battery);
                }
            } else {
                updateStatus("⚡ " + clicked.getType() + " выбран");
            }
            updateSelectionByClick(clicked, event);
            setupComponentPane();
        }
    }

    private Component findComponentAt(double x, double y) {
        // Проверяем компоненты в обратном порядке (последние добавленные сверху)
        for (int i = components.size() - 1; i >= 0; i--) {
            Component comp = components.get(i);
            if (comp.contains(x, y)) {
                return comp;
            }
        }
        return null;
    }

    private boolean hasWireBetween(Component a, Component b) {
        if (a == null || b == null) {
            return false;
        }
        for (Wire wire : wires) {
            Component from = wire.getFrom();
            Component to = wire.getTo();
            if ((from == a && to == b) || (from == b && to == a)) {
                return true;
            }
        }
        return false;
    }

    private WireAnchor resolveAnchor(Component component, MouseEvent event) {
        Component other = sourceAnchor != null ? sourceAnchor.getComponent() : null;
        return connectionAnchor(component, other, event);
    }

    private void handleWireConnection(WireAnchor target) {
        if (target == null) {
            return;
        }
        if (sourceAnchor == null) {
            sourceAnchor = target;
            sourceComponent = target.getComponent();
            if (sourceComponent != null) {
                sourceComponent.setSelected(true);
            }
            setupComponentPane();
            updateStatus("✓ Первая точка: " + target.getDisplayName() + ". Выберите вторую (компонент, ножку или шину +/−)");
            return;
        }
        if (sourceAnchor.isSameEndpoint(target)) {
            clearWireConnectionSelection();
            updateStatus("Выбор отменён. Выберите первую точку соединения");
            return;
        }
        for (Wire wire : wires) {
            if (wire.connects(sourceAnchor, target)) {
                clearWireConnectionSelection();
                updateStatus("Между этими точками уже есть провод");
                return;
            }
        }
        String fromLabel = sourceAnchor.getDisplayName();
        Wire wire = new Wire(sourceAnchor, target);
        wires.add(wire);
        currentCircuit.addWire(wire);
        Component a = sourceAnchor.getComponent();
        Component b = target.getComponent();
        if (a != null && b != null) {
            a.addConnection(b);
            b.addConnection(a);
        }
        clearWireConnectionSelection();
        setupComponentPane();
        updateStatus("✓ Соединено: " + fromLabel + " → " + target.getDisplayName());
    }

    private void clearWireConnectionSelection() {
        if (sourceComponent != null) {
            sourceComponent.setSelected(false);
        }
        sourceComponent = null;
        sourceAnchor = null;
    }

    /** Находит провод по координатам клика (расстояние до линии провода). */
    private Wire findWireAt(double x, double y) {
        double minDistance = Double.MAX_VALUE;
        Wire closestWire = null;
        
        for (Wire wire : wires) {
            javafx.scene.shape.Line line = wire.getLine();
            if (line == null) continue;
            double x1 = line.getStartX();
            double y1 = line.getStartY();
            double x2 = line.getEndX();
            double y2 = line.getEndY();
            double dx = x2 - x1;
            double dy = y2 - y1;
            double lengthSquared = dx * dx + dy * dy;
            if (lengthSquared == 0) {
                double dist = Math.sqrt((x - x1) * (x - x1) + (y - y1) * (y - y1));
                if (dist < 10 && dist < minDistance) {
                    minDistance = dist;
                    closestWire = wire;
                }
            } else {
                double t = Math.max(0, Math.min(1, ((x - x1) * dx + (y - y1) * dy) / lengthSquared));
                double closestX = x1 + t * dx;
                double closestY = y1 + t * dy;
                double dist = Math.sqrt((x - closestX) * (x - closestX) + (y - closestY) * (y - closestY));
                if (dist < 10 && dist < minDistance) {
                    minDistance = dist;
                    closestWire = wire;
                }
            }
        }
        
        return closestWire;
    }

    private void deleteWire(Wire wire) {
        if (wire != null) {
            Component from = wire.getFrom();
            Component to = wire.getTo();
            if (from != null) {
                from.getConnections().remove(to);
            }
            if (to != null) {
                to.getConnections().remove(from);
            }
            if (to instanceof LED) {
                ((LED) to).setInput(false);
            } else if (to instanceof Resistor) {
                ((Resistor) to).setInput(false);
            }
            if (from instanceof LED) {
                ((LED) from).setInput(false);
            } else if (from instanceof Resistor) {
                ((Resistor) from).setInput(false);
            }
            wires.remove(wire);
            currentCircuit.removeWire(wire);
            wirePane.getChildren().remove(wire.getLine());
            if (isSimulating) {
                simulate();
            }
            
            setupComponentPane();
            updateStatus("✓ Провод удален");
        }
    }

    @FXML
    private void onLEDClick() {
        // Если симуляция запущена, выключаем её при выборе компонента
        if (isSimulating) {
            onSimulateClick();
            updateStatus("Симуляция остановлена. Выберите место на макетной плате для LED");
        }
        selectedComponentType = "LED";
        isConnecting = false;
        isDeleting = false;
        resetModeButtons();
        updateButtonStyle(btnLED);
        if (!isSimulating) {
            updateStatus("Выберите место на макетной плате для LED");
        }
        setupComponentPane();
    }

    @FXML
    private void onButtonClick() {
        // Если симуляция запущена, выключаем её при выборе компонента
        if (isSimulating) {
            onSimulateClick();
            updateStatus("Симуляция остановлена. Выберите место на макетной плате для кнопки");
        }
        selectedComponentType = "Button";
        isConnecting = false;
        isDeleting = false;
        resetModeButtons();
        updateButtonStyle(btnButton);
        if (!isSimulating) {
            updateStatus("Выберите место на макетной плате для кнопки");
        }
        setupComponentPane();
    }

    @FXML
    private void onResistorClick() {
        // Если симуляция запущена, выключаем её при выборе компонента
        if (isSimulating) {
            onSimulateClick();
            updateStatus("Симуляция остановлена. Выберите место на макетной плате для резистора");
        }
        selectedComponentType = "Resistor";
        isConnecting = false;
        isDeleting = false;
        resetModeButtons();
        updateButtonStyle(btnResistor);
        if (!isSimulating) {
            updateStatus("Выберите место на макетной плате для резистора");
        }
        setupComponentPane();
    }

    @FXML
    private void onArduinoClick() {
        // Если симуляция запущена, выключаем её при выборе компонента
        if (isSimulating) {
            onSimulateClick();
            updateStatus("Симуляция остановлена. Выберите место на макетной плате для Arduino UNO");
        }
        selectedComponentType = "ArduinoUNO";
        isConnecting = false;
        isDeleting = false;
        resetModeButtons();
        updateButtonStyle(btnArduino);
        if (!isSimulating) {
            updateStatus("Выберите место на макетной плате для Arduino UNO");
        }
        setupComponentPane();
    }

    @FXML
    private void onTimerClick() {
        // Если симуляция запущена, выключаем её при выборе компонента
        if (isSimulating) {
            onSimulateClick();
            updateStatus("Симуляция остановлена. Выберите место на макетной плате для таймера");
        }
        selectedComponentType = "Timer";
        isConnecting = false;
        isDeleting = false;
        resetModeButtons();
        updateButtonStyle(btnTimer);
        if (!isSimulating) {
            updateStatus("Выберите место на макетной плате для таймера");
        }
        setupComponentPane();
    }

    @FXML
    private void onBatteryClick() {
        if (isSimulating) {
            onSimulateClick();
            updateStatus("Симуляция остановлена. Выберите место на макетной плате для батарейки");
        }
        selectedComponentType = "Battery";
        isConnecting = false;
        isDeleting = false;
        resetModeButtons();
        updateButtonStyle(btnBattery);
        if (!isSimulating) {
            updateStatus("Выберите место на макетной плате для батарейки (+/− клеммы)");
        }
        setupComponentPane();
    }
    
    private void resetModeButtons() {
        setToolButtonActive(btnConnect, false, false);
        setToolButtonActive(btnDelete, false, true);
    }

    private void setToolButtonActive(javafx.scene.control.Button button, boolean active, boolean deleteStyle) {
        if (button == null) {
            return;
        }
        button.getStyleClass().removeAll("tool-button-active", "tool-button-delete-active");
        if (active) {
            button.getStyleClass().add(deleteStyle ? "tool-button-delete-active" : "tool-button-active");
        }
    }

    private void setSimulateButtonRunning(boolean running) {
        if (btnSimulate == null) {
            return;
        }
        btnSimulate.getStyleClass().remove("tool-button-simulate-running");
        btnSimulate.setText(running ? "Стоп" : "Симуляция");
        if (running) {
            btnSimulate.getStyleClass().add("tool-button-simulate-running");
        }
    }

    @FXML
    private void onConnectClick() {
        isConnecting = !isConnecting;
        isDeleting = false; // Выключаем режим удаления
        selectedComponentType = null;
        resetButtonStyles();
        hidePreview();
        if (isConnecting) {
            setToolButtonActive(btnConnect, true, false);
            setToolButtonActive(btnDelete, false, true);
            updateStatus("Режим проводов: кликните первую точку, затем вторую (компонент или шину +/−)");
        } else {
            setToolButtonActive(btnConnect, false, false);
            clearWireConnectionSelection();
            updateStatus("Режим соединения выключен");
        }
        setupComponentPane();
    }

    @FXML
    private void onDeleteClick() {
        isDeleting = !isDeleting;
        isConnecting = false; // Выключаем режим соединения
        selectedComponentType = null;
        resetButtonStyles();
        hidePreview();
        if (isDeleting) {
            setToolButtonActive(btnDelete, true, true);
            setToolButtonActive(btnConnect, false, false);
            updateStatus("Режим удаления: кликайте по компонентам или проводам");
        } else {
            setToolButtonActive(btnDelete, false, true);
            clearComponentSelection();
            updateStatus("Режим удаления выключен");
        }
        setupComponentPane();
    }

    private void deleteComponent(Component comp) {
        if (comp != null) {
            // Удаляем этот компонент из списка connections у всех остальных (чтобы перегрузка не считала удалённые LED)
            for (Component c : components) {
                if (c != comp) {
                    c.getConnections().remove(comp);
                }
            }
            // Удаляем все провода, связанные с компонентом (и из списка контроллера, и из схемы)
            for (int i = wires.size() - 1; i >= 0; i--) {
                Wire wire = wires.get(i);
                if (wire.getFrom() == comp || wire.getTo() == comp
                    || wire.getFromAnchor().getComponent() == comp
                    || wire.getToAnchor().getComponent() == comp) {
                    wires.remove(i);
                    currentCircuit.removeWire(wire);
                }
            }
            components.remove(comp);
            currentCircuit.removeComponent(comp);
            refreshSelectionCache();
            if (sourceComponent == comp) sourceComponent = null;
            setupComponentPane();
            updateStatus("✓ Компонент " + comp.getType() + " удален");
        }
    }

    @FXML
    private void onSimulateClick() {
        if (isSimulating) {
            isSimulating = false;
            setSimulateButtonRunning(false);
            updateStatus("Симуляция остановлена");
            stopSimulation();
            setupComponentPane();
            return;
        }
        String validationError = validateCircuitForSimulation();
        if (validationError != null) {
            showAlert("Схема не будет работать", validationError);
            updateStatus(validationError);
            return;
        }
        isSimulating = true;
        setSimulateButtonRunning(true);
        isConnecting = false;
        isDeleting = false;
        sourceComponent = null;
        sourceAnchor = null;
        clearComponentSelection();
        resetModeButtons();
        updateStatus("Симуляция запущена — нажимайте кнопки, следите за LED и проводами");
        startSimulation();
        setupComponentPane();
    }

    /**
     * Проверяет, может ли схема работать. Возвращает сообщение об ошибке или null, если схема в порядке.
     */
    private String validateCircuitForSimulation() {
        if (components == null || components.isEmpty()) {
            return "Эта схема работать не будет. Добавьте компоненты на плату.";
        }
        if (wires == null || wires.isEmpty()) {
            return "Эта схема работать не будет. Соедините компоненты проводами (кнопка «Соединить»).";
        }
        boolean hasLed = false;
        for (Component comp : components) {
            if (comp instanceof LED) {
                hasLed = true;
                break;
            }
        }
        if (!hasLed) {
            return "Эта схема работать не будет. Добавьте нагрузку (например, LED).";
        }

        // Шины +5V и GND — полноценный источник питания (замкнутый контур через LED)
        for (Component comp : components) {
            if (comp instanceof LED led && PowerRailSimulator.isTopologicallyBetweenRails(led, wires)) {
                return null;
            }
        }

        boolean hasLogicSource = false;
        for (Component comp : components) {
            if (comp instanceof Button || comp instanceof Timer || comp instanceof ArduinoUNO) {
                hasLogicSource = true;
                break;
            }
        }

        if (!hasLogicSource) {
            if (hasWireToRail(true) || hasWireToRail(false)) {
                return "Эта схема работать не будет. Для шин +5V/GND нужен замкнутый контур: "
                    + "LED соедините и с +5V, и с GND (через резистор ~220 Ом).";
            }
            return "Эта схема работать не будет. Нужно питание: шины +5V/GND (замкнутый контур) "
                + "или Arduino UNO. Кнопка и таймер — переключатели, сами питание не дают.";
        }

        // Есть ли путь от логического источника к LED по проводам?
        java.util.Set<Component> reachableFromSources = new java.util.HashSet<>();
        for (Component comp : components) {
            if (comp instanceof Button || comp instanceof Timer || comp instanceof ArduinoUNO) {
                reachableFromSources.add(comp);
            }
        }
        // Достижимость по проводам в обе стороны (провод не направленный)
        boolean changed = true;
        while (changed) {
            changed = false;
            for (Wire w : wires) {
                Component from = w.getFrom();
                Component to = w.getTo();
                if (from instanceof LED) {
                    continue;
                }
                if (from != null && reachableFromSources.contains(from)
                    && to != null && !reachableFromSources.contains(to)) {
                    reachableFromSources.add(to);
                    changed = true;
                }
                if (to instanceof LED) {
                    if (from != null && reachableFromSources.contains(from) && !reachableFromSources.contains(to)) {
                        reachableFromSources.add(to);
                        changed = true;
                    }
                    continue;
                }
                if (to != null && reachableFromSources.contains(to)
                    && from != null && !reachableFromSources.contains(from)) {
                    reachableFromSources.add(from);
                    changed = true;
                }
            }
        }
        for (Component comp : components) {
            if (comp instanceof LED && reachableFromSources.contains(comp)) {
                return null;
            }
        }
        return "Эта схема работать не будет. Соедините LED с питанием (шины +5V/GND или Arduino) "
            + "или через цепь с кнопкой/таймером (они только замыкают сигнал, питание даёт Arduino).";
    }

    private boolean hasWireToRail(boolean plusRail) {
        for (Wire wire : wires) {
            if (plusRail) {
                if (wire.getFromAnchor().isRailPlus() || wire.getToAnchor().isRailPlus()) {
                    return true;
                }
            } else if (wire.getFromAnchor().isRailMinus() || wire.getToAnchor().isRailMinus()) {
                return true;
            }
        }
        return false;
    }

    @FXML
    private void onLoginClick() {
        if (authInProgress) {
            return;
        }
        String email = loginEmailField != null ? loginEmailField.getText().trim() : "";
        String password = loginPasswordField != null ? loginPasswordField.getText() : "";

        if (email.isEmpty()) {
            updateStatus("Введите email для входа");
            showAlert("Вход", "Пожалуйста, введите email.");
            return;
        }
        if (password == null || password.length() < 6) {
            updateStatus("Введите пароль (не менее 6 символов)");
            showAlert("Вход", "Пароль должен быть не короче 6 символов.");
            return;
        }

        runAuthTask(
            "Подключение к серверу, выполняется вход…",
            () -> firebaseAuthService.login(email, password),
            authResult -> {
                currentUserId = authResult.getUserId();
                currentIdToken = authResult.getIdToken();
                currentUserEmail = authResult.getEmail();
                updateAuthUi();
                loadCircuitListAsync();
                updateStatus("Выполнен вход как " + currentUserEmail);
            },
            e -> {
                showAlert("Ошибка входа", "Не удалось выполнить вход: " + e.getMessage());
                updateStatus("Ошибка входа: " + e.getMessage());
            }
        );
    }

    @FXML
    private void onRegisterClick() {
        if (authInProgress) {
            return;
        }
        String email = loginEmailField != null ? loginEmailField.getText().trim() : "";
        String password = loginPasswordField != null ? loginPasswordField.getText() : "";

        if (email.isEmpty()) {
            updateStatus("Введите email для регистрации");
            showAlert("Регистрация", "Пожалуйста, введите email.");
            return;
        }
        if (password == null || password.length() < 6) {
            updateStatus("Введите пароль (не менее 6 символов)");
            showAlert("Регистрация", "Пароль должен быть не короче 6 символов.");
            return;
        }

        runAuthTask(
            "Подключение к серверу, создаётся аккаунт…",
            () -> firebaseAuthService.register(email, password),
            authResult -> {
                currentUserId = authResult.getUserId();
                currentIdToken = authResult.getIdToken();
                currentUserEmail = authResult.getEmail();
                updateAuthUi();
                loadCircuitListAsync();
                updateStatus("Аккаунт создан и выполнен вход как " + currentUserEmail);
            },
            e -> {
                showAlert("Ошибка регистрации", "Не удалось создать аккаунт: " + e.getMessage());
                updateStatus("Ошибка регистрации: " + e.getMessage());
            }
        );
    }

    private void runAuthTask(
        String progressStatus,
        Callable<FirebaseAuthService.AuthResult> action,
        Consumer<FirebaseAuthService.AuthResult> onSuccess,
        Consumer<Exception> onError
    ) {
        authInProgress = true;
        setAuthControlsDisabled(true);
        updateStatus(progressStatus);

        Task<FirebaseAuthService.AuthResult> task = new Task<>() {
            @Override
            protected FirebaseAuthService.AuthResult call() throws Exception {
                return action.call();
            }
        };
        task.setOnSucceeded(event -> {
            authInProgress = false;
            setAuthControlsDisabled(false);
            onSuccess.accept(task.getValue());
        });
        task.setOnFailed(event -> {
            authInProgress = false;
            setAuthControlsDisabled(false);
            Throwable error = task.getException();
            Exception ex = error instanceof Exception exception
                ? exception
                : new Exception(error != null ? error.getMessage() : "Неизвестная ошибка", error);
            onError.accept(ex);
        });
        Thread worker = new Thread(task, "firebase-auth");
        worker.setDaemon(true);
        worker.start();
    }

    private void setAuthControlsDisabled(boolean disabled) {
        if (btnLogin != null) {
            btnLogin.setDisable(disabled);
        }
        if (btnRegister != null) {
            btnRegister.setDisable(disabled);
        }
        if (loginEmailField != null) {
            loginEmailField.setDisable(disabled);
        }
        if (loginPasswordField != null) {
            loginPasswordField.setDisable(disabled);
        }
    }

    @FXML
    private void onLogoutClick() {
        String emailToRestore = currentUserEmail;
        currentUserId = null;
        currentIdToken = null;
        currentUserEmail = null;
        updateAuthUi();
        if (emailToRestore != null && loginEmailField != null) {
            loginEmailField.setText(emailToRestore);
        }
        if (loginPasswordField != null) {
            loginPasswordField.clear();
        }
        if (circuitList != null) {
            circuitList.getItems().clear();
        }
        updateStatus("Выполнен выход из аккаунта");
    }

    private void startSimulation() {
        // Таймер в цепи питания (GND/+5V) включаем автоматически — иначе ключ разомкнут
        for (Component comp : components) {
            if (comp instanceof Timer timer && isConnectedToPowerRail(timer)) {
                timer.setActive(true);
            }
        }
        // Запускаем все активные таймеры при старте симуляции
        for (Component comp : components) {
            if (comp instanceof Timer) {
                Timer timer = (Timer) comp;
                if (timer.isActive()) {
                    timer.start();
                }
            }
        }
        
        simulationTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                // Обновляем таймеры с правильным временем
                long currentTimeMillis = System.currentTimeMillis();
                for (Component comp : components) {
                    if (comp instanceof Timer) {
                        Timer timer = (Timer) comp;
                        timer.update(currentTimeMillis);
                    }
                }
                simulate();
            }
        };
        simulationTimer.start();
    }

    private void stopSimulation() {
        if (simulationTimer != null) {
            simulationTimer.stop();
        }
        // Останавливаем все таймеры
        for (Component comp : components) {
            if (comp instanceof Timer) {
                Timer timer = (Timer) comp;
                timer.stop();
            }
        }
        // Сбрасываем все LED
        for (Component comp : components) {
            if (comp instanceof LED) {
                ((LED) comp).setOn(false);
            }
        }
        setupComponentPane();
    }

    /**
     * Проверяет, есть ли «активный» путь от Arduino к компоненту:
     * - через Таймер — только когда таймер выдаёт сигнал;
     * - через Кнопку — только когда кнопка нажата.
     * Цепь UNO -> Кнопка -> Таймер -> LED: при отжатой кнопке таймер и LED не получают питание.
     */
    private boolean hasActivePathFromArduinoTo(Component target) {
        return CircuitAnalyzer.hasPathFromArduino(target, components, wires);
    }

    /** Замкнутая цепь и расчёт I = (U − V_LED) / R для каждого LED. */
    private void analyzeAllLedCircuits() {
        String bestStatus = null;
        for (Component comp : components) {
            if (!(comp instanceof LED led)) {
                continue;
            }
            CircuitAnalyzer.LedOutcome outcome = CircuitAnalyzer.computeLedOutcome(led, components, wires);
            if (outcome == null) {
                led.setInput(false);
                continue;
            }
            if (outcome.burn()) {
                led.burn();
                led.setInput(false);
                bestStatus = outcome.statusLine();
            } else if (outcome.glow()) {
                if (led.isBurned()) {
                    led.resetBurn();
                }
                led.setInput(true);
                String line = outcome.statusLine();
                if (outcome.resistorWarning() || bestStatus == null) {
                    bestStatus = line;
                }
            } else {
                led.setInput(false);
                if (led.isBurned() && outcome.calc().getSafety() != CircuitPhysics.SafetyLevel.OVERLOAD
                    && outcome.calc().getSafety() != CircuitPhysics.SafetyLevel.NO_RESISTOR) {
                    led.resetBurn();
                }
                String line = outcome.statusLine();
                if (line != null && !line.isBlank()
                    && outcome.calc().getSafety() == CircuitPhysics.SafetyLevel.TOO_LOW) {
                    bestStatus = line;
                }
            }
        }
        if (bestStatus != null && !bestStatus.isBlank()) {
            updateStatus(bestStatus);
        }
    }
    
    private void simulate() {
        // Симуляция: если кнопка нажата и соединена с LED через провода, LED загорается
        // Резистор ограничивает ток (в простой модели - ослабляет сигнал)
        // Без резистора слишком много LED перегорят!

        // Сбрасываем все LED в начало кадра; потом каждый провод добавляет сигнал (OR)
        for (Component comp : components) {
            if (comp instanceof LED) {
                ((LED) comp).setInput(false);
            }
        }
        
        // ПЕРВЫЙ ПРОХОД: Устанавливаем входные сигналы для всех компонентов
        // (резисторы, Arduino получают сигналы от источников)
        for (Wire wire : wires) {
            WireAnchor fromA = wire.getFromAnchor();
            WireAnchor toA = wire.getToAnchor();
            Component from = wire.getFrom();
            Component to = wire.getTo();

            if (from instanceof LED) {
                continue;
            }

            boolean signal = WireSignals.readSourceSignal(fromA, from);

            if (from instanceof Resistor) {
                Resistor resistor = (Resistor) from;
                signal = WireSignals.readResistorOutput(resistor);
            }

            if (to instanceof Resistor) {
                Resistor resistor = (Resistor) to;
                resistor.setInput(signal);
            } else if (to instanceof ArduinoUNO) {
                ArduinoUNO arduino = (ArduinoUNO) to;
                if (arduino.isPowered()) {
                    arduino.setInputSignal(signal);
                }
            }
        }
        
        // ВТОРОЙ ПРОХОД: Передаем выходные сигналы к LED и другим компонентам
        // Используем уже объявленную переменную hasArduino из начала метода
        for (Wire wire : wires) {
            WireAnchor fromA = wire.getFromAnchor();
            Component from = wire.getFrom();
            Component to = wire.getTo();

            if (from instanceof LED) {
                wire.setActive(false);
                continue;
            }

            boolean signal = WireSignals.readSourceSignal(fromA, from);

            if (fromA.isRailPlus() && to instanceof LED ledDirect) {
                wire.setActive(!ledDirect.isBurned() && ledDirect.isOn());
                continue;
            }

            if (from instanceof Button && to instanceof LED) {
                Button button = (Button) from;
                signal = button.isPressed() && hasActivePathFromArduinoTo(button);
            } else if (from instanceof Timer && to instanceof LED) {
                Timer timer = (Timer) from;
                signal = timer.getOutput() && hasActivePathFromArduinoTo(timer);
            }
            
            // Если сигнал проходит через резистор, проверяем его входной сигнал
            if (from instanceof Resistor) {
                Resistor resistor = (Resistor) from;
                signal = WireSignals.readResistorOutput(resistor);
            }
            
            wire.setActive(signal);
            
            if (to instanceof LED) {
                LED led = (LED) to;
                if (led.isBurned()) {
                    continue;
                }
                // Состояние LED — только через analyzeAllLedCircuits() и формулу I=(U−V)/R
            } else if (to instanceof ArduinoUNO) {
                // Входной сигнал уже установлен в первом проходе
                ArduinoUNO arduino = (ArduinoUNO) to;
                wire.setActive(arduino.isPowered() && arduino.getOutput());
            }
        }
        
        // Обратное направление провода: если источник — второй конец (to), передаём сигнал первому (from)
        for (Wire wire : wires) {
            Component from = wire.getFrom();
            Component to = wire.getTo();
            if (from instanceof LED) {
                boolean signal = false;
                if (to instanceof Button) {
                    Button button = (Button) to;
                    if (button.isPressed() && hasActivePathFromArduinoTo(button)) {
                        signal = true;
                    }
                } else if (to instanceof Timer) {
                    Timer timer = (Timer) to;
                    if (timer.getOutput() && hasActivePathFromArduinoTo(timer)) {
                        signal = true;
                    }
                } else if (to instanceof ArduinoUNO) {
                    signal = ((ArduinoUNO) to).isPowered() && ((ArduinoUNO) to).getOutput();
                }
                wire.setActive(signal);
            } else if (from instanceof Resistor && (to instanceof Button || to instanceof Timer || to instanceof ArduinoUNO)) {
                boolean signal = false;
                if (to instanceof Button) {
                    Button button = (Button) to;
                    signal = button.isPressed() && hasActivePathFromArduinoTo(button);
                } else if (to instanceof Timer) {
                    Timer timer = (Timer) to;
                    signal = timer.getOutput() && hasActivePathFromArduinoTo(timer);
                } else if (to instanceof ArduinoUNO) {
                    signal = ((ArduinoUNO) to).isPowered() && ((ArduinoUNO) to).getOutput();
                }
                ((Resistor) from).setInput(signal);
                wire.setActive(signal);
            }
        }
        
        // Обрабатываем выходы от таймеров (цепь Arduino -> Таймер -> LED)
        for (Component comp : components) {
            if (comp instanceof Timer) {
                Timer timer = (Timer) comp;
                boolean output = timer.getOutput() && hasActivePathFromArduinoTo(timer);
                for (Wire wire : wires) {
                    if (wire.getFrom() == timer) {
                        Component to = wire.getTo();
                        wire.setActive(output);
                        if (to instanceof Resistor) {
                            Resistor resistor = (Resistor) to;
                            resistor.setInput(output);
                        }
                        // Arduino обрабатывается отдельно в другом цикле
                    }
                }
                // Прямые соединения (Arduino -> Таймер -> LED)
                for (Component connected : comp.getConnections()) {
                    if (connected instanceof ArduinoUNO) continue;
                    if (connected instanceof Resistor) {
                        Resistor resistor = (Resistor) connected;
                        resistor.setInput(output);
                    }
                }
            }
        }
        
        // Сначала собираем входные сигналы для Arduino UNO
        for (Component comp : components) {
            if (comp instanceof ArduinoUNO) {
                ArduinoUNO arduino = (ArduinoUNO) comp;
                if (arduino.isPowered()) {
                    boolean inputValue = false;
                    
                    // Проверяем провода, идущие К Arduino (это входы)
                    for (Wire wire : wires) {
                        if (wire.getTo() == arduino) {
                            WireAnchor fromA = wire.getFromAnchor();
                            Component from = wire.getFrom();
                            boolean signal = WireSignals.readSourceSignal(fromA, from);
                            if (from == null && !fromA.isRailPlus() && !fromA.isRailMinus()) {
                                continue;
                            }

                            if (from instanceof Resistor) {
                                Resistor resistor = (Resistor) from;
                                signal = WireSignals.readResistorOutput(resistor);
                            }

                            if (from instanceof Timer) {
                                signal = ((Timer) from).getOutput();
                            }
                            
                            if (signal) {
                                inputValue = true;
                                wire.setActive(true);
                                break;
                            }
                            wire.setActive(signal);
                        }
                    }
                    
                    // Проверяем прямые соединения (когда компоненты соединены напрямую)
                    for (Component connected : comp.getConnections()) {
                        if (connected instanceof Button) {
                            Button button = (Button) connected;
                            if (button.isPressed()) {
                                inputValue = true;
                                break;
                            }
                        } else if (connected instanceof Timer) {
                            Timer timer = (Timer) connected;
                            if (timer.getOutput()) {
                                inputValue = true;
                                break;
                            }
                        } else if (connected instanceof Resistor) {
                            Resistor resistor = (Resistor) connected;
                            if (resistor.getInput()) {
                                inputValue = true;
                                break;
                            }
                        }
                    }
                    
                    // Устанавливаем входной сигнал
                    arduino.setInputSignal(inputValue);
                }
            }
        }
        
        // Теперь передаем выходные сигналы от Arduino к подключенным компонентам
        for (Component comp : components) {
            if (comp instanceof ArduinoUNO) {
                ArduinoUNO arduino = (ArduinoUNO) comp;
                if (arduino.isPowered()) {
                    boolean output = arduino.getOutput();
                    // Передаем сигнал от Arduino к подключенным компонентам (это выходы)
                    for (Wire wire : wires) {
                        if (wire.getFrom() == arduino) {
                            Component to = wire.getTo();
                            wire.setActive(output);
                            if (to instanceof Resistor) {
                                Resistor resistor = (Resistor) to;
                                resistor.setInput(output);
                            }
                        }
                    }
                    // Прямые соединения (выходы)
                    for (Component connected : comp.getConnections()) {
                        if (connected instanceof Resistor) {
                            Resistor resistor = (Resistor) connected;
                            resistor.setInput(output);
                        }
                    }
                } else {
                    // Если Arduino выключена — только отключаем провода, LED мог получать сигнал от таймера/кнопки
                    for (Wire wire : wires) {
                        if (wire.getFrom() == arduino) {
                            wire.setActive(false);
                            Component to = wire.getTo();
                            if (to instanceof Resistor) {
                                ((Resistor) to).setInput(false);
                            }
                        }
                    }
                }
            }
        }
        
        // Проверяем прямые соединения через connections для кнопок (цепь Arduino -> Кнопка -> LED)
        for (Component comp : components) {
            if (comp instanceof Button) {
                Button button = (Button) comp;
                // Кнопка передаёт сигнал только если нажата и получает питание от Arduino
                boolean output = button.getOutput() && hasActivePathFromArduinoTo(button);
                for (Component connected : comp.getConnections()) {
                    if (connected instanceof ArduinoUNO) {
                        // К Arduino от кнопки ничего не передаём (Arduino питает кнопку)
                        continue;
                    }
                    if (connected instanceof Resistor) {
                        Resistor resistor = (Resistor) connected;
                        resistor.setInput(output);
                    }
                }
            }
        }
        
        analyzeAllLedCircuits();
        refreshWireActiveFromLeds();
        
        setupComponentPane();
    }

    private void refreshWireActiveFromLeds() {
        for (Wire wire : wires) {
            Component from = wire.getFrom();
            Component to = wire.getTo();
            if (from instanceof LED led) {
                wire.setActive(led.isOn() && !led.isBurned());
            } else if (to instanceof LED led) {
                wire.setActive(led.isOn() && !led.isBurned());
            }
        }
    }

    private boolean isConnectedToPowerRail(Component component) {
        for (Wire wire : wires) {
            if (wire.getFrom() == component
                && (wire.getToAnchor().isRailPlus() || wire.getToAnchor().isRailMinus())) {
                return true;
            }
            if (wire.getTo() == component
                && (wire.getFromAnchor().isRailPlus() || wire.getFromAnchor().isRailMinus())) {
                return true;
            }
        }
        return false;
    }

    // ОТЧЁТ ПМ02 РИС.2 — НАЧАЛО СКРИНА (onExportPngClick / onSaveLocalClick / onLoadLocalClick)
    @FXML
    private void onExportPngClick() {
        if (boardStack == null || boardStack.getScene() == null) {
            updateStatus("Невозможно экспортировать изображение: рабочая область недоступна");
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Сохранить схему в PNG");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG изображения", "*.png"));
        String baseName = (currentCircuit != null && currentCircuit.getName() != null && !currentCircuit.getName().isBlank())
            ? currentCircuit.getName().trim().replaceAll("[\\\\/:*?\"<>|]", "_")
            : "scheme";
        chooser.setInitialFileName(baseName + ".png");
        File file = chooser.showSaveDialog(boardStack.getScene().getWindow());
        if (file == null) {
            updateStatus("Экспорт PNG отменен");
            return;
        }
        try {
            WritableImage snapshot = boardStack.snapshot(new SnapshotParameters(), null);
            ImageIO.write(toBufferedImage(snapshot), "png", file);
            updateStatus("Схема сохранена в PNG: " + file.getName());
        } catch (IOException e) {
            showAlert("Ошибка", "Не удалось сохранить PNG: " + e.getMessage());
            updateStatus("Ошибка экспорта PNG: " + e.getMessage());
        }
    }

    @FXML
    private void onSaveLocalClick() {
        if (components == null || components.isEmpty()) {
            updateStatus("Нельзя сохранить пустую схему");
            showAlert("Пустая схема", "Добавьте хотя бы один компонент перед локальным сохранением.");
            return;
        }
        if (boardStack == null || boardStack.getScene() == null) {
            updateStatus("Невозможно сохранить локально: окно недоступно");
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Сохранить схему локально");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON файлы", "*.json"));
        String name = (circuitNameField != null && circuitNameField.getText() != null && !circuitNameField.getText().isBlank())
            ? circuitNameField.getText().trim().replaceAll("[\\\\/:*?\"<>|]", "_")
            : "local_scheme";
        chooser.setInitialFileName(name + ".json");
        File file = chooser.showSaveDialog(boardStack.getScene().getWindow());
        if (file == null) {
            updateStatus("Локальное сохранение отменено");
            return;
        }

        com.google.gson.JsonObject root = new com.google.gson.JsonObject();
        String circuitName = (circuitNameField != null && circuitNameField.getText() != null && !circuitNameField.getText().isBlank())
            ? circuitNameField.getText().trim()
            : "Локальная схема";
        if (currentCircuit != null) {
            currentCircuit.setName(circuitName);
        }
        root.addProperty("name", circuitName);
        root.add("components", serializeComponentsToJsonArray());
        root.add("wires", serializeWiresToJsonArray());

        try {
            String json = new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(root);
            Files.writeString(file.toPath(), json, StandardCharsets.UTF_8);
            updateStatus("Схема сохранена локально: " + file.getName());
        } catch (IOException e) {
            showAlert("Ошибка", "Не удалось сохранить JSON: " + e.getMessage());
            updateStatus("Ошибка локального сохранения: " + e.getMessage());
        }
    }

    @FXML
    private void onLoadLocalClick() {
        if (boardStack == null || boardStack.getScene() == null) {
            updateStatus("Невозможно загрузить локально: окно недоступно");
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Загрузить локальную схему");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON файлы", "*.json"));
        File file = chooser.showOpenDialog(boardStack.getScene().getWindow());
        if (file == null) {
            updateStatus("Локальная загрузка отменена");
            return;
        }

        try {
            String json = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            com.google.gson.JsonObject root = new com.google.gson.JsonParser().parse(json).getAsJsonObject();
            loadCircuitFromLocalJson(root);
            if (isSimulating) {
                onSimulateClick();
            }
            updateStatus("Локальная схема загружена: " + file.getName());
        } catch (Exception e) {
            showAlert("Ошибка", "Не удалось загрузить локальную схему: " + e.getMessage());
            updateStatus("Ошибка локальной загрузки: " + e.getMessage());
        }
    }
    // ОТЧЁТ ПМ02 РИС.2 — КОНЕЦ СКРИНА

    private com.google.gson.JsonArray serializeComponentsToJsonArray() {
        com.google.gson.JsonArray componentsArray = new com.google.gson.JsonArray();
        for (Component component : components) {
            com.google.gson.JsonObject compObj = new com.google.gson.JsonObject();
            compObj.addProperty("type", getCanonicalComponentType(component));
            compObj.addProperty("x", component.getX());
            compObj.addProperty("y", component.getY());
            if (component instanceof LED) {
                LED led = (LED) component;
                compObj.addProperty("state", led.isOn());
                compObj.addProperty("burned", led.isBurned());
            } else if (component instanceof Button) {
                compObj.addProperty("state", ((Button) component).isPressed());
            } else if (component instanceof Resistor) {
                compObj.addProperty("resistance", ((Resistor) component).getResistance());
            } else if (component instanceof ArduinoUNO) {
                compObj.addProperty("state", ((ArduinoUNO) component).isPowered());
            } else if (component instanceof Timer) {
                Timer timer = (Timer) component;
                compObj.addProperty("state", timer.isActive());
                compObj.addProperty("interval", timer.getInterval());
            } else if (component instanceof Battery) {
                compObj.addProperty("voltage", ((Battery) component).getVoltage());
            }
            componentsArray.add(compObj);
        }
        return componentsArray;
    }

    private com.google.gson.JsonArray serializeWiresToJsonArray() {
        com.google.gson.JsonArray wiresArray = new com.google.gson.JsonArray();
        for (Wire wire : wires) {
            com.google.gson.JsonObject wireObj = new com.google.gson.JsonObject();
            writeAnchorJson(wire.getFromAnchor(), wireObj, "from");
            writeAnchorJson(wire.getToAnchor(), wireObj, "to");
            wiresArray.add(wireObj);
        }
        return wiresArray;
    }

    private void writeAnchorJson(WireAnchor anchor, com.google.gson.JsonObject wireObj, String prefix) {
        if (anchor.isRailPlus()) {
            wireObj.addProperty(prefix + "Kind", "RAIL_PLUS");
            wireObj.addProperty(prefix + "RailX", anchor.getRailX());
            return;
        }
        if (anchor.isRailMinus()) {
            wireObj.addProperty(prefix + "Kind", "RAIL_MINUS");
            wireObj.addProperty(prefix + "RailX", anchor.getRailX());
            return;
        }
        Component comp = anchor.getComponent();
        int index = components.indexOf(comp);
        wireObj.addProperty(prefix + "Kind", "COMPONENT");
        wireObj.addProperty(prefix + "Index", index);
        wireObj.addProperty(prefix + "Pin", anchor.getPin());
    }

    private WireAnchor readAnchorJson(com.google.gson.JsonObject wireObj, String prefix) {
        if (wireObj.has(prefix + "Kind")) {
            String kind = wireObj.get(prefix + "Kind").getAsString();
            if ("RAIL_PLUS".equals(kind)) {
                double railX = wireObj.has(prefix + "RailX")
                    ? wireObj.get(prefix + "RailX").getAsDouble()
                    : BOARD_HOLE_MIN_X;
                return WireAnchor.railPlus(railX);
            }
            if ("RAIL_MINUS".equals(kind)) {
                double railX = wireObj.has(prefix + "RailX")
                    ? wireObj.get(prefix + "RailX").getAsDouble()
                    : BOARD_HOLE_MIN_X;
                return WireAnchor.railMinus(railX);
            }
            if ("COMPONENT".equals(kind) && wireObj.has(prefix + "Index")) {
                int index = wireObj.get(prefix + "Index").getAsInt();
                if (index >= 0 && index < components.size()) {
                    int pin = wireObj.has(prefix + "Pin") ? wireObj.get(prefix + "Pin").getAsInt() : 0;
                    return WireAnchor.component(components.get(index), pin);
                }
            }
        }
        if (wireObj.has(prefix + "Index")) {
            int index = wireObj.get(prefix + "Index").getAsInt();
            if (index >= 0 && index < components.size()) {
                return WireAnchor.componentCenter(components.get(index));
            }
        }
        return null;
    }

    private void loadCircuitFromLocalJson(com.google.gson.JsonObject root) {
        if (root == null) {
            throw new IllegalArgumentException("Файл не содержит данных схемы");
        }
        if (!root.has("components") || !root.get("components").isJsonArray()) {
            throw new IllegalArgumentException("Некорректный JSON: отсутствует массив components");
        }

        String name = root.has("name") && !root.get("name").isJsonNull()
            ? root.get("name").getAsString()
            : "Локальная схема";
        components.clear();
        wires.clear();
        clearComponentSelection();
        sourceComponent = null;
        sourceAnchor = null;
        selectedComponentType = null;
        currentCircuit = new Circuit(name);
        if (circuitNameField != null) {
            circuitNameField.setText(name);
        }

        com.google.gson.JsonArray componentsArray = root.getAsJsonArray("components");
        for (int i = 0; i < componentsArray.size(); i++) {
            com.google.gson.JsonObject compObj = componentsArray.get(i).getAsJsonObject();
            if (!compObj.has("type") || !compObj.has("x") || !compObj.has("y")) continue;
            String type = normalizeComponentType(compObj.get("type").getAsString());
            double x = compObj.get("x").getAsDouble();
            double y = compObj.get("y").getAsDouble();
            Component component = null;
            switch (type) {
                case "LED":
                    component = new LED(x, y);
                    if (compObj.has("state")) ((LED) component).setOn(compObj.get("state").getAsBoolean());
                    if (compObj.has("burned") && compObj.get("burned").getAsBoolean()) {
                        ((LED) component).burn();
                    } else if (compObj.has("resistance") && compObj.get("resistance").getAsDouble() == 1) {
                        ((LED) component).burn();
                    }
                    break;
                case "Button":
                    component = new Button(x, y);
                    if (compObj.has("state")) ((Button) component).setPressed(compObj.get("state").getAsBoolean());
                    break;
                case "Resistor":
                    component = new Resistor(x, y);
                    if (compObj.has("resistance")) ((Resistor) component).setResistance(compObj.get("resistance").getAsDouble());
                    break;
                case "ArduinoUNO":
                    component = new ArduinoUNO(x, y);
                    if (compObj.has("state")) ((ArduinoUNO) component).setPowered(compObj.get("state").getAsBoolean());
                    break;
                case "Timer":
                    component = new Timer(x, y);
                    if (compObj.has("state")) ((Timer) component).setActive(compObj.get("state").getAsBoolean());
                    if (compObj.has("interval")) ((Timer) component).setInterval((long) compObj.get("interval").getAsDouble());
                    else if (compObj.has("resistance")) ((Timer) component).setInterval((long) compObj.get("resistance").getAsDouble());
                    break;
                case "Battery":
                    component = new Battery(x, y);
                    if (compObj.has("voltage")) {
                        ((Battery) component).setVoltage(compObj.get("voltage").getAsDouble());
                    }
                    break;
                default:
                    break;
            }
            if (component != null) {
                components.add(component);
                currentCircuit.addComponent(component);
            }
        }

        if (root.has("wires") && root.get("wires").isJsonArray()) {
            com.google.gson.JsonArray wiresArray = root.getAsJsonArray("wires");
            for (int i = 0; i < wiresArray.size(); i++) {
                com.google.gson.JsonObject wireObj = wiresArray.get(i).getAsJsonObject();
                WireAnchor fromAnchor;
                WireAnchor toAnchor;
                if (wireObj.has("fromKind") || wireObj.has("toKind")) {
                    fromAnchor = readAnchorJson(wireObj, "from");
                    toAnchor = readAnchorJson(wireObj, "to");
                } else if (wireObj.has("fromIndex") && wireObj.has("toIndex")) {
                    int fromIndex = wireObj.get("fromIndex").getAsInt();
                    int toIndex = wireObj.get("toIndex").getAsInt();
                    if (fromIndex < 0 || toIndex < 0 || fromIndex >= components.size() || toIndex >= components.size()) {
                        continue;
                    }
                    fromAnchor = WireAnchor.componentCenter(components.get(fromIndex));
                    toAnchor = WireAnchor.componentCenter(components.get(toIndex));
                } else {
                    continue;
                }
                if (fromAnchor == null || toAnchor == null) {
                    continue;
                }
                Wire wire = new Wire(fromAnchor, toAnchor);
                wires.add(wire);
                currentCircuit.addWire(wire);
                Component from = fromAnchor.getComponent();
                Component to = toAnchor.getComponent();
                if (from != null && to != null) {
                    from.addConnection(to);
                    to.addConnection(from);
                }
            }
        }
        setupComponentPane();
    }

    private String getCanonicalComponentType(Component component) {
        if (component instanceof LED) return "LED";
        if (component instanceof Button) return "Button";
        if (component instanceof Resistor) return "Resistor";
        if (component instanceof ArduinoUNO) return "ArduinoUNO";
        if (component instanceof Timer) return "Timer";
        if (component instanceof Battery) return "Battery";
        return component != null ? component.getType() : "";
    }

    private String normalizeComponentType(String rawType) {
        if (rawType == null) return "";
        String normalized = rawType.trim().replace(" ", "").toLowerCase();
        switch (normalized) {
            case "led":
                return "LED";
            case "button":
            case "btn":
                return "Button";
            case "resistor":
                return "Resistor";
            case "arduinouno":
            case "arduino":
                return "ArduinoUNO";
            case "timer":
                return "Timer";
            case "battery":
            case "бат":
                return "Battery";
            default:
                return rawType.trim();
        }
    }

    private BufferedImage toBufferedImage(WritableImage image) {
        int width = (int) Math.max(1, Math.round(image.getWidth()));
        int height = (int) Math.max(1, Math.round(image.getHeight()));
        BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        PixelReader pixelReader = image.getPixelReader();
        if (pixelReader == null) {
            return bufferedImage;
        }
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                bufferedImage.setRGB(x, y, pixelReader.getArgb(x, y));
            }
        }
        return bufferedImage;
    }

    @FXML
    private void onSaveClick() {
        if (!isAuthenticated()) {
            updateStatus("Для сохранения схемы необходимо войти в аккаунт");
            showAlert("Вход требуется", "Пожалуйста, войдите или зарегистрируйтесь, чтобы сохранять схемы только для себя.");
            return;
        }
        if (components == null || components.isEmpty()) {
            updateStatus("Нельзя сохранить пустую схему");
            showAlert("Пустая схема", "Добавьте хотя бы один компонент на плату, чтобы сохранить схему.");
            return;
        }

        String name = circuitNameField.getText();
        if (name.isEmpty()) {
            name = "Схема " + System.currentTimeMillis();
        }
        currentCircuit.setName(name);

        com.google.gson.JsonObject root = new com.google.gson.JsonObject();
        root.addProperty("name", name);
        root.add("components", serializeComponentsToJsonArray());
        root.add("wires", serializeWiresToJsonArray());

        try {
            firebaseService.saveCircuit(root, currentUserId, currentIdToken);
            showAlert("Успех", "Схема сохранена!");
            loadCircuitList();
            updateStatus("Схема сохранена: " + name);
        } catch (Exception e) {
            showAlert("Ошибка", "Не удалось сохранить: " + e.getMessage());
            updateStatus("Ошибка сохранения: " + e.getMessage());
        }
    }

    @FXML
    private void onLoadClick() {
        if (!isAuthenticated()) {
            updateStatus("Для загрузки сохранённых схем необходимо войти в аккаунт");
            showAlert("Вход требуется", "Войдите, чтобы просматривать свои сохранённые схемы.");
            return;
        }

        String selected = circuitList.getSelectionModel().getSelectedItem();
        if (selected != null) {
            try {
                updateStatus("Загрузка схемы...");
                List<Circuit> circuits = firebaseService.loadCircuits(currentUserId, currentIdToken);
                Circuit loaded = circuits.stream()
                    .filter(c -> c.getName().equals(selected))
                    .findFirst()
                    .orElse(null);
                
                if (loaded != null) {
                    // Загружаем данные схемы из Firebase
                    loadCircuitFromFirebase(selected);
                    showAlert("Успех", "Схема загружена!");
                    updateStatus("Схема загружена: " + selected);
                    if (isSimulating) {
                        onSimulateClick();
                    }
                } else {
                    showAlert("Ошибка", "Схема не найдена в базе данных");
                    updateStatus("Схема не найдена");
                }
            } catch (Exception e) {
                showAlert("Ошибка", "Не удалось загрузить: " + e.getMessage());
                updateStatus("Ошибка загрузки: " + e.getMessage());
            }
        } else {
            updateStatus("Выберите схему из списка");
        }
    }

    @FXML
    private void onDeleteCircuitClick() {
        if (!isAuthenticated()) {
            updateStatus("Войдите в аккаунт для удаления схем из облака");
            return;
        }
        String selected = circuitList != null ? circuitList.getSelectionModel().getSelectedItem() : null;
        if (selected == null || selected.isEmpty()) {
            updateStatus("Выберите схему в списке для удаления");
            showAlert("Удаление схемы", "Выберите схему из списка сохранённых.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Удалить схему");
        confirm.setHeaderText(null);
        confirm.setContentText("Удалить схему \"" + selected + "\" из облака? Это действие нельзя отменить.");
        confirm.getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }
        try {
            firebaseService.deleteCircuit(selected, currentUserId, currentIdToken);
            loadCircuitList();
            if (circuitNameField != null && selected.equals(circuitNameField.getText())) {
                circuitNameField.clear();
            }
            updateStatus("Схема \"" + selected + "\" удалена из облака");
            showAlert("Готово", "Схема удалена из облака.");
        } catch (Exception e) {
            showAlert("Ошибка", "Не удалось удалить схему: " + e.getMessage());
            updateStatus("Ошибка удаления: " + e.getMessage());
        }
    }
    
    private com.google.gson.JsonObject normalizeCircuitRoot(com.google.gson.JsonObject obj) {
        if (obj == null || !obj.has("components") || !obj.get("components").isJsonObject()) {
            return obj;
        }
        com.google.gson.JsonObject nested = obj.getAsJsonObject("components");
        if (!nested.has("components") && !nested.has("wires")) {
            return obj;
        }
        com.google.gson.JsonObject flat = new com.google.gson.JsonObject();
        if (obj.has("name")) {
            flat.add("name", obj.get("name"));
        }
        if (nested.has("components")) {
            flat.add("components", nested.get("components"));
        }
        if (nested.has("wires")) {
            flat.add("wires", nested.get("wires"));
        }
        return flat;
    }

    private void loadCircuitFromFirebase(String circuitName) throws Exception {
        if (!isAuthenticated()) {
            throw new IllegalStateException("Пользователь не авторизован");
        }

        // Загружаем схему из Firebase Realtime Database
        String encodedName = java.net.URLEncoder.encode(circuitName, java.nio.charset.StandardCharsets.UTF_8);
        String path = "circuits/" + encodedName + ".json";
        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append(firebaseService.getDatabaseUrl());
        if (!firebaseService.getDatabaseUrl().endsWith("/")) {
            urlBuilder.append("/");
        }
        urlBuilder.append("users/").append(currentUserId).append("/").append(path);
        urlBuilder.append("?auth=").append(currentIdToken);

        okhttp3.Request request = new okhttp3.Request.Builder()
            .url(urlBuilder.toString())
            .get()
            .build();

        try (okhttp3.Response response = new okhttp3.OkHttpClient().newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                String jsonResponse = response.body().string();
                if (jsonResponse == null || jsonResponse.isEmpty() || "null".equals(jsonResponse.trim())) {
                    return;
                }

                com.google.gson.JsonObject obj = new com.google.gson.Gson().fromJson(jsonResponse, com.google.gson.JsonObject.class);
                if (obj == null || !obj.has("name") || obj.get("name").isJsonNull()) {
                    return;
                }

                loadCircuitFromLocalJson(normalizeCircuitRoot(obj));
                drawBreadboard();
            }
        }
    }

    @FXML
    private void onClearClick() {
        components.clear();
        wires.clear();
        componentPane.getChildren().clear();
        wirePane.getChildren().clear();
        currentCircuit = new Circuit("Новая схема");
        clearComponentSelection();
        selectedComponentType = null;
        isConnecting = false;
        sourceComponent = null;
        resetButtonStyles();
        // Перерисовываем макетную плату
        drawBreadboard();
        setupComponentPane();
        updateStatus("Макетная плата очищена");
        if (isSimulating) {
            onSimulateClick();
        }
    }

    private void loadCircuitList() {
        if (circuitList == null) {
            return;
        }
        if (!isAuthenticated()) {
            circuitList.getItems().clear();
            return;
        }
        loadCircuitListAsync();
    }

    private void loadCircuitListAsync() {
        if (circuitList == null || !isAuthenticated()) {
            return;
        }
        final String userId = currentUserId;
        final String idToken = currentIdToken;

        Task<List<Circuit>> task = new Task<>() {
            @Override
            protected List<Circuit> call() throws Exception {
                return firebaseService.loadCircuits(userId, idToken);
            }
        };
        task.setOnSucceeded(event -> {
            List<Circuit> circuits = task.getValue();
            if (circuits == null || circuitList == null) {
                return;
            }
            circuitList.getItems().clear();
            for (Circuit c : circuits) {
                circuitList.getItems().add(c.getName());
            }
        });
        task.setOnFailed(event -> {
            // Список схем необязателен для входа — не блокируем пользователя
        });
        Thread worker = new Thread(task, "firebase-circuits");
        worker.setDaemon(true);
        worker.start();
    }

    private void updateButtonStyle(javafx.scene.control.Button button) {
        resetButtonStyles();
        if (button != null) {
            if (!button.getStyleClass().contains("component-button-selected")) {
                button.getStyleClass().add("component-button-selected");
            }
        }
    }

    private void resetButtonStyles() {
        for (javafx.scene.control.Button btn : new javafx.scene.control.Button[] {
            btnLED, btnButton, btnResistor, btnArduino, btnTimer, btnBattery
        }) {
            if (btn != null) {
                btn.getStyleClass().remove("component-button-selected");
            }
        }
    }

    private double[] getPlacementBoundsForType(String type) {
        Component.Footprint footprint = footprintForType(type);
        double halfW = footprint.halfWidth();
        double halfH = footprint.halfHeight();
        return new double[] {
            BOARD_HOLE_MIN_X + halfW,
            BOARD_HOLE_MAX_X - halfW,
            BOARD_HOLE_MIN_Y + halfH,
            BOARD_HOLE_MAX_Y - halfH
        };
    }

    private void updateStatus(String message) {
        if (statusLabel != null) {
            statusLabel.setText("Статус: " + message);
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
