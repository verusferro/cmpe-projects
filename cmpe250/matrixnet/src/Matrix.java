import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedList;

public class Matrix {

    HashSet<String> hostIds = new HashSet<>();
    HashMap<String, Host> hosts = new HashMap<>();
    // Cache component count and cycles to avoid recalculation
    int cachedComponentCount = -1;
    boolean componentCountValid = false;
    boolean cycleValid = false;
    boolean cycleExists = false;
    int unsealedBackdoorCount = 0;
    int totalBandwidth = 0;
    int totalBackdoorCount = 0;
    int totalClearanceLevel = 0;

    public String spawn_host(String hostId, int clearanceLevel) {
        if (clearanceLevel < 1 || clearanceLevel > 5 || hostIds.contains(hostId)) {
            return "Some error occurred in spawn_host.";
        }
        for (char c : hostId.toCharArray()) {
            if (!((c >= 'A' && c <= 'Z') ||
                  (c >= '0' && c <= '9') ||
                   c == '_')) {
                return "Some error occurred in spawn_host.";
            }
        }
        hostIds.add(hostId);
        hosts.put(hostId, new Host(hostId, clearanceLevel));
        componentCountValid = false;
        totalClearanceLevel += clearanceLevel;
        return "Spawned host " + hostId + " with clearance level " + clearanceLevel + ".";
    }

    public String link_backdoor(String hostId1, String hostId2, int latency, int bandwidth, int firewallLevel) {
        Host host1 = hosts.get(hostId1);
        Host host2 = hosts.get(hostId2);
        if (firewallLevel < 1 || firewallLevel > 5 || bandwidth < 1 || latency < 1 ||
            hostId1.equals(hostId2) || host1 == null || host2 == null || host1.getBackdoor(host2) != null) {
            return "Some error occurred in link_backdoor.";
        }
        Backdoor backdoor = new Backdoor(latency, bandwidth, firewallLevel);
        host1.addNeighbour(host2, backdoor);
        host2.addNeighbour(host1, backdoor);
        componentCountValid = false;
        cycleValid = false;
        unsealedBackdoorCount++;
        totalBackdoorCount++;
        totalBandwidth += bandwidth;
        return String.format("Linked %s <-> %s with latency %dms, bandwidth %dMbps, firewall %d.",
                hostId1, hostId2, latency, bandwidth, firewallLevel);
    }

    public String seal_backdoor(String hostId1, String hostId2) {
        Host host1 = hosts.get(hostId1);
        Host host2 = hosts.get(hostId2);
        if (hostId1.equals(hostId2) || host1 == null || host2 == null) {
            return "Some error occurred in seal_backdoor.";
        }
        Backdoor backdoor = host1.getBackdoor(host2);
        if (backdoor == null) {
            return "Some error occurred in seal_backdoor.";
        }
        backdoor.isSealed = !backdoor.isSealed;
        unsealedBackdoorCount += backdoor.isSealed ? -1 : 1;
        totalBandwidth += backdoor.isSealed ? -backdoor.bandwidth : backdoor.bandwidth;
        componentCountValid = false;
        cycleValid = false;
        return String.format("Backdoor %s <-> %s %s.", hostId1, hostId2, backdoor.isSealed ? "sealed" : "unsealed");
    }

    public String scan_connectivity() {
        if (!componentCountValid) {
            recalculateComponentCount();
            componentCountValid = true;
        }
        if (cachedComponentCount == 1) {
            return "Network is fully connected.";
        }
        return "Network has " + cachedComponentCount + " disconnected components.";
    }

