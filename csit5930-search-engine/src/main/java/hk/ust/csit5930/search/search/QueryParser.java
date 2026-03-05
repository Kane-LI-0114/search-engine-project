package hk.ust.csit5930.search.search;

import hk.ust.csit5930.search.indexer.TextPreprocessor;
import hk.ust.csit5930.search.search.model.PhraseTerm;
import hk.ust.csit5930.search.search.model.Query;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses user search queries into structured Query objects.
 * Identifies phrase terms (enclosed in double quotes) and individual terms.
 * Applies the same text preprocessing pipeline (stopword removal, Porter stemming)
 * as the indexer to ensure consistent matching.
 */
public class QueryParser {

    private TextPreprocessor preprocessor;

    /** Regex pattern to match double-quoted phrases */
    private static final Pattern PHRASE_PATTERN = Pattern.compile("\"([^\"]+)\"");

    /**
     * Constructs a QueryParser with a text preprocessor.
     *
     * @throws Exception if preprocessor initialization fails
     */
    public QueryParser() throws Exception {
        this.preprocessor = new TextPreprocessor();
    }

    /**
     * Parses a query string into a structured Query object.
     * Extracts quoted phrases first, then processes remaining individual terms.
     * All text goes through the same preprocessing pipeline as indexing.
     *
     * @param queryStr the raw query string from the user
     * @return a parsed Query containing terms and phrase terms
     * @throws Exception if parsing fails
     */
    public Query parse(String queryStr) throws Exception {
        if (queryStr == null || queryStr.trim().isEmpty()) {
            return new Query(new ArrayList<>(), new ArrayList<>());
        }

        List<PhraseTerm> phrases = new ArrayList<>();
        List<String> terms = new ArrayList<>();

        // Step 1: Extract double-quoted phrases
        Matcher matcher = PHRASE_PATTERN.matcher(queryStr);
        StringBuffer remaining = new StringBuffer();

        while (matcher.find()) {
            String phraseText = matcher.group(1).trim();
            if (!phraseText.isEmpty()) {
                // Process phrase through same pipeline as indexer
                List<TextPreprocessor.TokenInfo> phraseTokens =
                    preprocessor.processText(phraseText);
                if (!phraseTokens.isEmpty()) {
                    List<String> stems = new ArrayList<>();
                    for (TextPreprocessor.TokenInfo token : phraseTokens) {
                        stems.add(token.getStem());
                    }
                    phrases.add(new PhraseTerm(phraseText, stems));
                }
            }
            matcher.appendReplacement(remaining, " ");
        }
        matcher.appendTail(remaining);

        // Step 2: Process remaining non-phrase terms
        String remainingText = remaining.toString().trim();
        if (!remainingText.isEmpty()) {
            List<TextPreprocessor.TokenInfo> tokens =
                preprocessor.processText(remainingText);
            for (TextPreprocessor.TokenInfo token : tokens) {
                terms.add(token.getStem());
            }
        }

        return new Query(terms, phrases);
    }
}
