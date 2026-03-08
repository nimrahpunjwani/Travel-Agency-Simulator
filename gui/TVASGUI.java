package gui;

import cli.Admin;
import cli.Agent;
import cli.Booking;
import cli.Client;
import cli.Payment;
import cli.TripPackage;
import cli.User;
import com.google.gson.*;
import db.DatabaseManager;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import java.sql.Statement;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;
import java.util.function.Consumer;


public class TVASGUI extends Application {
  private List<User> users = new LinkedList<>();
private List<TripPackage> packages = new LinkedList<>();
private List<Booking> bookings = new LinkedList<>();
private List<Payment> payments = new LinkedList<>();
    private User currentUser = null;

    private Stage primaryStage;
    private StackPane rootLayout;

    // Main containers
    private VBox mainMenu;
    private VBox loginScreen;
    private VBox registrationScreen;
    private VBox adminDashboard;
    private VBox agentDashboard;
    private VBox clientDashboard;

    // Common UI elements
    private Label statusLabel;
@Override
public void start(Stage primaryStage) {
    this.primaryStage = primaryStage;
    this.primaryStage.setTitle("Travel Agency Simulator");

    // REMOVE or COMMENT OUT this line:
    // initializeSystem();

    initRootLayout();
    loadUsersFromDatabase();
    loadPackagesFromDatabase();
    loadBookingsFromDatabase();
    loadPaymentsFromDatabase();
    showMainMenu();
}
    // In your TVASGUI or relevant class
// Add to TVASGUI or a helper class
private void savePaymentToDatabase(Payment payment) {
    if (payment == null) {
        statusLabel.setText("Cannot save: Payment is null.");
        return;
    }
    String sql = "INSERT INTO PAYMENTS (CLIENT_USERNAME, PACKAGE_ID, AMOUNT, STATUS) VALUES (?, ?, ?, ?)";
    try (Connection conn = DatabaseManager.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setString(1, payment.getClientUsername());
        ps.setString(2, payment.getPackageId());
        ps.setDouble(3, payment.getAmount());
        ps.setString(4, payment.getStatus());
        int rows = ps.executeUpdate();
        if (rows == 0) {
            statusLabel.setText("Payment not saved. Please try again.");
        }
    } catch (Exception e) {
        statusLabel.setText("Error saving payment: " + e.getMessage());
    }}
private void saveUserToDatabase(User user) {
    if (user == null) {
        statusLabel.setText("Cannot save: User is null.");
        return;
    }
    String sql = "INSERT INTO USERS (USERNAME, PASSWORD, EMAIL, NAME, TYPE, COUNTRY, CITY) VALUES (?, ?, ?, ?, ?, ?, ?)";
    try (Connection conn = DatabaseManager.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setString(1, user.getUsername());
        ps.setString(2, user.getPassword());
        ps.setString(3, user.getEmail());
        ps.setString(4, user.getName());
        ps.setString(5, user instanceof Admin ? "Admin" : user instanceof Agent ? "Agent" : "Client");
        if (user instanceof Client) {
            ps.setString(6, ((Client) user).getCountry());
            ps.setString(7, ((Client) user).getCity());
        } else {
            ps.setString(6, null);
            ps.setString(7, null);
        }
        int rows = ps.executeUpdate();
        if (rows == 0) {
            statusLabel.setText("User not saved. Please try again.");
        }
    } catch (Exception e) {
        statusLabel.setText("Error saving user: " + e.getMessage());
    }}
private String getNextPackageId() {
    int maxId = 0;
    for (TripPackage pkg : packages) {
        try {
            String id = pkg.getId();
            if (id != null) {
                String idNum = id.replaceAll("\\D+", ""); // Extract digits
                if (!idNum.isEmpty()) {
                    int num = Integer.parseInt(idNum);
                    if (num > maxId) maxId = num;
                }
            }
        } catch (Exception ignored) {}
    }
    return String.format("PKG%03d", maxId + 1);
}
private void showManageBookingsScreen() {
    loadBookingsFromDatabase(); // Always reload bookings from DB

    VBox screen = new VBox(15);
    screen.setAlignment(Pos.CENTER);

    Label titleLabel = new Label("Manage Bookings");
    titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

    ListView<String> bookingList = new ListView<>();
    Map<String, Booking> bookingMap = new HashMap<>();
    for (Booking booking : bookings) {
        String display = "Package: " + booking.getPackageId() +
            " | Client: " + booking.getClientUsername() +
            " | Date: " + booking.getBookingDate() +
            " | Status: " + booking.getStatus() +
            (booking.getAssignedAgent() != null && !booking.getAssignedAgent().isEmpty() ? " | Agent: " + booking.getAssignedAgent() : "");
        bookingList.getItems().add(display);
        bookingMap.put(display, booking);
    }
    bookingList.setPrefHeight(200);
    bookingList.setPrefWidth(700);

    Button approveBtn = new Button("Approve Booking");
    approveBtn.setOnAction(e -> {
        String selected = bookingList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            statusLabel.setText("Select a booking to approve.");
            return;
        }
        Booking booking = bookingMap.get(selected);
        booking.setStatus("Approved");
        booking.setAssignedAgent(currentUser.getUsername());
        updateBookingStatusInDatabase(booking.getPackageId(), booking.getClientUsername(), "Approved");
        updateBookingAgentInDatabase(booking.getPackageId(), booking.getClientUsername(), currentUser.getUsername());
        statusLabel.setText("Booking approved.");
        showManageBookingsScreen();
    });

    Button rejectBtn = new Button("Reject Booking");
    rejectBtn.setOnAction(e -> {
        String selected = bookingList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            statusLabel.setText("Select a booking to reject.");
            return;
        }
        Booking booking = bookingMap.get(selected);
        booking.setStatus("Rejected");
        booking.setAssignedAgent(currentUser.getUsername());
        updateBookingStatusInDatabase(booking.getPackageId(), booking.getClientUsername(), "Rejected");
        updateBookingAgentInDatabase(booking.getPackageId(), booking.getClientUsername(), currentUser.getUsername());
        statusLabel.setText("Booking rejected.");
        showManageBookingsScreen();
    });

    Button backBtn = new Button("Back");
    backBtn.setOnAction(e -> showAgentDashboard());

    screen.getChildren().addAll(titleLabel, bookingList, approveBtn, rejectBtn, backBtn, statusLabel);
    rootLayout.getChildren().setAll(screen);
}
private void updateUserInDatabase(String username, String name, String email) {
    if (username == null || name == null || email == null) {
        statusLabel.setText("Invalid user data for update.");
        return;
    }
    String sql = "UPDATE USERS SET NAME = ?, EMAIL = ? WHERE USERNAME = ?";
    try (Connection conn = DatabaseManager.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setString(1, name);
        ps.setString(2, email);
        ps.setString(3, username);
        int rows = ps.executeUpdate();
        if (rows == 0) {
            statusLabel.setText("No user updated. Username may not exist.");
        }
    } catch (Exception e) {
        statusLabel.setText("Error updating user: " + e.getMessage());
    }}
private void savePackageToDatabase(TripPackage pkg) {
    if (pkg == null) {
        statusLabel.setText("Cannot save: Package is null.");
        return;
    }
    String sql = "INSERT INTO PACKAGES (ID, NAME, DESTINATION, PRICE, DAYS, ACTIVITIES) VALUES (?, ?, ?, ?, ?, ?)";
    try (Connection conn = DatabaseManager.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setString(1, pkg.getId());
        ps.setString(2, pkg.getName());
        ps.setString(3, pkg.getDestination());
        ps.setDouble(4, pkg.getPrice());
        ps.setInt(5, pkg.getDays());
        ps.setString(6, pkg.getActivities());
        int rows = ps.executeUpdate();
        if (rows == 0) {
            statusLabel.setText("Package not saved. Please try again.");
        }
    } catch (Exception e) {
        statusLabel.setText("Error saving package: " + e.getMessage());
    }}
private void loadUsersFromDatabase() {
    users.clear();
    try (Connection conn = DatabaseManager.getConnection();
         Statement stmt = conn.createStatement();
         ResultSet rs = stmt.executeQuery("SELECT * FROM USERS")) {
        while (rs.next()) {
            // Print all columns for each row to debug what is actually being read
            System.out.println("DB Row: USERNAME='" + rs.getString("USERNAME") +
                "', PASSWORD='" + rs.getString("PASSWORD") +
                "', EMAIL='" + rs.getString("EMAIL") +
                "', NAME='" + rs.getString("NAME") +
                "', TYPE='" + rs.getString("TYPE") +
                "', COUNTRY='" + rs.getString("COUNTRY") +
                "', CITY='" + rs.getString("CITY") + "'");

            String type = rs.getString("TYPE");
            if (type != null) type = type.trim();
            else type = "";

            if ("admin".equalsIgnoreCase(type)) {
                users.add(new Admin(
                    rs.getString("USERNAME"),
                    rs.getString("PASSWORD"),
                    rs.getString("EMAIL"),
                    rs.getString("NAME")
                ));
            } else if ("agent".equalsIgnoreCase(type)) {
                users.add(new Agent(
                    rs.getString("USERNAME"),
                    rs.getString("PASSWORD"),
                    rs.getString("EMAIL"),
                    rs.getString("NAME")
                ));
            } else if ("client".equalsIgnoreCase(type)) {
                users.add(new Client(
                    rs.getString("USERNAME"),
                    rs.getString("PASSWORD"),
                    rs.getString("EMAIL"),
                    rs.getString("NAME"),
                    rs.getString("COUNTRY"),
                    rs.getString("CITY")
                ));
            } else {
                // Print a warning for unknown types
                System.out.println("Warning: Unknown user type '" + type + "' for user '" + rs.getString("USERNAME") + "'");
            }
        }
        // Debug print
        for (User user : users) {
            System.out.println("Loaded user: " + user.getUsername() + " (" + user.getClass().getSimpleName() + ")");
        }
    } catch (Exception e) {
        statusLabel.setText("Error loading users: " + e.getMessage());
        e.printStackTrace();
    }}
     private void loadPackagesFromDatabase() {
    packages.clear();
    try (Connection conn = DatabaseManager.getConnection();
         Statement stmt = conn.createStatement();
         ResultSet rs = stmt.executeQuery("SELECT * FROM PACKAGES")) {
        while (rs.next()) {
            packages.add(new TripPackage(
                rs.getString("ID"),
                rs.getString("NAME"),
                rs.getString("DESTINATION"),
                rs.getDouble("PRICE"),
                rs.getInt("DAYS"),
                rs.getString("ACTIVITIES")
            ));
        }
    } catch (Exception e) {
        statusLabel.setText("Error loading packages: " + e.getMessage());
    }}
private void loadBookingsFromDatabase() {
    bookings.clear();
    try (Connection conn = DatabaseManager.getConnection();
         Statement stmt = conn.createStatement();
         ResultSet rs = stmt.executeQuery("SELECT * FROM BOOKINGS")) {
        while (rs.next()) {
            Booking booking = new Booking(
                rs.getString("PACKAGE_ID"),
                rs.getString("CLIENT_USERNAME"),
                rs.getString("BOOKING_DATE"),
                rs.getString("STATUS")
            );
            booking.setAssignedAgent(rs.getString("ASSIGNED_AGENT"));
            bookings.add(booking);
        }
    } catch (Exception e) {
        statusLabel.setText("Error loading bookings: " + e.getMessage());
    }}
private void loadPaymentsFromDatabase() {
    payments.clear();
    try (Connection conn = DatabaseManager.getConnection();
         Statement stmt = conn.createStatement();
         ResultSet rs = stmt.executeQuery("SELECT * FROM PAYMENTS")) {
        while (rs.next()) {
            payments.add(new Payment(
                rs.getString("CLIENT_USERNAME"),
                rs.getString("PACKAGE_ID"),
                rs.getDouble("AMOUNT"),
                rs.getString("STATUS")
            ));
        }
    } catch (Exception e) {
        statusLabel.setText("Error loading payments: " + e.getMessage());
    }}
