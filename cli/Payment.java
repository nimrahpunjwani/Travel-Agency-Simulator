package cli;

import java.io.Serializable;

public class Payment implements Serializable {
    private String clientUsername;
    private String packageId;
    private double amount;
    private String status;

    public Payment(String clientUsername, String packageId, double amount, String status) {
        this.clientUsername = clientUsername;
        this.packageId = packageId;
        this.amount = amount;
        this.status = status;
    }

    public String getClientUsername() { return clientUsername; }
    public String getPackageId() { return packageId; }
    public double getAmount() { return amount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}