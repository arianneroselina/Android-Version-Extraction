# Android Version Extraction

## Introduction
This repository is introduced in my Bachelor thesis with the title "Vulnerability Detection for Android Apps Using Version Information".
This tool automates the version extraction of an Android application (frameworks and languages used) and use this information
to determine the security and privacy vulnerabilities of the app.

Android mobile application development frameworks supported by this tool, along with the earliest release date can be 
found in the optimal situation:
1. Flutter, up to version 1.17.0 (06.05.2020)
2. React Native, up to version 0.62.0 (26.03.2020)
3. Qt, up to version 5.14.2 (31.03.2020)
4. Xamarin, up to version 11.0.0.3 (05.08.2020)
5. Unity, all versions are covered
6. Apache Cordova, all versions are covered

## Usage
Given an APK file or folder path, this tool creates a JSON file stating the versions and the corresponding vulnerability website links.
The JSON file can be found in the same directory as the APK file.

### Two ways to run the tool
- Run directly using sbt.
    ````
    sbt
    ~run -f C:/path/to/apk/filename.apk 
    ````
  
- Run with a generated .jar file.
    ````
    sbt assembly // just once
    java -jar ./target/scala-2.13/android_version_extraction-assembly-0.1.0-SNAPSHOT.jar -f C:/path/to/apk/filename.apk 
    ````

### Available flags
- `-f <arg>` or `--apk-filepath <arg>` : input file \<apk file path\>
- `-d <arg>` or `--apk-filepaths <arg> ` : path to file containing \<apk file paths\>
- `-a` or `--android-general` : include Android security vulnerability links that apply to all versions

## Other Scripts

### Write File Hashes
This repository also includes [a python script](src/main/python/write_file_hashes.py) that can append a new hashed .lib/.dll file 
of the frameworks to the existing tables in [the hashes folder](src/files/hashes).
The hash in the tables will later be compared to the one given as an input.
If the hashes match, it means that they have the same version.
This method is used to extract Flutter, Qt, React Native, Unity, and Xamarin's versions.

### Extract Android CVE
A function to extract Android API CVE vulnerability links can also be found in 
[this python script](src/main/python/extract_android_cve.py), which adds vulnerability links into the corresponding csv 
file in [the AndroidAPI folder](src/files/vulnerability_links/AndroidAPI).
The input for this script can be obtained by downloading a CVE page using the "Download Results" button and rename it to
a csv file.

### Evaluation
[The python script](src/main/python/evaluation.py) provides graph visualizations for evaluation of the tool given a text 
file containing the JSON filepaths.

## List of Files
Apart from the files generated by the [Write File Hashes](#write-file-hashes) and the 
[Extract Android CVE](#extract-android-cve) scripts, there are:
- 5 files containing the security vulnerability links of the frameworks, namely Cordova, Flutter, Qt, Xamarin and 
  React Native. These files were created manually using information I obtained from my own research.
- 6 files containing the list of release dates of each of the framework's version. If no version can be extracted with 
  the original method, the release date of the version and the creation date of the app are compared.
