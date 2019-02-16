#!/bin/bash

err_report() {
	echo "Error on line $1" >&2
	exit 1;
}

trap 'err_report $LINENO' ERR

if [ -e bin/Vision.class ]; then
	trash bin/Vision.class
fi
