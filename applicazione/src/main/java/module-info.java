// JavaFX client module for Agenda FPS.
module xyz.nardone.agenda_fps.applicazione {
    requires com.google.gson;
    requires org.apache.logging.log4j;
    requires javafx.controls;
    requires javafx.fxml;
    requires java.logging;
    requires java.desktop;
    requires com.google.errorprone.annotations;

    opens xyz.nardone.agenda_fps.applicazione to com.google.gson, javafx.base;
    exports xyz.nardone.agenda_fps.applicazione.controllers to javafx.fxml;
    opens xyz.nardone.agenda_fps.applicazione.controllers to javafx.fxml;
    exports xyz.nardone.agenda_fps.applicazione to javafx.graphics; // Export App to javafx.graphics
}
