# Version Control System Design Document

## Classes and Data Structures

### Main Class

#### Fields

1. `Main function`: this function receive arguments to execute the git command, with the arguments are formatted as `[comamnd] [operand1] [operand2]...`. For example: `java CVS.Main [add] [file1] [file2]`. Initially, the main function determine how many arguments are received. If no argument are provided, it displays an error message. If one or more arguments are available, the function uses the first argument to match the `init` command (not included in `switch`) and other cases in `switch`statement. If the command does not match any keyword, it displays an error message stating that the command is invalid. If the operands number of format after command does not match the requirement of that command, it displays an error message stating that the operands are invalid.

2. `commandInit`: This function starts with the `initGitlet` function. This function leverages the Commit class to create the initial commit object, referred to as "commit 0". This commit is assigned a new ID, and all related information is captured in the first blob snapshot. Subsequently, a hashmap is created to manage the details of the active head pointer and the current active branch, which defaults to "main". In this initial hashmap, two key-value pairs are established: one maps <HEAD, main>, and the other maps <main, commit0_ID>. This hashmap, referred to as headAndMaster, is then saved to a file (headAndMaster) for future reference. The hashmap information is serialized into a byte stream using the Utils function, which handles the writing of this data to persistent storage.

3. `initGitlet`: this function makes two persistences if not exist: .gitlet and .gitlet/commit. The two are used to initialize a new git repository. commit folder will store the first commit blob snapshot (for init commit). 

4. `commandAdd`: This function first checks for the keyword "add" and counts the arguments that follow. If any required persistences are missing, it establishes the necessary ones. Then, it adds all the files specified in the arguments to the `STAGING_DIR` by invoking the `addToStage` function.

5. `addToStage`: Helper funciton of `commandAdd`. This function duplicates the original file from the Current Working Directory (CWD) to the addition staging directory. The file is first copied to the adding staging area, with its content and format preserved identically to the original (**pre-staging**). If a file with same name already exist in the addition staging directory, it will be overwrriten. The function then retrievs the `files` hashmap <file_name, blob_id> from the parent commit.  Next, the function checks if the file hashmap already contains a file with the same name as pre-staging file. If such a file exists, the function generates the blob ID of pre-staging file (computed using utils.sha1(file name + file contents)) and then compares this hashing blobID (string) with the blod_id of the file with the same file name already stored in `files` hashmap. If they are identical, the file in the pre-staging area is removed (meaning this addition action fails because the file is already committed with identical content, not no error message shown here). As a result, the second `add` command acts like unstage commnad to call back any identical file (name and contents) added to addtion staging area. Subsequently, the commit command will verify whether the staging area is empty.

3. `setupPersistence`: this functions sets up three persistences in .gitlet: direcory for staging files to be added, staging files to be removed, and blob files of all commits.


### Commit Class

#### Fields
**class member**
1. `message`: the message of this commit
2. `timeStamp`: the time stamp of this commit
3. `parent`: the parent commit of this commit (maybe more than one): for rolloing back
4. `parent2`: the 2nd parent commit of this commit
5. `files`: File pointer to blobs. `<file name, blob_ID>`
6. `commitID`: hash code for each commit

**class methods**
1. `Commit(String message)`: this function is designed for straightforward scenarios where there is only one parent commit, typically not involving a branch split. After reading `headMasterMap` file, tt begins by identifying the active branch using the HEAD pointer and retrieves the most recent commit from this branch to serve as the parent for the new commit. The function then accesses all related blob files for the current branch, which are stored in the `file` hashmap (inherited from the class member **file** of parent commit). It proceeds by invoking the `helperCommit` function to commit files that are staged for addition, removes files in the staging area set for deletion, and generates a unique commitID. Finally, it saves this new commit under the current branch in the headMasterMap.

2. `helperCommit`: First, the function retrieves the lists of files that are staged for addition and removal from the directories `STAGING_DIR` and `STAGING_RM_DIR`, respectively. If both lists are empty, the system outputs "No change added to the commit" and exits successfully. This is consistent with our previous discussion that an empty staging area is detected and reported during the commit action. Next, the function calls `moveFilesSTOB` to transform all files in the addition staging area into blob snapshots, storing each **<file name, blob id>** pair in the files hashmap of `this` commit. The function then processes the removal staging area by iterating over all files in `STAGING_RM_DIR`. If any file is found in the current `files` hashmap, it is `erased` from the hashmap, and the file itself is `deleted` from the removal staging area. Finally, the function generates a commit ID for the new commit. It updates the `headMasterMap` with the commit ID, associating it with the currently active branch, and saves the commit information (a commit file) to the commit directory by `saveCommit` function.

