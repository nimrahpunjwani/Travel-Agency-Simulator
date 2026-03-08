package cli;

// Client class
public class Client extends User {
    private String country;
    private String city;

    public Client(String username, String password, String email, String name, String country, String city) {
        super(username, password, email, name);
        this.country = country;
        this.city = city;
    }

    public String getCountry() { return country; }
    public String getCity() { return city; }
}