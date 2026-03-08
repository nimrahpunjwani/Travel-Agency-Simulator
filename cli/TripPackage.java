package cli;
import java.io.Serializable;

// TripPackage class
public class TripPackage implements Serializable {
    private String packageId;
    private String name;
    private String destination;
    private double price;
    private int duration;
    private String activities;
    private int matchScore; // Used for customized package recommendations

    public TripPackage(String packageId, String name, String destination,
                      double price, int duration, String activities) {
        this.packageId = packageId;
        this.name = name;
        this.destination = destination;
        this.price = price;
        this.duration = duration;
        this.activities = activities;
        this.matchScore = 0;
    }

    // Getters
    public String getPackageId() { return packageId; }
    public String getId() { return packageId; } // For GUI compatibility
    public String getName() { return name; }
    public String getDestination() { return destination; }
    public double getPrice() { return price; }
    public int getDuration() { return duration; }
    public int getDays() { return duration; } // For GUI compatibility
    public String getActivities() { return activities; }
    public int getMatchScore() { return matchScore; }

    // Setters
    public void setName(String name) { this.name = name; }
    public void setDestination(String destination) { this.destination = destination; }
    public void setPrice(double price) { this.price = price; }
    public void setDuration(int duration) { this.duration = duration; }
    public void setActivities(String activities) { this.activities = activities; }
    public void setMatchScore(int matchScore) { this.matchScore = matchScore; }
}