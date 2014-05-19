# D2J
d2j is a library that parses Android dex bytecode and transform it in Soot internal jimple representation. Soot is a bytecode analysis framework developed by McGill University (http://www.sable.mcgill.ca/soot/).

## Compiling
This is a regular maven project. Soot libraries are fetched from conjars.org. You can also upload them to your local repository if you prefer to 
recompile them yourself.

## Running the test cases
You must populate the directories in src/test/cases with either apk files or directly dex files (for example compiled with smali). 
An android.jar stub is provided.

