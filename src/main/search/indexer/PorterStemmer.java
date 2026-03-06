package search.indexer;

/**
 * Implementation of the Porter Stemming Algorithm for English text.
 * This is the standard algorithm as described by Martin Porter, used for
 * reducing words to their root/stem form for indexing and search.
 *
 * Reference: Porter, M.F., "An algorithm for suffix stripping",
 * Program 14(3), pp 130-137, July 1980.
 *
 * Provided as part of the CSIT5930 Lab3 materials.
 */
public class PorterStemmer {

    /** Word buffer */
    private char[] b;

    /** Offset into b */
    private int i;

    /** Offset to end of stemmed word */
    private int j;

    /** Working offset */
    private int k;

    /**
     * Stems a word using the Porter algorithm.
     *
     * @param word the word to stem (should be lowercase)
     * @return the stemmed form of the word
     */
    public String stem(String word) {
        if (word == null || word.length() < 3) {
            return word;
        }

        b = word.toCharArray();
        i = word.length();
        k = i - 1;

        step1();
        step2();
        step3();
        step4();
        step5();

        return new String(b, 0, k + 1);
    }

    /**
     * Returns true if b[idx] is a consonant.
     * A consonant is any letter that is not a, e, i, o, u.
     * 'y' is a consonant when preceded by a vowel.
     */
    private boolean cons(int idx) {
        switch (b[idx]) {
            case 'a':
            case 'e':
            case 'i':
            case 'o':
            case 'u':
                return false;
            case 'y':
                return (idx == 0) ? true : !cons(idx - 1);
            default:
                return true;
        }
    }

    /**
     * Measures the number of consonant sequences between 0 and j.
     * If c is a consonant sequence and v a vowel sequence, then:
     * <c><v>       gives 0
     * <c>vc<v>     gives 1
     * <c>vcvc<v>   gives 2
     * <c>vcvcvc<v> gives 3
     * etc.
     */
    private int m() {
        int n = 0;
        int idx = 0;

        while (true) {
            if (idx > j) return n;
            if (!cons(idx)) break;
            idx++;
        }
        idx++;
        while (true) {
            while (true) {
                if (idx > j) return n;
                if (cons(idx)) break;
                idx++;
            }
            idx++;
            n++;
            while (true) {
                if (idx > j) return n;
                if (!cons(idx)) break;
                idx++;
            }
            idx++;
        }
    }

    /**
     * Returns true if b[0..j] contains a vowel.
     */
    private boolean vowelinstem() {
        for (int idx = 0; idx <= j; idx++) {
            if (!cons(idx)) return true;
        }
        return false;
    }

    /**
     * Returns true if b[idx] and b[idx-1] are the same consonant.
     */
    private boolean doublec(int idx) {
        if (idx < 1) return false;
        if (b[idx] != b[idx - 1]) return false;
        return cons(idx);
    }

    /**
     * Returns true if i-2, i-1, i has the form consonant-vowel-consonant
     * and also if the second consonant is not w, x or y.
     * This is used when trying to restore an e at the end of a short word.
     * e.g. cav(e), lov(e), hop(e), crim(e), but snow, box, tray.
     */
    private boolean cvc(int idx) {
        if (idx < 2 || !cons(idx) || cons(idx - 1) || !cons(idx - 2)) return false;
        int ch = b[idx];
        return ch != 'w' && ch != 'x' && ch != 'y';
    }

    /**
     * Returns true if b[0..k] ends with the string s.
     * If so, sets j to the position before the suffix.
     */
    private boolean ends(String s) {
        int l = s.length();
        int o = k - l + 1;
        if (o < 0) return false;
        for (int idx = 0; idx < l; idx++) {
            if (b[o + idx] != s.charAt(idx)) return false;
        }
        j = k - l;
        return true;
    }

    /**
     * Sets b[j+1..] to the characters in s, readjusting k.
     */
    private void setto(String s) {
        int l = s.length();
        int o = j + 1;
        for (int idx = 0; idx < l; idx++) {
            b[o + idx] = s.charAt(idx);
        }
        k = j + l;
    }

    /**
     * Used by steps 2, 3, 4: sets suffix to s if measure of b[0..j] > 0.
     */
    private void r(String s) {
        if (m() > 0) setto(s);
    }

