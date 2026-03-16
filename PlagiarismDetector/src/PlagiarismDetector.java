import java.io.*;
import java.nio.file.*;
import java.util.*;

public class PlagiarismDetector {

    private final int N_GRAM_SIZE;
    private final Map<String, Set<String>> nGramIndex = new HashMap<>();

    public PlagiarismDetector(int nGramSize) {
        this.N_GRAM_SIZE = nGramSize;
    }

    private List<String> extractNGrams(String text) {
        String[] words = text.toLowerCase().replaceAll("[^a-z0-9 ]", "").split("\\s+");
        List<String> ngrams = new ArrayList<>();
        for (int i = 0; i <= words.length - N_GRAM_SIZE; i++) {
            StringBuilder sb = new StringBuilder();
            for (int j = 0; j < N_GRAM_SIZE; j++) {
                sb.append(words[i + j]).append(" ");
            }
            ngrams.add(sb.toString().trim());
        }
        return ngrams;
    }

    public void indexDocument(String docId, String content) {
        List<String> ngrams = extractNGrams(content);
        for (String ngram : ngrams) {
            nGramIndex.computeIfAbsent(ngram, k -> new HashSet<>()).add(docId);
        }
    }

    public Map<String, Double> analyzeDocument(String docId, String content) {
        List<String> ngrams = extractNGrams(content);
        Map<String, Integer> matchCounts = new HashMap<>();
        for (String ngram : ngrams) {
            Set<String> docs = nGramIndex.getOrDefault(ngram, Collections.emptySet());
            for (String otherDoc : docs) {
                if (!otherDoc.equals(docId)) {
                    matchCounts.put(otherDoc, matchCounts.getOrDefault(otherDoc, 0) + 1);
                }
            }
        }
        Map<String, Double> similarity = new HashMap<>();
        for (Map.Entry<String, Integer> entry : matchCounts.entrySet()) {
            double percent = (entry.getValue() * 100.0) / ngrams.size();
            similarity.put(entry.getKey(), percent);
        }
        return similarity;
    }

    public static void main(String[] args) throws IOException {
        PlagiarismDetector detector = new PlagiarismDetector(5);

        Map<String, String> documents = new HashMap<>();
        documents.put("essay_089.txt", Files.readString(Paths.get("essay_089.txt")));
        documents.put("essay_092.txt", Files.readString(Paths.get("essay_092.txt")));
        documents.put("essay_123.txt", Files.readString(Paths.get("essay_123.txt")));

        for (Map.Entry<String, String> entry : documents.entrySet()) {
            detector.indexDocument(entry.getKey(), entry.getValue());
        }

        String testDocId = "essay_123.txt";
        Map<String, Double> results = detector.analyzeDocument(testDocId, documents.get(testDocId));

        System.out.println("Analysis for " + testDocId + ":");
        for (Map.Entry<String, Double> sim : results.entrySet()) {
            System.out.printf("→ Found %.0f matching n-grams with \"%s\" → Similarity: %.1f%%\n",
                    detector.extractNGrams(documents.get(testDocId)).size() * sim.getValue() / 100,
                    sim.getKey(),
                    sim.getValue());
        }
    }
}
