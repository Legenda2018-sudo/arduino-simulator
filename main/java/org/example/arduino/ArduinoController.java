package org.example.arduino;

import javafx.animation.AnimationTimer;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import org.example.arduino.model.Component;
import org.example.arduino.model.Circuit;
import org.example.arduino.model.LED;
import org.example.arduino.model.Button;
import org.example.arduino.model.Resistor;
import org.example.arduino.model.Wire;
import org.example.arduino.model.ArduinoUNO;
import org.example.arduino.model.Timer;
import org.example.arduino.service.FirebaseAuthService;
import org.example.arduino.service.FirebaseService;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class ArduinoController implements Initializable {
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
    private Label statusLabel;
    @FXML
    private Label userLabel;
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
    private boolean isConnecting;
    private boolean isDeleting;
    private boolean isSimulating;
    private String currentUserId;
    private String currentIdToken;
    private String currentUserEmail;
    private FirebaseService firebaseService;
    private FirebaseAuthService firebaseAuthService;
    private AnimationTimer simulationTimer;
    private javafx.scene.shape.Shape previewShape;

    private static final double BOARD_GRID_LEFT = 30;
    private static final double BOARD_GRID_TOP = 30;
    private static final double BOARD_CELL = 20;
    private static final int BOARD_COLS = 45;
    private static final int BOARD_ROWS = 30;
    private static final double BOARD_HOLE_MIN_X = BOARD_GRID_LEFT + BOARD_CELL / 2;
    private static final double BOARD_HOLE_MIN_Y = BOARD_GRID_TOP + BOARD_CELL / 2;
    private static final double BOARD_HOLE_MAX_X = BOARD_GRID_LEFT + (BOARD_COLS - 1) * BOARD_CELL + BOARD_CELL / 2;
    private static final double BOARD_HOLE_MAX_Y = BOARD_GRID_TOP + (BOARD_ROWS - 1) * BOARD_CELL + BOARD_CELL / 2;

    private static final double BOARD_VIEW_WIDTH = 1000;
    private static final double BOARD_VIEW_HEIGHT = 750;

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
        gc.setFill(Color.rgb(245, 245, 220));
        gc.fillRect(0, 0, width, height);
        gc.setStroke(Color.rgb(139, 69, 19));
        gc.setLineWidth(3);
        gc.strokeRect(5, 5, width - 10, height - 10);
        gc.setFill(Color.WHITE);
        gc.fillRect(20, 20, width - 40, height - 40);
        gc.setStroke(Color.rgb(200, 200, 200));
        gc.setLineWidth(1);
        double startX = BOARD_GRID_LEFT;
        double startY = BOARD_GRID_TOP;
        double cellSize = BOARD_CELL;
        int cols = BOARD_COLS;
        int rows = BOARD_ROWS;
        for (int i = 0; i <= cols; i++) {
            double x = startX + i * cellSize;
            gc.strokeLine(x, startY, x, startY + rows * cellSize);
        }
        for (int j = 0; j <= rows; j++) {
            double y = startY + j * cellSize;
            gc.strokeLine(startX, y, startX + cols * cellSize, y);
        }
        gc.setFill(Color.rgb(220, 220, 220));
        for (int i = 0; i < cols; i++) {
            for (int j = 0; j < rows; j++) {
                double x = BOARD_HOLE_MIN_X + i * cellSize;
                double y = BOARD_HOLE_MIN_Y + j * cellSize;
                gc.fillOval(x - 3, y - 3, 6, 6);
                gc.setStroke(Color.rgb(180, 180, 180));
                gc.setLineWidth(1);
                gc.strokeOval(x - 3, y - 3, 6, 6);
            }
        }
        gc.setFill(Color.rgb(100, 100, 100));
        gc.setFont(javafx.scene.text.Font.font("Arial", 14));
        gc.fillText("МАКЕТНАЯ ПЛАТА", 20, height - 10);
    }

    private void setupComponentPane() {
        componentPane.getChildren().clear();
        wirePane.getChildren().clear();
        
        // Сначала рисуем провода
        for (Wire wire : wires) {
            wire.updateLine();
            javafx.scene.shape.Line line = wire.getLine();
            if (line != null) {
                line.setMouseTransparent(!isDeleting);
                line.setPickOnBounds(true);
                line.setStrokeWidth(3);
                wirePane.getChildren().add(line);
            }
        }
        
        for (Component comp : components) {
            javafx.scene.shape.Shape shape = comp.getShape();
            if (shape != null) {
                if (shape instanceof javafx.scene.shape.Circle) {
                    javafx.scene.shape.Circle circle = (javafx.scene.shape.Circle) shape;
                    circle.setCenterX(comp.getX());
                    circle.setCenterY(comp.getY());
                    } else if (shape instanceof javafx.scene.shape.Rectangle) {
                    javafx.scene.shape.Rectangle rect = (javafx.scene.shape.Rectangle) shape;
                    if (comp instanceof ArduinoUNO) {
                        rect.setX(comp.getX() - 50);
                        rect.setY(comp.getY() - 25);
                    } else if (comp instanceof Timer) {
                        rect.setX(comp.getX() - 35);
                        rect.setY(comp.getY() - 15);
                    } else if (comp instanceof Resistor) {
                        rect.setX(comp.getX() - 30);
                        rect.setY(comp.getY() - 10);
                    } else {
                        rect.setX(comp.getX() - 25);
                        rect.setY(comp.getY() - 12);
                    }
                }
                shape.setVisible(true);
                shape.setMouseTransparent(false);
                shape.setPickOnBounds(true);
                shape.setOnMouseClicked(null);
                shape.setOnMouseClicked(e -> {
                    e.consume();
                    handleComponentInteraction(comp, e);
                });
                componentPane.getChildren().add(shape);
            }
        }
        componentPane.setVisible(true);
        componentPane.setMouseTransparent(false);
        componentPane.toFront();
    }

    @FXML
    private void onBreadboardMouseMoved(MouseEvent event) {
        if (selectedComponentType != null && !isConnecting && !isDeleting) {
            double x = event.getX();
            double y = event.getY();
            double snappedX = Math.round((x - BOARD_HOLE_MIN_X) / BOARD_CELL) * BOARD_CELL + BOARD_HOLE_MIN_X;
            double snappedY = Math.round((y - BOARD_HOLE_MIN_Y) / BOARD_CELL) * BOARD_CELL + BOARD_HOLE_MIN_Y;
            double[] bounds = getPlacementBoundsForType(selectedComponentType);
            snappedX = Math.max(bounds[0], Math.min(bounds[1], snappedX));
            snappedY = Math.max(bounds[2], Math.min(bounds[3], snappedY));
            if (snappedX >= bounds[0] && snappedX <= bounds[1] && snappedY >= bounds[2] && snappedY <= bounds[3]) {
                boolean canPlace = true;
                double rNew = getPlacementRadiusForType(selectedComponentType);
                for (Component c : components) {
                    double rExisting = getPlacementRadius(c);
                    double dx = snappedX - c.getX();
                    double dy = snappedY - c.getY();
                    double dist = Math.sqrt(dx * dx + dy * dy);
                    if (dist < rNew + rExisting + 10) {
                        canPlace = false;
                        break;
                    }
                }
                if (canPlace) {
                    showPreview(snappedX, snappedY);
                } else {
                    hidePreview();
                }
            } else {
                hidePreview();
            }
        } else {
            hidePreview();
        }
    }

    @FXML
    private void onBreadboardClick(MouseEvent event) {
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
        Component clicked = findComponentAt(x, y);
        if (clicked != null) {
            handleComponentInteraction(clicked, event);
            return;
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
            double rNew = getPlacementRadiusForType(selectedComponentType);
            for (Component c : components) {
                double rExisting = getPlacementRadius(c);
                double dx = snappedX - c.getX();
                double dy = snappedY - c.getY();
                double dist = Math.sqrt(dx * dx + dy * dy);
                if (dist < rNew + rExisting + 10) {
                    updateStatus("Слишком близко к другому компоненту. Выберите другое место.");
                    hidePreview();
                    return;
                }
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
                default:
                    newComponent = null;
                    break;
            }
            
            if (newComponent != null) {
                components.add(newComponent);
                currentCircuit.addComponent(newComponent);
                javafx.scene.shape.Shape shape = newComponent.getShape();
                if (shape != null) {
                    shape.setVisible(true);
                    shape.setMouseTransparent(false);
                    shape.setPickOnBounds(true);
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

    private void showPreview(double x, double y) {
        if (selectedComponentType == null) {
            hidePreview();
            return;
        }
        hidePreview();
        javafx.scene.shape.Shape shape = null;
        javafx.scene.paint.Color previewColor = javafx.scene.paint.Color.rgb(100, 200, 255, 0.5);
        
        switch (selectedComponentType) {
            case "LED":
                shape = new javafx.scene.shape.Circle(x, y, 18);
                break;
            case "Button":
                shape = new javafx.scene.shape.Rectangle(x - 25, y - 12, 50, 24);
                break;
            case "Resistor":
                shape = new javafx.scene.shape.Rectangle(x - 30, y - 10, 60, 20);
                break;
            case "ArduinoUNO":
                shape = new javafx.scene.shape.Rectangle(x - 50, y - 25, 100, 50);
                break;
            case "Timer":
                shape = new javafx.scene.shape.Rectangle(x - 35, y - 15, 70, 30);
                break;
        }
        
        if (shape != null) {
            shape.setFill(previewColor);
            shape.setStroke(javafx.scene.paint.Color.BLUE);
            shape.setStrokeWidth(2);
            if (shape instanceof javafx.scene.shape.Rectangle) {
                ((javafx.scene.shape.Rectangle) shape).getStrokeDashArray().addAll(5.0, 5.0);
            } else if (shape instanceof javafx.scene.shape.Circle) {
                ((javafx.scene.shape.Circle) shape).getStrokeDashArray().addAll(5.0, 5.0);
            }
            shape.setMouseTransparent(true);
            componentPane.getChildren().add(shape);
            previewShape = shape;
        }
    }

    private void hidePreview() {
        if (previewShape != null) {
            componentPane.getChildren().remove(previewShape);
            previewShape = null;
        }
    }
    
    private void handleComponentInteraction(Component clicked, MouseEvent event) {
        if (clicked == null) return;
        if (isDeleting) {
            deleteComponent(clicked);
            return;
        }
        
        if (isConnecting) {
            if (sourceComponent == null) {
                sourceComponent = clicked;
                clicked.setSelected(true);
                setupComponentPane();
                updateStatus("✓ Выбран первый компонент (" + clicked.getType() + "). Кликните по второму для соединения");
            } else if (sourceComponent != clicked) {
                if (hasWireBetween(sourceComponent, clicked)) {
                    sourceComponent.setSelected(false);
                    sourceComponent = null;
                    setupComponentPane();
                    updateStatus("Между этими компонентами уже проведён провод. Выберите первую точку заново.");
                    return;
                }
                Wire wire = new Wire(sourceComponent, clicked);
                wires.add(wire);
                currentCircuit.addWire(wire);
                sourceComponent.addConnection(clicked);
                sourceComponent.setSelected(false);
                setupComponentPane();
                updateStatus("✓ Компоненты соединены! Выберите следующий компонент для соединения или выключите режим");
                sourceComponent = null;
            } else {
                sourceComponent.setSelected(false);
                sourceComponent = null;
                setupComponentPane();
                updateStatus("Выбор отменен. Выберите первый компонент для соединения");
            }
        } else {
            if (clicked instanceof Button) {
                Button button = (Button) clicked;
                button.toggle();
                setupComponentPane();
                updateStatus("🔘 Кнопка " + (button.isPressed() ? "НАЖАТА" : "отпущена"));
                // Показываем справку при двойном клике
                if (clicked.isSelected() && !button.isPressed()) {
                    HelpWindow.showHelp("Button");
                }
            } else if (clicked instanceof LED) {
                LED led = (LED) clicked;
                String status = led.isBurned() ? "ПЕРЕГОРЕЛ (серый)" : (led.isOn() ? "горит" : "не горит");
                updateStatus("💡 LED " + status);
                // Показываем справку при двойном клике
                if (clicked.isSelected()) {
                    HelpWindow.showHelp("LED");
                }
                if (selectedComponent != null && selectedComponent != clicked) {
                    selectedComponent.setSelected(false);
                }
                clicked.setSelected(!clicked.isSelected());
                selectedComponent = clicked.isSelected() ? clicked : null;
                setupComponentPane();
            } else if (clicked instanceof Resistor) {
                Resistor resistor = (Resistor) clicked;
                if (selectedComponent != null && selectedComponent != clicked) {
                    selectedComponent.setSelected(false);
                }
                clicked.setSelected(!clicked.isSelected());
                selectedComponent = clicked.isSelected() ? clicked : null;
                setupComponentPane();
                updateStatus("⚡ Резистор выбран. Сопротивление: " + 
                    String.format("%.0f", resistor.getResistance()) + " Ом. " +
                    "Защищает LED от перегрузки!");
                if (clicked.isSelected()) {
                    HelpWindow.showHelp("Resistor");
                }
            } else if (clicked instanceof ArduinoUNO) {
                ArduinoUNO arduino = (ArduinoUNO) clicked;
                boolean wasSelected = clicked.isSelected();
                arduino.toggle();
                setupComponentPane();
                updateStatus(arduino.getStatusText());
                if (selectedComponent != null && selectedComponent != clicked) {
                    selectedComponent.setSelected(false);
                }
                if (wasSelected) {
                    ArduinoConfigWindow.showConfig(arduino, components, wires);
                }
                clicked.setSelected(!clicked.isSelected());
                selectedComponent = clicked.isSelected() ? clicked : null;
            } else if (clicked instanceof Timer) {
                Timer timer = (Timer) clicked;
                boolean wasSelected = clicked.isSelected();
                timer.toggle();
                if (timer.isActive() && !timer.isRunning() && isSimulating) {
                    timer.start();
                } else if (!timer.isActive()) {
                    timer.stop();
                }
                setupComponentPane();
                updateStatus(timer.getStatusText());
                if (selectedComponent != null && selectedComponent != clicked) {
                    selectedComponent.setSelected(false);
                }
                if (wasSelected) {
                    TimerConfigWindow.showConfig(timer);
                }
                clicked.setSelected(!clicked.isSelected());
                selectedComponent = clicked.isSelected() ? clicked : null;
            } else {
                if (selectedComponent != null && selectedComponent != clicked) {
                    selectedComponent.setSelected(false);
                }
                clicked.setSelected(!clicked.isSelected());
                selectedComponent = clicked.isSelected() ? clicked : null;
                setupComponentPane();
                updateStatus("⚡ " + clicked.getType() + " выбран");
            }
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
        if (a == null || b == null) return false;
        for (Wire wire : wires) {
            Component from = wire.getFrom();
            Component to = wire.getTo();
            if ((from == a && to == b) || (from == b && to == a)) return true;
        }
        return false;
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
    
    private void resetModeButtons() {
        btnConnect.setStyle("-fx-background-color: #9b59b6; -fx-text-fill: white;");
        btnDelete.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");
    }

    @FXML
    private void onConnectClick() {
        isConnecting = !isConnecting;
        isDeleting = false; // Выключаем режим удаления
        selectedComponentType = null;
        resetButtonStyles();
        hidePreview();
        if (isConnecting) {
            btnConnect.setStyle("-fx-background-color: #8e44ad; -fx-text-fill: white; -fx-font-weight: bold;");
            btnDelete.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");
            updateStatus("✓ Режим соединения ВКЛЮЧЕН. Кликайте по компонентам для соединения");
        } else {
            btnConnect.setStyle("-fx-background-color: #9b59b6; -fx-text-fill: white;");
            if (sourceComponent != null) {
                sourceComponent.setSelected(false);
                sourceComponent = null;
            }
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
            btnDelete.setStyle("-fx-background-color: #c0392b; -fx-text-fill: white; -fx-font-weight: bold;");
            btnConnect.setStyle("-fx-background-color: #9b59b6; -fx-text-fill: white;");
            updateStatus("✓ Режим удаления ВКЛЮЧЕН. Кликайте по компонентам или проводам для удаления");
        } else {
            btnDelete.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");
            if (selectedComponent != null) {
                selectedComponent.setSelected(false);
                selectedComponent = null;
            }
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
                if (wire.getFrom() == comp || wire.getTo() == comp) {
                    wires.remove(i);
                    currentCircuit.removeWire(wire);
                }
            }
            components.remove(comp);
            currentCircuit.removeComponent(comp);
            if (selectedComponent == comp) selectedComponent = null;
            if (sourceComponent == comp) sourceComponent = null;
            setupComponentPane();
            updateStatus("✓ Компонент " + comp.getType() + " удален");
        }
    }

    @FXML
    private void onSimulateClick() {
        if (isSimulating) {
            isSimulating = false;
            btnSimulate.setText("Запустить симуляцию");
            btnSimulate.setStyle("");
            updateStatus("Симуляция остановлена");
            stopSimulation();
            return;
        }
        String validationError = validateCircuitForSimulation();
        if (validationError != null) {
            showAlert("Схема не будет работать", validationError);
            updateStatus(validationError);
            return;
        }
        isSimulating = true;
        btnSimulate.setText("Остановить симуляцию");
        btnSimulate.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold;");
        // Выключаем режимы «Соединить» и «Удалить» при запуске симуляции
        isConnecting = false;
        isDeleting = false;
        sourceComponent = null;
        if (selectedComponent != null) {
            selectedComponent.setSelected(false);
            selectedComponent = null;
        }
        resetModeButtons();
        btnConnect.setStyle("-fx-background-color: #9b59b6; -fx-text-fill: white;");
        btnDelete.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");
        updateStatus("Симуляция запущена. Кликайте по кнопкам для проверки схемы!");
        startSimulation();
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
        boolean hasSource = false;
        boolean hasLed = false;
        for (Component comp : components) {
            if (comp instanceof Button || comp instanceof Timer) {
                hasSource = true;
            }
            if (comp instanceof ArduinoUNO) {
                hasSource = true;
            }
            if (comp instanceof LED) {
                hasLed = true;
            }
        }
        if (!hasSource) {
            return "Эта схема работать не будет. Нужен источник сигнала: кнопка, таймер или Arduino.";
        }
        if (!hasLed) {
            return "Эта схема работать не будет. Добавьте нагрузку (например, LED).";
        }
        // Есть ли путь от источника к LED по проводам?
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
                if (from instanceof LED) continue; // от LED не распространяем
                if (reachableFromSources.contains(from) && !reachableFromSources.contains(to)) {
                    reachableFromSources.add(to);
                    changed = true;
                }
                if (to instanceof LED) continue; // к LED от источника — ок, обратно не идём
                if (reachableFromSources.contains(to) && !reachableFromSources.contains(from)) {
                    reachableFromSources.add(from);
                    changed = true;
                }
            }
        }
        for (Component comp : components) {
            if (comp instanceof LED && reachableFromSources.contains(comp)) {
                return null; // есть путь от источника до LED — схема может работать
            }
        }
        return "Эта схема работать не будет. Соедините источник сигнала (кнопка, таймер или Arduino) с LED проводами.";
    }

    @FXML
    private void onLoginClick() {
        // Вход по уже введённым email и паролю
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

        try {
            FirebaseAuthService.AuthResult authResult =
                firebaseAuthService.login(email, password);

            currentUserId = authResult.getUserId();
            currentIdToken = authResult.getIdToken();
            currentUserEmail = authResult.getEmail();

            updateAuthUi();
            loadCircuitList();
            updateStatus("Выполнен вход как " + currentUserEmail);
        } catch (Exception e) {
            showAlert("Ошибка входа", "Не удалось выполнить вход: " + e.getMessage());
            updateStatus("Ошибка входа: " + e.getMessage());
        }
    }

    @FXML
    private void onRegisterClick() {
        // Регистрация по уже введённым email и паролю
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

        try {
            FirebaseAuthService.AuthResult authResult =
                firebaseAuthService.register(email, password);

            currentUserId = authResult.getUserId();
            currentIdToken = authResult.getIdToken();
            currentUserEmail = authResult.getEmail();

            updateAuthUi();
            loadCircuitList();
            updateStatus("Аккаунт создан и выполнен вход как " + currentUserEmail);
            offerDemoCircuitAfterRegister();
        } catch (Exception e) {
            showAlert("Ошибка регистрации", "Не удалось создать аккаунт: " + e.getMessage());
            updateStatus("Ошибка регистрации: " + e.getMessage());
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
        // Запускаем все таймеры при старте симуляции
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
        if (target == null) return false;
        java.util.Queue<Component> queue = new java.util.LinkedList<>();
        java.util.Set<Component> visited = new java.util.HashSet<>();
        for (Component comp : components) {
            if (comp instanceof ArduinoUNO) {
                ArduinoUNO arduino = (ArduinoUNO) comp;
                if (arduino.isPowered()) {
                    queue.add(arduino);
                    visited.add(arduino);
                }
            }
        }
        while (!queue.isEmpty()) {
            Component current = queue.poll();
            if (current == target) return true;
            // Через таймер сигнал идёт только когда таймер выдаёт выход
            if (current instanceof Timer) {
                if (!((Timer) current).getOutput()) continue;
            }
            // Через кнопку сигнал идёт только когда кнопка нажата
            if (current instanceof Button) {
                if (!((Button) current).isPressed()) continue;
            }
            for (Wire wire : wires) {
                Component next = null;
                if (wire.getFrom() == current) next = wire.getTo();
                else if (wire.getTo() == current) next = wire.getFrom();
                if (next != null && !visited.contains(next)) {
                    visited.add(next);
                    queue.add(next);
                }
            }
            for (Component connected : current.getConnections()) {
                if (!visited.contains(connected)) {
                    visited.add(connected);
                    queue.add(connected);
                }
            }
        }
        return false;
    }

    private void checkOverload(Component source, String sourceName) {
        // Подсчитываем уникальные LED, подключённые к источнику (без двойного учёта wire+connection)
        java.util.Set<Component> ledsWithoutResistor = new java.util.HashSet<>();
        boolean hasResistor = false;
        
        for (Wire wire : wires) {
            if (wire.getFrom() == source) {
                if (wire.getTo() instanceof LED) {
                    ledsWithoutResistor.add(wire.getTo());
                } else if (wire.getTo() instanceof Resistor) {
                    for (Wire w2 : wires) {
                        if (w2.getFrom() == wire.getTo() && w2.getTo() instanceof LED) {
                            hasResistor = true;
                            break;
                        }
                    }
                }
            }
            if (wire.getTo() == source && wire.getFrom() instanceof LED) {
                ledsWithoutResistor.add(wire.getFrom());
            }
        }
        for (Component connected : source.getConnections()) {
            if (connected instanceof LED) {
                ledsWithoutResistor.add(connected);
            } else if (connected instanceof Resistor) {
                hasResistor = true;
            }
        }
        
        int ledCount = ledsWithoutResistor.size();
        if (ledCount > 3 && !hasResistor) {
            for (Component c : ledsWithoutResistor) {
                LED led = (LED) c;
                if (!led.isBurned()) {
                    led.burn();
                }
            }
            updateStatus("⚠️ ПЕРЕГРУЗКА! LED перегорели. Добавьте резистор для защиты.");
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
        
        // Проверяем перегрузку для всех источников сигнала
        for (Component comp : components) {
            if (comp instanceof Button) {
                Button button = (Button) comp;
                if (button.isPressed()) {
                    // Кнопка питает LED только если получает питание от Arduino (активный путь: через таймер только при его выходе)
                    if (!hasActivePathFromArduinoTo(button)) continue;
                    checkOverload(button, "кнопка");
                }
            } else if (comp instanceof Timer) {
                Timer timer = (Timer) comp;
                if (timer.isActive() && timer.isRunning() && timer.getOutputState()) {
                    if (!hasActivePathFromArduinoTo(timer)) continue;
                    checkOverload(timer, "таймер");
                }
            } else if (comp instanceof ArduinoUNO) {
                ArduinoUNO arduino = (ArduinoUNO) comp;
                if (arduino.isPowered() && arduino.getOutput()) {
                    checkOverload(arduino, "Arduino UNO");
                }
            }
        }
        
        // ПЕРВЫЙ ПРОХОД: Устанавливаем входные сигналы для всех компонентов
        // (резисторы, Arduino получают сигналы от источников)
        for (Wire wire : wires) {
            Component from = wire.getFrom();
            Component to = wire.getTo();
            
            // LED не может быть источником сигнала - пропускаем такие провода
            if (from instanceof LED) {
                continue;
            }
            
            boolean signal = from.getOutput();
            
            // Если сигнал проходит через резистор, он может быть ослаблен
            if (from instanceof Resistor) {
                Resistor resistor = (Resistor) from;
                signal = resistor.getInput(); // Берем входной сигнал резистора
                if (resistor.getResistance() > 5000) {
                    signal = false;
                }
            }
            
            // Устанавливаем входные сигналы для компонентов-приемников
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
            Component from = wire.getFrom();
            Component to = wire.getTo();
            
            // LED не может быть источником сигнала - пропускаем такие провода
            if (from instanceof LED) {
                wire.setActive(false);
                continue;
            }
            
            boolean signal = from.getOutput();
            
            // Цепь Arduino -> Кнопка -> LED (или через Таймер): активный путь — через таймер только при его выходе
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
                // Резистор передает сигнал только если получил входной сигнал
                signal = resistor.getInput();
                // Если сопротивление слишком большое, сигнал блокируется
                if (resistor.getResistance() > 5000) {
                    signal = false;
                }
            }
            
            wire.setActive(signal);
            
            if (to instanceof LED) {
                LED led = (LED) to;
                if (led.isBurned()) {
                    led.setInput(false); // Перегоревший LED не работает
                    continue;
                }
                
                // Несколько проводов могут вести к одному LED — объединяем сигналы по ИЛИ
                if (from instanceof Button || from instanceof Timer) {
                    led.setInput(led.getInput() || signal);
                } else if (from instanceof Resistor) {
                    Resistor r = (Resistor) from;
                    if (r.getResistance() > 0 && r.getResistance() < 10000) {
                        led.setInput(led.getInput() || signal);
                    }
                } else if (from instanceof ArduinoUNO) {
                    ArduinoUNO arduino = (ArduinoUNO) from;
                    if (arduino.isPowered()) {
                        led.setInput(led.getInput() || arduino.getOutput());
                    }
                } else {
                    led.setInput(led.getInput() || signal);
                }
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
                LED led = (LED) from;
                if (!led.isBurned()) led.setInput(led.getInput() || signal);
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
                        if (to instanceof LED) {
                            LED led = (LED) to;
                            if (!led.isBurned()) led.setInput(led.getInput() || output);
                        } else if (to instanceof Resistor) {
                            Resistor resistor = (Resistor) to;
                            resistor.setInput(output);
                        }
                        // Arduino обрабатывается отдельно в другом цикле
                    }
                }
                // Прямые соединения (Arduino -> Таймер -> LED)
                for (Component connected : comp.getConnections()) {
                    if (connected instanceof ArduinoUNO) continue;
                    if (connected instanceof LED) {
                        LED led = (LED) connected;
                        if (!led.isBurned()) led.setInput(led.getInput() || output);
                    } else if (connected instanceof Resistor) {
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
                            Component from = wire.getFrom();
                            boolean signal = from.getOutput();
                            
                            // Если сигнал проходит через резистор
                            if (from instanceof Resistor) {
                                Resistor resistor = (Resistor) from;
                                if (resistor.getResistance() > 5000) {
                                    signal = false;
                                }
                            }
                            
                            // Если сигнал от таймера
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
                            if (to instanceof LED) {
                                LED led = (LED) to;
                                if (!led.isBurned()) led.setInput(led.getInput() || output);
                            } else if (to instanceof Resistor) {
                                Resistor resistor = (Resistor) to;
                                resistor.setInput(output);
                            }
                        }
                    }
                    // Прямые соединения (выходы)
                    for (Component connected : comp.getConnections()) {
                        if (connected instanceof LED) {
                            LED led = (LED) connected;
                            if (!led.isBurned()) led.setInput(led.getInput() || output);
                        } else if (connected instanceof Resistor) {
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
                    if (connected instanceof LED) {
                        LED led = (LED) connected;
                        if (!led.isBurned()) led.setInput(led.getInput() || output);
                    } else if (connected instanceof Resistor) {
                        Resistor resistor = (Resistor) connected;
                        resistor.setInput(output);
                    }
                }
            }
        }
        
        // Резистор передаёт сигнал на подключённые LED (по проводам и по connections) — цепочка Кнопка–Резистор–LED работает в любом порядке создания
        for (Component comp : components) {
            if (comp instanceof Resistor) {
                Resistor resistor = (Resistor) comp;
                boolean sig = resistor.getInput();
                for (Component connected : comp.getConnections()) {
                    if (connected instanceof LED) {
                        LED led = (LED) connected;
                        if (!led.isBurned()) led.setInput(led.getInput() || sig);
                    }
                }
            }
        }
        
        setupComponentPane();
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
        
        try {
            firebaseService.saveCircuit(currentCircuit, currentUserId, currentIdToken);
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

                String name = obj.get("name").getAsString();

                components.clear();
                wires.clear();
                componentPane.getChildren().clear();
                wirePane.getChildren().clear();
                selectedComponent = null;
                selectedComponentType = null;
                sourceComponent = null;
                currentCircuit = new Circuit(name);
                
                // Парсим данные компонентов
                if (obj.has("components") && !obj.get("components").isJsonNull()) {
                    com.google.gson.JsonObject componentsData = obj.get("components").getAsJsonObject();
                    
                    if (componentsData.has("components")) {
                        com.google.gson.JsonArray compsArray = componentsData.get("components").getAsJsonArray();
                        
                        for (int i = 0; i < compsArray.size(); i++) {
                            com.google.gson.JsonObject compObj = compsArray.get(i).getAsJsonObject();
                            if (!compObj.has("type") || !compObj.has("x") || !compObj.has("y")) continue;
                            String type = compObj.get("type").getAsString();
                            double x = compObj.get("x").getAsDouble();
                            double y = compObj.get("y").getAsDouble();
                            Component comp = null;
                            switch (type) {
                                case "LED":
                                    comp = new LED(x, y);
                                    if (compObj.has("state")) {
                                        ((LED) comp).setOn(compObj.get("state").getAsBoolean());
                                    }
                                    if (compObj.has("resistance") && compObj.get("resistance").getAsDouble() == 1) {
                                        ((LED) comp).burn();
                                    }
                                    break;
                                case "Button":
                                    comp = new Button(x, y);
                                    if (compObj.has("state")) {
                                        ((Button) comp).setPressed(compObj.get("state").getAsBoolean());
                                    }
                                    break;
                                case "Resistor":
                                    comp = new Resistor(x, y);
                                    if (compObj.has("resistance")) {
                                        ((Resistor) comp).setResistance(compObj.get("resistance").getAsDouble());
                                    }
                                    break;
                                case "ArduinoUNO":
                                    comp = new ArduinoUNO(x, y);
                                    if (compObj.has("state")) {
                                        ((ArduinoUNO) comp).setPowered(compObj.get("state").getAsBoolean());
                                    }
                                    break;
                                case "Timer":
                                    comp = new Timer(x, y);
                                    if (compObj.has("state")) {
                                        ((Timer) comp).setActive(compObj.get("state").getAsBoolean());
                                    }
                                    // Интервал сохраняется в resistance
                                    if (compObj.has("resistance")) {
                                        double interval = compObj.get("resistance").getAsDouble();
                                        ((Timer) comp).setInterval((long) interval);
                                    }
                                    break;
                            }
                            
                            if (comp != null) {
                                components.add(comp);
                                currentCircuit.addComponent(comp);
                                
                                // Добавляем обработчик кликов для загруженного компонента
                                javafx.scene.shape.Shape shape = comp.getShape();
                                if (shape != null) {
                                    shape.setVisible(true);
                                    shape.setMouseTransparent(false);
                                    shape.setPickOnBounds(true);
                                    
                                    final Component finalComp = comp;
                                    shape.setOnMouseClicked(e -> {
                                        e.consume();
                                        handleComponentInteraction(finalComp, e);
                                    });
                                }
                            }
                        }
                    }
                    
                    // Восстанавливаем провода
                    if (componentsData.has("wires")) {
                        com.google.gson.JsonArray wiresArray = componentsData.get("wires").getAsJsonArray();
                        
                        for (int i = 0; i < wiresArray.size(); i++) {
                            com.google.gson.JsonObject wireObj = wiresArray.get(i).getAsJsonObject();
                            if (!wireObj.has("fromIndex") || !wireObj.has("toIndex")) continue;
                            int fromIndex = wireObj.get("fromIndex").getAsInt();
                            int toIndex = wireObj.get("toIndex").getAsInt();
                            if (fromIndex >= 0 && toIndex >= 0 && fromIndex < components.size() && toIndex < components.size()) {
                                Component from = components.get(fromIndex);
                                Component to = components.get(toIndex);
                                Wire wire = new Wire(from, to);
                                wires.add(wire);
                                currentCircuit.addWire(wire);
                                from.addConnection(to);
                            }
                        }
                    }
                }
                
                setupComponentPane();
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
        selectedComponent = null;
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
        if (circuitList == null) return;
        try {
            if (!isAuthenticated()) {
                circuitList.getItems().clear();
                return;
            }

            List<Circuit> circuits = firebaseService.loadCircuits(currentUserId, currentIdToken);
            circuitList.getItems().clear();
            for (Circuit c : circuits) {
                circuitList.getItems().add(c.getName());
            }
        } catch (Exception e) {
            // Тихая ошибка при загрузке списка
        }
    }

    private void updateButtonStyle(javafx.scene.control.Button button) {
        resetButtonStyles();
        if (button != null) button.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
    }

    private void resetButtonStyles() {
        if (btnLED != null) btnLED.setStyle("");
        if (btnButton != null) btnButton.setStyle("");
        if (btnResistor != null) btnResistor.setStyle("");
        if (btnArduino != null) btnArduino.setStyle("");
        if (btnTimer != null) btnTimer.setStyle("");
    }

    private void offerDemoCircuitAfterRegister() {
        Alert offer = new Alert(Alert.AlertType.CONFIRMATION);
        offer.setTitle("Демо-схема");
        offer.setHeaderText("Загрузить демо-схему для знакомства?");
        offer.setContentText(
            "В демо-схеме 7 цепей, у каждой своя Arduino UNO:\n\n" +
            "1) UNO → Кнопка → LED — нажал кнопку, лампочка горит\n" +
            "2) UNO → Таймер → LED — мигание без кнопки\n" +
            "3) UNO → Кнопка → Таймер → LED — кнопка включает/выключает мигание\n" +
            "4) UNO → Кнопка → Резистор → LED — резистор защищает лампочку\n" +
            "5) UNO → Кнопка → 4 LED (без резистора) — перегрузка, лампочки перегорят\n" +
            "6) UNO → Кнопка → 1 резистор → 3 LED — один резистор на три лампы\n" +
            "7) UNO → LED — лампочка не загорится без кнопки (нужен источник сигнала)\n\n" +
            "Включите нужные UNO (клик), при необходимости таймеры и нажмите «Запустить симуляцию».");
        offer.getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);
        if (offer.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            loadDemoCircuit();
        }
    }

    private void loadDemoCircuit() {
        components.clear();
        wires.clear();
        componentPane.getChildren().clear();
        wirePane.getChildren().clear();
        currentCircuit = new Circuit("Демо-схема");
        if (circuitNameField != null) {
            circuitNameField.setText("Демо-схема");
        }
        selectedComponent = null;
        selectedComponentType = null;
        isConnecting = false;
        sourceComponent = null;
        resetButtonStyles();

        // Сетка: центр ячейки = 40 + 20*k (k = 0,1,2,...)
        double cx = 40;
        double cy = 40;
        double step = 20;

        // 1) UNO -> Кнопка -> LED (базовая цепь)
        ArduinoUNO uno1 = new ArduinoUNO(cx + 1 * step, cy + 2 * step);
        Button b1 = new Button(cx + 6 * step, cy + 2 * step);
        LED l1 = new LED(cx + 12 * step, cy + 2 * step);
        addDemoComponents(uno1, b1, l1);
        addDemoWire(uno1, b1);
        addDemoWire(b1, l1);

        // 2) UNO -> Таймер -> LED (мигание без кнопки)
        ArduinoUNO uno2 = new ArduinoUNO(cx + 1 * step, cy + 5 * step);
        Timer t2 = new Timer(cx + 6 * step, cy + 5 * step);
        LED l2 = new LED(cx + 12 * step, cy + 5 * step);
        addDemoComponents(uno2, t2, l2);
        addDemoWire(uno2, t2);
        addDemoWire(t2, l2);

        // 3) UNO -> Кнопка -> Таймер -> LED (кнопка включает/выключает мигание)
        ArduinoUNO uno3 = new ArduinoUNO(cx + 1 * step, cy + 8 * step);
        Button b3 = new Button(cx + 6 * step, cy + 8 * step);
        Timer t3 = new Timer(cx + 10 * step, cy + 8 * step);
        LED l3 = new LED(cx + 14 * step, cy + 8 * step);
        addDemoComponents(uno3, b3, t3, l3);
        addDemoWire(uno3, b3);
        addDemoWire(b3, t3);
        addDemoWire(t3, l3);

        // 4) UNO -> Кнопка -> Резистор -> LED (резистор защищает LED)
        ArduinoUNO uno4 = new ArduinoUNO(cx + 1 * step, cy + 11 * step);
        Button b4 = new Button(cx + 6 * step, cy + 11 * step);
        Resistor r4 = new Resistor(cx + 10 * step, cy + 11 * step);
        LED l4 = new LED(cx + 14 * step, cy + 11 * step);
        addDemoComponents(uno4, b4, r4, l4);
        addDemoWire(uno4, b4);
        addDemoWire(b4, r4);
        addDemoWire(r4, l4);

        // 5) Перегрузка: UNO -> Кнопка -> 4 LED без резистора
        ArduinoUNO uno5 = new ArduinoUNO(cx + 1 * step, cy + 14 * step);
        Button b5 = new Button(cx + 6 * step, cy + 14 * step);
        LED l5a = new LED(cx + 10 * step, cy + 14 * step);
        LED l5b = new LED(cx + 12 * step, cy + 14 * step);
        LED l5c = new LED(cx + 14 * step, cy + 14 * step);
        LED l5d = new LED(cx + 16 * step, cy + 14 * step);
        addDemoComponents(uno5, b5, l5a, l5b, l5c, l5d);
        addDemoWire(uno5, b5);
        addDemoWire(b5, l5a);
        addDemoWire(b5, l5b);
        addDemoWire(b5, l5c);
        addDemoWire(b5, l5d);

        // 6) UNO -> Кнопка -> 1 резистор -> 3 LED (больше расстояние между резистором и лампочками)
        ArduinoUNO uno6 = new ArduinoUNO(cx + 1 * step, cy + 18 * step);
        Button b6 = new Button(cx + 6 * step, cy + 18 * step);
        Resistor r6 = new Resistor(cx + 10 * step, cy + 18 * step);
        LED l6a = new LED(cx + 16 * step, cy + 18 * step);
        LED l6b = new LED(cx + 20 * step, cy + 18 * step);
        LED l6c = new LED(cx + 24 * step, cy + 18 * step);
        addDemoComponents(uno6, b6, r6, l6a, l6b, l6c);
        addDemoWire(uno6, b6);
        addDemoWire(b6, r6);
        addDemoWire(r6, l6a);
        addDemoWire(r6, l6b);
        addDemoWire(r6, l6c);

        // 7) UNO -> LED (лампочка не загорится без кнопки — в цепи нет источника сигнала)
        ArduinoUNO uno7 = new ArduinoUNO(cx + 1 * step, cy + 22 * step);
        LED l7 = new LED(cx + 6 * step, cy + 22 * step);
        addDemoComponents(uno7, l7);
        addDemoWire(uno7, l7);

        drawBreadboard();
        setupComponentPane();
        updateStatus("Демо-схема загружена. Включите нужные UNO (клик), таймеры при необходимости, затем «Запустить симуляцию». 1) Кнопка→LED 2) Таймер→LED 3) Кнопка→Таймер→LED 4) Кнопка→Резистор→LED 5) Перегрузка 6) 1 резистор на 3 LED 7) UNO→LED (не загорится без кнопки).");
    }

    private void addDemoComponents(Component... comps) {
        for (Component c : comps) {
            components.add(c);
            currentCircuit.addComponent(c);
        }
    }

    private void addDemoWire(Component from, Component to) {
        Wire w = new Wire(from, to);
        wires.add(w);
        currentCircuit.addWire(w);
        from.addConnection(to);
    }

    /** Радиус размещения для проверки наложения (половина размера компонента). */
    private double getPlacementRadius(Component c) {
        if (c instanceof LED) return 18;
        if (c instanceof ArduinoUNO) return 50;
        if (c instanceof Timer) return 35;
        if (c instanceof Resistor) return 30;
        if (c instanceof Button) return 25;
        return 25;
    }

    private double getPlacementRadiusForType(String type) {
        if (type == null) return 25;
        switch (type) {
            case "LED": return 18;
            case "ArduinoUNO": return 50;
            case "Timer": return 35;
            case "Resistor": return 30;
            case "Button": return 25;
            default: return 25;
        }
    }

    private double[] getPlacementBoundsForType(String type) {
        double halfW, halfH;
        if (type == null) {
            halfW = 25; halfH = 12;
        } else {
            switch (type) {
                case "LED": halfW = 18; halfH = 18; break;
                case "ArduinoUNO": halfW = 50; halfH = 25; break;
                case "Timer": halfW = 35; halfH = 15; break;
                case "Resistor": halfW = 30; halfH = 10; break;
                case "Button": halfW = 25; halfH = 12; break;
                default: halfW = 25; halfH = 12; break;
            }
        }
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
