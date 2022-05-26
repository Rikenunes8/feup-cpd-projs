mkdir ..\build
cd ..\src\

javac membership\*.java messages\*.java utils\*.java .\*.java  -d ..\build\

cd ..\scripts