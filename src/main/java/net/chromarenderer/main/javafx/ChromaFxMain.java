package net.chromarenderer.main.javafx;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;
import javafx.stage.Screen;
import javafx.stage.Stage;
import net.chromarenderer.main.Chroma;
import net.chromarenderer.main.ChromaSettings;
import net.chromarenderer.math.ImmutableVector3;
import net.chromarenderer.math.Vector3;
import net.chromarenderer.renderer.ChromaRenderMode;
import net.chromarenderer.renderer.scene.SceneType;
import net.chromarenderer.renderer.scene.acc.AccStructType;
import net.chromarenderer.utils.BufferPressedKeysEventHandler;

import java.io.File;
import java.nio.file.Path;
import java.util.Set;

public class ChromaFxMain extends Application {

    //-XX:+UnlockDiagnosticVMOptions -XX:+PrintAssembly -XX:+LogCompilation

    private static Chroma chroma;
    private ChromaSettings settings;

    private ChromaFxPreviewWindow previewStage;
    private ChromaFxStatusWindow statisticsStage;
    private AnimationTimer cameraAnimationTimer;


    @Override
    public void start(final Stage chromaMainStage) throws Exception {

        BorderPane mainBox = new BorderPane();
        GridPane controlPane = new GridPane();
        controlPane.setHgap(10);
        controlPane.setVgap(10);
        controlPane.setPadding(new Insets(10));

        final boolean[] recreatePreview = {false};
        int rowIdx = 0;

        Button applySettings = new Button("Apply Settings");
        Button start = new Button("Start");
        Button stop = new Button("Stop");
        Button screenShot = new Button("Save Image");
        HBox buttonPane = new HBox(10, applySettings, start, screenShot, stop);
        buttonPane.setPadding(new Insets(5));

        // **** Scene import row ****
        final Text sceneNameLabel = new Text("[SCENE_NAME]");
        Button sceneDialogButton = new Button("Select Scene");
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Chroma Scenes");
        chooser.setInitialDirectory(new File("./"));

        final File[] selectedDirectory = new File[1];
        sceneDialogButton.setOnAction(event -> {
            selectedDirectory[0] = chooser.showDialog(chromaMainStage);
            if (selectedDirectory[0] != null) {
                final String sceneName = selectedDirectory[0].toPath().getFileName().toString();
                final String[] blendFiles = selectedDirectory[0].list((dir, name) -> (sceneName + ".blend").equals(name));
                if (blendFiles.length == 1) {
                    sceneNameLabel.setText(sceneName);
                    // TODO: improve by doing further checks and a potential 'blender --background ./cornellv02.blend --python ../../chroma.py' if exported files do not exist yet.
                } else {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Scene Import Error");
                    alert.setHeaderText("Error");
                    alert.setContentText("The selected directory does not contain any scene data!");
                    alert.showAndWait();
                }
            }
        });

        final ComboBox<SceneType> sceneType =  new ComboBox<>(FXCollections.observableArrayList(SceneType.values()));
        sceneType.valueProperty().addListener((observable, oldValue, newValue) -> {
            if(SceneType.BLENDER_EXPORT.equals(newValue)){
                sceneDialogButton.setVisible(true);
            } else {
                sceneDialogButton.setVisible(false);
            }
        });

        sceneType.valueProperty().setValue(SceneType.CORNELL_BOX);
        controlPane.addRow(rowIdx++, new Text("Scene"), new HBox(10, sceneType, sceneDialogButton), sceneNameLabel);


        // **** Acc Struct Row ****
        controlPane.add(new Text("Acc Struct:"), 0, rowIdx);
        ComboBox<AccStructType> accStructCombo = new ComboBox<>(FXCollections.observableArrayList(AccStructType.values()));

        controlPane.add(accStructCombo, 1, rowIdx++);
        accStructCombo.setValue(AccStructType.AABB_BVH);

        controlPane.add(new Text("Render Mode:"), 0, rowIdx);
        ComboBox<ChromaRenderMode> renderModeCombo = new ComboBox<>(FXCollections.observableArrayList(ChromaRenderMode.values()));
        controlPane.add(renderModeCombo, 1, rowIdx++);
        renderModeCombo.setValue(ChromaRenderMode.MT_PTDL);

        controlPane.add(new Text("Resolution:"), 0, rowIdx);
        ComboBox<String> resolutionCombo = new ComboBox<>(FXCollections.observableArrayList(
                "256x256", "512x512", "768x768", "1024x1024"
        ));
        controlPane.add(resolutionCombo, 1, rowIdx++);
        resolutionCombo.setValue("512x512");
        resolutionCombo.valueProperty().addListener((ov, oldValue, newValue) -> {
            if (!oldValue.equals(newValue)) {
                recreatePreview[0] = true;
            }
        });

        controlPane.add(new Text("Multi Threading:"), 0, rowIdx);
        CheckBox parallelize = new CheckBox();
        controlPane.add(parallelize, 1, rowIdx++);
        parallelize.selectedProperty().setValue(true);

        controlPane.add(new Text("DL Estimation:"), 0, rowIdx);
        CheckBox directLightEstimation = new CheckBox();
        controlPane.add(directLightEstimation, 1, rowIdx++);
        directLightEstimation.selectedProperty().setValue(true);


        start.setOnAction(event -> {
            controlPane.setDisable(true);
            statisticsStage.start();
            chroma.start();
            cameraAnimationTimer.start();
            previewStage.start();
            applySettings.setDisable(true);
        });

        applySettings.setOnAction(event -> {
            start.setDisable(true);
            String[] split = resolutionCombo.getValue().split("x");
            final int imgWidth = Integer.parseInt(split[0]);
            final int height = Integer.parseInt(split[1]);
            final Path scenePath = selectedDirectory[0] != null ? selectedDirectory[0].toPath() : null;

            settings = new ChromaSettings(
                    parallelize.selectedProperty().getValue(),
                    imgWidth, height,
                    renderModeCombo.getValue(),
                    directLightEstimation.selectedProperty().getValue(),
                    accStructCombo.getValue(),
                    sceneType.getValue(),
                    scenePath);

            chroma.initialize(settings);

            if (recreatePreview[0]) {
                previewStage.close();
                previewStage = new ChromaFxPreviewWindow(chroma, imgWidth, height).init();
                previewStage.initOwner(chromaMainStage);
                previewStage.show();
                recreatePreview[0] = false;
            }
            start.setDisable(false);
        });

        screenShot.setOnAction(event -> chroma.takeScreenShot());
        stop.setOnAction(event -> {
            fullStop();
            controlPane.setDisable(false);
            applySettings.setDisable(false);
        });

        mainBox.setCenter(controlPane);
        mainBox.setBottom(buttonPane);

        chromaMainStage.setX(0);
        chromaMainStage.setX(0);
        chromaMainStage.setTitle("Chroma Renderer");
        Scene scene = new Scene(mainBox, 600, 400);
        chromaMainStage.setScene(scene);
        BufferPressedKeysEventHandler bufferPressedKeysEventHandler = new BufferPressedKeysEventHandler();
        chromaMainStage.addEventHandler(KeyEvent.ANY, bufferPressedKeysEventHandler);
        chromaMainStage.addEventHandler(KeyEvent.KEY_RELEASED, getKeyTypedEventHandler());

        String[] split = resolutionCombo.getValue().split("x");
        previewStage = new ChromaFxPreviewWindow(chroma, Integer.parseInt(split[0]), Integer.parseInt(split[1])).init();
        statisticsStage = new ChromaFxStatusWindow(chroma).init();

        previewStage.initOwner(chromaMainStage);
        statisticsStage.initOwner(chromaMainStage);

        MenuBar menuBar = new MenuBar();
        menuBar.setUseSystemMenuBar(true);
        final Menu windows = new Menu("Windows");
        final Menu menu3 = new Menu("Help");
        mainBox.setTop(menuBar);

        menuBar.getMenus().addAll(windows, menu3);
        MenuItem showPreview = new MenuItem("Preview");
        MenuItem showStatistics = new MenuItem("Statistics");
        windows.getItems().addAll(showPreview, showStatistics);
        showPreview.setOnAction(event -> previewStage.show());
        showStatistics.setOnAction(event -> statisticsStage.show());

        chromaMainStage.setOnCloseRequest((arg0 -> {
            arg0.consume();
            fullStop();
            statisticsStage.close();
            previewStage.close();
            chromaMainStage.close();
        }));

        cameraAnimationTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                Set<KeyCode> pressedKeys = bufferPressedKeysEventHandler.getPressedKeys();
                final Vector3 translation = getTranslationVector(pressedKeys);
                final Vector3 rotation = getRotationVector(pressedKeys);
                if (translation.nonZero() || rotation.nonZero()) {
                    chroma.getCamera().move(translation, rotation);
                }
            }
        };

        chromaMainStage.show();
        previewStage.show();
        statisticsStage.show();

        Rectangle2D visualBounds = Screen.getPrimary().getVisualBounds();
        statisticsStage.setX(visualBounds.getMaxX() - statisticsStage.getWidth());
        statisticsStage.setY(visualBounds.getMaxY() - statisticsStage.getHeight());

        chromaMainStage.toFront();
    }


    private void fullStop() {
        chroma.stop();
        statisticsStage.stop();
        previewStage.stop();
        cameraAnimationTimer.stop();
    }


    private Vector3 getRotationVector(Set<KeyCode> pressedKeys) {
        float rotX = 0.0f;
        float rotY = 0.0f;
        //float rotZ = 0.0f;

        boolean flushImage = false;

        for (KeyCode pressedKey : pressedKeys) {
            switch (pressedKey) {
                case NUMPAD7:
                    rotY -= 0.02f;
                    flushImage = true;
                    break;
                case NUMPAD9:
                    rotY += 0.02f;
                    flushImage = true;
                    break;
                case NUMPAD1:
                    rotX -= 0.02f;
                    flushImage = true;
                    break;
                case NUMPAD3:
                    rotX += 0.02f;
                    flushImage = true;
                    break;
            }

            if (flushImage) {
                chroma.flushOnNextImage();
            }
        }

        return new ImmutableVector3(rotX, rotY, 0.0f);
    }


    private Vector3 getTranslationVector(Set<KeyCode> pressedKeys) {

        float moveX = 0.0f;
        float moveY = 0.0f;
        float moveZ = 0.0f;
        boolean flushImage = false;

        // camera along negative z axis!
        for (KeyCode pressedKey : pressedKeys) {
            switch (pressedKey) {
                case NUMPAD4:
                case A:
                    moveX -= 0.1f;
                    flushImage = true;
                    break;
                case NUMPAD6:
                case D:
                    moveX += 0.1f;
                    flushImage = true;
                    break;
                case NUMPAD8:
                case W:
                    moveZ -= 0.1f;
                    flushImage = true;
                    break;
                case NUMPAD2:
                case S:
                    moveZ += 0.1f;
                    flushImage = true;
                    break;
                case PAGE_DOWN:
                case Q:
                    moveY -= 0.1f;
                    flushImage = true;
                    break;
                case PAGE_UP:
                case E:
                    moveY += 0.1f;
                    flushImage = true;
                    break;
            }

            if (flushImage) {
                chroma.flushOnNextImage();
            }
        }

        return new ImmutableVector3(moveX, moveY, moveZ);
    }


    private EventHandler<KeyEvent> getKeyTypedEventHandler() {
        return event -> {
            switch (event.getCode()) {
                case R:
                    chroma.getCamera().resetToInitial();
                    chroma.flushOnNextImage();
                    break;
                case L:
                    settings.toggleL1Computation();
            }
        };
    }


    public static void main(String[] args) {
        chroma = new Chroma();
        Thread thread = new Thread(chroma);
        thread.start();

        launch(ChromaFxMain.class, args);

        thread.interrupt();
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
