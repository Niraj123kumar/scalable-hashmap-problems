import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class FlashSaleInventoryManager {
    private final ConcurrentHashMap<String, AtomicInteger> stockMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, LinkedBlockingQueue<Integer>> waitingListMap = new ConcurrentHashMap<>();

    public void addProduct(String productId, int quantity) {
        stockMap.put(productId, new AtomicInteger(quantity));
        waitingListMap.put(productId, new LinkedBlockingQueue<>());
    }

    public int checkStock(String productId) {
        AtomicInteger stock = stockMap.get(productId);
        return stock != null ? stock.get() : 0;
    }

    public String purchaseItem(String productId, int userId) {
        AtomicInteger stock = stockMap.get(productId);
        if (stock == null) return "Product not found";

        while (true) {
            int currentStock = stock.get();
            if (currentStock > 0) {
                if (stock.compareAndSet(currentStock, currentStock - 1)) {
                    return "Success, " + (currentStock - 1) + " units remaining";
                }
            } else {
                LinkedBlockingQueue<Integer> queue = waitingListMap.get(productId);
                queue.add(userId);
                return "Added to waiting list, position #" + queue.size();
            }
        }
    }

    public List<Integer> getWaitingList(String productId) {
        LinkedBlockingQueue<Integer> queue = waitingListMap.get(productId);
        if (queue == null) return Collections.emptyList();
        return new ArrayList<>(queue);
    }

    public static void main(String[] args) {
        FlashSaleInventoryManager manager = new FlashSaleInventoryManager();
        manager.addProduct("IPHONE15_256GB", 100);

        System.out.println(manager.checkStock("IPHONE15_256GB"));
        System.out.println(manager.purchaseItem("IPHONE15_256GB", 12345));
        System.out.println(manager.purchaseItem("IPHONE15_256GB", 67890));

        for (int i = 0; i < 100; i++) {
            manager.purchaseItem("IPHONE15_256GB", 1000 + i);
        }

        System.out.println(manager.purchaseItem("IPHONE15_256GB", 99999));
        System.out.println(manager.getWaitingList("IPHONE15_256GB"));
    }
}
