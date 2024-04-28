package VCS;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

/** Represents a gitlet commit object.
 *  does at a high level.
 *
 *  @author
 *  Commit class represent one commit in the Gitlet. A commit is a tree node in Gitlet.
 *  Each commit (treeNode) should have a Metadata (include a message and a timetampe),
 *  pointer point to parent and parent2, pointer point to file of this commit.
 */
public class Commit implements Serializable { //
    /** List all instance variables of the Commit class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided one example for `message`.
     */

    /** The message of this Commit. */
    private String message;
    /** The time stamp of this Commit. */
    private Date timeStamp;
    /** Parent commit */
    private String parent;
    /** 2nd parent commit */
    private String parent2;
    /** File pointer to blobs. Key is the file name, value is the path. */
    private HashMap<String, String> files;
    /** Hash code for each commit */
    private String commitID;

    public Commit() {
        this.message = "initial commit";
        this.timeStamp = new Date(0);
        this.files = new HashMap<>();
        this.parent = "";
        this.parent2 = "";
    }

    public Commit(String message) {
        if (message.equals("")) {
            System.out.println("Please enter a commit message.");
            System.exit(0);
        }
        this.message = message;
        this.timeStamp = new Date();

        // Obtain hashMap storing head pointer
        File h = Repository.HEAD_MASTER;
        HashMap<String, String> headMap = Utils.readObject(h, HashMap.class);
        String branch = headMap.get("HEAD");
        this.parent = headMap.get(branch);
        this.parent2 = "";
        // obtain files of parent
        File p = Utils.join(Repository.COMMIT_DIR, headMap.get(branch));
        Commit parentCommit = Utils.readObject(p, Commit.class);
        files = parentCommit.files;

        helperCommit(headMap, branch);
    }

    public Commit(String message, Commit split, Commit curCommit, Commit branchHead, String givenBranch) {
        // Obtain hashMap storing head pointer
        File h = Repository.HEAD_MASTER;
        HashMap<String, String> headMap = Utils.readObject(h, HashMap.class);
        String branch = headMap.get("HEAD");
        HashMap<String, String> splitFiles = split.files;
        HashMap<String, String> curBranchFiles = curCommit.files;
        HashMap<String, String> otherBranchFiles = branchHead.files;
        // handle all failure problem at first
        List<String> workingFiles = Utils.plainFilenamesIn(Repository.CWD);
        for (int i = 0; i < workingFiles.size(); i++) {
            String f = workingFiles.get(i);
            if (!curBranchFiles.containsKey(workingFiles.get(i))) {
                if (otherBranchFiles.containsKey(f)) {
                    System.out.println("There is an untracked file in the way; delete it, "
                            + "or add and commit it first.");
                    System.exit(0);
                } else if (!otherBranchFiles.get(f).equals(curBranchFiles.get(f))) {
                    System.out.println("There is an untracked file in the way; delete it, "
                            + "or add and commit it first.");
                    System.exit(0);
                }
            }
        }

        this.parent = headMap.get(branch);
        this.parent2 = headMap.get(givenBranch);
        this.message = message;
        this.timeStamp = new Date();
        files = curBranchFiles;

        for (String f : splitFiles.keySet()) {
            if (curBranchFiles.containsKey(f) && otherBranchFiles.containsKey(f)) {
                if (curBranchFiles.get(f).equals(splitFiles.get(f))
                        && !otherBranchFiles.get(f).equals(splitFiles.get(f))) { // no conflict
                    // stage previous file for add
                    String blobID = otherBranchFiles.get(f);
                    File sourceFile = Utils.join(Repository.BLOB_DIR, blobID);
                    moveToStage(sourceFile, f, Repository.STAGING_DIR); // the blob will be handel later
                    overWriteInWorkDir(otherBranchFiles, f);
                } else if (!curBranchFiles.get(f).equals(splitFiles.get(f))
                        && !otherBranchFiles.get(f).equals(splitFiles.get(f))
                        && !curBranchFiles.get(f).equals(otherBranchFiles.get(f))) { // conflict
                    String toStage = conflict(f, curBranchFiles, otherBranchFiles);
                }
            } else if (curBranchFiles.containsKey(f) && !otherBranchFiles.containsKey(f)) {
                if (curBranchFiles.get(f).equals(splitFiles.get(f))) {
                    File sourceFile = Utils.join(Repository.CWD, f);
                    moveToStage(sourceFile, f, Repository.STAGING_RM_DIR);
                    Utils.restrictedDelete(sourceFile);
                } else { // handle cur different from split
                    String toStage = conflict(f, curBranchFiles, otherBranchFiles);
                }
            } else if (!curBranchFiles.containsKey(f) && otherBranchFiles.containsKey(f)) {
                if (!otherBranchFiles.get(f).equals(splitFiles.get(f))) {
                    String toStage = conflict(f, curBranchFiles, otherBranchFiles);
                }
            }
        }
        for (String fileName : otherBranchFiles.keySet()) {
            if (!splitFiles.containsKey(fileName) && !curBranchFiles.containsKey(fileName)) {
                overWriteInWorkDir(otherBranchFiles, fileName);
                File sourceFile = Utils.join(Repository.CWD, fileName);
                moveToStage(sourceFile, fileName, Repository.STAGING_DIR);
                files.put(fileName, otherBranchFiles.get(fileName)); // track it
            } else if (!splitFiles.containsKey(fileName)
                    && !otherBranchFiles.get(fileName).equals(curBranchFiles.get(fileName))) {
                String toStage = conflict(fileName, curBranchFiles, otherBranchFiles);
            }
        }
        helperCommit(headMap, branch);
    }

