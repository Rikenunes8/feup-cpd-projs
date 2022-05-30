@echo off

if %1 == 1 ( java -cp ..\build TestClient 127.0.0.1:9000 %2 %3 )
if %1 == 2 ( java -cp ..\build TestClient 127.0.0.2:9000 %2 %3 )
if %1 == 3 ( java -cp ..\build TestClient 127.0.0.3:9000 %2 %3 )
if %1 == 4 ( java -cp ..\build TestClient 127.0.0.4:9000 %2 %3 )

timeout 1
tree /F ..\build\network