    // Host breach
    public String simulate_breach(String hostId1) {
        Host host = hosts.get(hostId1);
        if (host == null) {
            return "Some error occurred in simulate_breach.";
        }
        if (!componentCountValid) {
            recalculateComponentCount();
            componentCountValid = true;
        }
        int oldCount = cachedComponentCount;
        ArrayList<Backdoor> tempSealedBackdoors = new ArrayList<>();
        for (Backdoor backdoor : host.neighbours.values()) {
            if (!backdoor.isSealed) {
                tempSealedBackdoors.add(backdoor);
                backdoor.isSealed = true;
            }
        }
        hosts.remove(hostId1);
        recalculateComponentCount();
        int newCount = cachedComponentCount;
        for (Backdoor backdoor : tempSealedBackdoors) {
            backdoor.isSealed = false;
        }
        hosts.put(hostId1, host);
        cachedComponentCount = oldCount;
        if (newCount <= oldCount) {
            return "Host " + hostId1 + " is NOT an articulation point. Network remains the same.";
        }
        return "Host " + hostId1 + " IS an articulation point.\n"
                + "Failure results in " + newCount + " disconnected components.";
    }

    // Backdoor breach
    public String simulate_breach(String hostId1, String hostId2) {
        Host host1 = hosts.get(hostId1);
        Host host2 = hosts.get(hostId2);
        if (host1 == null || host2 == null) {
            return "Some error occurred in simulate_breach.";
        }
        Backdoor backdoor = host1.neighbours.get(host2);
        if (backdoor == null) {
            return "Some error occurred in simulate_breach.";
        }
        if (!componentCountValid) {
            recalculateComponentCount();
            componentCountValid = true;
        }
        int oldCount = cachedComponentCount;
        boolean tempSealed = false;
        if (!backdoor.isSealed) {
            backdoor.isSealed = true;
            tempSealed = true;
            recalculateComponentCount();
        }
        int newCount = cachedComponentCount;
        if (tempSealed) {
            backdoor.isSealed = false;
        }
        cachedComponentCount = oldCount;
        if (oldCount == newCount) {
            return "Backdoor " + hostId1 + " <-> " + hostId2 + " is NOT a bridge. Network remains the same.";
        }
        return "Backdoor " + hostId1 + " <-> " + hostId2 + " IS a bridge.\n"
                + "Failure results in " + newCount + " disconnected components.";
    }

    public String oracle_report() {
        if (!componentCountValid) {
            recalculateComponentCount();
        }
        if (!cycleValid) {
            checkCycle();
        }
        return String.format(
                        """
                        --- Resistance Network Report ---
                        Total Hosts: %d
                        Total Unsealed Backdoors: %d
                        Network Connectivity: %s
                        Connected Components: %d
                        Contains Cycles: %s
                        Average Bandwidth: %sMbps
                        Average Clearance Level: %.1f""",
                hosts.size(), unsealedBackdoorCount,
                cachedComponentCount == 1 ? "Connected" : "Disconnected",
                cachedComponentCount, cycleExists ? "Yes" : "No",
                new BigDecimal(totalBandwidth)
                        .divide(new BigDecimal(unsealedBackdoorCount), 1, RoundingMode.HALF_UP)
                        .doubleValue(),
                new BigDecimal(totalClearanceLevel)
                        .divide(new BigDecimal(hosts.size()), 1, RoundingMode.HALF_UP)
                        .doubleValue()
        );
    }

    public String trace_route(String sourceId, String destId, int minBandwidth, int lambda) {
        Host sourceHost = hosts.get(sourceId);
        Host destHost = hosts.get(destId);
        if (sourceHost == null || destHost == null) {
            return "Some error occurred in trace_route.";
        }
        if (sourceId.equals(destId)) {
            return "Optimal route " + sourceId + " -> " + destId + ": " + destId + " (Latency = 0ms)";
        }

        if (lambda == 0) {
            // Use standard Dijkstra when lambda is 0 (no penalty for hops)
            return trace_route_dijkstra(sourceHost, destHost, minBandwidth);
        }

        return trace_route_state_space(sourceHost, destHost, minBandwidth, lambda);
    }

    private void ssInitIfNeeded(Host h) {
        if (h.ssKeys != null) {
            return;
        }
        int cap = 8;
        h.ssKeys = new int[cap];
        h.ssCosts = new int[cap];
        h.ssStates = new MinHeap.State[cap];
        h.ssTouchedSlots = new int[cap];
        h.ssMask = cap - 1;
        for (int i = 0; i < cap; i++) {
            h.ssKeys[i] = -1;
        }
        h.ssSize = 0;
        h.ssTouchedSize = 0;
        h.ssTouchedThisRun = false;
    }

