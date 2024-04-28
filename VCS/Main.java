package VCS;

import java.io.File;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.*;

public class Main implements Serializable {
    /** Hashmap to store information of HEAD and Master */

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND1> <OPERAND2> ... 
     */
    public static final int ARG3 = 3;
    public static final int IDLEN = 7;
    public static void main(String[] args) {
        // if no args
        if (args.length == 0) {
            System.out.println("Please enter a command.");
            System.exit(0);
        }
        String firstArg = args[0];
        if (!firstArg.equals("init")) {
            if (!Repository.GITLET_DIR.exists()) {
                System.out.println("Not in an initialized Gitlet directory.");
                System.exit(0);
            }
        }
        switch (firstArg) {
            case "init":
                validateNumArgs("init", args, 1);
                commandInit();
                break;
            case "add":
                commandAdd(args);
                break;
            case "commit":
                validateNumArgs("commit", args, 2);
                setupPersistence();
                new Commit(args[1]);
                break;
            case "rm":
                commandRemove(args);
                break;
            case "global-log":
                validateNumArgs("global-log", args, 1);
                commandGlobalLog();
                break;
            case "log":
                validateNumArgs("log", args, 1);
                commandLog();
                break;
            case "restore":
                validateNumArgs("restore", args, ARG3);
                commandRestore(args);
                break;
            case "find":
                commandFind(args);
                break;
            case "branch":
                commandBranch(args);
                break;
            case "switch":
                commandSwitch(args);
                break;
            case "rm-branch":
                commandRmBranch(args);
                break;
            case "reset":
                commandReset(args);
                break;
            case "status":
                commandStatus();
                break;
            case "merge":
                commandMerge(args);
                break;
            default:
                // if a command that doesn't exist.
                System.out.println("No command with that name exists.");
                System.exit(0);
        }
    }
    public static void commandMerge(String[] args) {
        setupPersistence();
        validateNumArgs("merge", args, 2);
        validateFormatArgs(args);
        String branchName = args[1];

        Commit curCommit = getHeadCommit();
        File h = Repository.HEAD_MASTER;
        HashMap<String, String> headMap = Utils.readObject(h, HashMap.class);
        List<String> addStaged = Utils.plainFilenamesIn(Repository.STAGING_DIR);
        List<String> rmStaged = Utils.plainFilenamesIn(Repository.STAGING_RM_DIR);

        if (!headMap.containsKey(branchName)) {
            System.out.println("A branch with that name does not exist.");
            System.exit(0);
        } else if (branchName.equals(headMap.get("HEAD"))) {
            System.out.println("Cannot merge a branch with itself.");
            System.exit(0);
        } else if (!addStaged.isEmpty() || !rmStaged.isEmpty()) {
            System.out.println("You have uncommitted changes.");
            System.exit(0);
        }
        // find split commit and given branch commit
        Commit branchHead = Utils.readObject(Utils.join(Repository.COMMIT_DIR, headMap.get(branchName)), Commit.class);
        Commit split = findSplit(curCommit, branchHead);
        if (split.getCommitID().equals(branchHead.getCommitID())) {
            System.out.println("Given branch is an ancestor of the current branch.");
            System.exit(0);
        } else if (split.getCommitID().equals(curCommit.getCommitID())) { // deal with different situation
            helperSwitchReset(null, branchHead.getCommitID());
            System.out.println("Current branch fast-forwarded.");
            System.exit(0);
        } else if (!split.getCommitID().equals(branchHead.getCommitID())) {
            Commit pointer = split;
            while (pointer != null) {
                if (pointer.getCommitID().equals(branchHead.getCommitID())) {
                    System.out.println("Given branch is an ancestor of the current branch.");
                    System.exit(0);
                }
                if (pointer.getParent().equals("")) {
                    break;
                }
                String parentC = pointer.getParent();
                pointer = Utils.readObject(Utils.join(Repository.COMMIT_DIR, parentC), Commit.class);
            }
            String message = "Merged " + branchName + " into " + headMap.get("HEAD") + ".";
            new Commit(message, split, curCommit, branchHead, branchName);
        }
    }
    public static Commit findSplit(Commit mainHead, Commit branchHead) {
        Commit head1 = mainHead;
        Commit head2 = branchHead;
        if (head1.getCommitID().equals(head2.getCommitID())) {
            return head1;
        }
        Set<String> p1 = new HashSet<>();
        Set<String> p2 = new HashSet<>();
        p1.add(head1.getParent());
        p1.add(head1.getParent2());
        p2.add(head2.getParent());
        p2.add(head2.getParent2());

        while (!p1.equals(p2)) {
            Set<String> intersection = new HashSet<>(p1);
            Set<String> inP2 = new HashSet<>(p2);
            intersection.retainAll(inP2);
            for (String split : intersection) {
                if (!split.equals("")) {
                    return Utils.readObject(Utils.join(Repository.COMMIT_DIR, split), Commit.class);
                }
            }
            Set<String> curP1 = new HashSet<>(p1);
            p1.removeAll(p1);
            for (String s : curP1) {
                if (curP1.size() == 1 && s.equals("")) {
                    p1.add(head2.getCommitID());
                    break;
                }
                if (!s.equals("")) {
                    Commit pp1 = Utils.readObject(Utils.join(Repository.COMMIT_DIR, s), Commit.class);
                    p1.add(pp1.getParent());
                    p1.add(pp1.getParent2());
                }
            }

            Set<String> curP2 = new HashSet<>(p2);
            p2.removeAll(p2);
            for (String s : curP2) {
                if (curP2.size() == 1 && s.equals("")) {
                    p2.add(head1.getCommitID());
                    break;
                }
                if (!s.equals("")) {
                    Commit pp2 = Utils.readObject(Utils.join(Repository.COMMIT_DIR, s), Commit.class);
                    p2.add(pp2.getParent());
                    p2.add(pp2.getParent2());
                }
            }
        }
        Set<String> intersection = new HashSet<>(p1);
        intersection.retainAll(p2);
        for (String split : intersection) {
            if (!split.equals("")) {
                return Utils.readObject(Utils.join(Repository.COMMIT_DIR, split), Commit.class);
            }
        }
        return null;
    }

