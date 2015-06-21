#!/bin/bash

echo "Downloading Apache Commons Codec"
wget -O apache-codec.tgz http://mirror.cogentco.com/pub/apache//commons/codec/binaries/commons-codec-1.10-bin.tar.gz
tar xvf apache-codec.tgz
rm -v apache-codec.tgz

echo "Downloading Apache Commons Validation"
wget -O apache-valiator.tgz http://mirrors.gigenet.com/apache//commons/validator/binaries/commons-validator-1.4.1-bin.tar.gz
tar xvf apache-valiator.tgz
rm -v apache-valiator.tgz

echo "Downloading OpenCSV"
wget -O opencsv.jar http://sourceforge.net/projects/opencsv/files/latest/download
