// A maxheap implementation to be used as a priority queue
// Sorts freelancers based on score then id
// 1-indexed
public class MaxHeap {
	private static final int DEFAULT_CAPACITY = 16;

	private Freelancer[] heap;
	private int size;

	public MaxHeap() {
		this(DEFAULT_CAPACITY);
	}

	public MaxHeap(int capacity) {
		if (capacity <= 0) {
			throw new IllegalArgumentException("Capacity must be positive");
		}
		heap = new Freelancer[capacity + 1];
		size = 0;
	}

	public boolean isEmpty() {
		return size == 0;
	}

	public void insert(Freelancer freelancer) {
		ensureCapacity(size + 1);
		int current = ++size;
		heap[0] = freelancer;
		while (heap[current / 2] != null && compare(heap[current / 2], freelancer) < 0) {
			heap[current] = heap[current / 2];
			heap[current].heapIndex = current;
			current /= 2;
		}
		heap[current] = freelancer;
		freelancer.heapIndex = current;
		heap[0] = null;
	}

	public Freelancer deleteMax() {
		if (isEmpty()) {
			throw new IllegalStateException("Heap is empty");
		}
		Freelancer max = heap[1];
		max.heapIndex = -1;
		heap[1] = heap[size];
		heap[size] = null;
		size--;
		if (!isEmpty()) {
			heap[1].heapIndex = 1;
			percolateDown(1);
		}
		return max;
	}

	public void update(Freelancer freelancer) {
		int index = freelancer.heapIndex;
		if (index > size || index < 1) {
			return; // Freelancer not in heap
		}
		
		percolateUp(index);
		// Check if freelancer is still at the original position
		if (heap[index] == freelancer) {
			percolateDown(index);
		}
	}

	public void remove(Freelancer freelancer) {
		int index = freelancer.heapIndex;
		if (index > size || index < 1) {
			return; // Freelancer not in heap
		}
		
		freelancer.heapIndex = -1;
		
		if (index == size) {
			heap[size] = null;
			size--;
			return;
		}
		
		heap[index] = heap[size];
		heap[index].heapIndex = index;
		heap[size] = null;
		size--;
		
		if (!isEmpty() && index <= size) {
			Freelancer moved = heap[index];
			percolateUp(index);
			if (moved.heapIndex == index) {
				percolateDown(index);
			}
		}
	}

	private void percolateUp(int index) {
		Freelancer temp = heap[index];
		heap[0] = temp;
		while (index > 1 && compare(heap[index / 2], temp) < 0) {
			heap[index] = heap[index / 2];
			heap[index].heapIndex = index;
			index /= 2;
		}
		heap[index] = temp;
		temp.heapIndex = index;
		heap[0] = null;
	}

	private void percolateDown(int index) {
		Freelancer temp = heap[index];
		int hole = index;
		
		while (hole * 2 <= size) {
			int left = hole * 2;
			int right = left + 1;
			int largest = left;
			
			if (right <= size && compare(heap[right], heap[left]) > 0) {
				largest = right;
			}

			if (compare(temp, heap[largest]) >= 0) {
				break;
			}

			heap[hole] = heap[largest];
			heap[hole].heapIndex = hole;
			hole = largest;
		}
		
		heap[hole] = temp;
		temp.heapIndex = hole;
	}

	private void ensureCapacity(int requiredIndex) {
		if (requiredIndex < heap.length) {
			return;
		}
		int newLength = heap.length * 2;
		if (newLength <= requiredIndex) {
			newLength = requiredIndex + 1;
		}
		Freelancer[] newHeap = new Freelancer[newLength];
		for (int i = 0; i < heap.length; i++) {
			newHeap[i] = heap[i];
		}
		heap = newHeap;
	}

	private int compare(Freelancer f1, Freelancer f2) {
		if (f1.compositeScore != f2.compositeScore) {
			return Integer.compare(f1.compositeScore, f2.compositeScore);
		}
		// When scores are equal, lexicographically smaller ID has higher priority
		// So reverse the comparison
		return f2.id.compareTo(f1.id);
	}
}