    public static void commandBranch(String[] args) {
        setupPersistence();
        validateNumArgs("branch", args, 2);
        validateFormatArgs(args);
        String branchName = args[1];

        Commit curCommit = getHeadCommit();
        File h = Repository.HEAD_MASTER;
        HashMap<String, String> headMap = Utils.readObject(h, HashMap.class);
        if (headMap.containsKey(branchName)) {
            System.out.println("A branch with that name already exists.");
            System.exit(0);
        }
        headMap.put(branchName, curCommit.getCommitID());
        headMap.put("split", curCommit.getCommitID()); // put split point here
        Utils.writeObject(h, (Serializable) headMap);
    }

    public static void commandSwitch(String[] args) {
        validateNumArgs("switch", args, 2);
        validateFormatArgs(args);
        String branchName = args[1];
        helperSwitchReset(branchName, null);
    }

    public static void helperSwitchReset(String branchName, String resetCommit) {
        File h = Repository.HEAD_MASTER;
        HashMap<String, String> headMap = Utils.readObject(h, HashMap.class);
        if (branchName != null && !headMap.containsKey(branchName)) {
            System.out.println("No such branch exists.");
            System.exit(0);
        }
        if (headMap.get("HEAD").equals(branchName)) {
            System.out.println("No need to switch to the current branch.");
            System.exit(0);
        }
        Commit curCommit = getHeadCommit();
        // get branch commit if switch branch
        Commit branchCommit;
        String fullID = "";
        String commitPointer = headMap.get(headMap.get("HEAD"));
        if (branchName != null) {
            String headID = headMap.get(branchName); // absolute path
            File b = Utils.join(Repository.COMMIT_DIR, headID);
            branchCommit = Utils.readObject(b, Commit.class);
        } else {
            branchName = headMap.get("HEAD");
            String shortCID = resetCommit.substring(0, IDLEN);
            List<String> commits = Utils.plainFilenamesIn(Repository.COMMIT_DIR);
            boolean exist = false;
            for (int i = 0; i < commits.size(); i++) {
                String shortID = commits.get(i).substring(0, IDLEN);
                if (shortID.equals(shortCID)) {
                    exist = true;
                    fullID = commits.get(i);
                    break;
                }
            }
            if (!exist) {
                System.out.println("No commit with that id exists.");
                System.exit(0);
            }
            branchCommit = Utils.readObject(Utils.join(Repository.COMMIT_DIR, fullID), Commit.class);
            while (!resetCommit.equals(commitPointer) && !commitPointer.equals("")) {
                Commit c = Utils.readObject(Utils.join(Repository.COMMIT_DIR, commitPointer), Commit.class);
                commitPointer = c.getParent();
            }
            if (commitPointer.equals("")) { // not find the commit in current branch
                for (String key : headMap.keySet()) {
                    if (!key.equals("HEAD") && !key.equals(headMap.get("HEAD")) && !key.equals("split")) {
                        branchName = key;
                    }
                }
            }
        }

        List<String> workingFiles = Utils.plainFilenamesIn(Repository.CWD);
        for (int i = 0; i < workingFiles.size(); i++) {
            String fileName = workingFiles.get(i);
            if (!curCommit.getFiles().containsKey(fileName) && branchCommit.getFiles().containsKey(fileName)) {
                System.out.println("There is an untracked file in the way; delete it, or add and commit it first.");
                System.exit(0);
            }
            if (curCommit.getFiles().containsKey(fileName) && !branchCommit.getFiles().containsKey(fileName)) {
                File f = Utils.join(Repository.CWD, fileName);
                Utils.restrictedDelete(f);
            }
        }
        // copy all files in branch commit to working dir
        for (String key : branchCommit.getFiles().keySet()) {
            overWriteInWorkDir(branchCommit.getFiles(), key);
        }
        cleanStage();
        headMap.put("HEAD", branchName);
        if (resetCommit != null) {
            headMap.put(branchName, fullID);
        }
        Utils.writeObject(h, (Serializable) headMap);
    }
    public static void commandRmBranch(String[] args) {
        validateNumArgs("branch", args, 2);
        validateFormatArgs(args);
        String branchName = args[1];
        File h = Repository.HEAD_MASTER;
        HashMap<String, String> headMap = Utils.readObject(h, HashMap.class);
        if (!headMap.containsKey(branchName)) {
            System.out.println("A branch with that name does not exist.");
            System.exit(0);
        }
        if (headMap.get("HEAD").equals(branchName)) {
            System.out.println("Cannot remove the current branch.");
            System.exit(0);
        }
        headMap.remove(branchName);
        Utils.writeObject(h, (Serializable) headMap);
    }
    public static void commandReset(String[] args) {
        validateNumArgs("reset", args, 2);
        validateFormatArgs(args);
        String resetCommit = args[1];
        helperSwitchReset(null, resetCommit);
    }