private void initializeSystem() {
        users.add(new Admin("admin", "admin123", "admin@travel.com", "Admin User"));
        users.add(new Agent("agent1", "agent123", "agent1@travel.com", "John Smith"));
        users.add(new Agent("agent2", "agent123", "agent2@travel.com", "Sarah Johnson"));

        packages.add(new TripPackage("PKG001", "Paris Getaway", "France", 1200.0, 5,
                "City tour, Eiffel Tower visit, Seine River cruise"));
        packages.add(new TripPackage("PKG002", "Tropical Bali", "Indonesia", 1500.0, 7,
                "Beach resort, Temple visits, Snorkeling"));
    }
private void saveBookingToDatabase(Booking booking) {
    loadPackagesFromDatabase();
    System.out.println("Trying to book package ID: '" + booking.getPackageId() + "'");
    for (TripPackage pkg : packages) {
        System.out.println("Available package ID: '" + pkg.getId() + "'");
    }
    String pkgId = booking.getPackageId();
    if (pkgId == null || !pkgId.trim().toUpperCase().startsWith("PKG")) {
        statusLabel.setText("Error: Invalid package ID for booking. Please contact support.");
        System.err.println("Booking failed: booking.getPackageId() is not a valid package ID: " + pkgId);
        return;
    }
    boolean packageExists = packages.stream()
        .anyMatch(pkg -> pkg.getId().trim().equalsIgnoreCase(pkgId.trim()));
    if (!packageExists) {
        statusLabel.setText("Error: Package ID does not exist in PACKAGES table.");
        return;
    }
    String checkSql = "SELECT COUNT(*) FROM BOOKINGS WHERE PACKAGE_ID = ? AND CLIENT_USERNAME = ?";
    try (Connection conn = DatabaseManager.getConnection();
         PreparedStatement checkPs = conn.prepareStatement(checkSql)) {
        checkPs.setString(1, pkgId.trim());
        checkPs.setString(2, booking.getClientUsername().trim());
        ResultSet rs = checkPs.executeQuery();
        if (rs.next() && rs.getInt(1) > 0) {
            statusLabel.setText("You have already booked this package.");
            return;
        }
    } catch (Exception e) {
        e.printStackTrace();
        statusLabel.setText("Error checking existing booking: " + e.getMessage());
        return;
    }
    String sql = "INSERT INTO BOOKINGS (PACKAGE_ID, CLIENT_USERNAME, BOOKING_DATE, STATUS, ASSIGNED_AGENT) VALUES (?, ?, ?, ?, ?)";
    try (Connection conn = DatabaseManager.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setString(1, pkgId.trim());
        ps.setString(2, booking.getClientUsername().trim());
        ps.setString(3, booking.getBookingDate());
        ps.setString(4, booking.getStatus());
        ps.setString(5, booking.getAssignedAgent() != null ? booking.getAssignedAgent().trim() : "");
        int rows = ps.executeUpdate();
        if (rows == 0) {
            statusLabel.setText("Booking not saved. Please try again.");
        }
    } catch (Exception e) {
        e.printStackTrace();
        statusLabel.setText("Error saving booking: " + e.getMessage());
    }}
     private void initRootLayout() {
    rootLayout = new StackPane();
    rootLayout.setPadding(new Insets(20));
    // Stylish gradient background
    rootLayout.setStyle(
        "-fx-background-color: linear-gradient(135deg, #e0eafc 0%, #cfdef3 100%);" +
        "-fx-background-radius: 20;"
    );
    statusLabel = new Label();
    statusLabel.setStyle("-fx-text-fill: #2a5058; -fx-font-weight: bold;");
    Scene scene = new Scene(rootLayout, 800, 600);
    try {
        scene.getStylesheets().add(getClass().getResource("style.css").toExternalForm());
    } catch (NullPointerException e) {
        System.out.println("CSS file not found, using default styling");
    }
    primaryStage.setScene(scene);
    primaryStage.show();
}
   private void showMainMenu() {
    mainMenu = new VBox(20);
    mainMenu.setAlignment(Pos.CENTER);

    Label titleLabel = new Label("Travel Agency Simulator");
    titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

    Button loginBtn = new Button("Login");
    loginBtn.setPrefWidth(200);
    loginBtn.setOnAction(e -> showLoginScreen());

    Button registerBtn = new Button("Register as Client");
    registerBtn.setPrefWidth(200);
    registerBtn.setOnAction(e -> showRegistrationScreen());

    Button exitBtn = new Button("Exit");
    exitBtn.setPrefWidth(200);
    exitBtn.setOnAction(e -> {
        // saveDataToFiles(); // REMOVE this line
        primaryStage.close();
    });

    mainMenu.getChildren().addAll(titleLabel, loginBtn, registerBtn, exitBtn, statusLabel);
    rootLayout.getChildren().setAll(mainMenu);
}
private void bubbleSortPackagesByPrice(List<TripPackage> packageList) {
    int n = packageList.size();
    for (int i = 0; i < n - 1; i++) {
        for (int j = 0; j < n - i - 1; j++) {
            if (packageList.get(j).getPrice() > packageList.get(j + 1).getPrice()) {
                // Swap
                TripPackage temp = packageList.get(j);
                packageList.set(j, packageList.get(j + 1));
                packageList.set(j + 1, temp);
            } }}}
