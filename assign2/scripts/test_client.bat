@echo off

if %1 == 1 ( java -cp ..\build client.TestClient 127.0.0.1:9000 %2 %3 )
if %1 == 2 ( java -cp ..\build client.TestClient 127.0.0.2:9000 %2 %3 )
if %1 == 3 ( java -cp ..\build client.TestClient 127.0.0.3:9000 %2 %3 )
if %1 == 4 ( java -cp ..\build client.TestClient 127.0.0.4:9000 %2 %3 )

:: sleep 1s
ping 127.0.0.1 -n 2 > nul

tree /F ..\build\network

