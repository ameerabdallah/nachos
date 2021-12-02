#!/usr/bin/env fish

BASEDIR=$(dirname "$0")
cd test
make
cd ..
cd proj2
make