package cli;
import java.io.Serializable;

public abstract class User implements Serializable {
    private String username;
    private String password;
    private String email;
    private String name;

    public User(String username, String password, String email, String name) {
        this.username = username != null ? username.trim() : "";
        this.password = password != null ? password.trim() : "";
        this.email = email != null ? email.trim() : "";
        this.name = name != null ? name.trim() : "";
    }

    public boolean authenticate(String password) {
        // Trim both sides to avoid login issues due to accidental spaces
        return this.password.equals(password != null ? password.trim() : "");
    }

    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getName() { return name; }
    public String getPassword() { return password; }

    public void setPassword(String password) { this.password = password != null ? password.trim() : ""; }
    public void setEmail(String email) { this.email = email != null ? email.trim() : ""; }
    public void setName(String name) { this.name = name != null ? name.trim() : ""; }
}