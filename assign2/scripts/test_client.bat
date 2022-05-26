@echo off

if %1 == 1 ( java -cp ..\build TestClient 127.0.0.1:3100 %2 %3 )
if %1 == 2 ( java -cp ..\build TestClient 127.0.0.2:3100 %2 %3 )
if %1 == 3 ( java -cp ..\build TestClient 127.0.0.3:3100 %2 %3 )
if %1 == 4 ( java -cp ..\build TestClient 127.0.0.4:3100 %2 %3 )

