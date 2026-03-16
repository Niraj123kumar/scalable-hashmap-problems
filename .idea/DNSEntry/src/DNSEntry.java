import java.util.*;
import java.util.concurrent.*;
import java.net.InetAddress;

class DNSEntry {
    String domain;
    String ipAddress;
    long expiryTime;

    DNSEntry(String domain, String ipAddress, long ttlMillis) {
        this.domain = domain;
        this.ipAddress = ipAddress;
        this.expiryTime = System.currentTimeMillis() + ttlMillis;
    }

    boolean isExpired() {
        return System.currentTimeMillis() > expiryTime;
    }
}

public class DNSCache {
    private final int capacity;
    private final long defaultTTL;
    private final Map<String, DNSEntry> cache;
    private final Deque<String> lruQueue;
    private final ScheduledExecutorService cleaner;
    private long hits = 0;
    private long misses = 0;

    public DNSCache(int capacity, long defaultTTLSeconds) {
        this.capacity = capacity;
        this.defaultTTL = defaultTTLSeconds * 1000;
        this.cache = new ConcurrentHashMap<>();
        this.lruQueue = new ConcurrentLinkedDeque<>();
        this.cleaner = Executors.newSingleThreadScheduledExecutor();
        cleaner.scheduleAtFixedRate(this::cleanExpiredEntries, defaultTTLSeconds, defaultTTLSeconds, TimeUnit.SECONDS);
    }

    public String resolve(String domain) {
        long start = System.nanoTime();
        DNSEntry entry = cache.get(domain);
        if (entry != null && !entry.isExpired()) {
            hits++;
            lruQueue.remove(domain);
            lruQueue.addFirst(domain);
            return entry.ipAddress;
        } else {
            misses++;
            String ip = queryUpstreamDNS(domain);
            put(domain, ip, defaultTTL);
            return ip;
        }
    }

    private void put(String domain, String ipAddress, long ttlMillis) {
        if (cache.size() >= capacity) {
            evictLRU();
        }
        DNSEntry entry = new DNSEntry(domain, ipAddress, ttlMillis);
        cache.put(domain, entry);
        lruQueue.addFirst(domain);
    }

    private void evictLRU() {
        String lruDomain = lruQueue.pollLast();
        if (lruDomain != null) {
            cache.remove(lruDomain);
        }
    }

    private void cleanExpiredEntries() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<String, DNSEntry>> iterator = cache.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, DNSEntry> mapEntry = iterator.next();
            if (mapEntry.getValue().isExpired()) {
                iterator.remove();
                lruQueue.remove(mapEntry.getKey());
            }
        }
    }

    private String queryUpstreamDNS(String domain) {
        try {
            InetAddress addr = InetAddress.getByName(domain);
            return addr.getHostAddress();
        } catch (Exception e) {
            return "0.0.0.0";
        }
    }

    public void getCacheStats() {
        long total = hits + misses;
        double hitRate = total == 0 ? 0 : (hits * 100.0 / total);
        System.out.println("Cache Hits: " + hits + ", Misses: " + misses + ", Hit Rate: " + String.format("%.2f", hitRate) + "%");
    }

    public static void main(String[] args) throws InterruptedException {
        DNSCache dnsCache = new DNSCache(3, 5);

        System.out.println("Resolve google.com → " + dnsCache.resolve("google.com"));
        System.out.println("Resolve google.com → " + dnsCache.resolve("google.com"));
        System.out.println("Resolve example.com → " + dnsCache.resolve("example.com"));
        System.out.println("Resolve openai.com → " + dnsCache.resolve("openai.com"));

        Thread.sleep(6000);

        System.out.println("Resolve google.com after TTL → " + dnsCache.resolve("google.com"));

        dnsCache.getCacheStats();
    }
}
