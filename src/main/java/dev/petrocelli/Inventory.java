package dev.petrocelli;

import java.util.*;

/** Single-process inventory. All mutations share one monitor. */
public final class Inventory {
    public enum Status { ACTIVE, CONFIRMED, CANCELLED }
    public record Reservation(String key, String sku, int quantity, Status status) {}
    private final Map<String,Integer> available = new HashMap<>();
    private final Map<String,Reservation> reservations = new HashMap<>();
    private static String text(String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Blank identifier");
        return value;
    }
    public synchronized void restock(String sku, int quantity) {
        text(sku);
        if (quantity <= 0) throw new IllegalArgumentException("Quantity must be positive");
        available.put(sku, Math.addExact(available.getOrDefault(sku,0),quantity));
    }
    public synchronized int available(String sku) { return available.getOrDefault(text(sku),0); }
    public synchronized Reservation reserve(String key, String sku, int quantity) {
        text(key); text(sku);
        if (quantity <= 0) throw new IllegalArgumentException("Quantity must be positive");
        var previous=reservations.get(key);
        if (previous != null) {
            if (!previous.sku().equals(sku) || previous.quantity()!=quantity)
                throw new IllegalArgumentException("Idempotency key reused with different payload");
            return previous;
        }
        int stock=available(sku);
        if (stock < quantity) throw new IllegalStateException("Insufficient stock");
        var reservation=new Reservation(key,sku,quantity,Status.ACTIVE);
        available.put(sku,stock-quantity);
        reservations.put(key,reservation);
        return reservation;
    }
    public synchronized Reservation confirm(String key) { return transition(key,Status.CONFIRMED); }
    public synchronized Reservation cancel(String key) { return transition(key,Status.CANCELLED); }
    private Reservation transition(String key, Status target) {
        var current=reservations.get(text(key));
        if (current==null) throw new NoSuchElementException("Unknown reservation");
        if (current.status()==target) return current;
        if (current.status()!=Status.ACTIVE) throw new IllegalStateException("Reservation already finalized");
        if (target==Status.CANCELLED)
            available.put(current.sku(),Math.addExact(available(current.sku()),current.quantity()));
        var next=new Reservation(key,current.sku(),current.quantity(),target);
        reservations.put(key,next);
        return next;
    }
}
