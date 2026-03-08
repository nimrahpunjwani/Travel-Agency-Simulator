package cli;
// Admin class
public class Admin extends User {
    private String adminId;
    
    public Admin(String username, String password, String email, String name) {
        super(username, password, email, name);
        this.adminId = "ADM" + System.currentTimeMillis();
    }
    
    public String getAdminId() { return adminId; }
}