private void showLoginScreen() {
    VBox loginScreen = new VBox(15);
    loginScreen.setAlignment(Pos.CENTER);
    Label titleLabel = new Label("Login");
    titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");
    TextField usernameField = new TextField();
    usernameField.setPromptText("Username");
    usernameField.setPrefWidth(200);
    PasswordField passwordField = new PasswordField();
    passwordField.setPromptText("Password");
    passwordField.setPrefWidth(300);
    TextField visiblePasswordField = new TextField();
    visiblePasswordField.setPromptText("Password");
    visiblePasswordField.setPrefWidth(300);
    HBox passwordBox = createPasswordFieldWithToggle(passwordField, visiblePasswordField);
    Button loginBtn = new Button("Login");
    loginBtn.setPrefWidth(150);
    loginBtn.setOnAction(e -> {
        String username = usernameField.getText();
        String password = passwordField.getText(); // FIX: get password from field
        System.out.println("Trying login: '" + username + "' | '" + password + "'");
        for (User user : users) {
            System.out.println("Checking: '" + user.getUsername() + "' | '" + user.getPassword() + "' (" + user.getClass().getSimpleName() + ")");
            if (user.getUsername().equals(username) && user.authenticate(password)) {
                currentUser = user;
                statusLabel.setText("Login successful! Welcome, " + user.getName() + "!");
                // Show dashboard based on user type
                if (user instanceof Admin) {
                    showAdminDashboard();
                } else if (user instanceof Agent) {
                    showAgentDashboard();
                } else if (user instanceof Client) {
                    showClientDashboard();
                }
                return;
            }
        }
        statusLabel.setText("Invalid username or password. Please try again.");
    });
    Button backBtn = new Button("Back");
    backBtn.setPrefWidth(150);
    backBtn.setOnAction(e -> showMainMenu());
    HBox buttonBox = new HBox(10, loginBtn, backBtn);
    buttonBox.setAlignment(Pos.CENTER);
    loginScreen.getChildren().addAll(titleLabel, usernameField, passwordBox, buttonBox, statusLabel);
    rootLayout.getChildren().setAll(loginScreen);
}
   private void showRegistrationScreen() {
    VBox registrationScreen = new VBox(15);
    registrationScreen.setAlignment(Pos.CENTER);
    Label titleLabel = new Label("Client Registration");
    titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");
    TextField usernameField = new TextField();
    usernameField.setPromptText("Username");
    usernameField.setPrefWidth(300);
    PasswordField passwordField = new PasswordField();
passwordField.setPromptText("Password");
passwordField.setPrefWidth(300);
TextField visiblePasswordField = new TextField();
visiblePasswordField.setPromptText("Password");
visiblePasswordField.setPrefWidth(300);
HBox passwordBox = createPasswordFieldWithToggle(passwordField, visiblePasswordField);
    TextField emailField = new TextField();
    emailField.setPromptText("Email");
    emailField.setPrefWidth(300);
    TextField nameField = new TextField();
    nameField.setPromptText("Full Name");
    nameField.setPrefWidth(300);
    List<String> countries = loadCountriesFromCSV("C:/Users/HP/Downloads/simplemaps_worldcities_basicv1.901/worldcities.csv");
ComboBox<String> countryComboBox = new ComboBox<>();
countryComboBox.getItems().addAll(countries);
countryComboBox.setPromptText("Select Country");
countryComboBox.setPrefWidth(300);
ComboBox<String> cityComboBox = new ComboBox<>();
cityComboBox.setPromptText("Select City");
cityComboBox.setPrefWidth(300);
countryComboBox.setOnAction(e -> {
    String selectedCountry = countryComboBox.getValue();
    if (selectedCountry != null) {
        List<String> cities = loadCitiesForCountry("C:/Users/HP/Downloads/simplemaps_worldcities_basicv1.901/worldcities.csv", selectedCountry);
        cityComboBox.getItems().setAll(cities);
    } else {
        cityComboBox.getItems().clear();
    }
});
    Button registerBtn = new Button("Register");
    registerBtn.setPrefWidth(150);
    registerBtn.setOnAction(e -> {
    String username = usernameField.getText();
    String password = passwordField.getText();
    String email = emailField.getText();
    String name = nameField.getText();
    String country = countryComboBox.getValue();
    String city = cityComboBox.getValue();
    if (username.isEmpty() || password.isEmpty() || email.isEmpty() || name.isEmpty() || country == null || city == null) {
        statusLabel.setText("All fields are required!");
        return;
    }
    if (!email.contains("@") || !email.endsWith(".com")) {
        statusLabel.setText("Email must contain '@' and end with '.com'");
        return;
    }
    for (User user : users) {
        if (user.getUsername().equals(username)) {
            statusLabel.setText("Username already exists. Please choose another.");
            return;
        }
    }
   Client newClient = new Client(username, password, email, name, country, city);
users.add(newClient);
saveUserToDatabase(newClient); // Save to database
statusLabel.setText("Registration successful! You can now login with your credentials.");
showMainMenu();
});
    Button backBtn = new Button("Back");
    backBtn.setPrefWidth(150);
    backBtn.setOnAction(e -> showMainMenu());
    HBox buttonBox = new HBox(10, registerBtn, backBtn);
    buttonBox.setAlignment(Pos.CENTER);
   registrationScreen.getChildren().addAll(
    titleLabel, usernameField, passwordBox, emailField, nameField, countryComboBox, cityComboBox, buttonBox, statusLabel
);
rootLayout.getChildren().setAll(registrationScreen);
}
private void showAdminDashboard() {
        VBox adminDashboard = new VBox(15);
    adminDashboard.setAlignment(Pos.CENTER);

    Label titleLabel = new Label("Admin Dashboard");
    titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

    Button manageUsersBtn = new Button("Manage Users");
    manageUsersBtn.setOnAction(e -> showManageUsersScreen());

    Button managePackagesBtn = new Button("Manage Packages");
    managePackagesBtn.setOnAction(e -> showManagePackagesScreen());
    Button reportBtn = new Button("Show Reports");
    reportBtn.setOnAction(e -> {
        int totalBookings = bookings.size();
        double totalRevenue = payments.stream().mapToDouble(Payment::getAmount).sum();
        Alert alert = new Alert(Alert.AlertType.INFORMATION,
            "Total Bookings: " + totalBookings + "\nTotal Revenue: $" + totalRevenue,
            ButtonType.OK);
        alert.setHeaderText("System Report");
        alert.showAndWait();
    });
     Button logoutBtn = new Button("Logout");
    logoutBtn.setOnAction(e -> {
        currentUser = null;
        showMainMenu();
    });
    Button changePasswordBtn = new Button("Change Password");
changePasswordBtn.setOnAction(e -> showChangePasswordScreen());
    adminDashboard.getChildren().addAll(titleLabel, manageUsersBtn, managePackagesBtn, reportBtn,changePasswordBtn, logoutBtn, statusLabel);
    rootLayout.getChildren().setAll(adminDashboard);
}
private void showAgentDashboard() {
    VBox agentDashboard = new VBox(15);
    agentDashboard.setAlignment(Pos.CENTER);
    Label titleLabel = new Label("Agent Dashboard");
    titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");
    Button manageBookingsBtn = new Button("Manage Bookings");
    manageBookingsBtn.setOnAction(e -> showManageBookingsScreen());
     Button logoutBtn = new Button("Logout");
    logoutBtn.setOnAction(e -> {
        currentUser = null;
        showMainMenu();
    });
    Button changePasswordBtn = new Button("Change Password");
changePasswordBtn.setOnAction(e -> showChangePasswordScreen());
agentDashboard.getChildren().addAll(titleLabel, manageBookingsBtn,changePasswordBtn, logoutBtn, statusLabel);
    rootLayout.getChildren().setAll(agentDashboard);
}
private void showClientDashboard() {
    VBox clientDashboard = new VBox(15);
    clientDashboard.setAlignment(Pos.CENTER);
    Label titleLabel = new Label("Client Dashboard");
    titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");
    Button viewPackagesBtn = new Button("View Packages");
    viewPackagesBtn.setOnAction(e -> showPackagesScreen());
    Button myBookingsBtn = new Button("My Bookings");
    myBookingsBtn.setOnAction(e -> showMyBookingsScreen());
    Button suggestBtn = new Button("Suggest Package");
    suggestBtn.setOnAction(e -> showCustomPackageWizard());
    Button logoutBtn = new Button("Logout");
    logoutBtn.setOnAction(e -> {
        currentUser = null;
        showMainMenu();
    });
    Button changePasswordBtn = new Button("Change Password");
    changePasswordBtn.setOnAction(e -> showChangePasswordScreen());
    clientDashboard.getChildren().addAll(
        titleLabel, viewPackagesBtn, myBookingsBtn, suggestBtn, changePasswordBtn, logoutBtn, statusLabel
    );
    rootLayout.getChildren().setAll(clientDashboard);
}
private void showCustomPackageWizard() {
    VBox wizardContent = new VBox(15);
    wizardContent.setAlignment(Pos.CENTER);
    Label titleLabel = new Label("Custom Package Wizard");
    titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");
    Label budgetLabel = new Label("Budget (USD):");
    TextField budgetField = new TextField();
    budgetField.setPromptText("Enter your budget");
    Label countryLabel = new Label("Country:");
    ComboBox<String> countryBox = new ComboBox<>();
    countryBox.getItems().addAll(loadCountriesFromCSV("C:/Users/HP/Downloads/simplemaps_worldcities_basicv1.901/worldcities.csv"));
    countryBox.setPromptText("Select Country");
    Label cityLabel = new Label("City:");
    ComboBox<String> cityBox = new ComboBox<>();
    cityBox.setPromptText("Select City");
    countryBox.setOnAction(e -> {
        String selectedCountry = countryBox.getValue();
        if (selectedCountry != null) {
            cityBox.getItems().setAll(loadCitiesForCountry("C:/Users/HP/Downloads/simplemaps_worldcities_basicv1.901/worldcities.csv", selectedCountry));
        } else {
            cityBox.getItems().clear();
        }
    });

    Label typeLabel = new Label("Trip Type:");
    ComboBox<String> typeBox = new ComboBox<>();
    typeBox.getItems().addAll("Adventure", "Relaxation", "Cultural", "Any");
    typeBox.setPromptText("Select Type");
    Label airlineLabel = new Label("Preferred Airline:");
    TextField airlineField = new TextField();
    airlineField.setPromptText("Enter airline or 'Any'");
    Label classLabel = new Label("Flight Class:");
    ComboBox<String> classBox = new ComboBox<>();
    classBox.getItems().addAll("ECONOMY", "BUSINESS", "FIRST");
    classBox.setPromptText("Select Class");
    Label hotelStarLabel = new Label("Hotel Stars:");
    ComboBox<String> hotelStarBox = new ComboBox<>();
    hotelStarBox.getItems().addAll("3", "4", "5");
    hotelStarBox.setPromptText("Select Stars");
    Button suggestBtn = new Button("Suggest My Package");
    TextArea resultArea = new TextArea();
    resultArea.setEditable(false);
    resultArea.setPrefHeight(220);
    resultArea.setPrefWidth(350);
    Button bookBtn = new Button("Book This Custom Package");
    bookBtn.setDisable(true);
    final TripPackage[] lastCreatedPkg = new TripPackage[1]; // To hold the last created package
    suggestBtn.setOnAction(e -> {
        resultArea.clear();
        double budget;
        try {
            budget = Double.parseDouble(budgetField.getText());
        } catch (Exception ex) {
            statusLabel.setText("Enter a valid budget.");
            return;
        }
        String country = countryBox.getValue();
        String city = cityBox.getValue();
        String type = typeBox.getValue();
        String airline = airlineField.getText().trim();
        String flightClass = classBox.getValue();
        String hotelStars = hotelStarBox.getValue();

        if (flightClass == null || hotelStars == null) {
            statusLabel.setText("Please answer all required questions.");
            return;
        }
        double basePrice = 500;
        StringBuilder breakdown = new StringBuilder("Bill Breakdown:\n");
        breakdown.append("Base Package: $500\n");

        if (country != null && !country.equals("Any")) {
            basePrice += 300;
            breakdown.append("Country preference: +$300\n");
        }
        if (city != null && !city.isEmpty()) {
            basePrice += 200;
            breakdown.append("City preference: +$200\n");
        }
        if (type != null && !type.equals("Any")) {
            basePrice += 150;
            breakdown.append("Trip type (").append(type).append("): +$150\n");
        }
        if (!airline.isEmpty() && !airline.equalsIgnoreCase("Any")) {
            basePrice += 100;
            breakdown.append("Preferred airline: +$100\n");
        }
        if ("BUSINESS".equals(flightClass)) {
            basePrice += 400;
            breakdown.append("Flight class (BUSINESS): +$400\n");
        } else if ("FIRST".equals(flightClass)) {
            basePrice += 800;
            breakdown.append("Flight class (FIRST): +$800\n");
        } else {
            breakdown.append("Flight class (ECONOMY): +$100\n");
        }
        if ("4".equals(hotelStars)) {
            basePrice += 200;
            breakdown.append("Hotel stars (4): +$200\n");
        } else if ("5".equals(hotelStars)) {
            basePrice += 400;
            breakdown.append("Hotel stars (5): +$400\n");
        } else {
            breakdown.append("Hotel stars (3): +$100\n");
        }

        boolean affordable = budget >= basePrice;

        String id = getNextPackageId();
        String destination = (city != null && !city.isEmpty() ? city + ", " : "") + (country != null && !country.equals("Any") ? country : "Any");
        String name = "Custom Package";
        int days = 5;
        String activities = (type != null ? type : "Any") +
            (airline.isEmpty() ? "" : ", Airline: " + airline) +
            ", Class: " + flightClass +
            ", Hotel: " + hotelStars + " star";

        TripPackage newPkg = new TripPackage(id, name, destination, basePrice, days, activities);
        // Save to DB first, then reload packages to ensure the ID is present for booking
        savePackageToDatabase(newPkg);
        loadPackagesFromDatabase();
        // Find the package just saved (by ID)
        TripPackage savedPkg = null;
        for (TripPackage pkg : packages) {
            if (pkg.getId().trim().equalsIgnoreCase(id.trim())) {
                savedPkg = pkg;
                break;
            }
        }
        if (savedPkg == null) {
            statusLabel.setText("Failed to save custom package. Please try again.");
            return;
        }
        lastCreatedPkg[0] = savedPkg; // Save for booking
        bookBtn.setDisable(false);  // Enable the book button

        StringBuilder sb = new StringBuilder();
        sb.append("Custom Package Created & Saved!\n");
        sb.append("ID: ").append(id);
        sb.append("\nDestination: ").append(destination);
        sb.append("\nType: ").append(type != null ? type : "Any");
        sb.append("\nAirline: ").append(!airline.isEmpty() ? airline : "Any");
        sb.append("\nFlight Class: ").append(flightClass);
        sb.append("\nHotel Stars: ").append(hotelStars);
        sb.append("\nEstimated Price: $").append(String.format("%.2f", basePrice));
        sb.append("\n\n").append(breakdown.toString());
        if (!affordable) sb.append("\nWarning: This package exceeds your budget!");

        resultArea.setText(sb.toString());
        statusLabel.setText(affordable ? "Custom package created and saved!   please note this package is the lum sum calculation real calculation might differ" : "Custom package exceeds budget, but saved.");
    });

    // Book the last created custom package
    bookBtn.setOnAction(e -> {
        if (lastCreatedPkg[0] == null) {
            statusLabel.setText("No custom package to book. Please create one first.");
            return;
        }
        if (!(currentUser instanceof Client)) {
            statusLabel.setText("Only clients can book packages.");
            return;
        }
        // Always reload packages before booking to ensure the package exists in DB
        loadPackagesFromDatabase();

        Booking booking = new Booking(
            lastCreatedPkg[0].getId(),
            currentUser.getUsername(),
            new Date().toString(),
            "Pending"
        );
        saveBookingToDatabase(booking);
        loadBookingsFromDatabase();
        boolean found = bookings.stream().anyMatch(b ->
            b.getPackageId().trim().equalsIgnoreCase(lastCreatedPkg[0].getId().trim()) &&
            b.getClientUsername().trim().equalsIgnoreCase(currentUser.getUsername().trim())
        );
        if (found) {
            statusLabel.setText("Booking successful for your custom package!");
            showMyBookingsScreen();
        } else {
            statusLabel.setText("Booking failed to save. Please try again.");
        }
    });

    Button backBtn = new Button("Back");
    backBtn.setOnAction(e -> showClientDashboard());
    wizardContent.getChildren().addAll(
        titleLabel, budgetLabel, budgetField, countryLabel, countryBox, cityLabel, cityBox,
        typeLabel, typeBox, airlineLabel, airlineField, classLabel, classBox, hotelStarLabel, hotelStarBox,
        suggestBtn, bookBtn, resultArea, backBtn, statusLabel
    );
    ScrollPane scrollPane = new ScrollPane(wizardContent);
    scrollPane.setFitToWidth(true);
    scrollPane.setPrefViewportHeight(500);
    scrollPane.setPrefViewportWidth(350);
    scrollPane.setStyle("-fx-background:transparent;");
    VBox wrapper = new VBox(scrollPane);
    wrapper.setAlignment(Pos.CENTER);
    wrapper.setPadding(new Insets(20));
    rootLayout.getChildren().setAll(wrapper);
}
private void showPackagesScreen() {
    loadPackagesFromDatabase();
    VBox packagesScreen = new VBox(15);
    packagesScreen.setAlignment(Pos.CENTER);
    Label titleLabel = new Label("Available Packages");
    titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");
    TextField searchField = new TextField();
    searchField.setPromptText("Search by name, destination, or price...");
    ListView<String> packageList = new ListView<>();
    Map<String, TripPackage> packageMap = new HashMap<>();
   // Details area
    TextArea detailsArea = new TextArea();
    detailsArea.setEditable(false);
    detailsArea.setPrefHeight(120);
    detailsArea.setPrefWidth(600);
    // Helper to refresh the list based on search
    Runnable refreshList = () -> {
        packageList.getItems().clear();
        packageMap.clear();
        String query = searchField.getText().toLowerCase();
        for (TripPackage pkg : packages) {
            String display = pkg.getId() + " - " + pkg.getName() + " (" + pkg.getDestination() + ") - $" + pkg.getPrice();
            if (query.isEmpty() ||
                pkg.getName().toLowerCase().contains(query) ||
                pkg.getDestination().toLowerCase().contains(query) ||
                String.valueOf(pkg.getPrice()).contains(query)) {
                packageList.getItems().add(display);
                packageMap.put(display, pkg);
            } }
        detailsArea.clear();
    };

    // Initial population
    refreshList.run();
    // Filter as user types
    searchField.textProperty().addListener((obs, oldVal, newVal) -> refreshList.run());
    packageList.setPrefHeight(200);
    packageList.setPrefWidth(600);
    // Show details when a package is selected
    packageList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
        if (newVal != null) {
            TripPackage pkg = packageMap.get(newVal);
            detailsArea.setText(
                "Package ID: " + pkg.getId() + "\n" +
                "Name: " + pkg.getName() + "\n" +
                "Destination: " + pkg.getDestination() + "\n" +
                "Price: $" + pkg.getPrice() + "\n" +
                "Days: " + pkg.getDays() + "\n" +
                "Itinerary/Activities: " + pkg.getActivities()
            );
        } else {
            detailsArea.clear();
        }});
    Button sortBtn = new Button("Sort by Price (Bubble Sort)");
    sortBtn.setOnAction(e -> {
        bubbleSortPackagesByPrice(packages);
        statusLabel.setText("Packages sorted by price using Bubble Sort!");
        refreshList.run();
    });
    Button bookBtn = new Button("Book Selected Package");
    bookBtn.setOnAction(e -> {
        String selected = packageList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            statusLabel.setText("Please select a package to book.");
            return;
        }
        if (!(currentUser instanceof Client)) {
            statusLabel.setText("Only clients can book packages.");
            return;
        }
        TripPackage selectedPkg = packageMap.get(selected);
        if (selectedPkg == null) {
            statusLabel.setText("Error: Selected package not found.");
            return;
        }
        // Always reload packages to ensure the package exists in DB
        loadPackagesFromDatabase();
        boolean packageExists = packages.stream()
            .anyMatch(pkg -> pkg.getId().trim().equalsIgnoreCase(selectedPkg.getId().trim()));
        if (!packageExists) {
            statusLabel.setText("Error: Package does not exist in the database.");
            return;
        }
        // Check if this client already has a booking for this package
        loadBookingsFromDatabase();
        boolean alreadyBooked = bookings.stream().anyMatch(b ->
            b.getPackageId().trim().equalsIgnoreCase(selectedPkg.getId().trim()) &&
            b.getClientUsername().trim().equalsIgnoreCase(currentUser.getUsername().trim())
        );
        if (alreadyBooked) {
            statusLabel.setText("You have already booked this package.");
            return;
        }
        // Create a new booking with the correct package ID
        Booking booking = new Booking(
            selectedPkg.getId(), // <-- Make sure this is the package ID, not the date!
            currentUser.getUsername(),
            new Date().toString(),
            "Pending"
        );
        saveBookingToDatabase(booking);
        // Check if booking was saved
        loadBookingsFromDatabase();
        boolean found = bookings.stream().anyMatch(b ->
            b.getPackageId().trim().equalsIgnoreCase(selectedPkg.getId().trim()) &&
            b.getClientUsername().trim().equalsIgnoreCase(currentUser.getUsername().trim())
        );
        if (found) {
            statusLabel.setText("Booking successful for package: " + selectedPkg.getName());
        } else {
            statusLabel.setText("Booking failed to save. Please try again.");
        }
    });
    Button backBtn = new Button("Back");
    backBtn.setOnAction(e -> showClientDashboard());
    packagesScreen.getChildren().addAll(titleLabel, searchField, sortBtn, packageList, detailsArea, bookBtn, backBtn, statusLabel);
    rootLayout.getChildren().setAll(packagesScreen);
}
private void showManagePackagesScreen() {
    VBox managePackagesScreen = new VBox(20);
     managePackagesScreen.setAlignment(Pos.CENTER);
    Label titleLabel = new Label("Manage Packages");
    titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");
    ListView<String> packageList = new ListView<>();
    Map<String, TripPackage> packageMap = new HashMap<>();
    for (TripPackage pkg : packages) {
        String display = pkg.getId() + " - " + pkg.getName() + " (" + pkg.getDestination() + ") - $" + pkg.getPrice();
        packageList.getItems().add(display);
        packageMap.put(display, pkg);
    }
    packageList.setPrefHeight(200);
    packageList.setPrefWidth(700);

    // Harmonized input fields
    TextField nameField = new TextField();
    nameField.setPromptText("Name");
    nameField.setPrefWidth(180);
      List<String> countries = loadCountriesFromCSV("C:/Users/HP/Downloads/simplemaps_worldcities_basicv1.901/worldcities.csv");
    ComboBox<String> countryComboBox = new ComboBox<>();
    countryComboBox.getItems().addAll(countries);
    countryComboBox.setPromptText("Select Country");
    countryComboBox.setPrefWidth(180);
    ComboBox<String> cityComboBox = new ComboBox<>();
    cityComboBox.setPromptText("Select City");
    cityComboBox.setPrefWidth(180);
    countryComboBox.setOnAction(e -> {
        String selectedCountry = countryComboBox.getValue();
        if (selectedCountry != null) {
            List<String> cities = loadCitiesForCountry("C:/Users/HP/Downloads/simplemaps_worldcities_basicv1.901/worldcities.csv", selectedCountry);
            cityComboBox.getItems().setAll(cities);
        } else {
            cityComboBox.getItems().clear();
        }  });
    TextField priceField = new TextField();
    priceField.setPromptText("Price");
    priceField.setPrefWidth(100);
    TextField daysField = new TextField();
    daysField.setPromptText("Days");
    daysField.setPrefWidth(80);
    TextField descField = new TextField();
    descField.setPromptText("Description");
    descField.setPrefWidth(200);
    HBox fields = new HBox(10, nameField, countryComboBox, cityComboBox, priceField, daysField, descField);
    fields.setAlignment(Pos.CENTER);
    fields.setPadding(new Insets(10));
    Button addBtn = new Button("Add");
    addBtn.setPrefWidth(100);
    addBtn.setOnAction(e -> {
        try {
            String name = nameField.getText();
            String country = countryComboBox.getValue();
            String city = cityComboBox.getValue();
            String priceText = priceField.getText();
            String daysText = daysField.getText();
            String desc = descField.getText();
            if (name.isEmpty() || country == null || city == null || priceText.isEmpty() || daysText.isEmpty() || desc.isEmpty()) {
                statusLabel.setText("All fields are required.");
                return;
            }
            double price = Double.parseDouble(priceText);
            int days = Integer.parseInt(daysText);

            if (price <= 0 || days <= 0) {
                statusLabel.setText("Price and Days must be positive numbers.");
                return;
            }
            String id = getNextPackageId();
            String destination = city + ", " + country;
            TripPackage newPkg = new TripPackage(id, name, destination, price, days, desc);
            packages.add(newPkg);
            savePackageToDatabase(newPkg);
            statusLabel.setText("Package added with ID: " + id);
            showManagePackagesScreen();
        } catch (NumberFormatException ex) {
            statusLabel.setText("Price and Days must be valid numbers.");
        } catch (Exception ex) {
            statusLabel.setText("Invalid input.");
        }
    });
    Button editBtn = new Button("Edit");
    editBtn.setPrefWidth(100);
    editBtn.setOnAction(e -> {
        String selected = packageList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            statusLabel.setText("Select a package to edit.");
            return;
        }
        TripPackage pkg = packageMap.get(selected);
        nameField.setText(pkg.getName());
        String[] destParts = pkg.getDestination().split(", ");
        if (destParts.length == 2) {
            cityComboBox.setValue(destParts[0]);
            countryComboBox.setValue(destParts[1]);
        }
        priceField.setText(String.valueOf(pkg.getPrice()));
        daysField.setText(String.valueOf(pkg.getDays()));
        descField.setText(pkg.getActivities());
        packages.remove(pkg);
    });
    Button deleteBtn = new Button("Delete");
    deleteBtn.setPrefWidth(100);
    deleteBtn.setOnAction(e -> {
        String selected = packageList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            statusLabel.setText("Select a package to delete.");
            return;
        }
        TripPackage pkg = packageMap.get(selected);

        // Prevent deletion if bookings exist for this package
        boolean hasBooking = bookings.stream().anyMatch(b -> b.getPackageId().equals(pkg.getId()));
        if (hasBooking) {
            statusLabel.setText("Cannot delete: Bookings exist for this package.");
            return;
        }
       packages.remove(pkg);
        deletePackageFromDatabase(pkg.getId());
        statusLabel.setText("Package deleted.");
        showManagePackagesScreen();
    });
     Button exportPackagesBtn = new Button("Export Packages to TXT");
    exportPackagesBtn.setPrefWidth(180);
    exportPackagesBtn.setOnAction(e -> {
        try (PrintWriter writer = new PrintWriter(new FileWriter("packages_export.txt"))) {
            for (TripPackage pkg : packages) {
                writer.println(
                    "ID: " + pkg.getId() +
                    " | Name: " + pkg.getName() +
                    " | Destination: " + pkg.getDestination() +
                    " | Price: $" + pkg.getPrice() +
                    " | Days: " + pkg.getDays() +
                    " | Activities: " + pkg.getActivities()
                );
            }
            statusLabel.setText("Packages exported to packages_export.txt");
        } catch (Exception ex) {
            statusLabel.setText("Export failed: " + ex.getMessage());
        }
    });

    Button backBtn = new Button("Back");
    backBtn.setPrefWidth(100);
    backBtn.setOnAction(e -> showAdminDashboard());
    HBox buttonBox = new HBox(15, addBtn, editBtn, deleteBtn, exportPackagesBtn, backBtn);
    buttonBox.setAlignment(Pos.CENTER);
    buttonBox.setPadding(new Insets(10));
    managePackagesScreen.getChildren().addAll(titleLabel, packageList, fields, buttonBox, statusLabel);
    rootLayout.getChildren().setAll(managePackagesScreen);
}
private void showManageUsersScreen() {
    VBox manageUsersScreen = new VBox(15);
    manageUsersScreen.setAlignment(Pos.CENTER);
    Label titleLabel = new Label("All Users");
    titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");
    ListView<String> userList = new ListView<>();
    Map<String, User> userMap = new HashMap<>();
    for (User user : users) {
        String display = user.getUsername() + " - " + user.getClass().getSimpleName() + " - " + user.getName();
        userList.getItems().add(display);
        userMap.put(display, user);
    }
    userList.setPrefHeight(200);
    userList.setPrefWidth(600);
    TextField nameField = new TextField();
    nameField.setPromptText("Name");
    TextField emailField = new TextField();
    emailField.setPromptText("Email");
    Button editBtn = new Button("Edit Selected User");
    editBtn.setOnAction(e -> {
        String selected = userList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            statusLabel.setText("Select a user to edit.");
            return;
        }
        User user = userMap.get(selected);
        nameField.setText(user.getName());
        emailField.setText(user.getEmail());
        // Remove so admin can re-add after editing
        users.remove(user);
    });
    Button saveBtn = new Button("Save Changes");
    saveBtn.setOnAction(e -> {
        String selected = userList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            statusLabel.setText("Select a user to save.");
            return;
        }
        User oldUser = userMap.get(selected);
        String name = nameField.getText();
        String email = emailField.getText();
        if (oldUser instanceof Client) {
            users.add(new Client(oldUser.getUsername(), oldUser.getPassword(), email, name, ((Client) oldUser).getCountry(), ((Client) oldUser).getCity()));
        } else if (oldUser instanceof Agent) {
            users.add(new Agent(oldUser.getUsername(), oldUser.getPassword(), email, name));
        } else if (oldUser instanceof Admin) {
            users.add(new Admin(oldUser.getUsername(), oldUser.getPassword(), email, name));
        }
        statusLabel.setText("User updated.");
        updateUserInDatabase(oldUser.getUsername(), name, email);
        showManageUsersScreen();
    });
    Button deleteBtn = new Button("Delete Selected User");
    deleteBtn.setOnAction(e -> {
        String selected = userList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            statusLabel.setText("Select a user to delete.");
            return;
        }
        User user = userMap.get(selected);
        if (user.getUsername().equals("admin")) {
            statusLabel.setText("Cannot delete the admin user.");
            return;
        }
        users.remove(user);
        deleteUserFromDatabase(user.getUsername());
        statusLabel.setText("User deleted.");
        showManageUsersScreen();
    });
    Button exportUsersBtn = new Button("Export Users to TXT");
    exportUsersBtn.setOnAction(e -> {
        try (PrintWriter writer = new PrintWriter(new FileWriter("users_export.txt"))) {
            for (User user : users) {
                writer.println(
                    "Username: " + user.getUsername() +
                    " | Type: " + user.getClass().getSimpleName() +
                    " | Name: " + user.getName() +
                    " | Email: " + user.getEmail()
                );
            }
            statusLabel.setText("Users exported to users_export.txt");
        } catch (Exception ex) {
            statusLabel.setText("Export failed: " + ex.getMessage());
        }
    });
    Button backBtn = new Button("Back");
    backBtn.setOnAction(e -> showAdminDashboard());
    manageUsersScreen.getChildren().addAll(
        titleLabel, userList, nameField, emailField, editBtn, saveBtn, deleteBtn, exportUsersBtn, backBtn, statusLabel
    );
    rootLayout.getChildren().setAll(manageUsersScreen);
}
private void showPaymentScreen(Booking booking) {
    VBox paymentScreen = new VBox(15);
    paymentScreen.setAlignment(Pos.CENTER);
    Label titleLabel = new Label("Payment for Booking: " + booking.getPackageId());
    titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");
    TextField cardField = new TextField();
    cardField.setPromptText("Card Number");
    cardField.setPrefWidth(300);
    // Always reload packages, bookings, and payments from database to ensure latest data
    loadPackagesFromDatabase();
    loadBookingsFromDatabase();
    loadPaymentsFromDatabase();
    // Debug: Print IDs to help trace issues
    System.out.println("Looking for package ID: " + booking.getPackageId());
    for (TripPackage p : packages) {
        System.out.println("Available package: " + p.getId());
    }
    // Check if payment already exists for this client and package
    boolean alreadyPaid = payments.stream().anyMatch(p ->
        p.getClientUsername().trim().equalsIgnoreCase(booking.getClientUsername().trim()) &&
        p.getPackageId().trim().equalsIgnoreCase(booking.getPackageId().trim()) &&
        "Paid".equalsIgnoreCase(p.getStatus())
    );
    if (alreadyPaid) {
        statusLabel.setText("You have already paid for this package.");
        showMyBookingsScreen();
        return;
    }

    // Find the package for this booking
    TripPackage pkg = null;
    for (TripPackage p : packages) {
        if (p.getId().equals(booking.getPackageId())) {
            pkg = p;
            break;
        }
    }

    double amount = (pkg != null) ? pkg.getPrice() : 0.0;
    Label amountLabel = new Label("Amount to Pay: $" + String.format("%.2f", amount));
    amountLabel.setPrefWidth(300);
    Button payBtn = new Button("Pay");
    payBtn.setPrefWidth(150);
    payBtn.setOnAction(e -> {
        String card = cardField.getText();
        if (card.isEmpty()) {
            statusLabel.setText("Enter card number.");
            return;
        }
        // Reload packages, bookings, and payments again before paying
        loadPackagesFromDatabase();
        loadBookingsFromDatabase();
        loadPaymentsFromDatabase();

        // Check again if payment already exists (in case of race condition)
        boolean paid = payments.stream().anyMatch(p ->
            p.getClientUsername().trim().equalsIgnoreCase(booking.getClientUsername().trim()) &&
            p.getPackageId().trim().equalsIgnoreCase(booking.getPackageId().trim()) &&
            "Paid".equalsIgnoreCase(p.getStatus())
        );
        if (paid) {
            statusLabel.setText("You have already paid for this package.");
            showMyBookingsScreen();
            return;
        }
        // Debug: Print IDs again
        System.out.println("Paying for package ID: " + booking.getPackageId());
        for (TripPackage p2 : packages) {
            System.out.println("Available package: " + p2.getId());
        }
        TripPackage payPkg = null;
        for (TripPackage p2 : packages) {
            if (p2.getId().equals(booking.getPackageId())) {
                payPkg = p2;
                break;
            }
        }
        double payAmount = (payPkg != null) ? payPkg.getPrice() : 0.0;
        if (payPkg == null) {
            statusLabel.setText("Package not found. Please contact support.");
            return;
        }
        Payment payment = new Payment(booking.getClientUsername(), booking.getPackageId(), payAmount, "Paid");
        payments.add(payment);
        savePaymentToDatabase(payment); // Save to database
        // Always reload payments and bookings after saving payment to keep in sync
        loadPaymentsFromDatabase();
        loadBookingsFromDatabase();
        statusLabel.setText("Payment successful! Paid $" + String.format("%.2f", payAmount));
        showMyBookingsScreen();
    });

    Button backBtn = new Button("Back");
    backBtn.setPrefWidth(150);
    backBtn.setOnAction(e -> showMyBookingsScreen());
    VBox.setMargin(amountLabel, new Insets(0, 0, 10, 0));
    VBox.setMargin(cardField, new Insets(0, 0, 10, 0));
    paymentScreen.getChildren().addAll(titleLabel, amountLabel, cardField, payBtn, backBtn, statusLabel);
    rootLayout.getChildren().setAll(paymentScreen);
}
private void showChangePasswordScreen() {
    VBox changePwScreen = new VBox(15);
    changePwScreen.setAlignment(Pos.CENTER);
    Label titleLabel = new Label("Change Password");
    titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");
    PasswordField oldPwField = new PasswordField();
    oldPwField.setPromptText("Current Password");
    oldPwField.setPrefWidth(300);
     PasswordField newPwField = new PasswordField();
    newPwField.setPromptText("New Password");
    newPwField.setPrefWidth(300);
    Button saveBtn = new Button("Save");
    saveBtn.setOnAction(e -> {
        String oldPw = oldPwField.getText();
        String newPw = newPwField.getText();
        if (oldPw.isEmpty() || newPw.isEmpty()) {
            statusLabel.setText("Both fields are required.");
            return;
        }
        if (!currentUser.authenticate(oldPw)) {
            statusLabel.setText("Current password is incorrect.");
            return;
        }
        currentUser.setPassword(newPw);
        updateUserPasswordInDatabase(currentUser.getUsername(), newPw);
        statusLabel.setText("Password changed successfully!");
        // Return to dashboard
        if (currentUser instanceof Admin) showAdminDashboard();
        else if (currentUser instanceof Agent) showAgentDashboard();
        else showClientDashboard();
    });

    Button backBtn = new Button("Back");
    backBtn.setOnAction(e -> {
        if (currentUser instanceof Admin) showAdminDashboard();
        else if (currentUser instanceof Agent) showAgentDashboard();
        else showClientDashboard();
    });

    changePwScreen.getChildren().addAll(titleLabel, oldPwField, newPwField, saveBtn, backBtn, statusLabel);
    rootLayout.getChildren().setAll(changePwScreen);
}
private String getIATACodeForCity(String city, String country) {
    try (BufferedReader br = new BufferedReader(new FileReader("C:/Users/HP/Downloads/simplemaps_worldcities_basicv1.901/worldcities.csv"))) {
        String line;
        boolean firstLine = true;
        while ((line = br.readLine()) != null) {
            if (firstLine) { firstLine = false; continue; }
            String[] parts = line.split(",");
            if (parts.length > 7) {
                String csvCity = parts[0].replaceAll("\"", "").trim();
                String csvCountry = parts[4].replaceAll("\"", "").trim();
                String iata = parts[7].replaceAll("\"", "").trim(); // Adjust index if needed
                if (csvCity.equalsIgnoreCase(city) && csvCountry.equalsIgnoreCase(country) && !iata.isEmpty()) {
                    return iata;
                } } }
    } catch (IOException e) {
        e.printStackTrace();
    }
    return "CDG"; // fallback
}

