@rem
@rem Copyright 2015 the original author or authors.
@rem
@rem Licensed under the Apache License, Version 2.0 (the "License");
@rem you may not use this file except in compliance with the License.
@rem You may obtain a copy of the License at
@rem
@rem      https://www.apache.org/licenses/LICENSE-2.0
@rem
@rem Unless required by applicable law or agreed to in writing, software
@rem distributed under the License is distributed on an "AS IS" BASIS,
@rem WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
@rem See the License for the specific language governing permissions and
@rem limitations under the License.
@rem

@if "%DEBUG%" == "" @echo off
@rem ##########################################################################
@rem
@rem  wavechain startup script for Windows
@rem
@rem ##########################################################################

@rem Set local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" setlocal

set DIRNAME=%~dp0
if "%DIRNAME%" == "" set DIRNAME=.
set APP_BASE_NAME=%~n0
set APP_HOME=%DIRNAME%..

@rem Resolve any "." and ".." in APP_HOME to make it shorter.
for %%i in ("%APP_HOME%") do set APP_HOME=%%~fi

@rem Add default JVM options here. You can also use JAVA_OPTS and WAVECHAIN_OPTS to pass JVM options to this script.
set DEFAULT_JVM_OPTS=

@rem Find java.exe
if defined JAVA_HOME goto findJavaFromJavaHome

set JAVA_EXE=java.exe
%JAVA_EXE% -version >NUL 2>&1
if "%ERRORLEVEL%" == "0" goto execute

echo.
echo ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.
echo.
echo Please set the JAVA_HOME variable in your environment to match the
echo location of your Java installation.

goto fail

:findJavaFromJavaHome
set JAVA_HOME=%JAVA_HOME:"=%
set JAVA_EXE=%JAVA_HOME%/bin/java.exe

if exist "%JAVA_EXE%" goto execute

echo.
echo ERROR: JAVA_HOME is set to an invalid directory: %JAVA_HOME%
echo.
echo Please set the JAVA_HOME variable in your environment to match the
echo location of your Java installation.

goto fail

:execute
@rem Setup the command line

