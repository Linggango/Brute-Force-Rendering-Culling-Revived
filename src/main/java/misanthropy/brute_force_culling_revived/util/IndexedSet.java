package misanthropy.brute_force_culling_revived.util;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.function.BiConsumer;

/**
 * A collection that provides O(1) containment checks while maintaining insertion order.
 * Note: Removal is O(n) due to ArrayList shifting.
 */
@SuppressWarnings("unused")
public class IndexedSet<E> {
    private final @NotNull ArrayList<E> list;
    private final @NotNull HashSet<E> set;

    public IndexedSet() {
        this.list = new ArrayList<>();
        this.set = new HashSet<>();
    }

    public IndexedSet(int initialCapacity) {
        this.list = new ArrayList<>(initialCapacity);
        this.set = new HashSet<>(initialCapacity);
    }

    public boolean add(E element) {
        if (set.add(element)) {
            list.add(element);
            return true;
        }
        return false;
    }

    /**
     * Warning: This is an O(n) operation.
     * If frequent removals are needed, consider a different data structure.
     */
    public boolean remove(E element) {
        if (set.remove(element)) {
            list.remove(element);
            return true;
        }
        return false;
    }

    /**
     * Standard for-each loop iteration is usually faster than this BiConsumer
     * in hot paths due to avoiding lambda object overhead.
     */
    public void forEach(@NotNull BiConsumer<? super E, Integer> action) {
        int size = list.size();
        for (int i = 0; i < size; ++i) {
            action.accept(list.get(i), i);
        }
    }

    public E get(int index) {
        return list.get(index);
    }

    public int size() {
        return list.size();
    }

    public boolean contains(E element) {
        return set.contains(element);
    }

    public void clear() {
        list.clear();
        set.clear();
    }

    @Override
    public String toString() {
        return list.toString();
    }
}