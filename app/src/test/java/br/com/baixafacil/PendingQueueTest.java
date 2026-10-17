package br.com.baixafacil;

import static org.junit.Assert.*;
import org.junit.Test;

public class PendingQueueTest {
    @Test public void fifoAndPrioritize() {
        PendingQueue<String> queue = new PendingQueue<>();
        assertTrue(queue.offer("1", "A"));
        assertTrue(queue.offer("2", "B"));
        assertTrue(queue.offer("3", "C"));
        assertEquals(3, queue.position("3"));
        assertTrue(queue.moveNext("3"));
        assertEquals(1, queue.position("3"));
        assertEquals("C", queue.poll());
        assertEquals("A", queue.poll());
        assertEquals("B", queue.poll());
        assertNull(queue.poll());
    }
    @Test public void cancelPendingAndAvoidDuplicates() {
        PendingQueue<String> queue = new PendingQueue<>();
        assertFalse(queue.offer(null, "A"));
        assertTrue(queue.offer("a", "A"));
        assertFalse(queue.offer("a", "duplicated"));
        assertTrue(queue.offer("b", "B"));
        assertEquals("A", queue.remove("a"));
        assertEquals(-1, queue.position("a"));
        assertFalse(queue.moveNext("unknown"));
        assertEquals(1, queue.size());
        assertEquals("b", queue.pollId());
        assertEquals(0, queue.size());
    }
}
