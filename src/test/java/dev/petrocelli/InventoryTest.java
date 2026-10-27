package dev.petrocelli;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public final class InventoryTest {
    private static void check(boolean value) { if (!value) throw new AssertionError(); }
    private static void rejects(Class<? extends Throwable> type,Runnable action) {
        try { action.run(); } catch (Throwable e) { if (type.isInstance(e)) return; throw e; }
        throw new AssertionError("Expected "+type);
    }
    public static void main(String[] args) throws Exception {
        var inventory=new Inventory(); inventory.restock("BOOK",10);
        var first=inventory.reserve("order-1","BOOK",3);
        check(first.equals(inventory.reserve("order-1","BOOK",3)));
        check(inventory.available("BOOK")==7);
        rejects(IllegalArgumentException.class,()->inventory.reserve("order-1","BOOK",2));
        inventory.cancel("order-1"); inventory.cancel("order-1");
        check(inventory.available("BOOK")==10);
        rejects(IllegalStateException.class,()->inventory.confirm("order-1"));
        inventory.reserve("order-2","BOOK",4); inventory.confirm("order-2"); inventory.confirm("order-2");
        rejects(IllegalStateException.class,()->inventory.cancel("order-2"));
        check(inventory.available("BOOK")==6);
        rejects(IllegalArgumentException.class,()->inventory.restock("BOOK",0));
        rejects(NoSuchElementException.class,()->inventory.cancel("missing"));
        inventory.restock("MAX",Integer.MAX_VALUE);
        rejects(ArithmeticException.class,()->inventory.restock("MAX",1));
        check(inventory.available("MAX")==Integer.MAX_VALUE);
        var concurrent=new Inventory(); concurrent.restock("LAST",10);
        var pool=Executors.newFixedThreadPool(8);
        var start=new CountDownLatch(1); var wins=new AtomicInteger();
        var futures=new ArrayList<Future<?>>();
        try {
            for (int i=0;i<100;i++) { final int id=i;
                futures.add(pool.submit(()->{ start.await();
                    try { concurrent.reserve("r"+id,"LAST",1); wins.incrementAndGet(); }
                    catch (IllegalStateException expected) { /* Sold out. */ }
                    return null;
                }));
            }
            start.countDown(); for (var future:futures) future.get(10,TimeUnit.SECONDS);
        } finally { pool.shutdownNow(); }
        check(wins.get()==10); check(concurrent.available("LAST")==0);
        System.out.println("Inventory: OK");
    }
}