    private int ssFindSlot(Host h, int steps) {
        int idx = (steps * 0x9E3779B9) & h.ssMask;
        while (true) {
            int k = h.ssKeys[idx];
            if (k == -1 || k == steps) {
                return idx;
            }
            idx = (idx + 1) & h.ssMask;
        }
    }

    private void ssGrow(Host h) {
        int[] oldKeys = h.ssKeys;
        int[] oldCosts = h.ssCosts;
        MinHeap.State[] oldStates = h.ssStates;

        int newCap = oldKeys.length * 2;
        h.ssKeys = new int[newCap];
        h.ssCosts = new int[newCap];
        h.ssStates = new MinHeap.State[newCap];
        h.ssTouchedSlots = new int[newCap];
        h.ssMask = newCap - 1;

        for (int i = 0; i < newCap; i++) {
            h.ssKeys[i] = -1;
        }

        h.ssSize = 0;
        h.ssTouchedSize = 0;

        for (int i = 0; i < oldKeys.length; i++) {
            int k = oldKeys[i];
            if (k == -1) continue;
            int slot = ssFindSlot(h, k);
            h.ssKeys[slot] = k;
            h.ssCosts[slot] = oldCosts[i];
            h.ssStates[slot] = oldStates[i];
            h.ssTouchedSlots[h.ssTouchedSize++] = slot;
            h.ssSize++;
        }
    }

    private int ssGetCost(Host h, int steps) {
        ssInitIfNeeded(h);
        int slot = ssFindSlot(h, steps);
        return h.ssKeys[slot] == -1 ? Integer.MAX_VALUE : h.ssCosts[slot];
    }

    private MinHeap.State ssGetState(Host h, int steps) {
        ssInitIfNeeded(h);
        int slot = ssFindSlot(h, steps);
        return h.ssKeys[slot] == -1 ? null : h.ssStates[slot];
    }

    private void ssPut(Host h, int steps, int cost, MinHeap.State state, ArrayList<Host> touchedHosts) {
        ssInitIfNeeded(h);
        if (!h.ssTouchedThisRun) {
            h.ssTouchedThisRun = true;
            touchedHosts.add(h);
        }

        // grow if load factor > ~0.66
        if ((h.ssSize * 3) >= (h.ssKeys.length * 2)) {
            ssGrow(h);
        }

        int slot = ssFindSlot(h, steps);
        if (h.ssKeys[slot] == -1) {
            h.ssKeys[slot] = steps;
            h.ssCosts[slot] = cost;
            h.ssStates[slot] = state;
            h.ssTouchedSlots[h.ssTouchedSize++] = slot;
            h.ssSize++;
        } else {
            h.ssCosts[slot] = cost;
            h.ssStates[slot] = state;
        }
    }

    private void ssResetTouched(ArrayList<Host> touchedHosts) {
        for (int hi = 0; hi < touchedHosts.size(); hi++) {
            Host h = touchedHosts.get(hi);
            for (int ti = 0; ti < h.ssTouchedSize; ti++) {
                int slot = h.ssTouchedSlots[ti];
                h.ssKeys[slot] = -1;
                h.ssStates[slot] = null;
            }
            h.ssSize = 0;
            h.ssTouchedSize = 0;
            h.ssTouchedThisRun = false;
        }
        touchedHosts.clear();
    }

    private boolean ssDominated(Host h, int steps, int cost) {
        if (h.ssKeys == null) return false;
        for (int ti = 0; ti < h.ssTouchedSize; ti++) {
            int slot = h.ssTouchedSlots[ti];
            int k = h.ssKeys[slot];
            if (k == -1) continue;
            int c = h.ssCosts[slot];
            if (k <= steps && c <= cost && (k < steps || c < cost)) {
                return true;
            }
        }
        return false;
    }

