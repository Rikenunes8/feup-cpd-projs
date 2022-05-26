cd ..\build\

:: Start registry
echo Initializing RMI registry on port 1099
start rmiregistry

:: Start stores
start cmd /k java -Djava.rmi.server.codebase=file:.\ Store 224.0.0.0 3000 127.0.0.1 3100
start cmd /k java -Djava.rmi.server.codebase=file:.\ Store 224.0.0.0 3000 127.0.0.2 3100
start cmd /k java -Djava.rmi.server.codebase=file:.\ Store 224.0.0.0 3000 127.0.0.3 3100
start cmd /k java -Djava.rmi.server.codebase=file:.\ Store 224.0.0.0 3000 127.0.0.4 3100
