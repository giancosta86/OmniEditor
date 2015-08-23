/*§
  ===========================================================================
  OmniEditor
  ===========================================================================
  Copyright (C) 2015 Gianluca Costa
  ===========================================================================
  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
  ===========================================================================
*/

package info.gianlucacosta.omnieditor;


import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Objects;
import java.util.Optional;


/**
 * Internally used by the IDE framework
 */
public class MainWindowController {
    private static final int outputRefreshRateInMilliseconds = 300;

    private Stage stage;
    private AppStrategy appStrategy;

    private StyledCodeEditor codeEditor;

    private Thread runningThread;


    private final ObjectProperty<Optional<File>> sourceFile = new SimpleObjectProperty<>(Optional.empty());

    public Optional<File> getSourceFile() {
        return sourceFile.get();
    }

    private void setSourceFile(Optional<File> value) {
        sourceFile.set(value);
    }

    public ObjectProperty<Optional<File>> sourceFileProperty() {
        return sourceFile;
    }


    private final BooleanProperty modified = new SimpleBooleanProperty(false);

    public boolean isModified() {
        return modified.get();
    }

    private void setModified(boolean value) {
        modified.set(value);
    }

    public BooleanProperty modifiedProperty() {
        return modified;
    }


    private final BooleanProperty running = new SimpleBooleanProperty(false);

    public boolean isRunning() {
        return running.get();
    }

    private void setRunning(boolean value) {
        running.set(value);
    }

    public BooleanProperty runningProperty() {
        return running;
    }


    private FileChooser sourceFileChooser;

    private FileChooser outputFileChooser;


    private Optional<File> latestSourceFile = Optional.empty();
    private Optional<File> latestOutputFile = Optional.empty();


    /*
     * GUI SETUP
     */
    public void init(Stage stage, AppStrategy appStrategy) {
        this.stage = stage;
        this.appStrategy = appStrategy;

        this.sourceFileChooser = appStrategy.createSourceFileChooser();
        this.codeEditor = appStrategy.createCodeEditor();

        editorPane.setCenter(codeEditor);

        stage.setOnCloseRequest(event -> {
            if (!canLeaveDocument()) {
                event.consume();
            }
        });

        createOutputFileChooser();

        initBindings();
    }


    private void createOutputFileChooser() {
        outputFileChooser = new FileChooser();

        outputFileChooser.getExtensionFilters().setAll(
                new FileChooser.ExtensionFilter("Text file", "*.txt"),
                new FileChooser.ExtensionFilter("Any file", "*.*")
        );

        outputFileChooser.setTitle(appStrategy.getAppTitle());
    }


    private void initBindings() {
        stage.titleProperty().bind(
                Bindings.createStringBinding(
                        () -> String.format("%s%s%s",
                                appStrategy.getAppTitle(),

                                getSourceFile().map(
                                        file -> " - " + file.getName()
                                ).orElseGet(
                                        () -> ""
                                ),

                                (isModified() ? " *" : "")
                        ),

                        sourceFile,
                        modified
                )
        );

        setupMenusAndToolbar();

        codeEditor.disableProperty().bind(running);

        codeEditor.textProperty().addListener(
                text -> setModified(true)
        );


        sourceFile.addListener(value -> {
            codeEditor.getUndoManager().forgetHistory();
            codeEditor.requestFocus();
        });
    }


