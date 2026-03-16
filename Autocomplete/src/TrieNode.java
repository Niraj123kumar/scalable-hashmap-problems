import java.util.*;

class TrieNode {
    Map<Character, TrieNode> children = new HashMap<>();
    boolean isEnd;
    String word;
    int frequency;
}

public class AutocompleteSystem {

    private final TrieNode root = new TrieNode();

    public void insert(String query, int freq) {
        TrieNode node = root;
        for (char c : query.toCharArray()) {
            node = node.children.computeIfAbsent(c, k -> new TrieNode());
        }
        node.isEnd = true;
        node.word = query;
        node.frequency += freq;
    }

    public void updateFrequency(String query) {
        insert(query, 1);
    }

    public List<String> search(String prefix) {
        TrieNode node = root;
        for (char c : prefix.toCharArray()) {
            node = node.children.get(c);
            if (node == null) return Collections.emptyList();
        }
        PriorityQueue<TrieNode> heap = new PriorityQueue<>(
                (a, b) -> a.frequency == b.frequency ? b.word.compareTo(a.word) : a.frequency - b.frequency);
        dfs(node, heap);
        List<String> result = new ArrayList<>();
        List<TrieNode> temp = new ArrayList<>();
        while (!heap.isEmpty()) temp.add(heap.poll());
        Collections.reverse(temp);
        for (TrieNode n : temp) result.add(n.word + " (" + n.frequency + " searches)");
        return result;
    }

    private void dfs(TrieNode node, PriorityQueue<TrieNode> heap) {
        if (node.isEnd) {
            heap.offer(node);
            if (heap.size() > 10) heap.poll();
        }
        for (TrieNode child : node.children.values()) {
            dfs(child, heap);
        }
    }

    public static void main(String[] args) {
        AutocompleteSystem ac = new AutocompleteSystem();
        ac.insert("java tutorial", 1234567);
        ac.insert("javascript", 987654);
        ac.insert("java download", 456789);
        ac.insert("java 21 features", 1);

        System.out.println(ac.search("jav"));

        ac.updateFrequency("java 21 features");
        ac.updateFrequency("java 21 features");

        System.out.println(ac.search("java 21"));
    }
}
