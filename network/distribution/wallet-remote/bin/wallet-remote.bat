@REM wallet-remote launcher script
@REM
@REM Environment:
@REM JAVA_HOME - location of a JDK home dir (optional if java on path)
@REM CFG_OPTS  - JVM options (optional)
@REM Configuration:
@REM WALLET_REMOTE_config.txt found in the WALLET_REMOTE_HOME.
@setlocal enabledelayedexpansion
@setlocal enableextensions

@echo off


if "%WALLET_REMOTE_HOME%"=="" (
  set "APP_HOME=%~dp0\\.."

  rem Also set the old env name for backwards compatibility
  set "WALLET_REMOTE_HOME=%~dp0\\.."
) else (
  set "APP_HOME=%WALLET_REMOTE_HOME%"
)

set "APP_LIB_DIR=%APP_HOME%\lib\"

rem Detect if we were double clicked, although theoretically A user could
rem manually run cmd /c
for %%x in (!cmdcmdline!) do if %%~x==/c set DOUBLECLICKED=1

rem FIRST we load the config file of extra options.
set "CFG_FILE=%APP_HOME%\WALLET_REMOTE_config.txt"
set CFG_OPTS=
call :parse_config "%CFG_FILE%" CFG_OPTS

rem We use the value of the JAVA_OPTS environment variable if defined, rather than the config.
set _JAVA_OPTS=%JAVA_OPTS%
if "!_JAVA_OPTS!"=="" set _JAVA_OPTS=!CFG_OPTS!

rem We keep in _JAVA_PARAMS all -J-prefixed and -D-prefixed arguments
rem "-J" is stripped, "-D" is left as is, and everything is appended to JAVA_OPTS
set _JAVA_PARAMS=
set _APP_ARGS=