    private void computeToDestBounds(Host destHost, int minBandwidth) {
        // Reverse Dijkstra (latency) + reverse BFS (min edges) on the directed-allowed graph.
        // Incoming edge into X from Y exists iff Y can traverse Y->X.
        for (Host h : hosts.values()) {
            h.dijkstraDist = Integer.MAX_VALUE; // reuse as latency-to-dest
            h.ssToDestEdges = Integer.MAX_VALUE;
        }

        // Min edges (reverse BFS)
        LinkedList<Host> q = new LinkedList<>();
        destHost.ssToDestEdges = 0;
        q.offer(destHost);
        while (!q.isEmpty()) {
            Host cur = q.poll();
            int curEdges = cur.ssToDestEdges;
            ArrayList<Host> nHosts = cur.neighbourHosts;
            ArrayList<Backdoor> nBackdoors = cur.neighbourBackdoors;
            for (int i = 0, n = nHosts.size(); i < n; i++) {
                Backdoor bd = nBackdoors.get(i);
                if (bd.isSealed) continue;
                if (bd.bandwidth < minBandwidth) continue;
                Host from = nHosts.get(i);
                if (from.clearanceLevel < bd.firewall_level) continue; // from -> cur allowed
                if (from.ssToDestEdges == Integer.MAX_VALUE) {
                    from.ssToDestEdges = curEdges + 1;
                    q.offer(from);
                }
            }
        }

        // Latency-to-dest (reverse Dijkstra)
        MinHeap pq = new MinHeap(64);
        destHost.dijkstraDist = 0;
        pq.insert(new MinHeap.State(0, 1, destHost, null));

        while (!pq.isEmpty()) {
            MinHeap.State s = pq.deleteMin();
            Host cur = s.host;
            int curLat = s.latency;
            if (cur.dijkstraDist != curLat) continue;

            ArrayList<Host> nHosts = cur.neighbourHosts;
            ArrayList<Backdoor> nBackdoors = cur.neighbourBackdoors;
            for (int i = 0, n = nHosts.size(); i < n; i++) {
                Backdoor bd = nBackdoors.get(i);
                if (bd.isSealed) continue;
                if (bd.bandwidth < minBandwidth) continue;
                Host from = nHosts.get(i);
                if (from.clearanceLevel < bd.firewall_level) continue; // from -> cur allowed

                int nextLat = curLat + bd.latency;
                if (nextLat < from.dijkstraDist) {
                    from.dijkstraDist = nextLat;
                    pq.insert(new MinHeap.State(nextLat, 1, from, null));
                }
            }
        }
    }

    private long minRemainingPenalty(int lambda, int curSteps, int remainingEdges) {
        if (lambda == 0 || remainingEdges <= 0) return 0;
        // Sum_{j=0..remainingEdges-1} lambda * ((curSteps + j) - 1)
        // = lambda * remainingEdges * (2*curSteps + remainingEdges - 3) / 2
        long r = remainingEdges;
        return (long) lambda * r * (2L * curSteps + r - 3L) / 2L;
    }

