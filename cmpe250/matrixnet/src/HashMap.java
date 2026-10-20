import java.util.ArrayList;
import java.util.LinkedList;

// A simple hashmap implementation to store id and object pointers
// Used to store and search all hosts
// Uses open hashing
public class HashMap<K, V> {
    public static class Entry<K, V> {
        K key;
        V value;
        
        Entry(K key, V value) {
            this.key = key;
            this.value = value;
        }
    }
    
    private int size = 0;
    private int capacity = 16;
    @SuppressWarnings("unchecked")
    private LinkedList<Entry<K, V>>[] buckets = new LinkedList[capacity];
    private ArrayList<V> cachedValues = new ArrayList<>();

    public HashMap() {
    }

    public int size() {
        return size;
    }
    
    private int hash(K key) {
        if (key == null) return 0;
        int hash = key.hashCode();
        if (hash < 0) hash = -hash;
        return hash % capacity;
    }
    
    public void put(K key, V value) {
        if (key == null) return;
        
        int index = hash(key);
        
        if (buckets[index] == null) {
            buckets[index] = new LinkedList<>();
        }
        
        // Check if key already exists and update value
        for (Entry<K, V> entry : buckets[index]) {
            if (entry.key.equals(key)) {
                entry.value = value;
                return;
            }
        }
        
        Entry<K, V> newEntry = new Entry<>(key, value);
        buckets[index].add(newEntry);
        cachedValues.add(value);
        size++;
        
        // Resize if needed
        if (size > capacity * 0.75) {
            resize();
        }
    }
    
    public V get(K key) {
        if (key == null) return null;
        
        int index = hash(key);
        
        if (buckets[index] == null) {
            return null;
        }
        
        for (Entry<K, V> entry : buckets[index]) {
            if (entry.key.equals(key)) {
                return entry.value;
            }
        }
        
        return null;
    }

    public void remove(K key) {
        if (key == null) return;

        int index = hash(key);

        if (buckets[index] == null) {
            return;
        }

        for (int i = 0; i < buckets[index].size(); i++) {
            Entry<K, V> entry = buckets[index].get(i);
            if (entry.key.equals(key)) {
                buckets[index].remove(i);
                cachedValues.remove(entry.value);
                size--;
                return;
            }
        }
    }

    public ArrayList<V> values() {
        return cachedValues;
    }

    @SuppressWarnings("unchecked")
    private void resize() {
        capacity = capacity * 2;
        LinkedList<Entry<K, V>>[] oldBuckets = buckets;
        buckets = new LinkedList[capacity];
        
        size = 0;
        cachedValues = new ArrayList<>();
        
        // Rehash all entries
        for (LinkedList<Entry<K, V>> bucket : oldBuckets) {
            if (bucket != null) {
                for (Entry<K, V> entry : bucket) {
                    put(entry.key, entry.value);
                }
            }
        }
    }
}
