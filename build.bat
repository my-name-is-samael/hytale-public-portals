@echo off
setlocal
set "PATH=C:\Program Files\Java\jdk-25\bin;%PATH%"
cd /d "%~dp0"

rem Name and version come from manifest.json, so the jar is always <Name>-<version>.jar
for /f "usebackq delims=" %%v in (`powershell -NoProfile -Command "(Get-Content src\main\resources\manifest.json -Raw | ConvertFrom-Json).Version"`) do set VERSION=%%v
for /f "usebackq delims=" %%n in (`powershell -NoProfile -Command "(Get-Content src\main\resources\manifest.json -Raw | ConvertFrom-Json).Name"`) do set NAME=%%n
set JAR=%NAME%-%VERSION%.jar
set OUT=build\classes

if exist build rmdir /s /q build
mkdir %OUT%
dir /s /b src\main\java\*.java > build\sources.txt
javac -encoding UTF-8 --release 25 -cp ..\HytaleServer.jar -d %OUT% @build\sources.txt || exit /b 1
xcopy /e /i /q /y src\main\resources %OUT% >nul
jar --create --file build\%JAR% -C %OUT% . || exit /b 1
echo Built build\%JAR%

if /i "%1"=="deploy" (
  del /q ..\Server\mods\%NAME%*.jar 2>nul
  copy /y build\%JAR% ..\Server\mods\%JAR% >nul
  echo Deployed to ..\Server\mods\%JAR%
)
