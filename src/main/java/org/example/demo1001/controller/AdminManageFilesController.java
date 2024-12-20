package org.example.demo1001.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.*;
import org.example.demo1001.MainApplication;
import org.example.demo1001.factory.DocumentFactory;
import org.example.demo1001.model.Document;
import org.example.demo1001.repository.DocumentRepository;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Optional;

public class AdminManageFilesController implements Initializable {

    @FXML
    private VBox fileContainer;

    @FXML
    protected void onUploadFileClick() {
        List<File> selectedFiles = showFileChooser();
        saveToDatabase(selectedFiles);
    }

    private List<File> showFileChooser() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("PDF Files (*.pdf)", "*.pdf"),
                new FileChooser.ExtensionFilter("Word Files (*.docx)", "*.docx"),
                new FileChooser.ExtensionFilter("Excel Files (*.xlsx)", "*.xlsx")
        );
        return fileChooser.showOpenMultipleDialog(new Stage());
    }

    protected void saveToDatabase(List<File> selectedFiles) {
        if (selectedFiles != null && !selectedFiles.isEmpty()) {
            for (File file : selectedFiles) {
                Document document = createDocument(file);
                if (DocumentRepository.getInstance().saveDocument(document)) {
                    addFileComponent(document);
                }
            }
        } else {
            showAlert("No Files Selected", "No files were selected.");
        }
    }

    private Document createDocument(File file) {
        DocumentFactory factory = DocumentFactory.getInstance();
        return factory.createDocument(file.getName(), LocalDateTime.now().toString(), file);
    }

    private void showAlert(String title, String contentText) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(contentText);
        alert.showAndWait();
    }

    public void addFileComponent(Document document) {
        HBox fileComponent = createFileComponent(document);
        fileComponent.setId(String.valueOf(document.getId()));
        fileContainer.getChildren().add(fileComponent);
    }



    private HBox createFileComponent(Document document) {
        HBox fileComponent = new HBox(110);
        fileComponent.setStyle("-fx-padding: 10; -fx-border-color: lightgray; -fx-border-width: 1; -fx-background-color: #f0f0f0;");
        fileComponent.setMinHeight(60);
        fileComponent.setPrefHeight(60);
        fileComponent.setMaxHeight(60);

        Label nameLabel = createLabel(document.getName());
        Label typeLabel = createLabel(document.getType());
        Label dateLabel = createLabel("created at\n" + document.getDate());

        HBox buttonContainer = createButtonContainer(document);

        fileComponent.getChildren().addAll(nameLabel, typeLabel, dateLabel, buttonContainer);
        return fileComponent;
    }

    private Label createLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-font-size: 14px; -fx-text-fill: #333333;");
        label.setPrefWidth(130);
        label.setMinWidth(130);
        label.setMaxWidth(130);
        return label;
    }

    private HBox createButtonContainer(Document document) {
        HBox buttonContainer = new HBox(10);
        buttonContainer.setAlignment(Pos.CENTER_RIGHT);
        buttonContainer.setSpacing(20);

        Button openButton = createOpenButton(document);
        Button editButton = createEditButton(document);

        buttonContainer.getChildren().addAll(openButton, editButton);
        return buttonContainer;
    }

    private Button createOpenButton(Document document) {
        Button openButton = new Button("Open");
        openButton.setStyle("-fx-font-size: 12px; -fx-background-color: #4CAF50; -fx-text-fill: white; -fx-padding: 5px 10px;");
        openButton.setMinWidth(Region.USE_COMPUTED_SIZE);
        openButton.setPrefWidth(Region.USE_COMPUTED_SIZE);
        openButton.setMaxWidth(Double.MAX_VALUE);

        openButton.setOnAction(event -> openFile(document.getFile()));
        return openButton;
    }

    private Button createEditButton(Document document) {
        Button editButton = new Button("Edit");
        editButton.setStyle("-fx-font-size: 12px; -fx-background-color: #2196F3; -fx-text-fill: white; -fx-padding: 5px 10px;");
        editButton.setMaxWidth(Double.MAX_VALUE);

        editButton.setOnAction(event -> openEditWindow(document));
        return editButton;
    }

    private void openFile(File file) {
        if (file != null && file.exists()) {
            try {
                Desktop desktop = Desktop.getDesktop();
                desktop.open(file);  // Opens the file with the default application
            } catch (IOException e) {
                showAlert("Error Opening File", "Unable to open the file: " + e.getMessage());
            }
        }
    }

    private void openEditWindow(Document document) {

        Stage editWindow = new Stage();
        editWindow.initModality(Modality.APPLICATION_MODAL);
        editWindow.setTitle("Edit Document");

        VBox editLayout = new VBox(20);
        editLayout.setAlignment(Pos.CENTER);

        Button deleteButton = new Button("Delete");
        Button changeNameButton = new Button("Change Name");

        deleteButton.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-padding: 5px 10px;");
        changeNameButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-padding: 5px 10px;");

        // Handle delete button click
        deleteButton.setOnAction(deleteEvent -> {
            Alert deleteAlert = new Alert(Alert.AlertType.CONFIRMATION);
            deleteAlert.setTitle("Delete Document");
            deleteAlert.setHeaderText("Are you sure you want to delete this document?");
            deleteAlert.setContentText("This action cannot be undone.");

            Optional<ButtonType> result = deleteAlert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                System.out.println("yes clicked!!");
                // Delete the document from repository and remove the UI component
                DocumentRepository.getInstance().deleteDocument(document);
                fileContainer.getChildren().removeIf(node -> {
                    HBox hbox = (HBox) node;
                    Label label = (Label) hbox.getChildren().get(0);
                    return label.getText().equals(document.getName());
                });
                editWindow.close();
            }
        });


        changeNameButton.setOnAction(changeEvent -> openChangeNameWindow(document));


        editLayout.getChildren().addAll(deleteButton, changeNameButton);

        Scene editScene = new Scene(editLayout, 300, 200);
        editWindow.setScene(editScene);
        editWindow.show();
    }

    private void openChangeNameWindow(Document document) {

        Stage changeNameWindow = new Stage();
        changeNameWindow.initModality(Modality.APPLICATION_MODAL);
        changeNameWindow.setTitle("Change Name");

        TextField nameField = new TextField(document.getName());
        nameField.setPromptText("Enter new name");

        Button changeButton = new Button("Change");
        Button cancelButton = new Button("Cancel");

        changeButton.setOnAction(changeNameEvent -> {
            if(changeNameEvent(nameField.getText(),document)){
                changeNameWindow.close();
            }

        });

        cancelButton.setOnAction(cancelEvent -> changeNameWindow.close()); // Close the change name window

        VBox changeNameLayout = new VBox(10, nameField, changeButton, cancelButton);
        changeNameLayout.setAlignment(Pos.CENTER);
        Scene changeNameScene = new Scene(changeNameLayout, 300, 200);
        changeNameWindow.setScene(changeNameScene);
        changeNameWindow.show();
    }

    private Boolean changeNameEvent(String newName,Document document){
        if (!newName.trim().isEmpty()) {
            DocumentRepository.getInstance().changeDocumentName(document,newName);
                document.setName(newName);
            updateFileComponentName(document);
            return true;
        } else {
            showAlert("Invalid Name", "Name cannot be empty.");
            return false;
        }
    }

    private void updateFileComponentName(Document document) {
        // Loop through all file components to find the one matching the document's ID and update it
        for (Node node : fileContainer.getChildren()) {
            if (node instanceof HBox) {
                HBox fileComponent = (HBox) node;

                // Check if the ID of the file component matches the document's ID
                if (fileComponent.getId().equals(String.valueOf(document.getId()))) {
                    // Get the name label (assuming it's the first child in HBox)
                    Label nameLabel = (Label) fileComponent.getChildren().get(0);
                    nameLabel.setText(document.getName()); // Update the name displayed in the UI
                    break;
                }
            }
        }
    }


    @FXML
    protected void onBackToDashboard() {
        try {
            // Load the dashboard FXML file (make sure the path is correct)
            FXMLLoader fxmlLoader = new FXMLLoader(MainApplication.class.getResource("AdminDashboard.fxml"));
            Parent dashboardRoot = fxmlLoader.load();

            // Get the current stage
            Stage stage = (Stage) fileContainer.getScene().getWindow();

            // Set the new scene (dashboard scene) on the stage
            Scene dashboardScene = new Scene(dashboardRoot);
            stage.setScene(dashboardScene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Could not load dashboard: " + e.getMessage());
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        DocumentRepository documentRepository = DocumentRepository.getInstance();
        List<Document> documentList = documentRepository.getAllDocuments();
        documentList.forEach(this::addFileComponent);
    }
}
