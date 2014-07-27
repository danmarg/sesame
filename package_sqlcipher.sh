#!/bin/bash

if [ -d lib ]; then
  echo "lib directory already exists!"
  exit
fi

mkdir lib
cp -r SesameApp/libs/armeabi lib
zip -r armeabi.jar lib
mv -i armeabi.jar SesameApp/native-lib/armeabi.jar

rm -R -f lib/*
cp -r SesameApp/libs/x86 lib
zip -r x86.jar lib
mv -i x86.jar SesameApp/native-lib/x86.jar