    private void setupMenusAndToolbar() {
        newMenuItem.disableProperty().bind(running);
        bindButton(newButton, newMenuItem);

        openMenuItem.disableProperty().bind(running);
        bindButton(openButton, openMenuItem);

        saveMenuItem.disableProperty().bind(
                Bindings.or(
                        running,
                        modified.not()
                )
        );
        bindButton(saveButton, saveMenuItem);

        saveAsMenuItem.disableProperty().bind(running);
        bindButton(saveAsButton, saveAsMenuItem);


        undoMenuItem.disableProperty().bind(
                Bindings.or(
                        running,
                        Bindings.not(codeEditor.undoAvailableProperty())
                )
        );
        bindButton(undoButton, undoMenuItem);

        redoMenuItem.disableProperty().bind(
                Bindings.or(
                        running,
                        Bindings.not(codeEditor.redoAvailableProperty()
                        )
                )
        );
        bindButton(redoButton, redoMenuItem);


        BooleanBinding noSelectedCodeText =
                Bindings.createBooleanBinding(
                        () -> codeEditor.getSelectedText().isEmpty(),
                        codeEditor.selectedTextProperty()
                );

        cutMenuItem.disableProperty().bind(
                Bindings.or(
                        noSelectedCodeText,
                        running
                )
        );
        bindButton(cutButton, cutMenuItem);

        copyMenuItem.disableProperty().bind(
                Bindings.or(
                        noSelectedCodeText,
                        running
                )
        );
        bindButton(copyButton, copyMenuItem);

        pasteMenuItem.disableProperty().bind(running);
        bindButton(pasteButton, pasteMenuItem);


        startMenuItem.disableProperty().bind(running);
        bindButton(startButton, startMenuItem);

        stopMenuItem.disableProperty().bind(running.not());
        bindButton(stopButton, stopMenuItem);

        settingsMenuItem.setVisible(appStrategy.isShowSettings());
        settingsMenuItem.disableProperty().bind(running);

        aboutMenuItem.disableProperty().bind(running);
        bindButton(aboutButton, aboutMenuItem);
    }


    private void bindButton(Button button, MenuItem menuItem) {
        String buttonId = button.getId();

        String actionName = buttonId.substring(0, buttonId.lastIndexOf("Button"));
        String expectedMenuItemId = actionName + "MenuItem";

        if (!Objects.equals(menuItem.getId(), expectedMenuItemId)) {
            throw new IllegalArgumentException(
                    String.format("'%s' should be named '%s' instead",
                            menuItem.getId(),
                            expectedMenuItemId
                    )
            );
        }


        Image actionImage = new Image(getClass().getResourceAsStream(
                String.format("actionIcons/%s.png", actionName)
        ));

        menuItem.setGraphic(new ImageView(actionImage));
        button.setGraphic(new ImageView(actionImage));


        button.disableProperty().bind(menuItem.disableProperty());
        button.setOnAction(menuItem.getOnAction());

        Tooltip tooltip = new Tooltip(menuItem.getText());
        button.setTooltip(tooltip);
    }


    /*
     * ACTIONS
     */
    public void newDocument() {
        if (!canLeaveDocument()) {
            return;
        }

        codeEditor.clear();
        setSourceFile(Optional.empty());

        setModified(false);
        codeEditor.getUndoManager().forgetHistory();
        codeEditor.requestFocus();
    }


    public void openDocument() {
        if (!canLeaveDocument()) {
            return;
        }


        if (latestSourceFile.isPresent()) {
            sourceFileChooser.setInitialDirectory(latestSourceFile.get().getParentFile());
        }


        File selectedFile = sourceFileChooser.showOpenDialog(stage);
        if (selectedFile != null) {
            try {
                String fileContent = new String(Files.readAllBytes(selectedFile.toPath()));
                codeEditor.setText(fileContent);

                setSourceFile(Optional.of(selectedFile));
                latestSourceFile = Optional.of(selectedFile);

                setModified(false);
            } catch (Exception ex) {
                showErrorBox(ex);
            }
        }
    }


    public boolean saveDocument() {
        if (!getSourceFile().isPresent()) {
            return saveAsDocument();
        }


        return doActualSaving(getSourceFile().get());
    }


    public boolean saveAsDocument() {
        if (latestSourceFile.isPresent()) {
            sourceFileChooser.setInitialDirectory(latestSourceFile.get().getParentFile());
        }

        File selectedFile = sourceFileChooser.showSaveDialog(stage);
        if (selectedFile == null) {
            return false;
        }

        File actualFile = appStrategy.getSavedSourceFile(sourceFileChooser, selectedFile);


        latestSourceFile = Optional.of(actualFile);

        if (!doActualSaving(actualFile)) {
            return false;
        }

        setSourceFile(Optional.of(actualFile));
        return true;
    }


    private boolean doActualSaving(File targetFile) {
        try {
            Files.write(targetFile.toPath(), codeEditor.getText().getBytes());
            setModified(false);
            return true;
        } catch (Exception ex) {
            showErrorBox(ex);
            return false;
        }
    }


    private void showErrorBox(Exception ex) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(appStrategy.getAppTitle());
        alert.setHeaderText("An error occurred");
        alert.setContentText(ex.getMessage());

