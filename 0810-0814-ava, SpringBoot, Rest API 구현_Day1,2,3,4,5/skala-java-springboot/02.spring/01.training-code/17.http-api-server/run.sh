#!/bin/bash

javac -d out $(find sk -name "*.java") && echo BUILD_OK
java -cp out sk.skala.com.httpserver.Main
