#!/usr/bin/env bash

#  Date      : 2019-04-21  22:50
#  Auth      : Hu Jian
#  Version   : 1.0
#  Function  : 仅用来解压core jar包，名字不要乱改

BASEDIR="$( cd "$( dirname "$0" )" && pwd )"

#
#  解压core jar包至指定目录
#
function unpack() {
   JAR_PATH=${BASEDIR}/../bin/lib/debug-core.jar
   TARGET_DIR=${BASEDIR}/../bin/jar
   if [ ! -f ${JAR_PATH} ]; then
        echo "" # do not speak
   else
        if [ ! -d ${TARGET_DIR} ]; then
            mkdir ${TARGET_DIR}
        else
            rm -rf ${TARGET_DIR}
        fi
        unzip ${JAR_PATH} -d ${TARGET_DIR}
        echo ${TARGET_DIR}
   fi
}

# unpack
unpack



