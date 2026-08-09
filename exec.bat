@echo off

:: SAP CPIC_TRACE
set CPIC_TRACE=3
set CPIC_TRACE_DIR=%~dp0cpic-trace

:: Jco Trace
set JCO_TRACE_DIR=%~dp0jco-trace

mkdir %CPIC_TRACE_DIR% >NUL 2>&1
mkdir %JCO_TRACE_DIR% >NUL 2>&1

javac -encoding UTF-8 -cp sapjco3.jar StfcConnectionSnc.java

java --enable-native-access=ALL-UNNAMED  -Djava.library.path=%dp~0 ^
  -Djco.trace_level=10 ^
  -Djco.trace_path=%JCO_TRACE_DIR% ^
  -cp ".;sapjco3.jar" ^
  StfcConnectionSnc ^
  %~dp0sap-snc.properties ^
  "Hello SAP via SNC"

pause