private String getDestIdForCity(String city, String country) {
    try (BufferedReader br = new BufferedReader(new FileReader("C:/Users/HP/Downloads/simplemaps_worldcities_basicv1.901/worldcities.csv"))) {
        String line;
        boolean firstLine = true;
        while ((line = br.readLine()) != null) {
            if (firstLine) { firstLine = false; continue; }
            String[] parts = line.split(",");
            if (parts.length > 10) {
                String csvCity = parts[0].replaceAll("\"", "").trim();
                String csvCountry = parts[4].replaceAll("\"", "").trim();
                String destId = parts[10].replaceAll("\"", "").trim(); // Adjust index if needed
                if (csvCity.equalsIgnoreCase(city) && csvCountry.equalsIgnoreCase(country) && !destId.isEmpty()) {
                    return destId;
                }
            }
        }
    } catch (IOException e) {
        e.printStackTrace();
    }
    return "-1456928"; // fallback
}
private List<String> loadCountriesFromCSV(String filePath) {
    Set<String> countrySet = new TreeSet<>(); // Sorted and unique
    try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
        String line;
        boolean firstLine = true;
        while ((line = br.readLine()) != null) {
            if (firstLine) { // skip header
                firstLine = false;
                continue;
            }
            String[] parts = line.split(",");
            if (parts.length > 4) {
                String country = parts[4].replaceAll("\"", "").trim(); // country column
                if (!country.isEmpty()) {
                    countrySet.add(country);
                }
            }
        }
    } catch (IOException e) {
        e.printStackTrace();
    }
    return new ArrayList<>(countrySet);
}
private List<String> loadCitiesForCountry(String filePath, String countryName) {
    Set<String> citySet = new TreeSet<>();
    try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
        String line;
        boolean firstLine = true;
        while ((line = br.readLine()) != null) {
            if (firstLine) { // skip header
                firstLine = false;
                continue;
            }
            String[] parts = line.split(",");
            if (parts.length > 4) {
                String country = parts[4].replaceAll("\"", "").trim();
                String city = parts[0].replaceAll("\"", "").trim();
                if (country.equals(countryName) && !city.isEmpty()) {
                    citySet.add(city);
                }
            }
        }
    } catch (IOException e) {
        e.printStackTrace();
    }
    return new ArrayList<>(citySet);
}
private HBox createPasswordFieldWithToggle(PasswordField passwordField, TextField visibleField) {
    visibleField.setManaged(false);
    visibleField.setVisible(false);
    visibleField.managedProperty().bind(visibleField.visibleProperty());
    visibleField.visibleProperty().bind(passwordField.visibleProperty().not());
    visibleField.textProperty().bindBidirectional(passwordField.textProperty());
    Button toggleBtn = new Button("\uD83D\uDC41"); // Unicode for eye 👁
    toggleBtn.setFocusTraversable(false);
    toggleBtn.setStyle("-fx-background-color: transparent; -fx-font-size: 14px;");
    toggleBtn.setOnAction(e -> {
        boolean showing = passwordField.isVisible();
        passwordField.setVisible(!showing);
        passwordField.setManaged(!showing);
        visibleField.setVisible(showing);
        visibleField.setManaged(showing);
    });
    HBox box = new HBox(passwordField, visibleField, toggleBtn);
    box.setAlignment(Pos.CENTER_LEFT);
    return box;
}