set "APP_CLASSPATH=%APP_LIB_DIR%\ru.sberbank.blockchainlab.cnft.wallet-remote-4.1.0-SNAPSHOT.jar;%APP_LIB_DIR%\ru.sberbank.blockchainlab.cnft.http-service-server-4.1.0-SNAPSHOT.jar;%APP_LIB_DIR%\ru.sberbank.blockchainlab.cnft.utility_jvm-4.1.0-SNAPSHOT.jar;%APP_LIB_DIR%\ru.sberbank.blockchainlab.cnft.types_jvm-4.1.0-SNAPSHOT.jar;%APP_LIB_DIR%\ru.sberbank.blockchainlab.cnft.commons_jvm-4.1.0-SNAPSHOT.jar;%APP_LIB_DIR%\ru.sberbank.blockchainlab.cnft.http_service_meta_jvm-4.1.0-SNAPSHOT.jar;%APP_LIB_DIR%\ru.sberbank.blockchainlab.cnft.scalapb_codec_jvm-4.1.0-SNAPSHOT.jar;%APP_LIB_DIR%\ru.sberbank.blockchainlab.cnft.contract-spec-4.1.0-SNAPSHOT.jar;%APP_LIB_DIR%\ru.sberbank.blockchainlab.cnft.codecs-4.1.0-SNAPSHOT.jar;%APP_LIB_DIR%\ru.sberbank.blockchainlab.cnft.wallet_lib_jvm-4.1.0-SNAPSHOT.jar;%APP_LIB_DIR%\ru.sberbank.blockchainlab.cnft.gate_spec_jvm-4.1.0-SNAPSHOT.jar;%APP_LIB_DIR%\ru.sberbank.blockchainlab.cnft.spec_jvm-4.1.0-SNAPSHOT.jar;%APP_LIB_DIR%\ru.sberbank.blockchainlab.cnft.model_jvm-4.1.0-SNAPSHOT.jar;%APP_LIB_DIR%\ru.sberbank.blockchainlab.cnft.cryptography_jvm-4.1.0-SNAPSHOT.jar;%APP_LIB_DIR%\ru.sberbank.blockchainlab.cnft.cryptography_model_jvm-4.1.0-SNAPSHOT.jar;%APP_LIB_DIR%\ru.sberbank.blockchainlab.cnft.http_service_client_jvm-4.1.0-SNAPSHOT.jar;%APP_LIB_DIR%\ru.sberbank.blockchainlab.cnft.wallet_lib_model_jvm-4.1.0-SNAPSHOT.jar;%APP_LIB_DIR%\org.scala-lang.scala-library-2.12.10.jar;%APP_LIB_DIR%\org.slf4j.slf4j-api-1.7.30.jar;%APP_LIB_DIR%\ch.qos.logback.logback-classic-1.2.3.jar;%APP_LIB_DIR%\org.eclipse.jetty.jetty-server-11.0.1.jar;%APP_LIB_DIR%\org.eclipse.jetty.jetty-servlet-11.0.1.jar;%APP_LIB_DIR%\org.eclipse.jetty.jetty-servlets-11.0.1.jar;%APP_LIB_DIR%\com.lihaoyi.upickle_2.12-1.3.8.jar;%APP_LIB_DIR%\com.thesamet.scalapb.scalapb-runtime_2.12-0.11.0.jar;%APP_LIB_DIR%\com.google.code.gson.gson-2.7.jar;%APP_LIB_DIR%\io.grpc.grpc-protobuf-1.9.0.jar;%APP_LIB_DIR%\io.grpc.grpc-stub-1.9.0.jar;%APP_LIB_DIR%\io.grpc.grpc-netty-1.9.0.jar;%APP_LIB_DIR%\io.grpc.grpc-netty-shaded-1.9.0.jar;%APP_LIB_DIR%\io.netty.netty-tcnative-boringssl-static-2.0.7.Final.jar;%APP_LIB_DIR%\org.scala-js.scalajs-stubs_2.12-1.0.0.jar;%APP_LIB_DIR%\io.getquill.quill-core_2.12-3.7.0.jar;%APP_LIB_DIR%\org.postgresql.postgresql-42.2.8.jar;%APP_LIB_DIR%\io.getquill.quill-jdbc_2.12-3.7.0.jar;%APP_LIB_DIR%\com.zaxxer.HikariCP-3.4.5.jar;%APP_LIB_DIR%\ch.qos.logback.logback-core-1.2.3.jar;%APP_LIB_DIR%\org.scala-lang.scala-reflect-2.12.10.jar;%APP_LIB_DIR%\org.eclipse.jetty.toolchain.jetty-jakarta-servlet-api-5.0.2.jar;%APP_LIB_DIR%\org.eclipse.jetty.jetty-http-11.0.1.jar;%APP_LIB_DIR%\org.eclipse.jetty.jetty-io-11.0.1.jar;%APP_LIB_DIR%\org.eclipse.jetty.jetty-security-11.0.1.jar;%APP_LIB_DIR%\org.eclipse.jetty.jetty-util-11.0.1.jar;%APP_LIB_DIR%\com.lihaoyi.ujson_2.12-1.3.8.jar;%APP_LIB_DIR%\com.lihaoyi.upack_2.12-1.3.8.jar;%APP_LIB_DIR%\com.lihaoyi.upickle-implicits_2.12-1.3.8.jar;%APP_LIB_DIR%\com.thesamet.scalapb.lenses_2.12-0.11.0.jar;%APP_LIB_DIR%\com.google.protobuf.protobuf-java-3.15.6.jar;%APP_LIB_DIR%\org.scala-lang.modules.scala-collection-compat_2.12-2.4.2.jar;%APP_LIB_DIR%\org.apache.httpcomponents.httpclient-4.5.13.jar;%APP_LIB_DIR%\org.bouncycastle.bcprov-jdk15on-1.60.jar;%APP_LIB_DIR%\org.bouncycastle.bcpkix-jdk15on-1.60.jar;%APP_LIB_DIR%\org.scala-lang.modules.scala-xml_2.12-1.0.6.jar;%APP_LIB_DIR%\io.grpc.grpc-core-1.9.0.jar;%APP_LIB_DIR%\com.google.guava.guava-19.0.jar;%APP_LIB_DIR%\com.google.protobuf.protobuf-java-util-3.5.1.jar;%APP_LIB_DIR%\com.google.api.grpc.proto-google-common-protos-1.0.0.jar;%APP_LIB_DIR%\io.grpc.grpc-protobuf-lite-1.9.0.jar;%APP_LIB_DIR%\io.netty.netty-codec-http2-4.1.17.Final.jar;%APP_LIB_DIR%\io.netty.netty-handler-proxy-4.1.17.Final.jar;%APP_LIB_DIR%\io.getquill.quill-core-portable_2.12-3.7.0.jar;%APP_LIB_DIR%\com.lihaoyi.pprint_2.12-0.5.5.jar;%APP_LIB_DIR%\com.typesafe.config-1.4.1.jar;%APP_LIB_DIR%\com.typesafe.scala-logging.scala-logging_2.12-3.9.2.jar;%APP_LIB_DIR%\io.getquill.quill-sql_2.12-3.7.0.jar;%APP_LIB_DIR%\com.lihaoyi.upickle-core_2.12-1.3.8.jar;%APP_LIB_DIR%\org.apache.httpcomponents.httpcore-4.4.13.jar;%APP_LIB_DIR%\commons-logging.commons-logging-1.2.jar;%APP_LIB_DIR%\commons-codec.commons-codec-1.11.jar;%APP_LIB_DIR%\io.grpc.grpc-context-1.9.0.jar;%APP_LIB_DIR%\com.google.errorprone.error_prone_annotations-2.1.2.jar;%APP_LIB_DIR%\com.google.code.findbugs.jsr305-3.0.0.jar;%APP_LIB_DIR%\com.google.instrumentation.instrumentation-api-0.4.3.jar;%APP_LIB_DIR%\io.opencensus.opencensus-api-0.10.0.jar;%APP_LIB_DIR%\io.opencensus.opencensus-contrib-grpc-metrics-0.10.0.jar;%APP_LIB_DIR%\io.netty.netty-codec-http-4.1.17.Final.jar;%APP_LIB_DIR%\io.netty.netty-handler-4.1.17.Final.jar;%APP_LIB_DIR%\io.netty.netty-transport-4.1.17.Final.jar;%APP_LIB_DIR%\io.netty.netty-codec-socks-4.1.17.Final.jar;%APP_LIB_DIR%\com.twitter.chill_2.12-0.9.5.jar;%APP_LIB_DIR%\io.suzaku.boopickle_2.12-1.3.1.jar;%APP_LIB_DIR%\com.lihaoyi.fansi_2.12-0.2.7.jar;%APP_LIB_DIR%\com.lihaoyi.sourcecode_2.12-0.1.7.jar;%APP_LIB_DIR%\io.getquill.quill-sql-portable_2.12-3.7.0.jar;%APP_LIB_DIR%\com.github.vertical-blank.scala-sql-formatter_2.12-1.0.1.jar;%APP_LIB_DIR%\com.lihaoyi.geny_2.12-0.6.6.jar;%APP_LIB_DIR%\io.netty.netty-codec-4.1.17.Final.jar;%APP_LIB_DIR%\io.netty.netty-buffer-4.1.17.Final.jar;%APP_LIB_DIR%\io.netty.netty-resolver-4.1.17.Final.jar;%APP_LIB_DIR%\com.twitter.chill-java-0.9.5.jar;%APP_LIB_DIR%\com.esotericsoftware.kryo-shaded-4.0.2.jar;%APP_LIB_DIR%\org.apache.xbean.xbean-asm7-shaded-4.15.jar;%APP_LIB_DIR%\com.github.vertical-blank.sql-formatter-1.0.jar;%APP_LIB_DIR%\io.netty.netty-common-4.1.17.Final.jar;%APP_LIB_DIR%\com.esotericsoftware.minlog-1.3.0.jar;%APP_LIB_DIR%\org.objenesis.objenesis-2.5.1.jar"
set "APP_MAIN_CLASS=ru.sberbank.blockchain.cnft.wallet.remote.CNFTWalletRemoteMain"
set "SCRIPT_CONF_FILE=%APP_HOME%\conf\application.ini"

