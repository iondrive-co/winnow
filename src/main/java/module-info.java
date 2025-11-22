module winnow {
    requires javafx.controls;
    requires javafx.swing;
    requires ij;
    requires org.apache.commons.lang3;
    requires java.desktop;

    opens org.win to javafx.graphics;
    opens org.win.view to javafx.graphics;
    opens org.win.control to javafx.graphics;
    opens org.win.model to javafx.graphics;

    exports org.win;
}