        alert.showAndWait();
    }


    public void exitProgram() {
        stage.close();
    }


    private boolean canLeaveDocument() {
        if (!isModified()) {
            return true;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(appStrategy.getAppTitle());
        alert.setHeaderText("Do you wish to save the source file?");

        ButtonType yesButton = new ButtonType("Yes");
        ButtonType noButton = new ButtonType("No");
        ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(yesButton, noButton, cancelButton);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.get() == yesButton) {
            return saveDocument();
        } else if (result.get() == noButton) {
            return true;
        } else {
            return false;
        }
    }


    public void undo() {
        codeEditor.undo();
    }


    public void redo() {
        codeEditor.redo();
    }


    public void cut() {
        codeEditor.cut();
    }


    public void copy() {
        codeEditor.copy();
    }

    public void paste() {
        codeEditor.paste();
    }

    public void selectAll() {
        codeEditor.selectAll();
    }


    public void saveOutput() {
        if (latestOutputFile.isPresent()) {
            outputFileChooser.setInitialDirectory(latestOutputFile.get().getParentFile());
        }

        File chosenFile = outputFileChooser.showSaveDialog(stage);

        if (chosenFile == null) {
            return;
        }

        File actualFile = chosenFile;
        FileChooser.ExtensionFilter defaultExtensionFilter = outputFileChooser.getExtensionFilters().get(0);
        if (outputFileChooser.getSelectedExtensionFilter() == defaultExtensionFilter) {
            String defaultExtension = defaultExtensionFilter.getExtensions().get(0).substring(1);

            if (!chosenFile.getName().endsWith(defaultExtension)) {
                actualFile = new File(chosenFile.getAbsolutePath() + defaultExtension);
            }
        }

        latestOutputFile = Optional.of(actualFile);
        try {
            Files.write(actualFile.toPath(), outputArea.getText().getBytes());
        } catch (IOException ex) {
            showErrorBox(ex);
        }
    }


    public void start() {
        outputArea.clear();
        setRunning(true);

        final AtomicStringBuffer outputBuffer = new AtomicStringBuffer();


        final Thread outputThread = new OutputThread(
                outputRefreshRateInMilliseconds,
                outputBuffer,
                outputString -> outputArea.appendText(outputString)
        );
        outputThread.setDaemon(true);
        outputThread.start();


        runningThread = new Thread(() -> {
            try {
                appStrategy.run(codeEditor.getText(), outputBuffer);
            } catch (InterruptedException ex) {
                //Just do nothing
            }


            outputThread.interrupt();
            try {
                outputThread.join();
            } catch (InterruptedException e) {
                //Just do nothing
            }

            setRunning(false);
        });

        runningThread.setDaemon(true);
        runningThread.start();
    }


    public void stop() {
        runningThread.interrupt();
    }


    public void showSettings() {
        appStrategy.showSettings();
    }


    public void showAboutWindow() {
        appStrategy.showAboutWindow();
    }


    @FXML
    private BorderPane editorPane;

    @FXML
    private MenuItem newMenuItem;

    @FXML
    private MenuItem openMenuItem;

    @FXML
    private MenuItem saveMenuItem;

    @FXML
    private MenuItem saveAsMenuItem;

    @FXML
    private MenuItem undoMenuItem;

    @FXML
    private MenuItem redoMenuItem;


    @FXML
    private MenuItem cutMenuItem;

    @FXML
    private MenuItem copyMenuItem;

    @FXML
    private MenuItem pasteMenuItem;

    @FXML
    private MenuItem saveOutputMenuItem;

    @FXML
    private MenuItem startMenuItem;

    @FXML
    private MenuItem stopMenuItem;

    @FXML
    private MenuItem settingsMenuItem;

    @FXML
    private MenuItem aboutMenuItem;


    @FXML
    private Button newButton;

    @FXML
    private Button openButton;

    @FXML
    private Button saveButton;

    @FXML
    private Button saveAsButton;


    @FXML
    private Button undoButton;

    @FXML
    private Button redoButton;

    @FXML
    private Button cutButton;

    @FXML
    private Button copyButton;

    @FXML
    private Button pasteButton;


    @FXML
    private Button startButton;

    @FXML
    private Button stopButton;


    @FXML
    private Button aboutButton;


    @FXML
    private TextArea outputArea;
}
