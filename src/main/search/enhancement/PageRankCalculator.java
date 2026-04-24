package search.enhancement;

import search.common.Config;
import search.common.ExceptionHandler;
import search.common.JDBMManager;

import jdbm.helper.FastIterator;
import java.io.IOException;
import java.util.*;

/**
 * Enhancement 6: Computes PageRank for all crawled pages.
 * Standard iterative algorithm:
 *   PR(p) = (1 - d) / N  +  d * sum_over_inbound( PR(q) / outdegree(q) )
 * where d = PAGERANK_DAMPING (0.85), N = total pages.
 *
 * Called once at the end of Spider.startCrawl().
 * Writes normalised scores [0,1] to the pageRankScores HTree.
 */
public class PageRankCalculator {

    private JDBMManager dbManager;

    public PageRankCalculator() throws IOException {
        this.dbManager = JDBMManager.getInstance();
    }

    /**
     * Runs PageRank over all pages in the JDBM link graph.
     *
     * @param totalPages total number of crawled pages
     * @throws Exception if any DB operation fails
     */
    public void calculate(int totalPages) throws Exception {
        if (totalPages == 0) {
            ExceptionHandler.warn("PageRank: no pages to process.");
            return;
        }
        ExceptionHandler.info("PageRank: starting calculation for " + totalPages + " pages.");

        double d = Config.PAGERANK_DAMPING;
        double basePr = (1.0 - d) / totalPages;

        // Build adjacency maps from JDBM childLinks HTree
        Map<Integer, List<Integer>> outLinks = new HashMap<Integer, List<Integer>>();
        Map<Integer, List<Integer>> inLinks  = new HashMap<Integer, List<Integer>>();

        for (int i = 0; i < totalPages; i++) {
            outLinks.put(i, new ArrayList<Integer>());
            inLinks.put(i,  new ArrayList<Integer>());
        }

        FastIterator iter = dbManager.getChildLinks().keys();
        Object keyObj;
        while ((keyObj = iter.next()) != null) {
            Integer parentId = (Integer) keyObj;
            @SuppressWarnings("unchecked")
            ArrayList<Integer> children =
                (ArrayList<Integer>) dbManager.getChildLinks().get(parentId);
            if (children == null) continue;
            outLinks.put(parentId, children);
            for (Integer childId : children) {
                if (!inLinks.containsKey(childId)) {
                    inLinks.put(childId, new ArrayList<Integer>());
                }
                inLinks.get(childId).add(parentId);
            }
        }

        // Initialise scores uniformly
        Map<Integer, Double> scores = new HashMap<Integer, Double>();
        for (int i = 0; i < totalPages; i++) {
            scores.put(i, 1.0 / totalPages);
        }

        // Iterative update
        for (int iteration = 0; iteration < Config.PAGERANK_ITERATIONS; iteration++) {
            Map<Integer, Double> newScores = new HashMap<Integer, Double>();
            for (int pageId = 0; pageId < totalPages; pageId++) {
                double sum = 0.0;
                List<Integer> parents = inLinks.get(pageId);
                if (parents != null) {
                    for (Integer parentId : parents) {
                        List<Integer> parentOut = outLinks.get(parentId);
                        int outdegree = (parentOut != null && !parentOut.isEmpty())
                            ? parentOut.size() : 1;
                        Double parentScore = scores.get(parentId);
                        sum += (parentScore != null ? parentScore : 0.0) / outdegree;
                    }
                }
                newScores.put(pageId, basePr + d * sum);
            }
            scores = newScores;
        }

        // Normalise to [0,1] and persist
        double maxScore = 0.0;
        for (Double s : scores.values()) {
            if (s > maxScore) maxScore = s;
        }
        if (maxScore == 0.0) maxScore = 1.0;

        for (Map.Entry<Integer, Double> entry : scores.entrySet()) {
            dbManager.getPageRankScores().put(entry.getKey(), entry.getValue() / maxScore);
        }

        dbManager.commit();
        ExceptionHandler.info("PageRank: calculation complete. Max raw score: " + maxScore);
    }
}