set CLASSPATH=%APP_HOME%\lib\wavechain-1.0-SNAPSHOT.jar;%APP_HOME%\lib\ktor-server-netty-jvm-1.5.0.jar;%APP_HOME%\lib\ktor-locations-jvm-1.5.0.jar;%APP_HOME%\lib\ktor-jackson-jvm-1.5.0.jar;%APP_HOME%\lib\ktor-server-sessions-jvm-1.5.0.jar;%APP_HOME%\lib\ktor-server-host-common-jvm-1.5.0.jar;%APP_HOME%\lib\ktor-auth-jvm-1.5.0.jar;%APP_HOME%\lib\ktor-server-core-jvm-1.5.0.jar;%APP_HOME%\lib\exposed-dao-0.29.1.jar;%APP_HOME%\lib\exposed-jdbc-0.29.1.jar;%APP_HOME%\lib\exposed-core-0.29.1.jar;%APP_HOME%\lib\ktor-client-apache-jvm-1.5.0.jar;%APP_HOME%\lib\ktor-client-core-jvm-1.5.0.jar;%APP_HOME%\lib\jackson-databind-2.12.0.jar;%APP_HOME%\lib\jackson-annotations-2.12.0.jar;%APP_HOME%\lib\jackson-core-2.12.0.jar;%APP_HOME%\lib\jackson-module-kotlin-2.12.0.jar;%APP_HOME%\lib\kotlin-reflect-1.4.21.jar;%APP_HOME%\lib\ktor-http-cio-jvm-1.5.0.jar;%APP_HOME%\lib\ktor-http-jvm-1.5.0.jar;%APP_HOME%\lib\ktor-network-jvm-1.5.0.jar;%APP_HOME%\lib\ktor-utils-jvm-1.5.0.jar;%APP_HOME%\lib\kotlinx-coroutines-jdk8-1.4.2-native-mt.jar;%APP_HOME%\lib\ktor-io-jvm-1.5.0.jar;%APP_HOME%\lib\kotlinx-coroutines-core-jvm-1.4.2-native-mt.jar;%APP_HOME%\lib\clikt-jvm-3.1.0.jar;%APP_HOME%\lib\kotlin-stdlib-jdk8-1.4.21.jar;%APP_HOME%\lib\kotlin-stdlib-jdk7-1.4.21.jar;%APP_HOME%\lib\kotlin-stdlib-1.4.21.jar;%APP_HOME%\lib\bcprov-jdk15on-1.68.jar;%APP_HOME%\lib\logback-classic-1.2.3.jar;%APP_HOME%\lib\Java-WebSocket-1.5.1.jar;%APP_HOME%\lib\sqlite-jdbc-3.34.0.jar;%APP_HOME%\lib\junit-jupiter-5.4.2.jar;%APP_HOME%\lib\BIP39-2019.01.27.jar;%APP_HOME%\lib\kotlin-stdlib-common-1.4.21.jar;%APP_HOME%\lib\annotations-13.0.jar;%APP_HOME%\lib\slf4j-api-1.7.30.jar;%APP_HOME%\lib\netty-codec-http2-4.1.54.Final.jar;%APP_HOME%\lib\alpn-api-1.1.3.v20160715.jar;%APP_HOME%\lib\netty-transport-native-kqueue-4.1.54.Final.jar;%APP_HOME%\lib\netty-transport-native-epoll-4.1.54.Final.jar;%APP_HOME%\lib\logback-core-1.2.3.jar;%APP_HOME%\lib\config-1.3.1.jar;%APP_HOME%\lib\httpasyncclient-4.1.4.jar;%APP_HOME%\lib\httpcore-nio-4.4.13.jar;%APP_HOME%\lib\httpclient-4.5.6.jar;%APP_HOME%\lib\httpcore-4.4.13.jar;%APP_HOME%\lib\junit-jupiter-params-5.4.2.jar;%APP_HOME%\lib\junit-jupiter-engine-5.4.2.jar;%APP_HOME%\lib\junit-jupiter-api-5.4.2.jar;%APP_HOME%\lib\core-1.58.0.0.jar;%APP_HOME%\lib\SHA256-2019.01.27.jar;%APP_HOME%\lib\ToRuntime-2019.01.27.jar;%APP_HOME%\lib\netty-codec-http-4.1.54.Final.jar;%APP_HOME%\lib\netty-handler-4.1.54.Final.jar;%APP_HOME%\lib\netty-codec-4.1.54.Final.jar;%APP_HOME%\lib\netty-transport-native-unix-common-4.1.54.Final.jar;%APP_HOME%\lib\netty-transport-4.1.54.Final.jar;%APP_HOME%\lib\netty-buffer-4.1.54.Final.jar;%APP_HOME%\lib\netty-resolver-4.1.54.Final.jar;%APP_HOME%\lib\netty-common-4.1.54.Final.jar;%APP_HOME%\lib\commons-logging-1.2.jar;%APP_HOME%\lib\junit-platform-engine-1.4.2.jar;%APP_HOME%\lib\junit-platform-commons-1.4.2.jar;%APP_HOME%\lib\apiguardian-api-1.0.0.jar;%APP_HOME%\lib\opentest4j-1.1.1.jar;%APP_HOME%\lib\json-simple-1.1.1.jar;%APP_HOME%\lib\commons-codec-1.10.jar


@rem Execute wavechain
"%JAVA_EXE%" %DEFAULT_JVM_OPTS% %JAVA_OPTS% %WAVECHAIN_OPTS%  -classpath "%CLASSPATH%" io.ktor.server.netty.EngineMain %*

:end
@rem End local scope for the variables with windows NT shell
if "%ERRORLEVEL%"=="0" goto mainEnd

:fail
rem Set variable WAVECHAIN_EXIT_CONSOLE if you need the _script_ return code instead of
rem the _cmd.exe /c_ return code!
if  not "" == "%WAVECHAIN_EXIT_CONSOLE%" exit 1
exit /b 1

:mainEnd
if "%OS%"=="Windows_NT" endlocal

:omega
