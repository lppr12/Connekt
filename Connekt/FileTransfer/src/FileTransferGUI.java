import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.*;
import java.net.*;
import java.util.*;
public class FileTransferGUI extends Application {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 8888;
    private ProgressBar progressBar;
    private Label statusLabel;
    private Button uploadButton;
    private Button downloadButton;
    private Button pauseButton;
    private Button resumeButton;
    private FileTransferClient client;   
    public static void main(String[] args) {
        launch(args);
    }
    @Override
    public void start(Stage primaryStage) {
        // Specify the absolute path to the icon file
        String iconPath = new File("icon2.png").toURI().toString();
        // Establish connection as soon as the GUI window is opened
        connectToServer();
        // Set the icon using the absolute path
        primaryStage.getIcons().add(new Image(iconPath));
        primaryStage.setTitle("ConneKt");
        // Create UI components
        uploadButton = new Button("Upload");
        downloadButton = new Button("Download");
        progressBar = new ProgressBar(0);
        statusLabel = new Label();
        pauseButton = new Button("Pause");
        resumeButton = new Button("Resume");

        // Set up file chooser
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select File");

        // Set up layout
        GridPane gridPane = new GridPane();
        gridPane.setHgap(10);
        gridPane.setVgap(10);
        gridPane.setPadding(new Insets(10, 10, 10, 10));

        // Add components to the layout
        gridPane.add(createHeader(), 0, 0, 2, 1);
        gridPane.add(uploadButton, 0, 1);
        gridPane.add(downloadButton, 1, 1);
        gridPane.add(createProgressBar(), 0, 2, 2, 1);
        gridPane.add(statusLabel, 0, 3, 2, 1);
        gridPane.add(createButtons(), 0, 4, 2, 1);

        // Set up button actions
        uploadButton.setOnAction(e -> uploadButtonClicked(fileChooser));
        downloadButton.setOnAction(e -> downloadButtonClicked(fileChooser));
        pauseButton.setOnAction(e -> pauseButtonClicked());
        resumeButton.setOnAction(e -> resumeButtonClicked());

        // Set up the scene
        Scene scene = new Scene(gridPane, 400, 300);
        scene.getStylesheets().add(getClass().getResource("style.css").toExternalForm());
        primaryStage.setScene(scene);

        // Set background with GIF
        setGifBackground(gridPane);

        // Show the stage
        primaryStage.show();
    }
    
    private void connectToServer() {
        try {
            Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

            // Inform the server about the client connection
            out.writeObject("CONNECTED");

            System.out.println("Connected to the server.");

            // You may want to keep this socket, out, and in as class variables if you need to use them later.

            // Close the connection for this example
            out.close();
            in.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
   
    private StackPane createHeader() {
        Label headerLabel = new Label("ConneKt");
        headerLabel.getStyleClass().add("header-label");

        StackPane headerPane = new StackPane(headerLabel);
        headerPane.setAlignment(Pos.CENTER);

        return headerPane;
    }

    private HBox createProgressBar() {
        Label progressLabel = new Label("Progress:");
        progressBar.setPrefWidth(300);

        HBox progressBarBox = new HBox(progressLabel, progressBar);
        progressBarBox.setSpacing(10);
        progressBarBox.setAlignment(Pos.CENTER);

        return progressBarBox;
    }

    private HBox createButtons() {
        HBox buttonsBox = new HBox(10, pauseButton, resumeButton);
        buttonsBox.setAlignment(Pos.CENTER);

        return buttonsBox;
    }

    private void uploadButtonClicked(FileChooser fileChooser) {
        File selectedFile = fileChooser.showOpenDialog(null);
        if (selectedFile != null) {
            client = new FileTransferClient();
            client.setProgressBar(progressBar);
            client.setStatusLabel(statusLabel);
            client.uploadFile(selectedFile);
        }
    }

    private void downloadButtonClicked(FileChooser fileChooser) {
        try {
            Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

            out.writeObject("LIST_FILES");

            @SuppressWarnings("unchecked")
            List<String> files = (List<String>) in.readObject();

            ChoiceDialog<String> dialog = new ChoiceDialog<>(files.get(0), files);
            dialog.setTitle("Select File");
            dialog.setHeaderText("Select the file you want to download:");

            Optional<String> result = dialog.showAndWait();

            result.ifPresent(fileName -> {
                // Prompt user for save location
                File selectedFile = fileChooser.showSaveDialog(null);
                if (selectedFile != null) {
                    // Save in the selected directory
                    String savePath = selectedFile.getAbsolutePath();

                    // Initialize the client here
                    client = new FileTransferClient();
                    client.setProgressBar(progressBar);
                    client.setStatusLabel(statusLabel);

                    // Start a new thread for download to avoid blocking the UI
                    new Thread(() -> {
                        client.downloadFile(fileName, savePath);
                    }).start();
                }
            });

            out.close();
            in.close();
            socket.close();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void pauseButtonClicked() {
        if (client != null) {
            client.pauseTransfer();
        }
    }

    private void resumeButtonClicked() {
        if (client != null) {
            client.resumeTransfer();
        }
    }

    private void setGifBackground(GridPane gridPane) {
        // Specify the file path of the GIF
        String gifFilePath = "file:bggif1.gif";

        // Create a Style for the gridPane to set the background
        String backgroundStyle = "-fx-background-image: url('" + gifFilePath + "'); " +
                                 "-fx-background-size: cover;";

        // Set the style to the gridPane
        gridPane.setStyle(backgroundStyle);
    }
}