private void showHotelSearchScreen() {
        VBox hotelScreen = new VBox(15);
    hotelScreen.setAlignment(Pos.CENTER);
    Label titleLabel = new Label("Real-Time Hotel Search");
    titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");
    TextField cityField = new TextField();
    cityField.setPromptText("Enter city (e.g. Paris)");
    Button searchBtn = new Button("Search");
    ListView<String> hotelList = new ListView<>();
    Map<String, String> hotelIdMap = new HashMap<>(); // Map display string to hotel_id
    searchBtn.setOnAction(e -> {
        hotelList.getItems().clear();
        hotelIdMap.clear();
        String city = cityField.getText().trim();
        if (city.isEmpty()) {
            statusLabel.setText("Enter a city.");
            return;
        }
        String destId = city.equalsIgnoreCase("Dubai") ? "-2092174" : "-1456928";
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://booking-com15.p.rapidapi.com/api/v1/hotels/searchHotels?dest_id=" + destId + "&search_type=CITY&adults=1&children_age=0%2C17&room_qty=1&page_number=1&units=metric&temperature_unit=c&languagecode=en-us&currency_code=AED&location=US"))
                .header("x-rapidapi-key", "0f43e34666msh37c1128e19ad979p1fd01ejsnb00068111738")
                .header("x-rapidapi-host", "booking-com15.p.rapidapi.com")
                .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
            JsonArray hotels = json.getAsJsonArray("data");
            if (hotels != null && hotels.size() > 0) {
                for (JsonElement el : hotels) {
                    JsonObject hotel = el.getAsJsonObject();
                    String name = hotel.get("hotel_name").getAsString();
                    String price = hotel.has("min_total_price") ? hotel.get("min_total_price").getAsString() : "N/A";
                    String hotelId = hotel.has("hotel_id") ? hotel.get("hotel_id").getAsString() : "";
                    String display = name + " - " + price + " AED";
                    hotelList.getItems().add(display);
                    hotelIdMap.put(display, hotelId);
                }
            } else {
                hotelList.getItems().add("No hotels found.");
            }
        } catch (Exception ex) {
            hotelList.getItems().add("Error fetching hotels.");
            ex.printStackTrace();
        }
    });
    hotelList.setOnMouseClicked(event -> {
        if (event.getClickCount() == 2) {
            String selected = hotelList.getSelectionModel().getSelectedItem();
            if (selected != null && hotelIdMap.containsKey(selected)) {
                String hotelId = hotelIdMap.get(selected);
                fetchHotelDescription(hotelId, desc -> {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION, desc, ButtonType.OK);
                    alert.setHeaderText("Hotel Description");
                    alert.showAndWait();
                });
            }
        }
    });

    Button backBtn = new Button("Back");
    backBtn.setOnAction(e -> showClientDashboard());
    hotelScreen.getChildren().addAll(titleLabel, cityField, searchBtn, hotelList, backBtn, statusLabel);
        rootLayout.getChildren().setAll(hotelScreen);
}
private void fetchHotelDescription(String hotelId, Consumer<String> callback) {
    try {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("https://booking-com15.p.rapidapi.com/api/v1/hotels/getDescriptionAndInfo?hotel_id=" + hotelId + "&languagecode=en-us"))
            .header("x-rapidapi-key", "0f43e34666msh37c1128e19ad979p1fd01ejsnb00068111738")
            .header("x-rapidapi-host", "booking-com15.p.rapidapi.com")
            .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        // Parse JSON and extract description
        JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
        String description = json.has("description") ? json.get("description").getAsString() : "No description available.";
        callback.accept(description);
    } catch (Exception e) {
        callback.accept("Error fetching hotel info.");
        e.printStackTrace();
    }
}
private void showFlightSearchScreen() {
    VBox flightScreen = new VBox(15);
    flightScreen.setAlignment(Pos.CENTER);

    Label titleLabel = new Label("Real-Time Flight Search");
    titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

    TextField fromField = new TextField();
    fromField.setPromptText("From (IATA code, e.g. BOM)");

    TextField toField = new TextField();
    toField.setPromptText("To (IATA code, e.g. DEL)");

    Button searchBtn = new Button("Search");
    ListView<String> flightList = new ListView<>();

    searchBtn.setOnAction(e -> {
        flightList.getItems().clear();
        String from = fromField.getText().trim();
        String to = toField.getText().trim();
        if (from.isEmpty() || to.isEmpty()) {
            statusLabel.setText("Both fields required.");
            return;
        }

        // Example: fromId=BOM.AIRPORT, toId=DEL.AIRPORT
        String fromId = from + ".AIRPORT";
        String toId = to + ".AIRPORT";

        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://booking-com15.p.rapidapi.com/api/v1/flights/searchFlights?fromId=" + fromId + "&toId=" + toId + "&stops=none&pageNo=1&adults=1&children=0%2C17&sort=BEST&cabinClass=ECONOMY&currency_code=AED"))
                .header("x-rapidapi-key", "0f43e34666msh37c1128e19ad979p1fd01ejsnb00068111738")
                .header("x-rapidapi-host", "booking-com15.p.rapidapi.com")
                .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
            JsonArray flights = json.getAsJsonArray("data");
            if (flights != null && flights.size() > 0) {
                for (JsonElement el : flights) {
                    JsonObject flight = el.getAsJsonObject();
                    String airline = flight.has("airline") ? flight.get("airline").getAsString() : "Unknown Airline";
                    String price = flight.has("price") ? flight.get("price").getAsString() : "N/A";
                    String depTime = flight.has("departure_time") ? flight.get("departure_time").getAsString() : "";
                    String arrTime = flight.has("arrival_time") ? flight.get("arrival_time").getAsString() : "";
                    String display = airline + " | " + depTime + " → " + arrTime + " | " + price + " AED";
                    flightList.getItems().add(display);
                }
            } else {
                flightList.getItems().add("No flights found.");
            }
        } catch (Exception ex) {
            flightList.getItems().add("Error fetching flights.");
            ex.printStackTrace();
        }
    });

    Button backBtn = new Button("Back");
    backBtn.setOnAction(e -> showClientDashboard());

    flightScreen.getChildren().addAll(titleLabel, fromField, toField, searchBtn, flightList, backBtn, statusLabel);
    rootLayout.getChildren().setAll(flightScreen);
}
private void showMultiStopFlightSearchScreen() {
    VBox multiStopScreen = new VBox(15);
    multiStopScreen.setAlignment(Pos.CENTER);

    Label titleLabel = new Label("Multi-Stop Flight Search");
    titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

    TextField from1Field = new TextField();
    from1Field.setPromptText("First Leg From (IATA, e.g. BOM)");
    TextField to1Field = new TextField();
    to1Field.setPromptText("First Leg To (IATA, e.g. AMD)");
    TextField date1Field = new TextField();
    date1Field.setPromptText("First Leg Date (YYYY-MM-DD)");

    TextField from2Field = new TextField();
    from2Field.setPromptText("Second Leg From (IATA, e.g. AMD)");
    TextField to2Field = new TextField();
    to2Field.setPromptText("Second Leg To (IATA, e.g. BOM)");
    TextField date2Field = new TextField();
    date2Field.setPromptText("Second Leg Date (YYYY-MM-DD)");

    Button searchBtn = new Button("Search Multi-Stop Flights");
    ListView<String> flightList = new ListView<>();

    searchBtn.setOnAction(e -> {
        flightList.getItems().clear();
        String from1 = from1Field.getText().trim();
        String to1 = to1Field.getText().trim();
        String date1 = date1Field.getText().trim();
        String from2 = from2Field.getText().trim();
        String to2 = to2Field.getText().trim();
        String date2 = date2Field.getText().trim();

        if (from1.isEmpty() || to1.isEmpty() || date1.isEmpty() || from2.isEmpty() || to2.isEmpty() || date2.isEmpty()) {
            statusLabel.setText("All fields required.");
            return;
        }

        // Build legs JSON
        String legs = String.format("[{'fromId':'%s.AIRPORT','toId':'%s.AIRPORT','date':'%s'},{'fromId':'%s.AIRPORT','toId':'%s.AIRPORT','date':'%s'}]",
            from1, to1, date1, from2, to2, date2);
        String encodedLegs = java.net.URLEncoder.encode(legs, java.nio.charset.StandardCharsets.UTF_8);

        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://booking-com15.p.rapidapi.com/api/v1/flights/searchFlightsMultiStops?legs=" + encodedLegs + "&pageNo=1&adults=1&children=0%2C17&sort=BEST&cabinClass=ECONOMY&currency_code=AED"))
                .header("x-rapidapi-key", "0f43e34666msh37c1128e19ad979p1fd01ejsnb00068111738")
                .header("x-rapidapi-host", "booking-com15.p.rapidapi.com")
                .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
            JsonArray flights = json.getAsJsonArray("data");
            if (flights != null && flights.size() > 0) {
                for (JsonElement el : flights) {
                    JsonObject flight = el.getAsJsonObject();
                    String price = flight.has("price") ? flight.get("price").getAsString() : "N/A";
                    String summary = flight.has("summary") ? flight.get("summary").getAsString() : "";
                    flightList.getItems().add("Price: " + price + " AED | " + summary);
                }
            } else {
                flightList.getItems().add("No multi-stop flights found.");
            }
        } catch (Exception ex) {
            flightList.getItems().add("Error fetching multi-stop flights.");
            ex.printStackTrace();
        }
    });

    Button backBtn = new Button("Back");
    backBtn.setOnAction(e -> showClientDashboard());

    multiStopScreen.getChildren().addAll(titleLabel, from1Field, to1Field, date1Field, from2Field, to2Field, date2Field, searchBtn, flightList, backBtn, statusLabel);
    rootLayout.getChildren().setAll(multiStopScreen);
}
private void showFlightDetailsScreen() {
    VBox detailsScreen = new VBox(15);
    detailsScreen.setAlignment(Pos.CENTER);

    Label titleLabel = new Label("Flight Details Lookup");
    titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

    TextField flightIdField = new TextField();
    flightIdField.setPromptText("Enter Flight ID");

    Button fetchBtn = new Button("Fetch Details");
    TextArea detailsArea = new TextArea();
    detailsArea.setEditable(false);
    detailsArea.setPrefWidth(500);
    detailsArea.setPrefHeight(200);

    fetchBtn.setOnAction(e -> {
        detailsArea.clear();
        String flightId = flightIdField.getText().trim();
        if (flightId.isEmpty()) {
            statusLabel.setText("Enter a flight ID.");
            return;
        }
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://booking-com15.p.rapidapi.com/api/v1/flights/getFlightDetails?flight_id=" + flightId + "&currency_code=AED"))
                .header("x-rapidapi-key", "0f43e34666msh37c1128e19ad979p1fd01ejsnb00068111738")
                .header("x-rapidapi-host", "booking-com15.p.rapidapi.com")
                .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
            detailsArea.setText(json.toString());
        } catch (Exception ex) {
            detailsArea.setText("Error fetching flight details.");
            ex.printStackTrace();
        }
    });
    Button backBtn = new Button("Back");
    backBtn.setOnAction(e -> showClientDashboard());
    detailsScreen.getChildren().addAll(titleLabel, flightIdField, fetchBtn, detailsArea, backBtn, statusLabel);
    rootLayout.getChildren().setAll(detailsScreen);
}

