@echo off
setlocal

if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" (
    "%JAVA_HOME%\bin\java.exe" -version 2>&1 | findstr /i "21." >nul
    if not errorlevel 1 goto runBuild
)

if defined JAVA21_HOME if exist "%JAVA21_HOME%\bin\java.exe" (
    set "JAVA_HOME=%JAVA21_HOME%"
    set "PATH=%JAVA21_HOME%\bin;%PATH%"
    goto runBuild
)

if defined JDK21_HOME if exist "%JDK21_HOME%\bin\java.exe" (
    set "JAVA_HOME=%JDK21_HOME%"
    set "PATH=%JDK21_HOME%\bin;%PATH%"
    goto runBuild
)

for /d %%D in (
    "C:\Program Files\Eclipse Adoptium\jdk-21*"
    "C:\Program Files\Java\jdk-21*"
    "C:\Program Files\Microsoft\jdk-21*"
    "C:\Program Files\Amazon Corretto\jdk21*"
    "C:\Program Files\Zulu\zulu-21*"
    "C:\Program Files\BellSoft\LibericaJDK-21*"
    "%USERPROFILE%\.jdks\*21*"
    "%LOCALAPPDATA%\Programs\Eclipse Adoptium\jdk-21*"
) do (
    if exist "%%~D\bin\java.exe" (
        set "JAVA_HOME=%%~D"
        set "PATH=%%~D\bin;%PATH%"
        goto runBuild
    )
)

:runBuild
if exist gradlew.bat (
    call .\gradlew.bat shadowJar
) else (
    echo Gradle Wrapper not found. Attempting to use global gradle...
    call gradle shadowJar
)
pause
