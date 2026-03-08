package cli;

public class Agent extends User {
    private String agentId;

    public Agent(String username, String password, String email, String name) {
        super(username, password, email, name);
        this.agentId = "AGT" + System.currentTimeMillis();
    }

    public String getAgentId() { return agentId; }
}