private void showFlightMinPriceScreen() {
    VBox minPriceScreen = new VBox(15);
    minPriceScreen.setAlignment(Pos.CENTER);

    Label titleLabel = new Label("Flight Minimum Price Lookup");
    titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

    TextField fromField = new TextField();
    fromField.setPromptText("From (IATA code, e.g. BOM)");
    TextField toField = new TextField();
    toField.setPromptText("To (IATA code, e.g. DEL)");

    Button fetchBtn = new Button("Get Min Price");
    TextArea resultArea = new TextArea();
    resultArea.setEditable(false);
    resultArea.setPrefWidth(400);
    resultArea.setPrefHeight(100);

    fetchBtn.setOnAction(e -> {
        resultArea.clear();
        String from = fromField.getText().trim();
        String to = toField.getText().trim();
        if (from.isEmpty() || to.isEmpty()) {
            statusLabel.setText("Both fields required.");
            return;
        }
        String fromId = from + ".AIRPORT";
        String toId = to + ".AIRPORT";
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://booking-com15.p.rapidapi.com/api/v1/flights/getMinPrice?fromId=" + fromId + "&toId=" + toId + "&cabinClass=ECONOMY&currency_code=AED"))
                .header("x-rapidapi-key", "0f43e34666msh37c1128e19ad979p1fd01ejsnb00068111738")
                .header("x-rapidapi-host", "booking-com15.p.rapidapi.com")
                .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
            resultArea.setText(json.toString());
        } catch (Exception ex) {
            resultArea.setText("Error fetching minimum price.");
            ex.printStackTrace();
        }
    });

    Button backBtn = new Button("Back");
    backBtn.setOnAction(e -> showClientDashboard());

    minPriceScreen.getChildren().addAll(titleLabel, fromField, toField, fetchBtn, resultArea, backBtn, statusLabel);
    rootLayout.getChildren().setAll(minPriceScreen);
}
private void showFlightMinPriceMultiStopsScreen() {
    VBox minPriceMultiScreen = new VBox(15);
    minPriceMultiScreen.setAlignment(Pos.CENTER);

    Label titleLabel = new Label("Multi-Stop Flight Minimum Price");
    titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

    TextField from1Field = new TextField();
    from1Field.setPromptText("First Leg From (IATA, e.g. BOM)");
    TextField to1Field = new TextField();
    to1Field.setPromptText("First Leg To (IATA, e.g. AMD)");
    TextField date1Field = new TextField();
    date1Field.setPromptText("First Leg Date (YYYY-MM-DD)");

    TextField from2Field = new TextField();
    from2Field.setPromptText("Second Leg From (IATA, e.g. AMD)");
    TextField to2Field = new TextField();
    to2Field.setPromptText("Second Leg To (IATA, e.g. BOM)");
    TextField date2Field = new TextField();
    date2Field.setPromptText("Second Leg Date (YYYY-MM-DD)");

    Button fetchBtn = new Button("Get Min Price");
    TextArea resultArea = new TextArea();
    resultArea.setEditable(false);
    resultArea.setPrefWidth(400);
    resultArea.setPrefHeight(100);

    fetchBtn.setOnAction(e -> {
        resultArea.clear();
        String from1 = from1Field.getText().trim();
        String to1 = to1Field.getText().trim();
        String date1 = date1Field.getText().trim();
        String from2 = from2Field.getText().trim();
        String to2 = to2Field.getText().trim();
        String date2 = date2Field.getText().trim();

        if (from1.isEmpty() || to1.isEmpty() || date1.isEmpty() || from2.isEmpty() || to2.isEmpty() || date2.isEmpty()) {
            statusLabel.setText("All fields required.");
            return;
        }

        String legs = String.format("[{'fromId':'%s.AIRPORT','toId':'%s.AIRPORT','date':'%s'},{'fromId':'%s.AIRPORT','toId':'%s.AIRPORT','date':'%s'}]",
            from1, to1, date1, from2, to2, date2);
        String encodedLegs = java.net.URLEncoder.encode(legs, java.nio.charset.StandardCharsets.UTF_8);

        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://booking-com15.p.rapidapi.com/api/v1/flights/getMinPriceMultiStops?legs=" + encodedLegs + "&cabinClass=ECONOMY%2CPREMIUM_ECONOMY%2CBUSINESS%2CFIRST&currency_code=AED"))
                .header("x-rapidapi-key", "0f43e34666msh37c1128e19ad979p1fd01ejsnb00068111738")
                .header("x-rapidapi-host", "booking-com15.p.rapidapi.com")
                .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
            resultArea.setText(json.toString());
        } catch (Exception ex) {
            resultArea.setText("Error fetching multi-stop min price.");
            ex.printStackTrace();
        }
    });

    Button backBtn = new Button("Back");
    backBtn.setOnAction(e -> showClientDashboard());

    minPriceMultiScreen.getChildren().addAll(
        titleLabel, from1Field, to1Field, date1Field, from2Field, to2Field, date2Field, fetchBtn, resultArea, backBtn, statusLabel
    );
    rootLayout.getChildren().setAll(minPriceMultiScreen);
}
private void showHotelFilterScreen() {
    VBox filterScreen = new VBox(15);
    filterScreen.setAlignment(Pos.CENTER);

    Label titleLabel = new Label("Hotel Filter Metadata");
    titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

    TextField destIdField = new TextField();
    destIdField.setPromptText("Destination ID (e.g. -2092174 for Dubai)");

    Button fetchBtn = new Button("Get Filters");
    TextArea resultArea = new TextArea();
    resultArea.setEditable(false);
    resultArea.setPrefWidth(500);
    resultArea.setPrefHeight(200);

    fetchBtn.setOnAction(e -> {
        resultArea.clear();
        String destId = destIdField.getText().trim();
        if (destId.isEmpty()) {
            statusLabel.setText("Enter a destination ID.");
            return;
        }
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://booking-com15.p.rapidapi.com/api/v1/hotels/getFilter?dest_id=" + destId + "&search_type=CITY&adults=1&children_age=1%2C17&room_qty=1"))
                .header("x-rapidapi-key", "0f43e34666msh37c1128e19ad979p1fd01ejsnb00068111738")
                .header("x-rapidapi-host", "booking-com15.p.rapidapi.com")
                .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
            resultArea.setText(json.toString());
        } catch (Exception ex) {
            resultArea.setText("Error fetching hotel filters.");
            ex.printStackTrace();
        }
    });

    Button backBtn = new Button("Back");
    backBtn.setOnAction(e -> showClientDashboard());

    filterScreen.getChildren().addAll(titleLabel, destIdField, fetchBtn, resultArea, backBtn, statusLabel);
    rootLayout.getChildren().setAll(filterScreen);
}

