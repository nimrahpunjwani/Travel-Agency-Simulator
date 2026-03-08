package cli;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;



public class tvas {
    private static Scanner scanner = new Scanner(System.in);
    private static List<User> users = new ArrayList<>();
    private static List<TripPackage> packages = new ArrayList<>();
    private static List<Booking> bookings = new ArrayList<>();
    private static List<Payment> payments = new ArrayList<>();
    private static User currentUser = null;

    public static void main(String[] args) {
        initializeSystem();
        showMainMenu();
    }

    private static void initializeSystem() {
        // Create default admin
        users.add(new Admin("admin", "admin123", "admin@travel.com", "Admin User"));

        // Create sample agents
        users.add(new Agent("agent1", "agent123", "agent1@travel.com", "John Smith"));
        users.add(new Agent("agent2", "agent123", "agent2@travel.com", "Sarah Johnson"));

        // Create sample packages
        packages.add(new TripPackage("PKG001", "Paris Getaway", "France", 1200.0, 5,
                "City tour, Eiffel Tower visit, Seine River cruise"));
        packages.add(new TripPackage("PKG002", "Tropical Bali", "Indonesia", 1500.0, 7,
                "Beach resort, Temple visits, Snorkeling"));
        packages.add(new TripPackage("PKG003", "New York Adventure", "USA", 1800.0, 6,
                "Broadway show, Statue of Liberty, Central Park"));

        loadDataFromFiles();
    }

    private static void loadDataFromFiles() {
        try {
            // Load users
            File userFile = new File("users.dat");
            if (userFile.exists()) {
                ObjectInputStream ois = new ObjectInputStream(new FileInputStream(userFile));
                users = (List<User>) ois.readObject();
                ois.close();
            }

            // Load packages
            File packageFile = new File("packages.dat");
            if (packageFile.exists()) {
                ObjectInputStream ois = new ObjectInputStream(new FileInputStream(packageFile));
                packages = (List<TripPackage>) ois.readObject();
                ois.close();
            }

            // Load bookings
            File bookingFile = new File("bookings.dat");
            if (bookingFile.exists()) {
                ObjectInputStream ois = new ObjectInputStream(new FileInputStream(bookingFile));
                bookings = (List<Booking>) ois.readObject();
                ois.close();
            }

            // Load payments
            File paymentFile = new File("payments.dat");
            if (paymentFile.exists()) {
                ObjectInputStream ois = new ObjectInputStream(new FileInputStream(paymentFile));
                payments = (List<Payment>) ois.readObject();
                ois.close();
            }
        } catch (Exception e) {
            System.out.println("Error loading data: " + e.getMessage());
        }
    }

