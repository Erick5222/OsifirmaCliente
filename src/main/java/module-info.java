module com.osinergmin.firma.desktop {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.swing;
    requires org.apache.pdfbox;
    requires com.google.gson;
    requires java.net.http;
    requires java.prefs;
    requires firmaOsinergmin;

    opens com.osinergmin.firma.desktop to javafx.fxml;
    opens com.osinergmin.firma.desktop.ui.login to javafx.fxml;
    opens com.osinergmin.firma.desktop.ui.main to javafx.fxml;
    opens com.osinergmin.firma.desktop.ui.viewer to javafx.fxml;
    opens com.osinergmin.firma.desktop.service.auth.dto to com.google.gson;
    opens com.osinergmin.firma.desktop.service.document.dto to com.google.gson;
    opens com.osinergmin.firma.desktop.service.audit.dto to com.google.gson;
    opens com.osinergmin.firma.desktop.integration.invoke to com.google.gson;
    opens com.osinergmin.firma.desktop.service.signing to com.google.gson;
    exports com.osinergmin.firma.desktop;
}
