package hk.ust.csit5930.search.search;

import java.util.Map;

/**
 * Calculates TF-IDF weights and cosine similarity scores.
 * Strictly follows the assignment-specified formulas:
 * - TF-IDF = (tf * idf) / max(tf)
 * - idf = ln(N / df)
 * - Cosine Similarity = dot_product / (|query_vector| * |doc_vector|)
 */
public class SimilarityCalculator {

    /**
     * Calculates the TF-IDF weight for a term in a document.
     * Formula: TF-IDF = (tf * idf) / max(tf)
     *
     * @param tf        the term frequency in the document
     * @param maxTf     the maximum term frequency in the document (for normalization)
     * @param totalDocs the total number of documents in the collection (N)
     * @param df        the document frequency (number of docs containing this term)
     * @return the TF-IDF weight
     */
    public double calculateTfIdf(int tf, int maxTf, int totalDocs, int df) {
        if (maxTf == 0 || df == 0) {
            return 0.0;
        }
        // idf = ln(N / df) as specified by assignment
        double idf = Math.log((double) totalDocs / df);
        // TF-IDF = (tf * idf) / max(tf)
        return (tf * idf) / maxTf;
    }

    /**
     * Calculates the cosine similarity between a query vector and a document vector.
     * Formula: cosine_similarity = dot_product / (|query| * |doc|)
     *
     * @param queryVector the query term weight vector (stem -> TF-IDF weight)
     * @param docVector   the document term weight vector (stem -> TF-IDF weight)
     * @return the cosine similarity score, or 0.0 if either vector has zero magnitude
     * @throws Exception if calculation fails
     */
    public double calculateCosineSimilarity(Map<String, Double> queryVector,
                                            Map<String, Double> docVector)
            throws Exception {
        // Calculate dot product: sum of (queryWeight * docWeight) for matching terms
        double dotProduct = 0.0;
        for (Map.Entry<String, Double> entry : queryVector.entrySet()) {
            String term = entry.getKey();
            double queryWeight = entry.getValue();
            Double docWeight = docVector.get(term);
            if (docWeight != null) {
                dotProduct += queryWeight * docWeight;
            }
        }

        // Calculate vector magnitudes (L2 norms)
        double queryMagnitude = calculateMagnitude(queryVector);
        double docMagnitude = calculateMagnitude(docVector);

        // Avoid division by zero
        if (queryMagnitude == 0.0 || docMagnitude == 0.0) {
            return 0.0;
        }

        return dotProduct / (queryMagnitude * docMagnitude);
    }

    /**
     * Calculates the magnitude (L2 norm) of a weight vector.
     * magnitude = sqrt(sum(weight^2))
     *
     * @param vector the weight vector
     * @return the magnitude
     */
    private double calculateMagnitude(Map<String, Double> vector) {
        double sumSquares = 0.0;
        for (double weight : vector.values()) {
            sumSquares += weight * weight;
        }
        return Math.sqrt(sumSquares);
    }
}