    /**
     * Step 1: Deals with plurals, past participles, and -ed/-ing endings.
     * Step 1a: SSES -> SS, IES -> I, SS -> SS, S -> (delete)
     * Step 1b: (m>0) EED -> EE, (*v*) ED ->, (*v*) ING ->
     * Step 1c: (*v*) Y -> I
     */
    private void step1() {
        // Step 1a
        if (b[k] == 's') {
            if (ends("sses")) {
                k -= 2;
            } else if (ends("ies")) {
                setto("i");
            } else if (b[k - 1] != 's') {
                k--;
            }
        }

        // Step 1b
        if (ends("eed")) {
            if (m() > 0) k--;
        } else if ((ends("ed") || ends("ing")) && vowelinstem()) {
            k = j;
            // Step 1b additional rules
            if (ends("at")) {
                setto("ate");
            } else if (ends("bl")) {
                setto("ble");
            } else if (ends("iz")) {
                setto("ize");
            } else if (doublec(k)) {
                k--;
                int ch = b[k];
                if (ch == 'l' || ch == 's' || ch == 'z') k++;
            } else if (m() == 1 && cvc(k)) {
                setto("e");
            }
        }

        // Step 1c
        if (ends("y") && vowelinstem()) {
            b[k] = 'i';
        }
    }

    /**
     * Step 2: Maps double suffices to single ones.
     * Handles suffixes like -ational -> -ate, -tional -> -tion, etc.
     */
    private void step2() {
        if (k == 0) return;
        switch (b[k - 1]) {
            case 'a':
                if (ends("ational")) { r("ate"); break; }
                if (ends("tional")) { r("tion"); break; }
                break;
            case 'c':
                if (ends("enci")) { r("ence"); break; }
                if (ends("anci")) { r("ance"); break; }
                break;
            case 'e':
                if (ends("izer")) { r("ize"); break; }
                break;
            case 'l':
                if (ends("bli")) { r("ble"); break; }
                if (ends("alli")) { r("al"); break; }
                if (ends("entli")) { r("ent"); break; }
                if (ends("eli")) { r("e"); break; }
                if (ends("ousli")) { r("ous"); break; }
                break;
            case 'o':
                if (ends("ization")) { r("ize"); break; }
                if (ends("ation")) { r("ate"); break; }
                if (ends("ator")) { r("ate"); break; }
                break;
            case 's':
                if (ends("alism")) { r("al"); break; }
                if (ends("iveness")) { r("ive"); break; }
                if (ends("fulness")) { r("ful"); break; }
                if (ends("ousness")) { r("ous"); break; }
                break;
            case 't':
                if (ends("aliti")) { r("al"); break; }
                if (ends("iviti")) { r("ive"); break; }
                if (ends("biliti")) { r("ble"); break; }
                break;
            case 'g':
                if (ends("logi")) { r("log"); break; }
                break;
        }
    }

    /**
     * Step 3: Deals with -ic-, -full, -ness etc. suffixes.
     */
    private void step3() {
        switch (b[k]) {
            case 'e':
                if (ends("icate")) { r("ic"); break; }
                if (ends("ative")) { r(""); break; }
                if (ends("alize")) { r("al"); break; }
                break;
            case 'i':
                if (ends("iciti")) { r("ic"); break; }
                break;
            case 'l':
                if (ends("ical")) { r("ic"); break; }
                if (ends("ful")) { r(""); break; }
                break;
            case 's':
                if (ends("ness")) { r(""); break; }
                break;
        }
    }

    /**
     * Step 4: Takes off -ant, -ence etc., in context <c>vcvc<v>.
     */
    private void step4() {
        if (k == 0) return;
        switch (b[k - 1]) {
            case 'a':
                if (ends("al")) break;
                return;
            case 'c':
                if (ends("ance")) break;
                if (ends("ence")) break;
                return;
            case 'e':
                if (ends("er")) break;
                return;
            case 'i':
                if (ends("ic")) break;
                return;
            case 'l':
                if (ends("able")) break;
                if (ends("ible")) break;
                return;
            case 'n':
                if (ends("ant")) break;
                if (ends("ement")) break;
                if (ends("ment")) break;
                if (ends("ent")) break;
                return;
            case 'o':
                if (ends("ion") && j >= 0 && (b[j] == 's' || b[j] == 't')) break;
                if (ends("ou")) break;
                return;
            case 's':
                if (ends("ism")) break;
                return;
            case 't':
                if (ends("ate")) break;
                if (ends("iti")) break;
                return;
            case 'u':
                if (ends("ous")) break;
                return;
            case 'v':
                if (ends("ive")) break;
                return;
            case 'z':
                if (ends("ize")) break;
                return;
            default:
                return;
        }
        if (m() > 1) k = j;
    }

    /**
     * Step 5: Removes a final -e if m() > 1, and changes -ll to -l if m() > 1.
     */
    private void step5() {
        j = k;

        // Step 5a: remove final 'e' if appropriate
        if (b[k] == 'e') {
            int a = m();
            if (a > 1 || (a == 1 && !cvc(k - 1))) {
                k--;
            }
        }

        // Step 5b: remove double 'l' if m() > 1
        if (b[k] == 'l' && doublec(k) && m() > 1) {
            k--;
        }
    }
}
