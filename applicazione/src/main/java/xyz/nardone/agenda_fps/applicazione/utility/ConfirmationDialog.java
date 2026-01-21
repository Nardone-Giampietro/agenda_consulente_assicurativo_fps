package xyz.nardone.agenda_fps.applicazione.utility;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.Region;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.util.Optional;

/**
 * Simple confirmation dialog helper for JavaFX.
 */
public class ConfirmationDialog {

    /**
     * Shows a modal confirmation dialog with custom button labels.
     */
    public static boolean show(String title, String message, String okButtonText, String cancelButtonText){
        Alert alert = new Alert(AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        // Use TextFlow to automatically adjust size
        TextFlow textFlow = new TextFlow(new Text(message));
        textFlow.setMaxWidth(400);
        textFlow.setPrefWidth(Region.USE_COMPUTED_SIZE);

        alert.getDialogPane().setContent(textFlow);
        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        alert.getDialogPane().setMinWidth(Region.USE_PREF_SIZE);

        // Custom button texts
        ButtonType okButton = new ButtonType(okButtonText);
        ButtonType cancelButton = new ButtonType(cancelButtonText);

        // Set custom buttons
        alert.getButtonTypes().setAll(okButton, cancelButton);

        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == okButton;
    }
}