    public String conflict(String fileName, HashMap<String, String> curBranch, HashMap<String, String> givenBranch) {
        System.out.println("Encountered a merge conflict.");
        String toMerge = "";
        toMerge = toMerge + "<<<<<<< HEAD\n";
        if (curBranch.containsKey(fileName)) {
            String fileID = curBranch.get(fileName);
            File restoreFile = Utils.join(Repository.BLOB_DIR, fileID);
            String curContent = Utils.readContentsAsString(restoreFile);
            toMerge = toMerge + curContent;
        }
        toMerge += "=======\n";
        if (givenBranch.containsKey(fileName)) {
            String fileID = givenBranch.get(fileName);
            File restoreFile = Utils.join(Repository.BLOB_DIR, fileID);
            String mergeFile = Utils.readContentsAsString(restoreFile);
            toMerge = toMerge + mergeFile;
        }
        toMerge = toMerge + ">>>>>>>\n";
        Utils.writeContents(Utils.join(Repository.CWD, fileName), toMerge);
        Utils.writeContents(Utils.join(Repository.STAGING_DIR, fileName), toMerge); // Move to addStage
        return toMerge;
    }
    /** Handle stage files, generate blob ID and write content to blob files, stored in Blob folder
     * blob content is the same as original file, no SHA-id or serialization */
    public void helperCommit(HashMap<String, String> headMap, String branch) {
        // get all plain files in staging folder
        List<String> stagedFiles = Utils.plainFilenamesIn(Repository.STAGING_DIR);
        List<String> stagedRemove = Utils.plainFilenamesIn(Repository.STAGING_RM_DIR);
        if (stagedFiles.isEmpty() && stagedRemove.isEmpty()) { // if no file staged
            System.out.println("No changes added to the commit.");
            System.exit(0);
        }

        // handle staegAdd and stageRemove folder
        moveFilesSTOB(stagedFiles, Repository.BLOB_DIR, files); // generate blob content and stored
        // untrack the files in parent files but staging for remove now.
        for (int i = 0; i < stagedRemove.size(); i++) {
            String removeName = stagedRemove.get(i);
            if (files.containsKey(removeName)) { // untrack this file
                files.remove(removeName);
            }
            File rmFile = Utils.join(Repository.STAGING_RM_DIR, removeName);
            Boolean deleted = rmFile.delete();
            if (!deleted) {
                // The file wasn't successfully deleted, handle this case here
                System.out.println("Failed to delete file");
            }
        }
        // store commit to commit folder
        genCommitID();
        saveCommit();
        // change head to current ID， update branch pointer as well and save headMap.
        // update that head pointer and HEAD pointer
        headMap.put(branch, commitID); // remember this.
        headMap.put("HEAD", branch);

        File map = Repository.HEAD_MASTER;
        Utils.writeObject(map, (Serializable) headMap);
    }