rem Bundled JRE has priority over standard environment variables
if defined BUNDLED_JVM (
  set "_JAVACMD=%BUNDLED_JVM%\bin\java.exe"
) else (
  if "%JAVACMD%" neq "" (
    set "_JAVACMD=%JAVACMD%"
  ) else (
    if "%JAVA_HOME%" neq "" (
      if exist "%JAVA_HOME%\bin\java.exe" set "_JAVACMD=%JAVA_HOME%\bin\java.exe"
    )
  )
)

if "%_JAVACMD%"=="" set _JAVACMD=java

rem Detect if this java is ok to use.
for /F %%j in ('"%_JAVACMD%" -version  2^>^&1') do (
  if %%~j==java set JAVAINSTALLED=1
  if %%~j==openjdk set JAVAINSTALLED=1
)

rem BAT has no logical or, so we do it OLD SCHOOL! Oppan Redmond Style
set JAVAOK=true
if not defined JAVAINSTALLED set JAVAOK=false

if "%JAVAOK%"=="false" (
  echo.
  echo A Java JDK is not installed or can't be found.
  if not "%JAVA_HOME%"=="" (
    echo JAVA_HOME = "%JAVA_HOME%"
  )
  echo.
  echo Please go to
  echo   http://www.oracle.com/technetwork/java/javase/downloads/index.html
  echo and download a valid Java JDK and install before running wallet-remote.
  echo.
  echo If you think this message is in error, please check
  echo your environment variables to see if "java.exe" and "javac.exe" are
  echo available via JAVA_HOME or PATH.
  echo.
  if defined DOUBLECLICKED pause
  exit /B 1
)

