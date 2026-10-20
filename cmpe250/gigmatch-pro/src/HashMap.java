import java.util.LinkedList;

// A simple hashmap implementation to store id and object pointers
// Used to store and search all customers and freelancers
// Uses open hashing
public class HashMap<V> {
    private static class Entry<V> {
        String key;
        V value;
        
        Entry(String key, V value) {
            this.key = key;
            this.value = value;
        }
    }
    
    private LinkedList<Entry<V>>[] buckets;
    private int size;
    private int capacity;

    @SuppressWarnings("unchecked")
    public HashMap() {
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
    
    public void put(String key, V value) {
        if (key == null) return;
        
        int index = hash(key);
        
        if (buckets[index] == null) {
            buckets[index] = new LinkedList<>();
        }
        
        // Check if key already exists and update value
        for (Entry<V> entry : buckets[index]) {
            if (entry.key.equals(key)) {
                entry.value = value;
                return;
            }
        }
        
        buckets[index].add(new Entry<>(key, value));
        size++;
        
        // Resize if needed
        if (size > capacity * 0.75) {
            resize();
        }
    }
    
    public V get(String key) {
        if (key == null) return null;
        
        int index = hash(key);
        
        if (buckets[index] == null) {
            return null;
        }
        
        for (Entry<V> entry : buckets[index]) {
            if (entry.key.equals(key)) {
                return entry.value;
            }
        }
        
        return null;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean containsKey(String key) {
        if (key == null) return false;
        
        int index = hash(key);
        
        if (buckets[index] == null) {
            return false;
        }
        
        for (Entry<V> entry : buckets[index]) {
            if (entry.key.equals(key)) {
                return true;
            }
        }
        
        return false;
    }
    
    public V remove(String key) {
        if (key == null) return null;
        
        int index = hash(key);
        
        if (buckets[index] == null) {
            return null;
        }
        
        for (int i = 0; i < buckets[index].size(); i++) {
            Entry<V> entry = buckets[index].get(i);
            if (entry.key.equals(key)) {
                buckets[index].remove(i);
                size--;
                return entry.value;
            }
        }
        
        return null;
    }

    public LinkedList<V> values() {
        LinkedList<V> valuesList = new LinkedList<>();
        for (int i = 0; i < capacity; i++) {
            if (buckets[i] != null) {
                for (Entry<V> entry : buckets[i]) {
                    valuesList.add(entry.value);
                }
            }
        }
        return valuesList;
    }

    @SuppressWarnings("unchecked")
    private void resize() {
        int oldCapacity = capacity;
        capacity = capacity * 2;
        LinkedList<Entry<V>>[] oldBuckets = buckets;
        buckets = new LinkedList[capacity];
        size = 0;
        
        // Rehash all entries
        for (int i = 0; i < oldCapacity; i++) {
            if (oldBuckets[i] != null) {
                for (Entry<V> entry : oldBuckets[i]) {
                    put(entry.key, entry.value);
                }
            }
        }
    }
}