    public void moveToStage(File sourceFile, String fileName, File destinFolder) {
        File stageFile = Utils.join(destinFolder, fileName);
        String content = Utils.readContentsAsString(sourceFile);
        Utils.writeContents(stageFile, content);
    }

    /** Each commit is identified by its SHA-1 id,
     *  which must include the file (blob) references of its files,
     *  parent reference, log message, and commit time.
     */
    public void genCommitID() {
        byte[] serTimeStampe = Utils.serialize(timeStamp);

        // Note: abstract class like Map, List, Set
        // cannot be serializable. Cause their input is <T>
        byte[] serFiles = Utils.serialize(files);
        byte[] serCommitP1 = Utils.serialize(parent);
        byte[] serCommitP2 = Utils.serialize(parent2);

        this.commitID = Utils.sha1(message, serTimeStampe, serFiles, serCommitP1, serCommitP2);
    }

    public void saveCommit() {
        File c = Utils.join(Repository.COMMIT_DIR, commitID);
        Utils.writeObject(c, this);
    }

    public static Commit readCommit(String commitID) {
        File f = Utils.join(Repository.COMMIT_DIR, commitID);
        Commit readC = Utils.readObject(f, Commit.class);
        return readC;
    }
    public static void overWriteInWorkDir(HashMap<String, String> files, String fileName) {
        if (files.get(fileName) == null) {
            System.out.println("File does not exist in that commit.");
            System.exit(0);
        } else {
            String fileID = files.get(fileName);
            File restoreFile = Utils.join(Repository.BLOB_DIR, fileID);
            String content = Utils.readContentsAsString(restoreFile);
            File destinFile = Utils.join(Repository.CWD, fileName);
            Utils.writeContents(destinFile, content);
        }
    }

    public static HashMap<String, String> moveFilesSTOB(List<String> stagedFiles,
                                                        File blobs, HashMap<String, String> files) {
        String blobPath = blobs.getAbsolutePath();
        int failCount = 0;
        int stageFileSize = stagedFiles.size();
        for (int i = 0; i < stagedFiles.size(); i++) {
            File sourceFile = Utils.join(Repository.STAGING_DIR, stagedFiles.get(i));
            String sourceFilePath = sourceFile.getAbsolutePath();

            try {
                Path sourcePath = Paths.get(sourceFilePath);
                Path desPath = Paths.get(blobPath);

                // generate SHA1 according to content
                String source = Utils.readContentsAsString(sourceFile);
                source = stagedFiles.get(i) + source; // using the file name as a label for blobs
                String blobID = Utils.sha1(source);
                // blobID is the ID in blobs and content in .txt copied
                Files.copy(sourcePath, desPath.resolve(blobID));
                // store file name as key and blobID as value in a HashMap
                files.put(stagedFiles.get(i), blobID);
            } catch (IOException e) {
                //System.out.println("An error occurred while copying the file: " + e.getMessage());
                failCount += 1;
            }
            // delete file in staging place
            sourceFile.delete();
        }
        return files;
    }
    public String getMessage() {
        return message;
    }
    public Date getTimeStamp() {
        return timeStamp;
    }
    public String getParent() {
        return parent;
    }
    public String getParent2() {
        return parent2;
    }
    public HashMap<String, String> getFiles() {
        return files;
    }
    public String getCommitID() {
        return commitID;
    }
}
