package search.indexer;

import search.common.Config;

import java.io.*;
import java.util.*;

/**
 * Handles text preprocessing for indexing and query processing.
 * Pipeline: tokenize -> lowercase -> stopword removal -> Porter stemming -> record positions.
 * Uses the course-provided stopwords list and Porter stemming algorithm (Lab3).
 */
public class TextPreprocessor {

    /**
     * Represents a processed token with its stem and position in the original text.
     * Position information is essential for phrase search support.
     */
    public static class TokenInfo {

        private String stem;
        private int position;

        /**
         * Constructs a TokenInfo.
         *
         * @param stem     the stemmed form of the token
         * @param position the position index in the original text
         */
        public TokenInfo(String stem, int position) {
            this.stem = stem;
            this.position = position;
        }

        public String getStem() { return stem; }
        public int getPosition() { return position; }

        @Override
        public String toString() {
            return "TokenInfo{stem='" + stem + "', pos=" + position + "}";
        }
    }

    /** Set of stopwords loaded from the course-provided file */
    private Set<String> stopwords;

    /** Porter stemmer instance */
    private PorterStemmer stemmer;

    /**
     * Constructs a TextPreprocessor and loads the stopwords list.
     *
     * @throws IOException if the stopwords file cannot be read
     */
    public TextPreprocessor() throws IOException {
        this.stopwords = loadStopwords();
        this.stemmer = new PorterStemmer();
    }

    /**
     * Processes input text through the full preprocessing pipeline:
     * 1. Convert to lowercase and tokenize (split by non-alphabetic characters)
     * 2. Remove stopwords using the course-provided stopwords list
     * 3. Apply Porter stemming algorithm
     * 4. Record position information for phrase search support
     *
     * @param text the raw text to process
     * @return list of TokenInfo objects with stem and position data
     */
    public List<TokenInfo> processText(String text) {
        List<TokenInfo> result = new ArrayList<>();
        if (text == null || text.trim().isEmpty()) {
            return result;
        }

        // Step 1: Convert to lowercase and split into tokens
        String lowerText = text.toLowerCase();
        String[] tokens = lowerText.split("[^a-z]+");

        int position = 0;
        for (String token : tokens) {
            if (token.isEmpty()) {
                continue;
            }

            // Step 2: Filter stopwords
            if (stopwords.contains(token)) {
                position++;
                continue;
            }

            // Step 3: Apply Porter stemming
            String stem = stemmer.stem(token);
            if (stem != null && !stem.isEmpty()) {
                // Step 4: Record stem and position
                result.add(new TokenInfo(stem, position));
            }
            position++;
        }

        return result;
    }

    /**
     * Computes term frequencies from a list of processed tokens.
     *
     * @param tokens the processed tokens
     * @return map of stem to frequency count
     */
    public static Map<String, Integer> computeTermFrequencies(List<TokenInfo> tokens) {
        Map<String, Integer> frequencies = new HashMap<>();
        for (TokenInfo token : tokens) {
            frequencies.merge(token.getStem(), 1, Integer::sum);
        }
        return frequencies;
    }

    /**
     * Computes the maximum term frequency from a frequency map.
     * Used for TF normalization in the TF-IDF formula: TF-IDF = (tf * idf) / max(tf).
     *
     * @param frequencies the term frequency map
     * @return the maximum frequency value, or 1 if empty
     */
    public static int computeMaxTermFrequency(Map<String, Integer> frequencies) {
        int max = 1;
        for (int freq : frequencies.values()) {
            if (freq > max) {
                max = freq;
            }
        }
        return max;
    }

    /**
     * Groups token positions by stem for phrase search support.
     *
     * @param tokens the processed tokens
     * @return map of stem to list of positions
     */
    public static Map<String, List<Integer>> groupPositionsByStem(List<TokenInfo> tokens) {
        Map<String, List<Integer>> positionMap = new HashMap<>();
        for (TokenInfo token : tokens) {
            positionMap.computeIfAbsent(token.getStem(), k -> new ArrayList<>())
                       .add(token.getPosition());
        }
        return positionMap;
    }

    /**
     * Loads the stopwords file from the configured path.
     * Tries the file system path first, then the classpath as fallback.
     *
     * @return set of stopwords (all lowercase)
     * @throws IOException if file read fails from all locations
     */
    private Set<String> loadStopwords() throws IOException {
        Set<String> words = new HashSet<>();

        // Try file system path first
        File file = new File(Config.STOPWORDS_PATH);
        if (file.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim().toLowerCase();
                    if (!line.isEmpty()) {
                        words.add(line);
                    }
                }
            }
            return words;
        }

        // Fallback: try classpath
        InputStream is = getClass().getClassLoader().getResourceAsStream("stopwords.txt");
        if (is != null) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim().toLowerCase();
                    if (!line.isEmpty()) {
                        words.add(line);
                    }
                }
            }
            return words;
        }

        throw new IOException("Stopwords file not found at: " + Config.STOPWORDS_PATH +
                              " or on classpath");
    }
}
