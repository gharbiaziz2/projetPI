package tn.esprit.gui;

import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.DialogPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.control.Label;
import javafx.scene.Node;

public final class DialogStyleHelper {

    private DialogStyleHelper() {}

    public static void styleDialogPane(DialogPane pane) {
        pane.getStyleClass().add("crud-dialog-pane");
        try {
            pane.getStylesheets().add(DialogStyleHelper.class.getResource("/css/back.css").toExternalForm());
        } catch (Exception ignored) {}
    }

    public static GridPane buildGrid() {
        GridPane g = new GridPane();
        g.setHgap(16);
        g.setVgap(14);
        return g;
    }

    public static void addRow(GridPane g, int row, String labelText, Node field) {
        Label lb = new Label(labelText);
        lb.getStyleClass().add("crud-dialog-label");
        g.add(lb, 0, row);
        g.add(field, 1, row);
    }

    public static void styleField(TextField t) {
        t.getStyleClass().add("crud-dialog-field");
        t.setPrefWidth(260);
    }

    public static void styleDatePicker(DatePicker dp) {
        dp.getStyleClass().add("crud-dialog-datepicker");
        dp.setPrefWidth(260);
    }

    @SuppressWarnings("rawtypes")
    public static void styleCombo(ComboBox cb) {
        cb.getStyleClass().add("crud-dialog-field");
        cb.setPrefWidth(260);
    }

    /** Style dialog pane for front (client) reservation popups. */
    public static void styleFrontDialogPane(DialogPane pane) {
        pane.getStyleClass().add("front-dialog-pane");
        try {
            pane.getStylesheets().add(DialogStyleHelper.class.getResource("/css/front.css").toExternalForm());
        } catch (Exception ignored) {}
    }
}
