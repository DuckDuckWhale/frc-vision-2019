#!/bin/bash

err_report() {
	echo "Error on line $1" >&2
}

trap 'err_report $LINENO' ERR

javac -classpath lib/*.jar Vision.java
mkdir -p bin
mv Vision.class bin
