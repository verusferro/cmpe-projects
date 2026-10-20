public class Backdoor {
    int latency;
    int bandwidth;
    int firewall_level;
    boolean isSealed = false;

    public Backdoor(int latency, int bandwidth, int firewall_level) {
        this.latency = latency;
        this.bandwidth = bandwidth;
        this.firewall_level = firewall_level;
    }
}
