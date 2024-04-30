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
1. `java VCS.main init` (under project folder)
2. A new folder will be generated under lu_version_control_project (or the project you renamed) folder, named .gitlet. It will contained the first commit file. Any following output files (blob snapshots, main and headmaster hashmap) or temporary files (staging files) will be stored in this folder. 
3. if you want initialize a new proj with init. You need to manually delete the .gitlet folder. 
`rm -r .gitlet`

### add
1. `java VCS.main add [file_name_1] [file_name_1] ...`
2. if you want unstage any staging file (not modified after staging and before you do this action). you can do `java VCS.main add [file_name_1]`
3. if you already modified the file in `CWD` and running `java VCS.main add [file_name_1]`, you will not unstage the staging file but overwritten the staging file with modified contents. 

### rm
1. `java VCS.main rm [file_name_1] [file_name_1] ...`
2. rm files from CWD, and retrieve the content from blob snapshots. The matching rules is the blob which has the same name as this file in the last commit `files` hash map. 

### commit 
1. `java VCS.main commit "message"`
2. move all files in addtion staging area to blob directory (convert files to blob snapshots) and track these files (put them in `files` hashmap). 
3. delete all files in removal staging area and untrack these files (erase them from `files` hashmap).

### log
1. `java VCS.main log`

### global-log
1. `java VCS.main global-log`

### find
1. `java VCS.main find "message"`