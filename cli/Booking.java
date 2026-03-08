package cli;
import java.io.Serializable;

// Booking class
public class Booking implements Serializable {
    private String packageId;
    private String clientId; // Used as username for GUI compatibility
    private String bookingDate;
    private String status; // Pending, Confirmed, Cancelled
    private String assignedAgent;

    public Booking(String packageId, String clientId, String bookingDate, String status) {
        this.packageId = packageId;
        this.clientId = clientId;
        this.bookingDate = bookingDate;
        this.status = status;
    }

    // Getters
    public String getPackageId() { return packageId; }
    public String getClientId() { return clientId; }
    public String getBookingDate() { return bookingDate; }
    public String getStatus() { return status; }

    // For GUI compatibility
    public String getClientUsername() { return clientId; }
    public String getAssignedAgent() { return assignedAgent; }

    // Setters
    public void setStatus(String status) { this.status = status; }
    public void setAssignedAgent(String assignedAgent) { this.assignedAgent = assignedAgent; }
}