    public static void commandInit() {
        initGitlet();
        Commit commit0 = new Commit();
        commit0.genCommitID();
        commit0.saveCommit();
        HashMap<String, String> headAndMaster = new HashMap<>();

        headAndMaster.put("HEAD", "main"); // Actively head pointer
        headAndMaster.put("main", commit0.getCommitID()); // main branch
        // save this hashMap for later use. Note, don't mkdir, resulting in a dir.
        File map = Repository.HEAD_MASTER;
        Utils.writeObject(map, (Serializable) headAndMaster);
    }
    public static void commandRestore(String[] args) {
        if (args[1].equals("--")) {
            String fileName = args[2];
            if (!(args[2] instanceof String)) {
                System.out.println("Incorrect operands.");
                System.exit(0);
            }
            // check whether the file is in the head commit
            Commit headCommit = getHeadCommit();
            HashMap<String, String> files = headCommit.getFiles();
            // Overwrite
            overWriteInWorkDir(files, fileName);
        } else if (args[2].equals("--")) {
            if (!(args[1] instanceof String) || !(args[ARG3] instanceof String)) {
                System.out.println("Incorrect operands.");
                System.exit(0);
            }
            String commitID = args[1];
            String shortCID = commitID.substring(0, IDLEN);
            String fileName = args[ARG3];
            List<String> commits = Utils.plainFilenamesIn(Repository.COMMIT_DIR);
            for (int i = 0; i < commits.size(); i++) {
                String shortID = commits.get(i).substring(0, IDLEN);
                if (shortID.equals(shortCID)) {
                    String fullID = commits.get(i);
                    HashMap<String, String> files = getCommitFiles(fullID);
                    overWriteInWorkDir(files, fileName);
                    return;
                }
            }
            System.out.println("No commit with that id exists.");
            System.exit(0);
        } else {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }
    }
    public static void commandFind(String[] args) {
        setupPersistence();
        validateNumArgs("find", args, 2);
        validateFormatArgs(args);
        String message = args[1];

        List<String> commits = Utils.plainFilenamesIn(Repository.COMMIT_DIR);
        int count = 0;
        for (int i = 0; i < commits.size(); i++) {
            File c = Utils.join(Repository.COMMIT_DIR, commits.get(i));
            Commit commit = Utils.readObject(c, Commit.class);
            if (commit.getMessage().equals(message)) {
                System.out.println(commit.getCommitID());
                count += 1;
            }
        }
        if (count == 0) {
            System.out.println("Found no commit with that message.");
            System.exit(0);
        }
    }
    public static void commandStatus() {
        setupPersistence();
        Commit curCommit = getHeadCommit();
        // print branches
        System.out.println("=== Branches ===");
        File h = Repository.HEAD_MASTER;
        HashMap<String, String> headMap = Utils.readObject(h, HashMap.class);
        String headBranch = headMap.get("HEAD");
        System.out.println("*" + headBranch);
        for (String key : headMap.keySet()) {
            if (!key.equals("HEAD") && !key.equals(headBranch) && !key.equals("split")) {
                System.out.println(key);
            }
        }
        System.out.println();
        // print staged file and removed file
        System.out.println("=== Staged Files ===");
        List<String> stageFiles = Utils.plainFilenamesIn(Repository.STAGING_DIR);
        for (int i = 0; i < stageFiles.size(); i++) {
            System.out.println(stageFiles.get(i));
        }
        System.out.println();
        System.out.println("=== Removed Files ===");
        List<String> removeFiles = Utils.plainFilenamesIn(Repository.STAGING_RM_DIR);
        for (int i = 0; i < removeFiles.size(); i++) {
            System.out.println(removeFiles.get(i));
        }
        System.out.println();
        // print modification not staged
        System.out.println("=== Modifications Not Staged For Commit ===");
        System.out.println();
        System.out.println("=== Untracked Files ===");
        System.out.println();
    }
    public static void commandLog() {
        setupPersistence();
        Commit curCommit = getHeadCommit();
        while (curCommit != null) {
            printInfo(curCommit);
            // move to parent commit
            File p = Utils.join(Repository.COMMIT_DIR, curCommit.getParent());
            if (!curCommit.getParent().equals("")) {
                Commit parentCommit = Utils.readObject(p, Commit.class);
                curCommit = parentCommit;
            } else {
                break;
            }
        }
    }
    public static void commandGlobalLog() {
        setupPersistence();
        // get all plain files in commit folder
        List<String> commits = Utils.plainFilenamesIn(Repository.COMMIT_DIR);
        for (int i = 0; i < commits.size(); i++) {
            File c = Utils.join(Repository.COMMIT_DIR, commits.get(i));
            Commit commit = Utils.readObject(c, Commit.class);
            printInfo(commit);
        }
    }
    /** Helper function for log and global log */
    public static void printInfo(Commit curCommit) {
        System.out.println("===");
        System.out.println("commit " + curCommit.getCommitID());
        if (!curCommit.getParent2().equals("")) {
            System.out.println("Merge: " + curCommit.getParent().substring(0, IDLEN)
                    + " " + curCommit.getParent2().substring(0, IDLEN));
        }
        String newDate = formatedTimeStamp(curCommit.getTimeStamp());
        System.out.println("Date: " + newDate);
        System.out.println(curCommit.getMessage() + '\n');
    }