rem if configuration files exist, prepend their contents to the script arguments so it can be processed by this runner
call :parse_config "%SCRIPT_CONF_FILE%" SCRIPT_CONF_ARGS

call :process_args %SCRIPT_CONF_ARGS% %%*

set _JAVA_OPTS=!_JAVA_OPTS! !_JAVA_PARAMS!

if defined CUSTOM_MAIN_CLASS (
    set MAIN_CLASS=!CUSTOM_MAIN_CLASS!
) else (
    set MAIN_CLASS=!APP_MAIN_CLASS!
)

rem Call the application and pass all arguments unchanged.
"%_JAVACMD%" !_JAVA_OPTS! !WALLET_REMOTE_OPTS! -cp "%APP_CLASSPATH%" %MAIN_CLASS% !_APP_ARGS!

@endlocal

exit /B %ERRORLEVEL%


rem Loads a configuration file full of default command line options for this script.
rem First argument is the path to the config file.
rem Second argument is the name of the environment variable to write to.
:parse_config
  set _PARSE_FILE=%~1
  set _PARSE_OUT=
  if exist "%_PARSE_FILE%" (
    FOR /F "tokens=* eol=# usebackq delims=" %%i IN ("%_PARSE_FILE%") DO (
      set _PARSE_OUT=!_PARSE_OUT! %%i
    )
  )
  set %2=!_PARSE_OUT!
exit /B 0


:add_java
  set _JAVA_PARAMS=!_JAVA_PARAMS! %*
exit /B 0


:add_app
  set _APP_ARGS=!_APP_ARGS! %*
exit /B 0


rem Processes incoming arguments and places them in appropriate global variables
:process_args
  :param_loop
  call set _PARAM1=%%1
  set "_TEST_PARAM=%~1"

  if ["!_PARAM1!"]==[""] goto param_afterloop


  rem ignore arguments that do not start with '-'
  if "%_TEST_PARAM:~0,1%"=="-" goto param_java_check
  set _APP_ARGS=!_APP_ARGS! !_PARAM1!
  shift
  goto param_loop

  :param_java_check
  if "!_TEST_PARAM:~0,2!"=="-J" (
    rem strip -J prefix
    set _JAVA_PARAMS=!_JAVA_PARAMS! !_TEST_PARAM:~2!
    shift
    goto param_loop
  )

  if "!_TEST_PARAM:~0,2!"=="-D" (
    rem test if this was double-quoted property "-Dprop=42"
    for /F "delims== tokens=1,*" %%G in ("!_TEST_PARAM!") DO (
      if not ["%%H"] == [""] (
        set _JAVA_PARAMS=!_JAVA_PARAMS! !_PARAM1!
      ) else if [%2] neq [] (
        rem it was a normal property: -Dprop=42 or -Drop="42"
        call set _PARAM1=%%1=%%2
        set _JAVA_PARAMS=!_JAVA_PARAMS! !_PARAM1!
        shift
      )
    )
  ) else (
    if "!_TEST_PARAM!"=="-main" (
      call set CUSTOM_MAIN_CLASS=%%2
      shift
    ) else (
      set _APP_ARGS=!_APP_ARGS! !_PARAM1!
    )
  )
  shift
  goto param_loop
  :param_afterloop

exit /B 0
