package hk.ust.csit5930.search.common;

import jdbm.RecordManager;
import jdbm.RecordManagerFactory;
import jdbm.htree.HTree;

import java.io.IOException;

/**
 * Singleton manager for JDBM persistent storage.
 * Manages all HTree instances used across the search engine modules.
 * All core data is persisted to disk via JDBM; no in-memory data structures
 * are used for primary storage (assignment compliance).
 */
public class JDBMManager {

    private static JDBMManager instance;
    private RecordManager recman;

    // Crawler module HTrees
    private HTree url2id;       // String URL -> Integer PageID
    private HTree id2url;       // Integer PageID -> String URL
    private HTree parentLinks;  // Integer childPageId -> ArrayList<Integer> parentPageIds
    private HTree childLinks;   // Integer parentPageId -> ArrayList<Integer> childPageIds

    // Indexer module HTrees
    private HTree titleIndex;   // String stem -> PostingList (title inverted index)
    private HTree bodyIndex;    // String stem -> PostingList (body inverted index)
    private HTree pageMetadata; // Integer pageId -> PageMetadata

    // System configuration HTree
    private HTree systemConfig; // String key -> String value (counters, metadata)

    /**
     * Private constructor. Initializes JDBM RecordManager and loads or creates all HTrees.
     *
     * @throws IOException if database initialization fails
     */
    private JDBMManager() throws IOException {
        recman = RecordManagerFactory.createRecordManager(Config.DB_PATH);
        url2id = loadOrCreateHTree("url2id");
        id2url = loadOrCreateHTree("id2url");
        parentLinks = loadOrCreateHTree("parentLinks");
        childLinks = loadOrCreateHTree("childLinks");
        titleIndex = loadOrCreateHTree("titleIndex");
        bodyIndex = loadOrCreateHTree("bodyIndex");
        pageMetadata = loadOrCreateHTree("pageMetadata");
        systemConfig = loadOrCreateHTree("systemConfig");
    }

    /**
     * Returns the singleton instance of JDBMManager.
     *
     * @return the JDBMManager singleton
     * @throws IOException if initialization fails
     */
    public static synchronized JDBMManager getInstance() throws IOException {
        if (instance == null) {
            instance = new JDBMManager();
        }
        return instance;
    }

    /**
     * Loads an existing HTree by name, or creates a new one if it does not exist.
     *
     * @param name the named object identifier for the HTree
     * @return the loaded or newly created HTree
     * @throws IOException if database operation fails
     */
    private HTree loadOrCreateHTree(String name) throws IOException {
        long recid = recman.getNamedObject(name);
        if (recid != 0) {
            return HTree.load(recman, recid);
        } else {
            HTree htree = HTree.createInstance(recman);
            recman.setNamedObject(name, htree.getRecid());
            return htree;
        }
    }

    // === Getter methods for all HTrees ===

    /** Returns the URL to PageID mapping HTree. */
    public HTree getUrl2Id() { return url2id; }

    /** Returns the PageID to URL mapping HTree. */
    public HTree getId2Url() { return id2url; }

    /** Returns the child-to-parent link HTree. */
    public HTree getParentLinks() { return parentLinks; }

    /** Returns the parent-to-child link HTree. */
    public HTree getChildLinks() { return childLinks; }

    /** Returns the title inverted index HTree. */
    public HTree getTitleIndex() { return titleIndex; }

    /** Returns the body inverted index HTree. */
    public HTree getBodyIndex() { return bodyIndex; }

    /** Returns the page metadata HTree. */
    public HTree getPageMetadata() { return pageMetadata; }

    /** Returns the system configuration HTree. */
    public HTree getSystemConfig() { return systemConfig; }

    /**
     * Commits all pending changes to disk.
     *
     * @throws IOException if commit fails
     */
    public void commit() throws IOException {
        recman.commit();
    }

    /**
     * Closes the JDBM RecordManager and releases all resources.
     *
     * @throws IOException if close fails
     */
    public void close() throws IOException {
        if (recman != null) {
            recman.close();
            recman = null;
            instance = null;
        }
    }

    /**
     * Returns the underlying RecordManager for advanced operations.
     *
     * @return the RecordManager instance
     */
    public RecordManager getRecordManager() {
        return recman;
    }

    /**
     * Resets the singleton instance. Used for testing and re-initialization.
     */
    public static synchronized void reset() {
        instance = null;
    }
}