    private String trace_route_state_space(Host sourceHost, Host destHost, int minBandwidth, int lambda) {
        int hostCount = hosts.size();
        int maxSteps = hostCount;
        ArrayList<Host> touchedHosts = new ArrayList<>();

        // Upper bound (latency-only Dijkstra) to prune both cost and step counts.
        MinHeap.State ubState = dijkstra_latency_upper_bound(sourceHost, destHost, minBandwidth);
        int upperCost = Integer.MAX_VALUE;
        if (ubState != null) {
            long edges = (long) ubState.steps - 1;
            long penaltyTotal = (long) lambda * edges * (edges - 1) / 2;
            long cost = (long) ubState.latency + penaltyTotal;
            if (cost <= Integer.MAX_VALUE) {
                upperCost = (int) cost;
                maxSteps = computeStepUpperBound(hostCount, lambda, upperCost);
            }
        }

        // Stronger admissible lower bounds to prune expansions.
        computeToDestBounds(destHost, minBandwidth);
        if (sourceHost.dijkstraDist == Integer.MAX_VALUE) {
            return "No route found from " + sourceHost.id + " to " + destHost.id;
        }

        int bestLatency = Integer.MAX_VALUE;
        int bestSteps = Integer.MAX_VALUE;
        MinHeap.State bestState = null;

        MinHeap pq = new MinHeap(64);
        MinHeap.State start = new MinHeap.State(0, 1, sourceHost, null);
        ssPut(sourceHost, 1, 0, start, touchedHosts);
        pq.insert(start);

        while (!pq.isEmpty()) {
            MinHeap.State s = pq.deleteMin();
            int curCost = s.latency;
            int curSteps = s.steps;
            Host current = s.host;

            if (curCost > bestLatency) break;
            if (curCost == bestLatency && curSteps > bestSteps) break;
            if (upperCost != Integer.MAX_VALUE && curCost > upperCost) break;

            if (curSteps > maxSteps) continue;
            if (ssGetCost(current, curSteps) != curCost) continue;
            if (ssGetState(current, curSteps) != s) continue;

            // Dominance pruning: if there's a state with <= steps and <= cost, this one can't win.
            if (ssDominated(current, curSteps, curCost)) continue;

            // Admissible bound: g + latency_to_dest + min_penalty(remainingEdges)
            if (current.dijkstraDist == Integer.MAX_VALUE || current.ssToDestEdges == Integer.MAX_VALUE) {
                continue;
            }
            long lb = (long) curCost + (long) current.dijkstraDist +
                    minRemainingPenalty(lambda, curSteps, current.ssToDestEdges);
            if (upperCost != Integer.MAX_VALUE && lb > (long) upperCost) continue;
            if (bestLatency != Integer.MAX_VALUE && lb > (long) bestLatency) continue;

            if (current == destHost) {
                if (bestState == null ||
                    curCost < bestLatency ||
                    (curCost == bestLatency && curSteps < bestSteps) ||
                    (curCost == bestLatency && curSteps == bestSteps &&
                     MinHeap.comparePathNames(s.buildPath(), bestState.buildPath()) < 0)) {
                    bestLatency = curCost;
                    bestSteps = curSteps;
                    bestState = s;
                }
                continue;
            }

            ArrayList<Host> nHosts = current.neighbourHosts;
            ArrayList<Backdoor> nBackdoors = current.neighbourBackdoors;
            for (int i = 0, n = nHosts.size(); i < n; i++) {
                Backdoor bd = nBackdoors.get(i);
                if (bd.bandwidth < minBandwidth) continue;
                if (current.clearanceLevel < bd.firewall_level) continue;
                if (bd.isSealed) continue;

                Host neighbourHost = nHosts.get(i);

                int nextSteps = curSteps + 1;
                if (nextSteps > maxSteps) continue;

                long penalty = (long) lambda * (curSteps - 1);
                long nextCostL = (long) curCost + (long) bd.latency + penalty;
                if (nextCostL > Integer.MAX_VALUE) continue;
                int nextCost = (int) nextCostL;

                if (nextCost > bestLatency) continue;
                if (upperCost != Integer.MAX_VALUE && nextCost > upperCost) continue;

                if (neighbourHost.dijkstraDist != Integer.MAX_VALUE && neighbourHost.ssToDestEdges != Integer.MAX_VALUE) {
                    long nlb = (long) nextCost + (long) neighbourHost.dijkstraDist +
                            minRemainingPenalty(lambda, nextSteps, neighbourHost.ssToDestEdges);
                    if (upperCost != Integer.MAX_VALUE && nlb > (long) upperCost) continue;
                    if (bestLatency != Integer.MAX_VALUE && nlb > (long) bestLatency) continue;
                }

                // Skip if already dominated at this host.
                if (ssDominated(neighbourHost, nextSteps, nextCost)) continue;

                int prevBest = ssGetCost(neighbourHost, nextSteps);
                if (nextCost < prevBest) {
                    MinHeap.State nextState = new MinHeap.State(nextCost, nextSteps, neighbourHost, s);
                    ssPut(neighbourHost, nextSteps, nextCost, nextState, touchedHosts);
                    pq.insert(nextState);
                } else if (nextCost == prevBest) {
                    MinHeap.State existing = ssGetState(neighbourHost, nextSteps);
                    MinHeap.State nextState = new MinHeap.State(nextCost, nextSteps, neighbourHost, s);
                    if (existing == null ||
                        MinHeap.comparePathNames(nextState.buildPath(), existing.buildPath()) < 0) {
                        ssPut(neighbourHost, nextSteps, nextCost, nextState, touchedHosts);
                        pq.insert(nextState);
                    }
                }
            }
        }

        ssResetTouched(touchedHosts);

        if (bestState == null) {
            return "No route found from " + sourceHost.id + " to " + destHost.id;
        }

        ArrayList<String> bestPath = bestState.buildPath();
        StringBuilder output = new StringBuilder();
        output.append("Optimal route ").append(sourceHost.id).append(" -> ").append(destHost.id).append(": ");
        for (int i = 0; i < bestPath.size() - 1; i++) {
            output.append(bestPath.get(i)).append(" -> ");
        }
        output.append(destHost.id).append(" (Latency = ").append(bestLatency).append("ms)");
        return output.toString();
    }

