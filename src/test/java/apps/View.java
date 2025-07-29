package apps;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;

public class View extends StackPane {
    private final Label label;

    {
        label = new Label("Hello World!");
        label.setText("Maybe it works now");
        label.setTextFill(Color.RED);
        label.setPadding(new Insets(12.0));
        getChildren().add(label);
    }

    public Node getGraphic() {
        return label.getGraphic();
    }

    public void setGraphic(Node node) {
        label.setGraphic(node);
    }
}
