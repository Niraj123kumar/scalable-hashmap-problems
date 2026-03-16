import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

class Transaction {
    int id;
    int amount;
    String merchant;
    String account;
    LocalTime time;

    Transaction(int id, int amount, String merchant, String account, String timeStr) {
        this.id = id;
        this.amount = amount;
        this.merchant = merchant;
        this.account = account;
        this.time = LocalTime.parse(timeStr);
    }
}

public class TransactionAnalyzer {

    private final List<Transaction> transactions;

    public TransactionAnalyzer(List<Transaction> transactions) {
        this.transactions = transactions;
    }

    public List<int[]> findTwoSum(int target) {
        Map<Integer, Transaction> map = new HashMap<>();
        List<int[]> results = new ArrayList<>();
        for (Transaction t : transactions) {
            int complement = target - t.amount;
            if (map.containsKey(complement)) {
                results.add(new int[]{map.get(complement).id, t.id});
            }
            map.put(t.amount, t);
        }
        return results;
    }

    public List<int[]> findTwoSumWithinHour(int target) {
        List<int[]> results = new ArrayList<>();
        transactions.sort(Comparator.comparing(t -> t.time));
        for (int i = 0; i < transactions.size(); i++) {
            for (int j = i + 1; j < transactions.size(); j++) {
                if (transactions.get(j).time.isAfter(transactions.get(i).time.plusHours(1))) break;
                if (transactions.get(i).amount + transactions.get(j).amount == target) {
                    results.add(new int[]{transactions.get(i).id, transactions.get(j).id});
                }
            }
        }
        return results;
    }

    public List<List<Integer>> findKSum(int k, int target) {
        List<List<Integer>> results = new ArrayList<>();
        transactions.sort(Comparator.comparingInt(t -> t.amount));
        kSumHelper(transactions, k, target, 0, new ArrayList<>(), results);
        return results;
    }

    private void kSumHelper(List<Transaction> txs, int k, int target, int index,
                            List<Integer> path, List<List<Integer>> res) {
        if (k == 2) {
            int left = index, right = txs.size() - 1;
            while (left < right) {
                int sum = txs.get(left).amount + txs.get(right).amount;
                if (sum == target) {
                    List<Integer> combo = new ArrayList<>(path);
                    combo.add(txs.get(left).id);
                    combo.add(txs.get(right).id);
                    res.add(combo);
                    left++;
                    right--;
                } else if (sum < target) left++;
                else right--;
            }
        } else {
            for (int i = index; i < txs.size() - k + 1; i++) {
                path.add(txs.get(i).id);
                kSumHelper(txs, k - 1, target - txs.get(i).amount, i + 1, path, res);
                path.remove(path.size() - 1);
            }
        }
    }

    public List<Map<String, Object>> detectDuplicates() {
        Map<String, List<Transaction>> map = new HashMap<>();
        for (Transaction t : transactions) {
            String key = t.amount + "|" + t.merchant;
            map.computeIfAbsent(key, k -> new ArrayList<>()).add(t);
        }
        List<Map<String, Object>> results = new ArrayList<>();
        for (List<Transaction> list : map.values()) {
            Set<String> accounts = list.stream().map(t -> t.account).collect(Collectors.toSet());
            if (accounts.size() > 1) {
                Map<String, Object> dup = new HashMap<>();
                dup.put("amount", list.get(0).amount);
                dup.put("merchant", list.get(0).merchant);
                dup.put("accounts", accounts);
                results.add(dup);
            }
        }
        return results;
    }

    public static void main(String[] args) {
        List<Transaction> txs = Arrays.asList(
                new Transaction(1, 500, "Store A", "acc1", "10:00"),
                new Transaction(2, 300, "Store B", "acc2", "10:15"),
                new Transaction(3, 200, "Store C", "acc3", "10:30"),
                new Transaction(4, 500, "Store A", "acc2", "11:00")
        );

        TransactionAnalyzer analyzer = new TransactionAnalyzer(txs);

        System.out.println("Two-Sum 500: " + analyzer.findTwoSum(500));
        System.out.println("Two-Sum within 1 hour 500: " + analyzer.findTwoSumWithinHour(500));
        System.out.println("K-Sum 3 target 1000: " + analyzer.findKSum(3, 1000));
        System.out.println("Duplicate detection: " + analyzer.detectDuplicates());
    }
}
