package hk.ust.csit5930.search.search;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Unit tests for SimilarityCalculator.
 * Verifies TF-IDF calculation and cosine similarity computation
 * against the assignment-specified formulas.
 */
public class SimilarityCalculatorTest {

    private SimilarityCalculator calculator = new SimilarityCalculator();

    @Test
    public void testTfIdfCalculation() {
        // TF-IDF = (tf * idf) / max(tf)
        // idf = ln(N / df)
        // With tf=3, maxTf=5, N=100, df=10:
        // idf = ln(100/10) = ln(10) ≈ 2.3026
        // TF-IDF = (3 * 2.3026) / 5 ≈ 1.3816
        double tfidf = calculator.calculateTfIdf(3, 5, 100, 10);
        assertEquals(1.3816, tfidf, 0.001);
    }

    @Test
    public void testTfIdfZeroMaxTf() {
        double tfidf = calculator.calculateTfIdf(3, 0, 100, 10);
        assertEquals(0.0, tfidf, 0.0001);
    }

    @Test
    public void testTfIdfZeroDf() {
        double tfidf = calculator.calculateTfIdf(3, 5, 100, 0);
        assertEquals(0.0, tfidf, 0.0001);
    }

    @Test
    public void testCosineSimilarityIdentical() throws Exception {
        // Identical vectors should have similarity of 1.0
        Map<String, Double> vec = new HashMap<>();
        vec.put("term1", 1.0);
        vec.put("term2", 2.0);
        vec.put("term3", 3.0);

        double sim = calculator.calculateCosineSimilarity(vec, vec);
        assertEquals(1.0, sim, 0.0001);
    }

    @Test
    public void testCosineSimilarityOrthogonal() throws Exception {
        // Orthogonal vectors (no common terms) should have similarity of 0
        Map<String, Double> vec1 = new HashMap<>();
        vec1.put("term1", 1.0);

        Map<String, Double> vec2 = new HashMap<>();
        vec2.put("term2", 1.0);

        double sim = calculator.calculateCosineSimilarity(vec1, vec2);
        assertEquals(0.0, sim, 0.0001);
    }

    @Test
    public void testCosineSimilarityEmptyVector() throws Exception {
        Map<String, Double> vec1 = new HashMap<>();
        Map<String, Double> vec2 = new HashMap<>();
        vec2.put("term1", 1.0);

        double sim = calculator.calculateCosineSimilarity(vec1, vec2);
        assertEquals(0.0, sim, 0.0001);
    }

    @Test
    public void testCosineSimilarityPartialOverlap() throws Exception {
        Map<String, Double> query = new HashMap<>();
        query.put("term1", 1.0);
        query.put("term2", 1.0);

        Map<String, Double> doc = new HashMap<>();
        doc.put("term1", 1.0);
        doc.put("term3", 1.0);

        // dot = 1*1 = 1
        // |q| = sqrt(1+1) = sqrt(2)
        // |d| = sqrt(1+1) = sqrt(2)
        // cos = 1 / (sqrt(2) * sqrt(2)) = 1/2 = 0.5
        double sim = calculator.calculateCosineSimilarity(query, doc);
        assertEquals(0.5, sim, 0.0001);
    }
}
