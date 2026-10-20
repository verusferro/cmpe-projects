import java.util.ArrayList;

// MinHeap for State priority queue in trace_route
public class MinHeap {
	private State[] heap;
	private int size;

	public MinHeap(int capacity) {
		heap = new State[capacity + 1];
		size = 0;
	}
	
	public boolean isEmpty() {
		return size == 0;
	}
	
	public void insert(State state) {
		if (size + 1 >= heap.length) {
			State[] newHeap = new State[heap.length * 2];
			for (int i = 0; i <= size; i++) newHeap[i] = heap[i];
			heap = newHeap;
		}
		int cur = ++size;
		while (cur > 1 && compare(heap[cur / 2], state) > 0) {
			heap[cur] = heap[cur / 2];
			cur /= 2;
		}
		heap[cur] = state;
	}
	
	public State deleteMin() {
		State min = heap[1];
		heap[1] = heap[size];
		heap[size--] = null;
		if (size > 0) {
			int hole = 1;
			State temp = heap[1];
			while (hole * 2 <= size) {
				int child = hole * 2;
				if (child + 1 <= size && compare(heap[child + 1], heap[child]) < 0) child++;
				if (compare(temp, heap[child]) <= 0) break;
				heap[hole] = heap[child];
				hole = child;
			}
			heap[hole] = temp;
		}
		return min;
	}
	
	private int compare(State a, State b) {
		if (a.latency != b.latency) return a.latency - b.latency;
		return a.steps - b.steps;
	}
	
	public static class State {
		public final int latency;
		public final int steps;
		public final Host host;
		public final State parent;
		
		public State(int latency, int steps, Host host, State parent) {
			this.latency = latency;
			this.steps = steps;
			this.host = host;
			this.parent = parent;
		}
		
		public boolean containsHost(Host h) {
			State s = this;
			while (s != null) {
				if (s.host == h) return true;
				s = s.parent;
			}
			return false;
		}
		
		public ArrayList<String> buildPath() {
			ArrayList<String> path = new ArrayList<>(steps);
			State s = this;
			while (s != null) {
				path.add(s.host.id);
				s = s.parent;
			}
			for (int i = 0, j = path.size() - 1; i < j; i++, j--) {
				String tmp = path.get(i);
				path.set(i, path.get(j));
				path.set(j, tmp);
			}
			return path;
		}
		
		public ArrayList<String> buildPathWith(Host nextHost) {
			ArrayList<String> path = new ArrayList<>(steps + 1);
			State s = this;
			while (s != null) {
				path.add(s.host.id);
				s = s.parent;
			}
			for (int i = 0, j = path.size() - 1; i < j; i++, j--) {
				String tmp = path.get(i);
				path.set(i, path.get(j));
				path.set(j, tmp);
			}
			path.add(nextHost.id);
			return path;
		}
	}

	public static int comparePathNames(ArrayList<String> p1, ArrayList<String> p2) {
		int n = Math.min(p1.size(), p2.size());
		for (int i = 0; i < n; i++) {
			int cmp = p1.get(i).compareTo(p2.get(i));
			if (cmp != 0) return cmp;
		}
		return p1.size() - p2.size();
	}
}
