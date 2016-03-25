package com.afterwhy;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import org.mozilla.universalchardet.UniversalDetector;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author d.karasev
 */
public class MainView extends VBox {
    private static ObservableList<Charset> supportedEncodings = FXCollections.observableArrayList(Charset.availableCharsets().values().stream().collect(Collectors.toList()));

    private Stage stage;

    private TextField textFieldFolderPath;
    private TextField textFieldMask;
    private ComboBox<Charset> comboBoxCharsets;
    private CheckBox checkBoxBackup;
    private Button buttonBrowse;
    private Button buttonProceed;

    private RadioButton radioButtonFolder;
    private RadioButton radioButtonFile;

    private Executor executor = Executors.newSingleThreadExecutor();

    public MainView(Stage stage) {
        this.stage = stage;

        setSpacing(10.0);
        setPadding(new Insets(20.0));
        setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                convert();
            }
        });
        getChildren().addAll(
                new HBox() {{
                    setSpacing(10.0);
                    setMaxWidth(Double.MAX_VALUE);
                    ToggleGroup toggleGroup = new ToggleGroup();
                    getChildren().addAll(
                            radioButtonFolder = new RadioButton("Folder") {{
                                setToggleGroup(toggleGroup);
                                setSelected(true);
                            }},
                            radioButtonFile = new RadioButton("File") {{
                                setToggleGroup(toggleGroup);
                                setSelected(false);
                            }}
                    );

                    toggleGroup.selectedToggleProperty().addListener((observable, oldValue, newValue) -> {
                        boolean folder = newValue == radioButtonFolder;
                        textFieldFolderPath.setText("");
                        textFieldFolderPath.setPromptText(folder ? "Choose folder..." : "Choose file...");
                        textFieldMask.setDisable(!folder);
                    });
                }},
                new HBox() {{
                    setSpacing(10.0);
                    setMaxWidth(Double.MAX_VALUE);
                    getChildren().addAll(
                            textFieldFolderPath = new TextField() {{
                                setPromptText("Choose folder...");
                                setMaxWidth(Double.MAX_VALUE);
                                setDisable(true);
                                setOpacity(1.0);
                                HBox.setHgrow(this, Priority.ALWAYS);
                            }},
                            buttonBrowse = new Button("...") {{
                                setOnAction(MainView.this::handleBrowse);
                                HBox.setHgrow(this, Priority.NEVER);
                            }}
                    );
                }},
                textFieldMask = new TextField() {{
                    setPromptText("Enter mask...");
                    setMaxWidth(Double.MAX_VALUE);
                    HBox.setHgrow(this, Priority.ALWAYS);
                }},
                new HBox() {{
                    setSpacing(10.0);
                    setMaxWidth(Double.MAX_VALUE);
                    getChildren().addAll(
                            new Label("Target encoding:"),
                            comboBoxCharsets = new ComboBox<Charset>(supportedEncodings) {{
                                setMaxWidth(Double.MAX_VALUE);
                                HBox.setHgrow(this, Priority.ALWAYS);
                                setConverter(new StringConverter<Charset>() {
                                    @Override
                                    public String toString(Charset object) {
                                        return object.displayName();
                                    }

                                    @Override
                                    public Charset fromString(String string) {
                                        return supportedEncodings.stream().filter(c -> c.displayName().equals(string)).findAny().orElse(null);
                                    }
                                });
                                getSelectionModel().select(supportedEncodings.stream().filter(c -> c.displayName().equals("IBM866")).findAny().orElse(null));
                            }}
                    );
                }},
                checkBoxBackup = new CheckBox("Backup") {{
                    setSelected(true);
                }},
                buttonProceed = new Button("Convert") {{
                    setOnAction(MainView.this::handleConvert);
                }}
        );
    }

    private void handleBrowse(ActionEvent event) {
        boolean chooseFolder = radioButtonFolder.isSelected();
        if (chooseFolder) {
            DirectoryChooser directoryChooser = new DirectoryChooser();
            directoryChooser.setTitle("Choose folder");
            File file = directoryChooser.showDialog(stage);

            if (file != null) {
                textFieldFolderPath.setText(file.getAbsolutePath());
            }
        } else {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Choose file");
            fileChooser.setSelectedExtensionFilter(new FileChooser.ExtensionFilter("All files", "*.*"));
            File file = fileChooser.showOpenDialog(stage);

            if (file != null) {
                textFieldFolderPath.setText(file.getAbsolutePath());
            }
        }
    }

    private void handleConvert(ActionEvent event) {
        convert();
    }

    private void convert() {
        boolean convertFolder = radioButtonFolder.isSelected();

        String path = textFieldFolderPath.getText();

        File source = new File(path);
        if (path.isEmpty() || !source.exists()) {
            return;
        }

        buttonBrowse.setDisable(true);
        buttonProceed.setDisable(true);
        textFieldMask.setDisable(true);
        checkBoxBackup.setDisable(true);

        executor.execute(() -> {
            try {
                if (convertFolder) {
                    convertFolder(source);
                } else {
                    convertFile(source);
                }

                Platform.runLater(() -> {
                    Alert successAlert = new Alert(Alert.AlertType.INFORMATION, "Everything successfully converted!", ButtonType.OK);
                    successAlert.initOwner(stage);
                    successAlert.showAndWait();
                });
            } catch (Exception e) {
                e.printStackTrace();

                Platform.runLater(() -> {
                    Alert errorAlert = new Alert(Alert.AlertType.ERROR, "Something went wrong :-(", ButtonType.OK);
                    errorAlert.initOwner(stage);
                    errorAlert.showAndWait();
                });
            } finally {
                Platform.runLater(() -> {
                    buttonBrowse.setDisable(false);
                    buttonProceed.setDisable(false);
                    textFieldMask.setDisable(false);
                    checkBoxBackup.setDisable(false);
                    Platform.runLater(() -> buttonProceed.requestFocus());
                });
            }
        });
    }

    private void convertFolder(File folder) throws IOException {
        String mask = textFieldMask.getText();
        Pattern pattern = Pattern.compile(mask.replace(".", "\\.").replace("*" ,".*").replace("?", "."));

        File[] content = folder.listFiles(file -> file.isDirectory() || mask.isEmpty() || pattern.matcher(file.getName().toLowerCase()).matches());
        if (content != null) {
            for (File f : content) {
                if (f.isDirectory()) {
                    convertFolder(f);
                } else {
                    convertFile(f);
                }
            }
        }
    }

    private void convertFile(File file) throws IOException {
        Charset targetEncoding = comboBoxCharsets.getSelectionModel().getSelectedItem();
        boolean backup = checkBoxBackup.isSelected();

        byte[] fileContent = Files.readAllBytes(Paths.get(file.getAbsolutePath()));

        if (backup) {
            Files.write(Paths.get(file.getParentFile().getAbsolutePath(), file.getName() + "_backup"), fileContent);
        }

        IOException[] error = new IOException[1];
        UniversalDetector detector = new UniversalDetector(realEncoding -> {
            try {
                byte[] convertedContent = new String(fileContent, realEncoding).getBytes(targetEncoding);
                Files.write(Paths.get(file.getAbsolutePath()), convertedContent);
            } catch (IOException e) {
                error[1] = e;
            }
        });

        detector.handleData(fileContent, 0, fileContent.length);
        detector.dataEnd();

        if (error[0] != null) {
            throw error[0];
        }
    }
}
