import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

class PageViewEvent {
    String url;
    String userId;
    String source;

    PageViewEvent(String url, String userId, String source) {
        this.url = url;
        this.userId = userId;
        this.source = source;
    }
}

public class RealTimeAnalyticsDashboard {

    private final ConcurrentHashMap<String, AtomicInteger> pageViews = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Set<String>> uniqueVisitors = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicInteger> trafficSources = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public RealTimeAnalyticsDashboard() {
        scheduler.scheduleAtFixedRate(this::updateDashboard, 5, 5, TimeUnit.SECONDS);
    }

    public void processEvent(PageViewEvent event) {
        pageViews.computeIfAbsent(event.url, k -> new AtomicInteger(0)).incrementAndGet();

        uniqueVisitors.computeIfAbsent(event.url, k -> ConcurrentHashMap.newKeySet()).add(event.userId);

        trafficSources.computeIfAbsent(event.source, k -> new AtomicInteger(0)).incrementAndGet();
    }

    private void updateDashboard() {
        System.out.println("=== Dashboard Update ===");

        List<Map.Entry<String, AtomicInteger>> topPages = pageViews.entrySet()
                .stream()
                .sorted((a, b) -> b.getValue().get() - a.getValue().get())
                .limit(10)
                .collect(Collectors.toList());

        System.out.println("Top Pages:");
        int rank = 1;
        for (Map.Entry<String, AtomicInteger> entry : topPages) {
            String url = entry.getKey();
            int views = entry.getValue().get();
            int unique = uniqueVisitors.get(url).size();
            System.out.printf("%d. %s - %d views (%d unique)\n", rank++, url, views, unique);
        }

        System.out.println("Traffic Sources:");
        trafficSources.forEach((source, count) -> System.out.printf("%s: %d visits\n", source, count.get()));

        System.out.println("========================\n");
    }

    public static void main(String[] args) throws InterruptedException {
        RealTimeAnalyticsDashboard dashboard = new RealTimeAnalyticsDashboard();

        dashboard.processEvent(new PageViewEvent("/article/breaking-news", "user_123", "google"));
        dashboard.processEvent(new PageViewEvent("/article/breaking-news", "user_456", "facebook"));
        dashboard.processEvent(new PageViewEvent("/sports/championship", "user_789", "direct"));

        // Simulate continuous traffic
        for (int i = 0; i < 100; i++) {
            dashboard.processEvent(new PageViewEvent("/article/breaking-news", "user_" + i, "google"));
        }

        Thread.sleep(6000); // wait for first dashboard update
        dashboard.scheduler.shutdown();
    }
}