private void showHotelDescriptionScreen() {
    VBox descScreen = new VBox(15);
    descScreen.setAlignment(Pos.CENTER);

    Label titleLabel = new Label("Hotel Description & Info");
    titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

    TextField hotelIdField = new TextField();
    hotelIdField.setPromptText("Enter Hotel ID (e.g. 1234567)");

    Button fetchBtn = new Button("Get Description");
    TextArea resultArea = new TextArea();
    resultArea.setEditable(false);
    resultArea.setPrefWidth(500);
    resultArea.setPrefHeight(200);

    fetchBtn.setOnAction(e -> {
        resultArea.clear();
        String hotelId = hotelIdField.getText().trim();
        if (hotelId.isEmpty()) {
            statusLabel.setText("Enter a hotel ID.");
            return;
        }
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://booking-com15.p.rapidapi.com/api/v1/hotels/getDescriptionAndInfo?hotel_id=" + hotelId + "&languagecode=en-us"))
                .header("x-rapidapi-key", "0f43e34666msh37c1128e19ad979p1fd01ejsnb00068111738")
                .header("x-rapidapi-host", "booking-com15.p.rapidapi.com")
                .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
            String description = json.has("description") ? json.get("description").getAsString() : "No description available.";
            resultArea.setText(description);
        } catch (Exception ex) {
            resultArea.setText("Error fetching hotel description.");
            ex.printStackTrace();
        }
    });

    Button backBtn = new Button("Back");
    backBtn.setOnAction(e -> showClientDashboard());

    descScreen.getChildren().addAll(titleLabel, hotelIdField, fetchBtn, resultArea, backBtn, statusLabel);
    rootLayout.getChildren().setAll(descScreen);
}
private void showHotelSortScreen() {
    VBox sortScreen = new VBox(15);
    sortScreen.setAlignment(Pos.CENTER);

    Label titleLabel = new Label("Hotel Sort Metadata");
    titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

    TextField destIdField = new TextField();
    destIdField.setPromptText("Destination ID (e.g. -2092174 for Dubai)");

    Button fetchBtn = new Button("Get Sort Options");
    TextArea resultArea = new TextArea();
    resultArea.setEditable(false);
    resultArea.setPrefWidth(500);
    resultArea.setPrefHeight(200);

    fetchBtn.setOnAction(e -> {
        resultArea.clear();
        String destId = destIdField.getText().trim();
        if (destId.isEmpty()) {
            statusLabel.setText("Enter a destination ID.");
            return;
        }
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://booking-com15.p.rapidapi.com/api/v1/hotels/getSort?dest_id=" + destId + "&search_type=CITY&adults=1&children_age=1%2C17&room_qty=1"))
                .header("x-rapidapi-key", "0f43e34666msh37c1128e19ad979p1fd01ejsnb00068111738")
                .header("x-rapidapi-host", "booking-com15.p.rapidapi.com")
                .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
            resultArea.setText(json.toString());
        } catch (Exception ex) {
            resultArea.setText("Error fetching hotel sort options.");
            ex.printStackTrace();
        }
    });

    Button backBtn = new Button("Back");
    backBtn.setOnAction(e -> showClientDashboard());

    sortScreen.getChildren().addAll(titleLabel, destIdField, fetchBtn, resultArea, backBtn, statusLabel);
    rootLayout.getChildren().setAll(sortScreen);
}
private void showHotelReviewsScreen() {
    VBox reviewsScreen = new VBox(15);
    reviewsScreen.setAlignment(Pos.CENTER);

    Label titleLabel = new Label("Hotel Reviews");
    titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

    TextField hotelIdField = new TextField();
    hotelIdField.setPromptText("Enter Hotel ID (e.g. 1234567)");
    Button backBtn = new Button("Back");
    backBtn.setOnAction(e -> showClientDashboard());
    Button fetchBtn = new Button("Get Reviews");
    TextArea resultArea = new TextArea();
    resultArea.setEditable(false);
    resultArea.setPrefWidth(500);
    resultArea.setPrefHeight(250);

    fetchBtn.setOnAction(e -> {
        resultArea.clear();
        String hotelId = hotelIdField.getText().trim();
        if (hotelId.isEmpty()) {
            statusLabel.setText("Enter a hotel ID.");
            return;
        }
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://booking-com15.p.rapidapi.com/api/v1/hotels/getReviews?hotel_id=" + hotelId + "&languagecode=en-us&page_number=1"))
                .header("x-rapidapi-key", "0f43e34666msh37c1128e19ad979p1fd01ejsnb00068111738")
                .header("x-rapidapi-host", "booking-com15.p.rapidapi.com")
                .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
            if (json.has("data")) {
                JsonArray reviews = json.getAsJsonArray("data");
                StringBuilder sb = new StringBuilder();
                for (JsonElement el : reviews) {
                    JsonObject review = el.getAsJsonObject();
                    String user = review.has("user_name") ? review.get("user_name").getAsString() : "Anonymous";
                    String comment = review.has("pros") ? review.get("pros").getAsString() : "";
                    String rating = review.has("average_score") ? review.get("average_score").getAsString() : "N/A";
                    sb.append(user).append(" (").append(rating).append("): ").append(comment).append("\n\n");
                }
                resultArea.setText(sb.length() > 0 ? sb.toString() : "No reviews found.");
            } else {
                resultArea.setText("No reviews found.");
            }
        } catch (Exception ex) {
            resultArea.setText("Error fetching hotel reviews.");
            ex.printStackTrace();
        }
    });
  reviewsScreen.getChildren().addAll(titleLabel, hotelIdField, fetchBtn, resultArea, backBtn, statusLabel);
    rootLayout.getChildren().setAll(reviewsScreen);
}
private void showHotelPhotosScreen() {
    VBox photosScreen = new VBox(15);
    photosScreen.setAlignment(Pos.CENTER);

    Label titleLabel = new Label("Hotel Photos");
    titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

    TextField hotelIdField = new TextField();
    hotelIdField.setPromptText("Enter Hotel ID (e.g. 1234567)");

    Button fetchBtn = new Button("Get Photos");
    TextArea resultArea = new TextArea();
    resultArea.setEditable(false);
    resultArea.setPrefWidth(500);
    resultArea.setPrefHeight(250);

    fetchBtn.setOnAction(e -> {
        resultArea.clear();
        String hotelId = hotelIdField.getText().trim();
        if (hotelId.isEmpty()) {
            statusLabel.setText("Enter a hotel ID.");
            return;
        }
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://booking-com15.p.rapidapi.com/api/v1/hotels/getHotelPhotos?hotel_id=" + hotelId))
                .header("x-rapidapi-key", "0f43e34666msh37c1128e19ad979p1fd01ejsnb00068111738")
                .header("x-rapidapi-host", "booking-com15.p.rapidapi.com")
                .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
            if (json.has("data")) {
                JsonArray photos = json.getAsJsonArray("data");
                StringBuilder sb = new StringBuilder();
                for (JsonElement el : photos) {
                    JsonObject photo = el.getAsJsonObject();
                    String url = photo.has("url") ? photo.get("url").getAsString() : "";
                    sb.append(url).append("\n");
                }
                resultArea.setText(sb.length() > 0 ? sb.toString() : "No photos found.");
            } else {
                resultArea.setText("No photos found.");
            }
        } catch (Exception ex) {
            resultArea.setText("Error fetching hotel photos.");
            ex.printStackTrace();
        }
    });

    Button backBtn = new Button("Back");
    backBtn.setOnAction(e -> showClientDashboard());

    photosScreen.getChildren().addAll(titleLabel, hotelIdField, fetchBtn, resultArea, backBtn, statusLabel);
    rootLayout.getChildren().setAll(photosScreen);
}

