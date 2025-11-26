module org.win {
    requires javafx.controls;
    requires javafx.swing;
    requires javafx.graphics;

    requires ij;  // ImageJ library
    requires org.apache.commons.lang3;

    // Note: Clojure is accessed via --add-reads compiler flag (see build.gradle)
    // IntelliJ users: Add "--add-reads org.win=ALL-UNNAMED" to compiler settings

    exports org.win;
    exports org.win.view;
    exports org.win.model;
    exports org.win.util;

    opens org.win to javafx.graphics;
}
