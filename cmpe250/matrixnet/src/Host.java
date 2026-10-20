import java.util.ArrayList;

public class Host {
    String id;
    int clearanceLevel;
    HashMap<Host, Backdoor> neighbours = new HashMap<>();
    // Fast parallel arrays for neighbor iteration
    ArrayList<Host> neighbourHosts = new ArrayList<>();
    ArrayList<Backdoor> neighbourBackdoors = new ArrayList<>();
    Host parent = null; // For cycle detection and dijkstra path reconstruction
    int componentId = -1; // Component ID for connectivity
    ArrayList<int[]> pareto = null; // Pareto frontier for trace_route
    int dijkstraDist = Integer.MAX_VALUE; // For Dijkstra shortest path
    boolean visited = false; // For BFS/DFS traversals
    // State-space Dijkstra (lambda > 0) bookkeeping
    int[] ssKeys = null; // step count, -1 means empty
    int[] ssCosts = null; // best cost for the step in ssKeys
    MinHeap.State[] ssStates = null; // best state (tie-broken by path name) for the step in ssKeys
    int ssMask = 0; // table size - 1
    int ssSize = 0; // number of occupied slots
    int[] ssTouchedSlots = null; // slots written this run (for fast reset)
    int ssTouchedSize = 0;
    boolean ssTouchedThisRun = false;
    int ssToDestEdges = Integer.MAX_VALUE; // Min #edges to destination (for pruning)

    public Host(String id, int clearanceLevel) {
        this.id = id;
        this.clearanceLevel = clearanceLevel;
    }

    public void addNeighbour(Host host, Backdoor backdoor) {
        neighbours.put(host, backdoor);
        neighbourHosts.add(host);
        neighbourBackdoors.add(backdoor);
    }

    public Backdoor getBackdoor(Host host) {
        return neighbours.get(host);
    }
}
