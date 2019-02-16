#!/bin/bash

err_report() {
	echo "Error on line $1" >&2
	exit 1
}

trap 'err_report $LINENO' ERR

javac -classpath lib/*.jar src/Vision.java
mkdir -p bin
mv src/*.class bin
