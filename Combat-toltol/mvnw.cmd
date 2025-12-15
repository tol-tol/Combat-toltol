@ECHO OFF
SETLOCAL

SET WRAPPER_DIR=%~dp0.mvn\wrapper
SET WRAPPER_JAR=%WRAPPER_DIR%\maven-wrapper.jar
SET WRAPPER_PROPERTIES=%WRAPPER_DIR%\maven-wrapper.properties

FOR /F "usebackq tokens=1,2 delims==" %%A IN ("%WRAPPER_PROPERTIES%") DO (
  IF "%%A"=="wrapperUrl" SET WRAPPER_URL=%%B
)

IF EXIST "%WRAPPER_JAR%" GOTO EXEC_MAVEN

IF "%WRAPPER_URL%"=="" SET WRAPPER_URL=https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.2.0/maven-wrapper-3.2.0.jar

ECHO Downloading Maven Wrapper JAR from %WRAPPER_URL%

powershell -NoProfile -ExecutionPolicy Bypass -Command "try { Invoke-WebRequest -UseBasicParsing -Uri '%WRAPPER_URL%' -OutFile '%WRAPPER_JAR%' } catch { Write-Host 'Failed to download Maven Wrapper JAR.'; exit 1 }"
IF %ERRORLEVEL% NEQ 0 (
  EXIT /B 1
)

:EXEC_MAVEN
SET MAVEN_PROJECTBASEDIR=%~dp0
IF "%MAVEN_PROJECTBASEDIR:~-1%"=="\" SET MAVEN_PROJECTBASEDIR=%MAVEN_PROJECTBASEDIR:~0,-1%

SET JAVA_EXE=java
IF NOT "%JAVA_HOME%"=="" SET JAVA_EXE=%JAVA_HOME%\bin\java

REM Try to resolve java.exe when it is not directly callable from cmd
IF "%JAVA_EXE%"=="java" (
  FOR /F "delims=" %%J IN ('where java 2^>NUL') DO (
    SET JAVA_EXE=%%J
    GOTO JAVA_FOUND
  )
)

IF "%JAVA_EXE%"=="java" (
  FOR /F "usebackq delims=" %%J IN (`powershell -NoProfile -Command "(Get-Command java -ErrorAction SilentlyContinue).Source"`) DO (
    IF NOT "%%J"=="" SET JAVA_EXE=%%J
  )
)

:JAVA_FOUND

"%JAVA_EXE%" -Dmaven.multiModuleProjectDirectory=%MAVEN_PROJECTBASEDIR% -classpath "%WRAPPER_JAR%" org.apache.maven.wrapper.MavenWrapperMain %*
ENDLOCAL