    private MinHeap.State dijkstra_latency_upper_bound(Host sourceHost, Host destHost, int minBandwidth) {
        // Dijkstra on pure latency (ignoring lambda penalty) to obtain a quick upper bound.
        for (Host h : hosts.values()) {
            h.dijkstraDist = Integer.MAX_VALUE;
        }

        MinHeap pq = new MinHeap(64);
        sourceHost.dijkstraDist = 0;
        pq.insert(new MinHeap.State(0, 1, sourceHost, null));

        while (!pq.isEmpty()) {
            MinHeap.State s = pq.deleteMin();
            Host current = s.host;
            int curLatency = s.latency;

            if (current.dijkstraDist != curLatency) continue;
            if (current == destHost) return s;

            ArrayList<Host> nHosts = current.neighbourHosts;
            ArrayList<Backdoor> nBackdoors = current.neighbourBackdoors;
            for (int i = 0, n = nHosts.size(); i < n; i++) {
                Backdoor bd = nBackdoors.get(i);
                if (bd.bandwidth < minBandwidth) continue;
                if (current.clearanceLevel < bd.firewall_level) continue;
                if (bd.isSealed) continue;

                Host neighbourHost = nHosts.get(i);
                int newLatency = curLatency + bd.latency;
                if (newLatency < neighbourHost.dijkstraDist) {
                    neighbourHost.dijkstraDist = newLatency;
                    pq.insert(new MinHeap.State(newLatency, s.steps + 1, neighbourHost, s));
                }
            }
        }

        return null;
    }

    private int computeStepUpperBound(int hostCount, int lambda, int upperCost) {
        // Max steps m (nodes visited, edges=m-1) such that:
        // lowerBound = (m-1) + lambda * (m-1)(m-2)/2 <= upperCost
        int lo = 1;
        int hi = hostCount;
        while (lo < hi) {
            int mid = (lo + hi + 1) >>> 1;
            long edges = (long) mid - 1;
            long penalty = (long) lambda * edges * (edges - 1) / 2;
            long lb = edges + penalty;
            if (lb <= (long) upperCost) lo = mid;
            else hi = mid - 1;
        }
        return lo;
    }

