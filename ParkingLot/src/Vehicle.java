import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

class Vehicle {
    String licensePlate;
    LocalDateTime entryTime;

    Vehicle(String licensePlate) {
        this.licensePlate = licensePlate;
        this.entryTime = LocalDateTime.now();
    }
}

class Spot {
    Vehicle vehicle;
    boolean deleted;

    Spot() {
        this.vehicle = null;
        this.deleted = false;
    }

    boolean isEmpty() {
        return vehicle == null && !deleted;
    }

    boolean isOccupied() {
        return vehicle != null;
    }
}

public class ParkingLot {

    private final int capacity;
    private final Spot[] spots;
    private int totalProbes = 0;
    private int totalParked = 0;
    private Map<Integer, Integer> occupancyByHour = new HashMap<>();

    public ParkingLot(int capacity) {
        this.capacity = capacity;
        this.spots = new Spot[capacity];
        for (int i = 0; i < capacity; i++) spots[i] = new Spot();
    }

    private int hash(String licensePlate) {
        return Math.abs(licensePlate.hashCode()) % capacity;
    }

    public int parkVehicle(String licensePlate) {
        int preferred = hash(licensePlate);
        int probes = 0;
        for (int i = 0; i < capacity; i++) {
            int spotIndex = (preferred + i) % capacity;
            Spot spot = spots[spotIndex];
            if (spot.isEmpty() || spot.deleted) {
                spot.vehicle = new Vehicle(licensePlate);
                spot.deleted = false;
                totalProbes += probes;
                totalParked++;
                int hour = spot.vehicle.entryTime.getHour();
                occupancyByHour.put(hour, occupancyByHour.getOrDefault(hour, 0) + 1);
                System.out.printf("Vehicle %s assigned spot #%d (%d probes)\n", licensePlate, spotIndex, probes);
                return spotIndex;
            }
            probes++;
        }
        System.out.println("Parking Lot Full");
        return -1;
    }

    public double exitVehicle(String licensePlate) {
        int preferred = hash(licensePlate);
        for (int i = 0; i < capacity; i++) {
            int spotIndex = (preferred + i) % capacity;
            Spot spot = spots[spotIndex];
            if (spot.isOccupied() && spot.vehicle.licensePlate.equals(licensePlate)) {
                Vehicle v = spot.vehicle;
                spot.vehicle = null;
                spot.deleted = true;
                Duration duration = Duration.between(v.entryTime, LocalDateTime.now());
                double hours = duration.toMinutes() / 60.0;
                double fee = Math.round(hours * 5 * 100.0) / 100.0; // $5 per hour
                totalParked--;
                System.out.printf("Vehicle %s exited from spot #%d, Duration: %.2fh, Fee: $%.2f\n",
                        licensePlate, spotIndex, hours, fee);
                return fee;
            }
        }
        System.out.println("Vehicle not found");
        return 0;
    }

    public void getStatistics() {
        double occupancy = (totalParked * 100.0) / capacity;
        double avgProbes = totalParked == 0 ? 0 : totalProbes * 1.0 / totalParked;
        int peakHour = occupancyByHour.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey).orElse(-1);

        System.out.printf("Occupancy: %.1f%%, Avg Probes: %.2f, Peak Hour: %d\n", occupancy, avgProbes, peakHour);
    }

    public static void main(String[] args) throws InterruptedException {
        ParkingLot lot = new ParkingLot(500);
        lot.parkVehicle("ABC-1234");
        lot.parkVehicle("ABC-1235");
        lot.parkVehicle("XYZ-9999");

        Thread.sleep(2000);
        lot.exitVehicle("ABC-1234");
        lot.getStatistics();
    }
}
