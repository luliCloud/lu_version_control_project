package VCS;

import java.io.File;
import static VCS.Utils.*;

/** Represents a gitlet repository.
 *  does at a high level.
 *
 *  @author
 */
public class Repository {
    /**
     * List all instance variables of the Repository class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided two examples for you.
     */

    /** The current working directory. */
    public static final File CWD = new File(System.getProperty("user.dir"));
    /** The .gitlet directory. */
    public static final File GITLET_DIR = join(CWD, ".gitlet");

    /** The staging directory for add */
    public static final File STAGING_DIR = join(GITLET_DIR, "stagingAdd");
    /**  The staging dir for remove */
    public static final File STAGING_RM_DIR = join(GITLET_DIR, "stagingRemove");
    /** The commit directory */
    public static final File COMMIT_DIR = join(GITLET_DIR, "commit");
    /** The blobs directory */
    public static final File BLOB_DIR = join(GITLET_DIR, "blobs");

    /** The head and master pointer information */
    public static final File HEAD_MASTER = join(GITLET_DIR, "headAndMaster");
}