    private String trace_route_dijkstra(Host sourceHost, Host destHost, int minBandwidth) {
        // Standard Dijkstra for lambda=0 case
        // Clear dijkstra fields
        for (Host h : hosts.values()) {
            h.dijkstraDist = Integer.MAX_VALUE;
            h.parent = null;
        }
        
        MinHeap pq = new MinHeap(64);
        sourceHost.dijkstraDist = 0;
        pq.insert(new MinHeap.State(0, 1, sourceHost, null));
        
        while (!pq.isEmpty()) {
            MinHeap.State s = pq.deleteMin();
            Host current = s.host;
            int curLatency = s.latency;
            
            // Skip if we've already processed with better latency
            if (current.dijkstraDist < curLatency) {
                continue;
            }
            
            if (current == destHost) {
                // Reconstruct path from parent chain
                ArrayList<String> path = new ArrayList<>();
                Host h = destHost;
                while (h != null) {
                    path.add(h.id);
                    h = h.parent;
                }
                StringBuilder output = new StringBuilder();
                output.append("Optimal route ").append(sourceHost.id).append(" -> ").append(destHost.id).append(": ");
                for (int i = path.size() - 1; i > 0; i--) {
                    output.append(path.get(i)).append(" -> ");
                }
                output.append(destHost.id).append(" (Latency = ").append(curLatency).append("ms)");
                return output.toString();
            }
            
            // Explore neighbors
            ArrayList<Host> nHosts = current.neighbourHosts;
            ArrayList<Backdoor> nBackdoors = current.neighbourBackdoors;
            for (int i = 0, n = nHosts.size(); i < n; i++) {
                Backdoor bd = nBackdoors.get(i);
                
                if (bd.bandwidth < minBandwidth) continue;
                if (current.clearanceLevel < bd.firewall_level) continue;
                if (bd.isSealed) continue;
                
                Host neighbourHost = nHosts.get(i);
                
                int newLatency = curLatency + bd.latency;
                
                if (newLatency < neighbourHost.dijkstraDist) {
                    neighbourHost.dijkstraDist = newLatency;
                    neighbourHost.parent = current;
                    pq.insert(new MinHeap.State(newLatency, s.steps + 1, neighbourHost, s));
                }
            }
        }
        
        return "No route found from " + sourceHost.id + " to " + destHost.id;
    }

    public void checkCycle() {
        if (hosts.size() < 3) {
            cycleExists = false;
            return;
        }

        for (Host h : hosts.values()) {
            h.visited = false;
        }
        // Use dfs to check for cycles
        LinkedList<Host> stack = new LinkedList<>();
        for (Host host : hosts.values()) {
            if (host.visited) {
                continue;
            }
            host.parent = null;
            host.visited = true;
            stack.push(host);
            while (!stack.isEmpty()) {
                Host current = stack.pop();
                ArrayList<Host> nHosts = current.neighbourHosts;
                ArrayList<Backdoor> nBackdoors = current.neighbourBackdoors;
                for (int i = 0, n = nHosts.size(); i < n; i++) {
                    Backdoor bd = nBackdoors.get(i);
                    if (bd.isSealed) {
                        continue;
                    }
                    Host neighbourHost = nHosts.get(i);
                    if (!neighbourHost.visited) {
                        neighbourHost.visited = true;
                        neighbourHost.parent = current;
                        stack.push(neighbourHost);
                    } else if (neighbourHost != current.parent) {
                        cycleExists = true;
                        return;
                    }
                }
            }
        }
        cycleExists = false;
    }

    private void recalculateComponentCount() {
        if (hosts.size() < 2) {
            cachedComponentCount = 1;
            for (Host h : hosts.values()) {
                h.componentId = 0;
            }
            return;
        }

        for (Host h : hosts.values()) {
            h.visited = false;
        }
        // Use bfs on every node to count components
        LinkedList<Host> queue = new LinkedList<>();
        int componentCount = 0;
        for (Host host : hosts.values()) {
            if (host.visited) {
                continue;
            }
            host.visited = true;
            host.componentId = componentCount;
            queue.offer(host);
            while (!queue.isEmpty()) {
                Host current = queue.poll();
                ArrayList<Host> nHosts = current.neighbourHosts;
                ArrayList<Backdoor> nBackdoors = current.neighbourBackdoors;
                for (int i = 0, n = nHosts.size(); i < n; i++) {
                    Backdoor bd = nBackdoors.get(i);
                    Host neighbourHost = nHosts.get(i);
                    if (!bd.isSealed && !neighbourHost.visited) {
                        neighbourHost.visited = true;
                        neighbourHost.componentId = componentCount;
                        queue.offer(neighbourHost);
                    }
                }
            }
            componentCount++;
        }
        cachedComponentCount = componentCount;
    }
}