private void showHotelFacilitiesScreen() {
    VBox facilitiesScreen = new VBox(15);
    facilitiesScreen.setAlignment(Pos.CENTER);

    Label titleLabel = new Label("Hotel Facilities");
    titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

    TextField hotelIdField = new TextField();
    hotelIdField.setPromptText("Enter Hotel ID (e.g. 1234567)");

    Button fetchBtn = new Button("Get Facilities");
    TextArea resultArea = new TextArea();
    resultArea.setEditable(false);
    resultArea.setPrefWidth(500);
    resultArea.setPrefHeight(250);

    fetchBtn.setOnAction(e -> {
        resultArea.clear();
        String hotelId = hotelIdField.getText().trim();
        if (hotelId.isEmpty()) {
            statusLabel.setText("Enter a hotel ID.");
            return;
        }
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://booking-com15.p.rapidapi.com/api/v1/hotels/getHotelFacilities?hotel_id=" + hotelId))
                .header("x-rapidapi-key", "0f43e34666msh37c1128e19ad979p1fd01ejsnb00068111738")
                .header("x-rapidapi-host", "booking-com15.p.rapidapi.com")
                .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
            if (json.has("data")) {
                JsonArray facilities = json.getAsJsonArray("data");
                StringBuilder sb = new StringBuilder();
                for (JsonElement el : facilities) {
                    JsonObject facility = el.getAsJsonObject();
                    String name = facility.has("facility_name") ? facility.get("facility_name").getAsString() : "";
                    sb.append(name).append("\n");
                }
                resultArea.setText(sb.length() > 0 ? sb.toString() : "No facilities found.");
            } else {
                resultArea.setText("No facilities found.");
            }
        } catch (Exception ex) {
            resultArea.setText("Error fetching hotel facilities.");
            ex.printStackTrace();
        }
    });

    Button backBtn = new Button("Back");
    backBtn.setOnAction(e -> showClientDashboard());

    facilitiesScreen.getChildren().addAll(titleLabel, hotelIdField, fetchBtn, resultArea, backBtn, statusLabel);
    rootLayout.getChildren().setAll(facilitiesScreen);
}

private void showFlightMetaDataScreen() {
    VBox metaScreen = new VBox(15);
    metaScreen.setAlignment(Pos.CENTER);

    Label titleLabel = new Label("Flight Metadata (Languages, Currencies, etc.)");
    titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

    Button langBtn = new Button("Get Supported Languages");
    Button currBtn = new Button("Get Supported Currencies");
    Button countryBtn = new Button("Get Supported Countries");
    TextArea resultArea = new TextArea();
    resultArea.setEditable(false);
    resultArea.setPrefWidth(500);
    resultArea.setPrefHeight(250);

    langBtn.setOnAction(e -> {
        resultArea.clear();
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://booking-com15.p.rapidapi.com/api/v1/flights/getLanguages"))
                .header("x-rapidapi-key", "0f43e34666msh37c1128e19ad979p1fd01ejsnb00068111738")
                .header("x-rapidapi-host", "booking-com15.p.rapidapi.com")
                .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
            if (json.has("data")) {
                JsonArray langs = json.getAsJsonArray("data");
                StringBuilder sb = new StringBuilder();
                for (JsonElement el : langs) {
                    JsonObject lang = el.getAsJsonObject();
                    String code = lang.has("code") ? lang.get("code").getAsString() : "";
                    String name = lang.has("name") ? lang.get("name").getAsString() : "";
                    sb.append(code).append(" - ").append(name).append("\n");
                }
                resultArea.setText(sb.length() > 0 ? sb.toString() : "No languages found.");
            } else {
                resultArea.setText("No languages found.");
            }
        } catch (Exception ex) {
            resultArea.setText("Error fetching languages.");
            ex.printStackTrace();
        } });

    currBtn.setOnAction(e -> {
        resultArea.clear();
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://booking-com15.p.rapidapi.com/api/v1/flights/getCurrencies"))
                .header("x-rapidapi-key", "0f43e34666msh37c1128e19ad979p1fd01ejsnb00068111738")
                .header("x-rapidapi-host", "booking-com15.p.rapidapi.com")
                .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
            if (json.has("data")) {
                JsonArray currs = json.getAsJsonArray("data");
                StringBuilder sb = new StringBuilder();
                for (JsonElement el : currs) {
                    JsonObject curr = el.getAsJsonObject();
                    String code = curr.has("code") ? curr.get("code").getAsString() : "";
                    String name = curr.has("name") ? curr.get("name").getAsString() : "";
                    sb.append(code).append(" - ").append(name).append("\n");
                }
                resultArea.setText(sb.length() > 0 ? sb.toString() : "No currencies found.");
            } else {
                resultArea.setText("No currencies found.");
            }
        } catch (Exception ex) {
            resultArea.setText("Error fetching currencies.");
            ex.printStackTrace();
        } });

    countryBtn.setOnAction(e -> {
        resultArea.clear();
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://booking-com15.p.rapidapi.com/api/v1/flights/getCountries"))
                .header("x-rapidapi-key", "0f43e34666msh37c1128e19ad979p1fd01ejsnb00068111738")
                .header("x-rapidapi-host", "booking-com15.p.rapidapi.com")
                .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
            if (json.has("data")) {
                JsonArray countries = json.getAsJsonArray("data");
                StringBuilder sb = new StringBuilder();
                for (JsonElement el : countries) {
                    JsonObject country = el.getAsJsonObject();
                    String code = country.has("code") ? country.get("code").getAsString() : "";
                    String name = country.has("name") ? country.get("name").getAsString() : "";
                    sb.append(code).append(" - ").append(name).append("\n");
                }
                resultArea.setText(sb.length() > 0 ? sb.toString() : "No countries found.");
            } else {
                resultArea.setText("No countries found.");
            }
        } catch (Exception ex) {
            resultArea.setText("Error fetching countries.");
            ex.printStackTrace();
        }
    });

    Button backBtn = new Button("Back");
    backBtn.setOnAction(e -> showClientDashboard());

    HBox buttonBox = new HBox(10, langBtn, currBtn, countryBtn);
    buttonBox.setAlignment(Pos.CENTER);

    metaScreen.getChildren().addAll(titleLabel, buttonBox, resultArea, backBtn, statusLabel);
    rootLayout.getChildren().setAll(metaScreen);
}
private void deletePackageFromDatabase(String packageId) {
    String sql = "DELETE FROM PACKAGES WHERE ID = ?";
    try (Connection conn = DatabaseManager.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setString(1, packageId);
        int rows = ps.executeUpdate();
        if (rows == 0) {
            statusLabel.setText("No package deleted. Package may not exist.");
        }
    } catch (Exception e) {
        statusLabel.setText("Error deleting package: " + e.getMessage());
    }
}
private void deleteUserFromDatabase(String username) {
    String sql = "DELETE FROM USERS WHERE USERNAME = ?";
    try (Connection conn = DatabaseManager.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setString(1, username);
        int rows = ps.executeUpdate();
        if (rows == 0) {
            statusLabel.setText("No user deleted. Username may not exist.");
        }
    } catch (Exception e) {
        statusLabel.setText("Error deleting user: " + e.getMessage());
    }
}
private void updateBookingStatusInDatabase(String packageId, String clientUsername, String status) {
    String sql = "UPDATE BOOKINGS SET STATUS = ? WHERE PACKAGE_ID = ? AND CLIENT_USERNAME = ?";
    try (Connection conn = DatabaseManager.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setString(1, status);
        ps.setString(2, packageId);
        ps.setString(3, clientUsername);
        int rows = ps.executeUpdate();
        if (rows == 0) {
            statusLabel.setText("No booking updated. Booking may not exist.");
        }
    } catch (Exception e) {
        statusLabel.setText("Error updating booking status: " + e.getMessage());
    }
}
private void updateBookingAgentInDatabase(String packageId, String clientUsername, String agentUsername) {
    String sql = "UPDATE BOOKINGS SET ASSIGNED_AGENT = ? WHERE PACKAGE_ID = ? AND CLIENT_USERNAME = ?";
    try (Connection conn = DatabaseManager.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setString(1, agentUsername);
        ps.setString(2, packageId);
        ps.setString(3, clientUsername);
        int rows = ps.executeUpdate();
        if (rows == 0) {
            statusLabel.setText("No booking updated. Booking may not exist.");
        }
    } catch (Exception e) {
        statusLabel.setText("Error updating assigned agent: " + e.getMessage());
    }
}
private void showMyBookingsScreen() {
    loadBookingsFromDatabase(); // Always reload bookings from DB

    VBox bookingsScreen = new VBox(15);
    bookingsScreen.setAlignment(Pos.CENTER);

    Label titleLabel = new Label("My Bookings");
    titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

    ListView<String> bookingList = new ListView<>();
    Map<String, Booking> bookingMap = new HashMap<>();
    if (currentUser instanceof Client) {
        for (Booking booking : bookings) {
            if (booking.getClientUsername() != null && currentUser.getUsername() != null &&
                booking.getClientUsername().trim().equalsIgnoreCase(currentUser.getUsername().trim())) {
                String display = "Package: " + booking.getPackageId() +
                    " | Date: " + booking.getBookingDate() +
                    " | Status: " + booking.getStatus();
                bookingList.getItems().add(display);
                bookingMap.put(display, booking);
            }
        }
    }
    bookingList.setPrefHeight(200);
    bookingList.setPrefWidth(600);

    Button payBtn = new Button("Pay for Approved");
    payBtn.setOnAction(e -> {
        String selected = bookingList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            statusLabel.setText("Select a booking to pay.");
            return;
        }
        Booking booking = bookingMap.get(selected);
        if (!"Approved".equalsIgnoreCase(booking.getStatus())) {
            statusLabel.setText("Only approved bookings can be paid.");
            return;
        }
        showPaymentScreen(booking);
    });
    Button cancelBtn = new Button("Cancel Booking");
    cancelBtn.setOnAction(e -> {
        String selected = bookingList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            statusLabel.setText("Select a booking to cancel.");
            return;
        }
        Booking booking = bookingMap.get(selected);
        bookings.remove(booking);
        deleteBookingFromDatabase(booking.getPackageId(), booking.getClientUsername());
        statusLabel.setText("Booking cancelled.");
        showMyBookingsScreen();
    });

    Button historyBtn = new Button("Show Payment History");
    historyBtn.setOnAction(e -> showPaymentHistoryScreen());

    Button backBtn = new Button("Back");
    backBtn.setOnAction(e -> showClientDashboard());

    bookingsScreen.getChildren().addAll(titleLabel, bookingList, payBtn, cancelBtn, historyBtn, backBtn, statusLabel);
    rootLayout.getChildren().setAll(bookingsScreen);
}
private void showPaymentHistoryScreen() {
    loadPaymentsFromDatabase(); // Always reload payments from DB

    VBox historyScreen = new VBox(15);
    historyScreen.setAlignment(Pos.CENTER);

    Label titleLabel = new Label("My Payment History");
    titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

    ListView<String> paymentList = new ListView<>();
    if (currentUser instanceof Client) {
        for (Payment payment : payments) {
            if (payment.getClientUsername() != null && currentUser.getUsername() != null &&
                payment.getClientUsername().trim().equalsIgnoreCase(currentUser.getUsername().trim())) {
                String display = "Package: " + payment.getPackageId() +
                    " | Amount: $" + String.format("%.2f", payment.getAmount()) +
                    " | Status: " + payment.getStatus();
                paymentList.getItems().add(display);
            }
        }
    }
    paymentList.setPrefHeight(200);
    paymentList.setPrefWidth(600);

    Button backBtn = new Button("Back");
    backBtn.setOnAction(e -> showMyBookingsScreen());

    historyScreen.getChildren().addAll(titleLabel, paymentList, backBtn, statusLabel);
    rootLayout.getChildren().setAll(historyScreen);
}
private void updateUserPasswordInDatabase(String username, String newPassword) {
    if (username == null || newPassword == null) {
        statusLabel.setText("Invalid user data for password update.");
        return;
    }
    String sql = "UPDATE USERS SET PASSWORD = ? WHERE USERNAME = ?";
    try (Connection conn = DatabaseManager.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setString(1, newPassword);
        ps.setString(2, username);
        int rows = ps.executeUpdate();
        if (rows == 0) {
            statusLabel.setText("No user updated. Username may not exist.");
        }
    } catch (Exception e) {
        statusLabel.setText("Error updating password: " + e.getMessage());
    }
}
private void deleteBookingFromDatabase(String packageId, String clientUsername) {
    String sql = "DELETE FROM BOOKINGS WHERE PACKAGE_ID = ? AND CLIENT_USERNAME = ?";
    try (Connection conn = DatabaseManager.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setString(1, packageId);
        ps.setString(2, clientUsername);
        int rows = ps.executeUpdate();
        if (rows == 0) {
            statusLabel.setText("No booking deleted. Booking may not exist.");
        }
    } catch (Exception e) {
        statusLabel.setText("Error deleting booking: " + e.getMessage());
    }}
public static void main(String[] args) {
    launch(args);
}}