    public static void commandAdd(String[] args) {
        validateNumArgs("add", args, 2);
        validateFormatArgs(args);
        setupPersistence();
        for (int i = 1; i < args.length; i++) {
            String secondArg = args[i];
            addToStage(secondArg);
        }
    }
    /** Helper function for add command */
    public static void addToStage(String fileName) {
        Commit head = getHeadCommit();
        File sourceFile = Utils.join(Repository.CWD, fileName);
        File stageFile = Utils.join(Repository.STAGING_DIR, fileName);
        File removeFile = Utils.join(Repository.STAGING_RM_DIR, fileName);

        if (!sourceFile.exists()) {
            System.out.println("File does not exist.");
            System.exit(0);
        }

        // Push the file to staging are at first. (Overwrite if exist)
        String content = Utils.readContentsAsString(sourceFile);
        Utils.writeContents(stageFile, content);

        // check whether exist in current commit. If exists, check content using blobID.
        // If equal, remove from stage folder.
        if (head.getFiles().containsKey(fileName)) {
            // check whether stage folders contains this files
            String blobID = head.getFiles().get(fileName);
            List<String> stageFiles = Utils.plainFilenamesIn(Repository.STAGING_DIR);
            // check whether staging folder contain same file and content is the same
            if (stageFiles.contains(fileName)) {
                String stageContent = Utils.readContentsAsString(stageFile); // content in stage file
                stageContent = fileName + stageContent;  // fileName as the label for blob files
                String potentialBlobID = Utils.sha1(stageContent);
                if (potentialBlobID.equals(blobID)) {
                    stageFile.delete();
                    removeFile.delete();
                }
            }
        }
    }
    public static void commandRemove(String[] args) {
        setupPersistence();
        validateNumArgs("rm", args, 2);
        validateFormatArgs(args);
        String fileName = args[1];

        File fileInWork = Utils.join(Repository.CWD, fileName);
        File fileInStageAdd = Utils.join(Repository.STAGING_DIR, fileName);
        Commit head = getHeadCommit();

        if (!fileInStageAdd.exists() && !head.getFiles().containsKey(fileName)) {
            System.out.println("No reason to remove the file.");
            System.exit(0);
        }

        if (fileInStageAdd.exists()) {
            fileInStageAdd.delete();
        }
        if (head.getFiles().containsKey(fileName)) {
            String fileID = head.getFiles().get(fileName);
            File restoreFile = Utils.join(Repository.BLOB_DIR, fileID);
            String content = Utils.readContentsAsString(restoreFile);
            File destinFile = Utils.join(Repository.STAGING_RM_DIR, fileName);
            Utils.writeContents(destinFile, content);
            if (fileInWork.exists()) {
                Utils.restrictedDelete(fileInWork);
            }
        }
    }

