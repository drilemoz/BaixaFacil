package br.com.baixafacil;

import java.util.LinkedList;
import java.util.ListIterator;

/** Fila FIFO independente das classes Android. IDs únicos e prioridade manual. */
public final class PendingQueue<T> {
    private static final class Entry<T> {
        final String id;
        final T data;
        Entry(String id, T data) { this.id = id; this.data = data; }
    }
    private final LinkedList<Entry<T>> list = new LinkedList<>();

    public boolean offer(String id, T data) {
        if (id == null || data == null || position(id) > 0) return false;
        list.addLast(new Entry<>(id, data));
        return true;
    }
    public T poll() {
        return list.isEmpty() ? null : list.removeFirst().data;
    }
    public String pollId() {
        return list.isEmpty() ? null : list.removeFirst().id;
    }
    public T remove(String id) {
        ListIterator<Entry<T>> iterator = list.listIterator();
        while (iterator.hasNext()) {
            Entry<T> entry = iterator.next();
            if (entry.id.equals(id)) { iterator.remove(); return entry.data; }
        }
        return null;
    }
    public boolean moveNext(String id) {
        ListIterator<Entry<T>> iterator = list.listIterator();
        while (iterator.hasNext()) {
            Entry<T> entry = iterator.next();
            if (entry.id.equals(id)) {
                iterator.remove();
                list.addFirst(entry);
                return true;
            }
        }
        return false;
    }
    public int position(String id) {
        int i = 1;
        for (Entry<T> entry : list) {
            if (entry.id.equals(id)) return i;
            i++;
        }
        return -1;
    }
    public int size() { return list.size(); }
}
