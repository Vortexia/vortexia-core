@echo off
if exist gradlew.bat (
    .\gradlew.bat shadowJar
) else (
    echo Gradle Wrapper not found. Attempting to use global gradle...
    call gradle shadowJar
)
pause
