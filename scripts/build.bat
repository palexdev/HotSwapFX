@echo off
call jbang export fatjar --force --output=binaries\UpdateChangelogs.jar .\src\UpdateChangelogs.java
call jbang export fatjar --force --output=binaries\Release.jar .\src\Release.java