3. `moveFileSTOB`: 'moveFIlesSTOB': this function convert all stagedFiles in adding stage area into blob sanp shot using sha1 hashing function. This function processes files in the `STAGING_DIR` by converting each staged file into a blob snapshot. During this conversion, it employs `Utils.readContentsAsString()` to read the file contents as a string. It then generates a `blobID` by hashing the combination of the file name and string contents using `Utils.sha1()`. The `Files.copy()` function is used to duplicate the original file's contents into a new file named by the sha1 blobID, effectively creating the blob snapshot. The `Files.copy(sourcePath, desPath.resolve(blobID))` function in Java copies the content of the source file directly to the destination file without altering the content, hashing it, or converting it into a byte stream. After successfully creating each blob snapshot, the file's name and its corresponding blobID are recorded in the `files` hashmap of `this` commit. Once converted, the original files in the `STAGING_DIR` are deleted.

4. `genCommitID`: This function generates a CommitID by hashing a combination of all Commit class variables, including files, parents, message, and timestamp, using the `Utils.sha1()` function. This process ensures that each CommitID uniquely represents the specific state and content of the commit.

5. `saveCommit`: This function converts the commit information (all its class variables but not methods) into a byte stream and writes it to a file. The file is named with Commit ID (generated by `genCommitID`) and is stored in the COMMIT_DIR directory. This method ensures that each commit's details are serialized and preserved uniquely, facilitating easy retrieval and reference.

## Algorithms
### HashMap 
We utilize a hashmap called headAndMaster (**HashMap<String, String>**) to keep track of the active head pointer and information about all branches. The key **head** specifies the branch currently in use by the program. Additionally, multiple branch keys track the latest commits (may not be the newest, as we may roll back) for each branch. Each commitId associated with these branches helps locate the commit blob snapshot stored within `.gitlet`.

We use a hashmap named `files` (HashMap<string, string>), which is class member of the Commit class, to maintain a record of all files associated with this commit. The keys in this hashmap are the names of successfully committed files, while the values are the blob IDs, which consist of the file name plus file contents hashed using sha1. This files hashmap is inheritable by child commits. Any new files committed by child commits are added to this hashmap and incorporated into their commit information.


### Linked List
children -> parent

### SHA1 (Secure Hash Algorithm)
THis is a cryptographic hash function designed to convert data into a fixed size (160 bits, ot 20 bytes) has value. This hash value is typically represented by a **40-digit hexadecimal number**. We use SHA1 for generating both `CommitID` and `blobID`. The principle of SHA-1 like other fucntions, aim to satisfy basic attributes: **Uniqueness, Irreversibility, Fast computation, Collision Ressistance**. 

Below is the working priciple of the `sha1` function:
1. Initialization: The method starts creating a `MessageDigest` object for SHA1 computation through `MessageDigest..getInstance("SHA-1")`.
2. Updating Data: The function iterate over all provided arguments (`vals` array in the function arguments). Depending on the type of the argument (`byte[]` or `String`). It updates the hash computation data differently. If it is a string, it first converts the string into bytes using UTF-8 encoding, then updates the hash data
3. Computing the Hash Value: The `md.digest()` completes the hash calculation for all input data, returning a byte array representing the hash value.
4. Formatting the Hash Value: A `Formatter` is used to format the **byte** array into a **hexadecimal string**. Each byte is converted into a two-digit hexadecimal number. 
5. Exception Handling: if the SHA1 algorithm is unavaible in the system, it catcehs a `NoSuchAlgorighmExcemption` and thrown an `IllegalArgumentException`.

## Persistence
This file stores all instance variables of the Repository class with a useful comment above them describing what that variable represents and how that variable is used.

1. `CWD`: The current working directory (lu_version_control_project)
2. `GITLET_DIR`: The .gitlet directory in CWD
3. `STAGING_DIR`: The staging directory for add (seperate from remove). The adding stage file list can be obtained from this dir using `Utils.mehtod`
4. `STAGING_RM_DIR`: The staging directory for remove. The removing stage file list can be obtained from this dir using `Utils.mehtod`
5. `COMMIT_DIR`: The commit directory, containing all commit files (name: commitID, content: serialization using Utils).
6. `BLOB_DIR`: The directory storing blob snapshot. Blob ID = sha1(file_name + file_contents)
7. `HEAD_MASTER`: The head and master pointer information file. HashMap: <HEAD, live branch>; <branch, lastest commit>

