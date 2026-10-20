import java.util.LinkedList;

// Simple hashset to store all generated ids
// Used to check for unique ids and blacklist
public class HashSet {
    private LinkedList<String>[] buckets;
    private int size;
    private int capacity;

    @SuppressWarnings("unchecked")
    public HashSet() {
        this.capacity = 16;
        this.buckets = new LinkedList[capacity];
        this.size = 0;
    }
    
    private int hash(String key) {
        if (key == null) return 0;
        int hash = key.hashCode();
        if (hash < 0) hash = -hash;
        return hash % capacity;
    }
    
    public boolean add(String value) {
        if (value == null) return false;
        
        int index = hash(value);
        
        if (buckets[index] == null) {
            buckets[index] = new LinkedList<>();
        }
        
        // Check if value already exists
        if (buckets[index].contains(value)) {
            return false;
        }
        
        buckets[index].add(value);
        size++;
        
        // Resize if needed
        if (size > capacity * 0.75) {
            resize();
        }
        
        return true;
    }
    
    public boolean contains(String value) {
        if (value == null) return false;
        
        int index = hash(value);
        
        if (buckets[index] == null) {
            return false;
        }
        
        return buckets[index].contains(value);
    }
    
    public boolean remove(String value) {
        if (value == null) return false;
        
        int index = hash(value);
        
        if (buckets[index] == null) {
            return false;
        }
        
        boolean removed = buckets[index].remove(value);
        if (removed) {
            size--;
        }
        
        return removed;
    }
    
    public int size() {
        return size;
    }

    @SuppressWarnings("unchecked")
    private void resize() {
        int oldCapacity = capacity;
        capacity = capacity * 2;
        LinkedList<String>[] oldBuckets = buckets;
        buckets = new LinkedList[capacity];
        size = 0;
        
        // Rehash all elements
        for (int i = 0; i < oldCapacity; i++) {
            if (oldBuckets[i] != null) {
                for (String value : oldBuckets[i]) {
                    add(value);
                }
            }
        }
    }
}