    /** Helper function for get Head Commit*/
    public static Commit getHeadCommit() {
        File h = Repository.HEAD_MASTER;
        HashMap<String, String> headMap = Utils.readObject(h, HashMap.class);
        String branch = headMap.get("HEAD");
        String headID = headMap.get(branch); // absolute path
        // get the files hashMap
        File f = Utils.join(Repository.COMMIT_DIR, headID);
        Commit headCommit = Utils.readObject(f, Commit.class);
        return headCommit;
    }
    /** Helper function for get HashMap of files pointing to blobs
     * May used for check head commit */
    public static HashMap<String, String> getCommitFiles(String commitID) {
        // get commit file in commit folder
        File h = Utils.join(Repository.COMMIT_DIR, commitID);
        // get files hashmap
        Commit desireCommit = Utils.readObject(h, Commit.class);
        HashMap<String, String> fileMap = desireCommit.getFiles();
        return fileMap;
    }
    /** Helper function for restore functions */
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
    public static void cleanStage() {
        List<String> addS = Utils.plainFilenamesIn(Repository.STAGING_DIR);
        List<String> rmS = Utils.plainFilenamesIn(Repository.STAGING_RM_DIR);
        for (int i = 0; i < addS.size(); i++) {
            Utils.join(Repository.STAGING_DIR, addS.get(i)).delete();
        }
        for (int i = 0; i < rmS.size(); i++) {
            Utils.join(Repository.STAGING_RM_DIR, rmS.get(i)).delete();
        }
    }

    public static void initGitlet() {
        File f = Repository.GITLET_DIR;
        if (f.exists()) {
            System.out.println("A Gitlet version-control system already exists in the current directory.");
            System.exit(0);
        }
        f.mkdir();
        File c = Repository.COMMIT_DIR;
        if (!c.exists()) {
            c.mkdir();
        }
    }

    public static void setupPersistence() {
        File s = Repository.STAGING_DIR;
        if (!s.exists()) {
            s.mkdir();
        }
        File r = Repository.STAGING_RM_DIR;
        if (!r.exists()) {
            r.mkdir();
        }
        File b = Repository.BLOB_DIR;
        if (!b.exists()) {
            b.mkdir();
        }
    }

    public static void validateNumArgs(String cmd, String[] args, int n) {
        if (cmd.equals("add")) {
            if (args.length < n) {
                System.out.println("Incorrect operands.");
                System.exit(0);
            }
        } else if (cmd.equals("restore")) {
            if (args.length < n || args.length > n + 1) {
                System.out.println("Incorrect operands.");
                System.exit(0);
            }
        } else if (cmd.equals("commit")) {
            if (args.length == 1) {
                System.out.println("Please enter a commit message.");
                System.exit(0);
            }
        } else {
            if (args.length != n) {
                System.out.println("Incorrect operands.");
                System.exit(0);
            }
        }
    }

    public static void validateFormatArgs(String[]args) {
        for (String arg : args) {
            if (!(arg instanceof String)) {
                System.out.println("Incorrect operands.");
                System.exit(0);
            }
        }
    }

    public static String formatedTimeStamp(Date timeStemp) {
        SimpleDateFormat outputFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss yyyy Z");
        String outputDate = outputFormat.format(timeStemp);
        return outputDate;
    }
}
