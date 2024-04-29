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
2. A new folder will be generated under lu_version_control_project (or the project you renamed) folder, named .gitlet. It will contained the first commit file. Any following output files or temporary files will be stored in this folder. 
3. if you want initialize a new proj with init. You need to manually delete the .gitlet folder. 
`rm -r .gitlet`