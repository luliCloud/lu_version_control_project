# lu_version-control-system

## Introduction
Similar to Git (version control system), with all basic functions: `init`, `add`, `commit`, `rm`, `log`, `global-log`, `find`, `status`, `restore`, `branch`, `switch`, `rm-branch`, `reset`, and `merge`.

Please check the **version-control-design.md** to understand to full design of this project, including data structures, algorithms, and persistence used in this project. 

Please follow the Manual below to compile and run this program.

## Manual

### Compile using command line
1. cd VCS folder
2. check your javac and java, ensuring you have these two softwares
3. `javac *.java` 
4. `ls -al` 
(you should see all java file compiled into .class file, that's what you will run)
4. `cd ..`
5. You are at parent folder of VCS folder now. You can use any git functions with following command.
6. `java VCS.main [args]`

Note: if you run `java VCS.main` alone, you will see the error output:
Please enter a command.

### init
1. `java VCS.Main init` (under project folder)
2. A new folder will be generated under lu_version_control_project (or the project you renamed) folder, named .gitlet. It will contained the first commit file. Any following output files (blob snapshots, main and headmaster hashmap) or temporary files (staging files) will be stored in this folder. 
3. if you want initialize a new proj with init. You need to manually delete the .gitlet folder. 
`rm -r .gitlet`

### add
1. `java VCS.Main add [file_name_1] [file_name_1] ...`
2. if you want unstage any staging file (not modified after staging and before you do this action). you can do `java VCS.Main add [file_name_1]`
3. if you already modified the file in `CWD` and running `java VCS.main add [file_name_1]`, you will not unstage the staging file but overwritten the staging file with modified contents. 

### rm
1. `java VCS.Main rm [file_name_1] [file_name_1] ...`
2. rm files from CWD, and retrieve the content from blob snapshots. The matching rules is the blob which has the same name as this file in the last commit `files` hash map. 

### commit 
1. `java VCS.Main commit "message"`
2. move all files in addtion staging area to blob directory (convert files to blob snapshots) and track these files (put them in `files` hashmap). 
3. delete all files in removal staging area and untrack these files (erase them from `files` hashmap).

### log
1. `java VCS.Main log`\
output
```
===
commit 707717b5ee7195b1f06cfca71db9413f9fe75282
Date: Mon Apr 29 23:00:47 2024 -0700
commit a.txt

===
commit 1310398e61cf6f73945acb35a3e4d16d2e7112a2
Date: Wed Dec 31 16:00:00 1969 -0800
initial commit
```

### global-log
1. `java VCS.Main global-log`\
output
```
===
commit 707717b5ee7195b1f06cfca71db9413f9fe75282
Date: Mon Apr 29 23:00:47 2024 -0700
commit a.txt

===
commit 1310398e61cf6f73945acb35a3e4d16d2e7112a2
Date: Wed Dec 31 16:00:00 1969 -0800
initial commit
```

### find
1. `java VCS.Main find "message"`\
output
```
17e1142ea3fe84291d1e3bb62bb37a48999b1e9a
707717b5ee7195b1f06cfca71db9413f9fe75282
```

### status
1. `java VCS.Main status`\
output
```
=== Branches ===
*main

=== Staged Files ===
a.txt
d.txt

=== Removed Files ===
c.txt

=== Modifications Not Staged For Commit ===

=== Untracked Files ===
```

### restore
1. `java VCS.Main restore -- [file name]`\
Takes the version of the file as it exist in the **head commit** and puts it in the working directory, overwrting hte version of the file that's alaready there if there is one. The new version of the file is not staged.\
2. `java VCS.Main restore [commit id] -- [file name]`\
Takes the version of the file as it exist in the **commit with the given id** (could be not active branch), and puts it in the working directory, overwriting the version of the file that's laready there if there is one. THe new version of the file is not staged.\ 

### branch
1. `java VCS.Main branch [branch name]`\
if input `java VCS.Main status`
```
=== Branches ===
*main
test
status

=== Staged Files ===

=== Removed Files ===

=== Modifications Not Staged For Commit ===

=== Untracked Files ===
```

### remove branch
1. `java VCS.Main rm-branch [branch name]`
if input `java VCS.Main rm-branch status` and then `java VCS.Main status`
```
=== Branches ===
*main
test

=== Staged Files ===

=== Removed Files ===

=== Modifications Not Staged For Commit ===

=== Untracked Files ===
```

### reset
1. `java VCS.Main reset [commit id]`: reverse branch to a specific commit (specified by the given commid id)

### merge
1. `java VCS.Main merge [branch name]`: the branch name is the branch to be merged into current branch
output:  
1. terminal messeage if successful: 
```
Merged test into main
```
3. The output file (to address potential conflicts) serves as a warning: this command does not resolve conflicts automatically. Instead, a file contents indicating conflicts is generated and overwrites existing conflicting file. The file contents are provided to users so they can manually resolve the conflicts themselves. However, this merge is still recorded as a commit and includes two parent commits (current branch and the merged branch). Any modifications needed to resolve conflicts in the file must be addressed in a subsequent commit.
Original file contents:
```
main in different version in test. 
```
After merge command the terminal output
```
Encountered a merge conflict.
```
and the same file have conflict changed into:

```
<<<<<<< HEAD
main in different version in test. 

=======
this file modified in main
>>>>>>>
```