    private static void saveDataToFiles() {
        try {
            // Save users
            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("users.dat"));
            oos.writeObject(users);
            oos.close();

            // Save packages
            oos = new ObjectOutputStream(new FileOutputStream("packages.dat"));
            oos.writeObject(packages);
            oos.close();

            // Save bookings
            oos = new ObjectOutputStream(new FileOutputStream("bookings.dat"));
            oos.writeObject(bookings);
            oos.close();

            // Save payments
            oos = new ObjectOutputStream(new FileOutputStream("payments.dat"));
            oos.writeObject(payments);
            oos.close();
        } catch (Exception e) {
            System.out.println("Error saving data: " + e.getMessage());
        }
    }

    private static void showMainMenu() {
        while (true) {
            System.out.println("\n=== Travel Agency Simulator ===");
            System.out.println("1. Login");
            System.out.println("2. Register as Client");
            System.out.println("3. Exit");
            System.out.print("Select option: ");

            String choice = scanner.nextLine();
            switch (choice) {
                case "1":
                    login();
                    break;
                case "2":
                    registerClient();
                    break;
                case "3":
                    saveDataToFiles();
                    System.out.println("Thank you for using Travel Agency Simulator!");
                    return;
                default:
                    System.out.println("Invalid option. Please try again.");
            }
        }
    }

    private static void login() {
        System.out.println("\n--- Login ---");
        System.out.print("Username: ");
        String username = scanner.nextLine();
        System.out.print("Password: ");
        String password = scanner.nextLine();

        for (User user : users) {
            if (user.getUsername().equals(username) && user.authenticate(password)) {
                currentUser = user;
                System.out.println("\nLogin successful! Welcome, " + user.getName() + "!");

                if (user instanceof Admin) {
                    showAdminMenu();
                } else if (user instanceof Agent) {
                    showAgentMenu();
                } else if (user instanceof Client) {
                    showClientMenu();
                }
                return;
            }
        }

        System.out.println("Invalid username or password. Please try again.");
    }

    private static void registerClient() {
        System.out.println("\n--- Client Registration ---");
        System.out.print("Username: ");
        String username = scanner.nextLine();

        // Check if username exists
        for (User user : users) {
            if (user.getUsername().equals(username)) {
                System.out.println("Username already exists. Please choose another.");
                return;
            }
        }

        System.out.print("Password: ");
        String password = scanner.nextLine();
        System.out.print("Email: ");
        String email = scanner.nextLine();
        System.out.print("Full Name: ");
        String name = scanner.nextLine();

        Client newClient = new Client(username, password, email, name);
        users.add(newClient);
        System.out.println("\nRegistration successful! You can now login with your credentials.");
    }

    private static void showAdminMenu() {
        Admin admin = (Admin) currentUser;
        while (true) {
            System.out.println("\n--- Admin Dashboard ---");
            System.out.println("1. Manage Users");
            System.out.println("2. Manage Packages");
            System.out.println("3. View Reports");
            System.out.println("4. Logout");
            System.out.print("Select option: ");

            String choice = scanner.nextLine();
            switch (choice) {
                case "1":
                    manageUsers();
                    break;
                case "2":
                    managePackages();
                    break;
                case "3":
                    viewReports();
                    break;
                case "4":
                    currentUser = null;
                    return;
                default:
                    System.out.println("Invalid option. Please try again.");
            }
        }
    }

    private static void manageUsers() {
        while (true) {
            System.out.println("\n--- Manage Users ---");
            System.out.println("1. List All Users");
            System.out.println("2. Add New Agent");
            System.out.println("3. Delete User");
            System.out.println("4. Back to Admin Menu");
            System.out.print("Select option: ");

            String choice = scanner.nextLine();
            switch (choice) {
                case "1":
                    listAllUsers();
                    break;
                case "2":
                    addNewAgent();
                    break;
                case "3":
                    deleteUser();
                    break;
                case "4":
                    return;
                default:
                    System.out.println("Invalid option. Please try again.");
            }
        }
    }

    private static void listAllUsers() {
        System.out.println("\n--- All Users ---");
        System.out.printf("%-15s %-20s %-15s %-30s\n", "Username", "Name", "Role", "Email");
        for (User user : users) {
            String role = user instanceof Admin ? "Admin" :
                         user instanceof Agent ? "Agent" : "Client";
            System.out.printf("%-15s %-20s %-15s %-30s\n",
                    user.getUsername(), user.getName(), role, user.getEmail());
        }
    }

    private static void addNewAgent() {
        System.out.println("\n--- Add New Agent ---");
        System.out.print("Username: ");
        String username = scanner.nextLine();

        // Check if username exists
        for (User user : users) {
            if (user.getUsername().equals(username)) {
                System.out.println("Username already exists. Please choose another.");
                return;
            }
        }

        System.out.print("Password: ");
        String password = scanner.nextLine();
        System.out.print("Email: ");
        String email = scanner.nextLine();
        System.out.print("Full Name: ");
        String name = scanner.nextLine();

        Agent newAgent = new Agent(username, password, email, name);
        users.add(newAgent);
        System.out.println("Agent added successfully!");
    }

    private static void deleteUser() {
        listAllUsers();
        System.out.print("\nEnter username to delete: ");
        String username = scanner.nextLine();

        if (username.equals(currentUser.getUsername())) {
            System.out.println("You cannot delete your own account!");
            return;
        }

        for (Iterator<User> iterator = users.iterator(); iterator.hasNext();) {
            User user = iterator.next();
            if (user.getUsername().equals(username)) {
                iterator.remove();
                System.out.println("User deleted successfully!");
                return;
            }
        }

        System.out.println("User not found.");
    }

    private static void managePackages() {
        while (true) {
            System.out.println("\n--- Manage Packages ---");
            System.out.println("1. List All Packages");
            System.out.println("2. Add New Package");
            System.out.println("3. Update Package");
            System.out.println("4. Delete Package");
            System.out.println("5. Back to Admin Menu");
            System.out.print("Select option: ");

            String choice = scanner.nextLine();
            switch (choice) {
                case "1":
                    listAllPackages();
                    break;
                case "2":
                    addNewPackage();
                    break;
                case "3":
                    updatePackage();
                    break;
                case "4":
                    deletePackage();
                    break;
                case "5":
                    return;
                default:
                    System.out.println("Invalid option. Please try again.");
            }
        }
    }

    private static void listAllPackages() {
        System.out.println("\n--- All Packages ---");
        System.out.printf("%-10s %-20s %-15s %-10s %-5s %-30s\n",
                "PackageID", "Name", "Destination", "Price", "Days", "Activities");
        for (TripPackage pkg : packages) {
            System.out.printf("%-10s %-20s %-15s $%-9.2f %-5d %-30s\n",
                    pkg.getPackageId(), pkg.getName(), pkg.getDestination(),
                    pkg.getPrice(), pkg.getDuration(), pkg.getActivities());
        }
    }

    private static void addNewPackage() {
        System.out.println("\n--- Add New Package ---");
        System.out.print("Package ID: ");
        String packageId = scanner.nextLine();

        // Check if package ID exists
        for (TripPackage pkg : packages) {
            if (pkg.getPackageId().equals(packageId)) {
                System.out.println("Package ID already exists. Please choose another.");
                return;
            }
        }

        System.out.print("Package Name: ");
        String name = scanner.nextLine();
        System.out.print("Destination: ");
        String destination = scanner.nextLine();
        System.out.print("Price: ");
        double price = Double.parseDouble(scanner.nextLine());
        System.out.print("Duration (days): ");
        int duration = Integer.parseInt(scanner.nextLine());
        System.out.print("Activities (comma separated): ");
        String activities = scanner.nextLine();

        packages.add(new TripPackage(packageId, name, destination, price, duration, activities));
        System.out.println("Package added successfully!");
    }

    private static void updatePackage() {
        listAllPackages();
        System.out.print("\nEnter Package ID to update: ");
        String packageId = scanner.nextLine();

        for (TripPackage pkg : packages) {
            if (pkg.getPackageId().equals(packageId)) {
                System.out.println("Leave blank to keep current value.");

                System.out.print("Package Name (" + pkg.getName() + "): ");
                String name = scanner.nextLine();
                if (!name.isEmpty()) pkg.setName(name);

                System.out.print("Destination (" + pkg.getDestination() + "): ");
                String destination = scanner.nextLine();
                if (!destination.isEmpty()) pkg.setDestination(destination);

                System.out.print("Price (" + pkg.getPrice() + "): ");
                String priceStr = scanner.nextLine();
                if (!priceStr.isEmpty()) pkg.setPrice(Double.parseDouble(priceStr));

                System.out.print("Duration (" + pkg.getDuration() + "): ");
                String durationStr = scanner.nextLine();
                if (!durationStr.isEmpty()) pkg.setDuration(Integer.parseInt(durationStr));

                System.out.print("Activities (" + pkg.getActivities() + "): ");
                String activities = scanner.nextLine();
                if (!activities.isEmpty()) pkg.setActivities(activities);

                System.out.println("Package updated successfully!");
                return;
            }
        }

        System.out.println("Package not found.");
    }

    private static void deletePackage() {
        listAllPackages();
        System.out.print("\nEnter Package ID to delete: ");
        String packageId = scanner.nextLine();

        for (Iterator<TripPackage> iterator = packages.iterator(); iterator.hasNext();) {
            TripPackage pkg = iterator.next();
            if (pkg.getPackageId().equals(packageId)) {
                iterator.remove();
                System.out.println("Package deleted successfully!");
                return;
            }
        }

        System.out.println("Package not found.");
    }

    private static void viewReports() {
        System.out.println("\n--- Reports ---");
        System.out.println("1. Booking Report");
        System.out.println("2. Payment Report");
        System.out.println("3. Revenue Report");
        System.out.println("4. Back to Admin Menu");
        System.out.print("Select option: ");

        String choice = scanner.nextLine();
        switch (choice) {
            case "1":
                bookingReport();
                break;
            case "2":
                paymentReport();
                break;
            case "3":
                revenueReport();
                break;
            case "4":
                return;
            default:
                System.out.println("Invalid option. Please try again.");
        }
    }

    private static void bookingReport() {
        System.out.println("\n--- Booking Report ---");
        System.out.printf("%-15s %-20s %-15s %-10s %-10s\n",
                "Booking ID", "Client", "Package", "Status", "Amount");

        double totalRevenue = 0;
        for (Booking booking : bookings) {
            String clientName = "Unknown";
            String packageName = "Unknown";
            double amount = 0;

            for (User user : users) {
                if (user instanceof Client && ((Client)user).getClientId().equals(booking.getClientId())) {
                    clientName = user.getName();
                    break;
                }
            }

            for (TripPackage pkg : packages) {
                if (pkg.getPackageId().equals(booking.getPackageId())) {
                    packageName = pkg.getName();
                    amount = pkg.getPrice();
                    break;
                }
            }

            System.out.printf("%-15s %-20s %-15s %-10s $%-9.2f\n",
                    booking.getBookingId(), clientName, packageName,
                    booking.getStatus(), amount);

            if (booking.getStatus().equals("Confirmed")) {
                totalRevenue += amount;
            }
        }

        System.out.println("\nTotal Revenue from Confirmed Bookings: $" + totalRevenue);
    }

    private static void paymentReport() {
        System.out.println("\n--- Payment Report ---");
        System.out.printf("%-15s %-15s %-20s %-10s %-10s\n",
                "Payment ID", "Booking ID", "Client", "Amount", "Status");

        double totalPaid = 0;
        for (Payment payment : payments) {
            String clientName = "Unknown";

            for (Booking booking : bookings) {
                if (booking.getBookingId().equals(payment.getBookingId())) {
                    for (User user : users) {
                        if (user instanceof Client && ((Client)user).getClientId().equals(booking.getClientId())) {
                            clientName = user.getName();
                            break;
                        }
                    }
                    break;
                }
            }

            System.out.printf("%-15s %-15s %-20s $%-9.2f %-10s\n",
                    payment.getPaymentId(), payment.getBookingId(),
                    clientName, payment.getAmount(), payment.getStatus());

            if (payment.getStatus().equals("Paid")) {
                totalPaid += payment.getAmount();
            }
        }

        System.out.println("\nTotal Amount Paid: $" + totalPaid);
    }

    private static void revenueReport() {
        System.out.println("\n--- Revenue Report ---");

        Map<String, Double> destinationRevenue = new HashMap<>();
        Map<String, Integer> destinationBookings = new HashMap<>();

        for (Booking booking : bookings) {
            if (booking.getStatus().equals("Confirmed")) {
                for (TripPackage pkg : packages) {
                    if (pkg.getPackageId().equals(booking.getPackageId())) {
                        String destination = pkg.getDestination();
                        double amount = pkg.getPrice();

                        destinationRevenue.put(destination,
                                destinationRevenue.getOrDefault(destination, 0.0) + amount);
                        destinationBookings.put(destination,
                                destinationBookings.getOrDefault(destination, 0) + 1);
                        break;
                    }
                }
            }
        }

        System.out.printf("%-15s %-15s %-15s\n", "Destination", "Bookings", "Revenue");
        for (Map.Entry<String, Double> entry : destinationRevenue.entrySet()) {
            String destination = entry.getKey();
            System.out.printf("%-15s %-15d $%-14.2f\n",
                    destination, destinationBookings.get(destination), entry.getValue());
        }
    }

    private static void showAgentMenu() {
        Agent agent = (Agent) currentUser;
        while (true) {
            System.out.println("\n--- Agent Dashboard ---");
            System.out.println("1. Assist Client with Booking");
            System.out.println("2. View All Packages");
            System.out.println("3. View Client Bookings");
            System.out.println("4. Process Payments");
            System.out.println("5. Logout");
            System.out.print("Select option: ");

            String choice = scanner.nextLine();
            switch (choice) {
                case "1":
                    assistClientBooking();
                    break;
                case "2":
                    listAllPackages();
                    break;
                case "3":
                    viewClientBookings();
                    break;
                case "4":
                    processPayments();
                    break;
                case "5":
                    currentUser = null;
                    return;
                default:
                    System.out.println("Invalid option. Please try again.");
            }
        }
    }

    private static void assistClientBooking() {
        System.out.println("\n--- Assist Client with Booking ---");
        System.out.print("Enter client username: ");
        String username = scanner.nextLine();

        Client client = null;
        for (User user : users) {
            if (user instanceof Client && user.getUsername().equals(username)) {
                client = (Client) user;
                break;
            }
        }

        if (client == null) {
            System.out.println("Client not found.");
            return;
        }

        System.out.println("\nAssisting client: " + client.getName());
        listAllPackages();
        System.out.print("\nEnter Package ID to book: ");
        String packageId = scanner.nextLine();

        TripPackage selectedPackage = null;
        for (TripPackage pkg : packages) {
            if (pkg.getPackageId().equals(packageId)) {
                selectedPackage = pkg;
                break;
            }
        }

        if (selectedPackage == null) {
            System.out.println("Package not found.");
            return;
        }

        System.out.println("\nSelected Package: " + selectedPackage.getName());
        System.out.println("Price: $" + selectedPackage.getPrice());
        System.out.print("Confirm booking? (Y/N): ");
        String confirm = scanner.nextLine();

        if (confirm.equalsIgnoreCase("Y")) {
            String bookingId = "B" + System.currentTimeMillis();
            bookings.add(new Booking(bookingId, client.getClientId(), packageId, "Confirmed"));
            System.out.println("Booking confirmed! Booking ID: " + bookingId);

            // Create payment record
            String paymentId = "P" + System.currentTimeMillis();
            payments.add(new Payment(paymentId, bookingId, selectedPackage.getPrice(), "Pending"));
            System.out.println("Payment record created. Please process the payment.");
        } else {
            System.out.println("Booking cancelled.");
        }
    }

    private static void viewClientBookings() {
        System.out.println("\n--- Client Bookings ---");
        System.out.print("Enter client username: ");
        String username = scanner.nextLine();

        Client client = null;
        for (User user : users) {
            if (user instanceof Client && user.getUsername().equals(username)) {
                client = (Client) user;
                break;
            }
        }

        if (client == null) {
            System.out.println("Client not found.");
            return;
        }

        System.out.println("\nBookings for " + client.getName() + ":");
        System.out.printf("%-15s %-20s %-15s %-10s %-10s\n",
                "Booking ID", "Package", "Destination", "Status", "Amount");

        boolean found = false;
        for (Booking booking : bookings) {
            if (booking.getClientId().equals(client.getClientId())) {
                found = true;
                String packageName = "Unknown";
                String destination = "Unknown";
                double amount = 0;

                for (TripPackage pkg : packages) {
                    if (pkg.getPackageId().equals(booking.getPackageId())) {
                        packageName = pkg.getName();
                        destination = pkg.getDestination();
                        amount = pkg.getPrice();
                        break;
                    }
                }

                System.out.printf("%-15s %-20s %-15s %-10s $%-9.2f\n",
                        booking.getBookingId(), packageName, destination,
                        booking.getStatus(), amount);
            }
        }

        if (!found) {
            System.out.println("No bookings found for this client.");
        }
    }

    private static void processPayments() {
        System.out.println("\n--- Process Payments ---");
        System.out.println("Pending Payments:");
        System.out.printf("%-15s %-15s %-20s %-10s\n",
                "Payment ID", "Booking ID", "Client", "Amount");

        List<Payment> pendingPayments = new ArrayList<>();
        for (Payment payment : payments) {
            if (payment.getStatus().equals("Pending")) {
                String clientName = "Unknown";

                for (Booking booking : bookings) {
                    if (booking.getBookingId().equals(payment.getBookingId())) {
                        for (User user : users) {
                            if (user instanceof Client && ((Client)user).getClientId().equals(booking.getClientId())) {
                                clientName = user.getName();
                                break;
                            }
                        }
                        break;
                    }
                }

                System.out.printf("%-15s %-15s %-20s $%-9.2f\n",
                        payment.getPaymentId(), payment.getBookingId(),
                        clientName, payment.getAmount());
                pendingPayments.add(payment);
            }
        }

        if (pendingPayments.isEmpty()) {
            System.out.println("No pending payments.");
            return;
        }

        System.out.print("\nEnter Payment ID to process: ");
        String paymentId = scanner.nextLine();

        for (Payment payment : pendingPayments) {
            if (payment.getPaymentId().equals(paymentId)) {
                System.out.println("Processing payment of $" + payment.getAmount());
                System.out.print("Mark as paid? (Y/N): ");
                String confirm = scanner.nextLine();

                if (confirm.equalsIgnoreCase("Y")) {
                    payment.setStatus("Paid");
                    System.out.println("Payment processed successfully!");

                    // Update corresponding booking status
                    for (Booking booking : bookings) {
                        if (booking.getBookingId().equals(payment.getBookingId())) {
                            booking.setStatus("Confirmed");
                            break;
                        }
                    }
                } else {
                    System.out.println("Payment processing cancelled.");
                }
                return;
            }
        }

        System.out.println("Payment not found or already processed.");
    }

    private static void showClientMenu() {
        Client client = (Client) currentUser;
        while (true) {
            System.out.println("\n--- Client Dashboard ---");
            System.out.println("1. Browse Packages");
            System.out.println("2. Book a Package");
            System.out.println("3. Customize Package");
            System.out.println("4. View My Bookings");
            System.out.println("5. Make Payment");
            System.out.println("6. Logout");
            System.out.print("Select option: ");

            String choice = scanner.nextLine();
            switch (choice) {
                case "1":
                    browsePackages();
                    break;
                case "2":
                    bookPackage(client);
                    break;
                case "3":
                    customizePackage(client);
                    break;
                case "4":
                    viewMyBookings(client);
                    break;
                case "5":
                    makePayment(client);
                    break;
                case "6":
                    currentUser = null;
                    return;
                default:
                    System.out.println("Invalid option. Please try again.");
            }
        }
    }

    private static void browsePackages() {
        System.out.println("\n--- Available Packages ---");
        System.out.printf("%-10s %-20s %-15s %-10s %-5s %-30s\n",
                "PackageID", "Name", "Destination", "Price", "Days", "Activities");
        for (TripPackage pkg : packages) {
            System.out.printf("%-10s %-20s %-15s $%-9.2f %-5d %-30s\n",
                    pkg.getPackageId(), pkg.getName(), pkg.getDestination(),
                    pkg.getPrice(), pkg.getDuration(), pkg.getActivities());
        }
    }

    private static void bookPackage(Client client) {
        browsePackages();
        System.out.print("\nEnter Package ID to book: ");
        String packageId = scanner.nextLine();

        TripPackage selectedPackage = null;
        for (TripPackage pkg : packages) {
            if (pkg.getPackageId().equals(packageId)) {
                selectedPackage = pkg;
                break;
            }
        }

        if (selectedPackage == null) {
            System.out.println("Package not found.");
            return;
        }

        System.out.println("\nSelected Package: " + selectedPackage.getName());
        System.out.println("Price: $" + selectedPackage.getPrice());
        System.out.print("Confirm booking? (Y/N): ");
        String confirm = scanner.nextLine();

        if (confirm.equalsIgnoreCase("Y")) {
            String bookingId = "B" + System.currentTimeMillis();
            bookings.add(new Booking(bookingId, client.getClientId(), packageId, "Pending"));
            System.out.println("Booking created! Booking ID: " + bookingId);

            // Create payment record
            String paymentId = "P" + System.currentTimeMillis();
            payments.add(new Payment(paymentId, bookingId, selectedPackage.getPrice(), "Pending"));
            System.out.println("Please make payment to confirm your booking.");
        } else {
            System.out.println("Booking cancelled.");
        }
    }

    private static void customizePackage(Client client) {
        System.out.println("\n--- Customize Your Package ---");
        System.out.println("Answer these questions to help us create your perfect trip!");

        // Akinator-style questioning
        System.out.print("1. What type of vacation do you prefer? (Beach/Mountain/City/Culture): ");
        String vacationType = scanner.nextLine();

        System.out.print("2. What's your budget range? (Low/Medium/High): ");
        String budget = scanner.nextLine();

        System.out.print("3. How many days are you planning to travel? ");
        int days = Integer.parseInt(scanner.nextLine());

        System.out.print("4. What activities interest you? (Relaxation/Adventure/Sightseeing/Shopping): ");
        String activities = scanner.nextLine();

        System.out.print("5. Preferred climate? (Warm/Cool/Cold/Tropical): ");
        String climate = scanner.nextLine();

        // Generate recommendations based on answers
        System.out.println("\n--- Recommended Packages ---");
        List<TripPackage> recommendations = new ArrayList<>();

        for (TripPackage pkg : packages) {
            int score = 0;

            // Score based on vacation type
            if (vacationType.equalsIgnoreCase("Beach") &&
                (pkg.getActivities().toLowerCase().contains("beach") ||
                 pkg.getActivities().toLowerCase().contains("sea"))) {
                score += 3;
            } else if (vacationType.equalsIgnoreCase("Mountain") &&
                      pkg.getActivities().toLowerCase().contains("mountain")) {
                score += 3;
            } else if (vacationType.equalsIgnoreCase("City") &&
                      pkg.getDestination().toLowerCase().contains(pkg.getDestination().toLowerCase())) {
                score += 3;
            } else if (vacationType.equalsIgnoreCase("Culture") &&
                      pkg.getActivities().toLowerCase().contains("culture")) {
                score += 3;
            }

            // Score based on budget
            if (budget.equalsIgnoreCase("Low") && pkg.getPrice() < 1000) {
                score += 2;
            } else if (budget.equalsIgnoreCase("Medium") && pkg.getPrice() >= 1000 && pkg.getPrice() < 2000) {
                score += 2;
            } else if (budget.equalsIgnoreCase("High") && pkg.getPrice() >= 2000) {
                score += 2;
            }

            // Score based on duration
            if (Math.abs(pkg.getDuration() - days) <= 2) {
                score += 2;
            }

            // Score based on activities
            if (activities.equalsIgnoreCase("Relaxation") &&
                pkg.getActivities().toLowerCase().contains("relax")) {
                score += 1;
            } else if (activities.equalsIgnoreCase("Adventure") &&
                      pkg.getActivities().toLowerCase().contains("adventure")) {
                score += 1;
            } else if (activities.equalsIgnoreCase("Sightseeing") &&
                      pkg.getActivities().toLowerCase().contains("sightseeing")) {
                score += 1;
            } else if (activities.equalsIgnoreCase("Shopping") &&
                      pkg.getActivities().toLowerCase().contains("shopping")) {
                score += 1;
            }

            // Score based on climate
            if (climate.equalsIgnoreCase("Warm") &&
                (pkg.getDestination().toLowerCase().contains("bali") ||
                 pkg.getDestination().toLowerCase().contains("thailand"))) {
                score += 1;
            } else if (climate.equalsIgnoreCase("Cool") &&
                      (pkg.getDestination().toLowerCase().contains("paris") ||
                       pkg.getDestination().toLowerCase().contains("europe"))) {
                score += 1;
            } else if (climate.equalsIgnoreCase("Cold") &&
                      (pkg.getDestination().toLowerCase().contains("canada") ||
                       pkg.getDestination().toLowerCase().contains("alaska"))) {
                score += 1;
            } else if (climate.equalsIgnoreCase("Tropical") &&
                      (pkg.getDestination().toLowerCase().contains("maldives") ||
                       pkg.getDestination().toLowerCase().contains("hawaii"))) {
                score += 1;
            }

            if (score > 0) {
                pkg.setMatchScore(score);
                recommendations.add(pkg);
            }
        }

        // Sort recommendations by score
        recommendations.sort((p1, p2) -> p2.getMatchScore() - p1.getMatchScore());

        if (recommendations.isEmpty()) {
            System.out.println("No packages match your preferences. Please try different criteria.");
            return;
        }

        System.out.println("\nBased on your preferences, we recommend:");
        for (int i = 0; i < Math.min(3, recommendations.size()); i++) {
            TripPackage pkg = recommendations.get(i);
            System.out.println((i+1) + ". " + pkg.getName() + " (" + pkg.getDestination() + ")");
            System.out.println("   Price: $" + pkg.getPrice() + " for " + pkg.getDuration() + " days");
            System.out.println("   Activities: " + pkg.getActivities());
            System.out.println("   Match score: " + pkg.getMatchScore() + "/9");
            System.out.println();
        }

        System.out.print("Would you like to book one of these packages? (Enter number or 0 to cancel): ");
        int selection = Integer.parseInt(scanner.nextLine());

        if (selection > 0 && selection <= recommendations.size()) {
            TripPackage selectedPackage = recommendations.get(selection-1);
            String bookingId = "B" + System.currentTimeMillis();
            bookings.add(new Booking(bookingId, client.getClientId(), selectedPackage.getPackageId(), "Pending"));
            System.out.println("Booking created! Booking ID: " + bookingId);

            // Create payment record
            String paymentId = "P" + System.currentTimeMillis();
            payments.add(new Payment(paymentId, bookingId, selectedPackage.getPrice(), "Pending"));
            System.out.println("Please make payment to confirm your booking.");
        } else {
            System.out.println("Custom package selection cancelled.");
        }
    }

    private static void viewMyBookings(Client client) {
        System.out.println("\n--- My Bookings ---");
        System.out.printf("%-15s %-20s %-15s %-10s %-10s\n",
                "Booking ID", "Package", "Destination", "Status", "Amount");

        boolean found = false;
        for (Booking booking : bookings) {
            if (booking.getClientId().equals(client.getClientId())) {
                found = true;
                String packageName = "Unknown";
                String destination = "Unknown";
                double amount = 0;

                for (TripPackage pkg : packages) {
                    if (pkg.getPackageId().equals(booking.getPackageId())) {
                        packageName = pkg.getName();
                        destination = pkg.getDestination();
                        amount = pkg.getPrice();
                        break;
                    }
                }

                System.out.printf("%-15s %-20s %-15s %-10s $%-9.2f\n",
                        booking.getBookingId(), packageName, destination,
                        booking.getStatus(), amount);
            }
        }

        if (!found) {
            System.out.println("No bookings found.");
        }
    }

    private static void makePayment(Client client) {
        System.out.println("\n--- Make Payment ---");
        System.out.println("Your Pending Payments:");
        System.out.printf("%-15s %-20s %-10s %-10s\n",
                "Payment ID", "Package", "Amount", "Status");

        List<Payment> myPayments = new ArrayList<>();
        for (Payment payment : payments) {
            for (Booking booking : bookings) {
                if (booking.getBookingId().equals(payment.getBookingId()) &&
                    booking.getClientId().equals(client.getClientId()) &&
                    payment.getStatus().equals("Pending")) {

                    String packageName = "Unknown";
                    for (TripPackage pkg : packages) {
                        if (pkg.getPackageId().equals(booking.getPackageId())) {
                            packageName = pkg.getName();
                            break;
                        }
                    }

                    System.out.printf("%-15s %-20s $%-9.2f %-10s\n",
                            payment.getPaymentId(), packageName,
                            payment.getAmount(), payment.getStatus());
                    myPayments.add(payment);
                    break;
                }
            }
        }

        if (myPayments.isEmpty()) {
            System.out.println("No pending payments.");
            return;
        }

        System.out.print("\nEnter Payment ID to pay: ");
        String paymentId = scanner.nextLine();

        for (Payment payment : myPayments) {
            if (payment.getPaymentId().equals(paymentId)) {
                System.out.println("Amount to pay: $" + payment.getAmount());
                System.out.print("Enter payment method (Cash/Card): ");
                String method = scanner.nextLine();

                System.out.print("Confirm payment of $" + payment.getAmount() + "? (Y/N): ");
                String confirm = scanner.nextLine();

                if (confirm.equalsIgnoreCase("Y")) {
                    payment.setStatus("Paid");

                    // Update booking status
                    for (Booking booking : bookings) {
                        if (booking.getBookingId().equals(payment.getBookingId())) {
                            booking.setStatus("Confirmed");
                            break;
                        }
                    }

                    System.out.println("Payment successful! Your booking is now confirmed.");
                } else {
                    System.out.println("Payment cancelled.");
                }
                return;
            }
        }

        System.out.println("Payment not found or already processed.");
    }
}