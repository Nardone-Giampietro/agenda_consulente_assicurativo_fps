package xyz.nardone.agenda_fps.applicazione;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

/**
 * JavaFX application entry point and scene manager.
 */
public class App extends Application {

    private static Stage stage; // Store the main stage
    private static Scene scene; // Store the scene

    /**
     * Initializes the primary stage and loads the start view.
     */
    @Override
    public void start(Stage primaryStage) throws IOException {
        stage = primaryStage; // Assign the primary stage to the static variable
        scene = new Scene(loadFXML("start"));

        stage.setScene(scene);

        // Set application icon
        stage.getIcons().add(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/icons/icon.png"))));

        stage.setTitle("Agenda FPS");

        stage.show();
    }

    public static void setRoot(String fxml) throws IOException {
        boolean wasFullscreen = stage.isFullScreen(); // Preserve fullscreen mode

        Parent root = loadFXML(fxml); // Load the new FXML
        scene.setRoot(root); // Update the root

        // Ensure the stage stays in fullscreen mode
        if (wasFullscreen) {
            stage.setFullScreen(true);
        }
    }

    private static Parent loadFXML(String fxml) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource(fxml + ".fxml"));
        return fxmlLoader.load();
    }

    /**
     * Returns the shared scene instance.
     */
    public static Scene getScene() {
        return scene;
    }

    /**
     * Launches the JavaFX application.
     */
    public static void main(String[] args) {
        launch();
    }
}
