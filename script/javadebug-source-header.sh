#!/usr/bin/env bash

#  Date      : 2019-04-18  23:00
#  Auth      : Hu Jian
#  Version   : 1.0
#  Function  : 用于给java文件加头

#
# get指定的文件加上文件头
function appendFileHeader() {
   FILE_PATH=$1
   if [[ -f ${FILE_PATH} ]]; then
     if [[ ${FILE_PATH} == *.java ]]; then
       if [[ `grep -c "Copyright" ${FILE_PATH}` -eq '0' ]]; then
          (echo '0a'; (cat ../header); echo '.'; echo 'wq') | ed -s ${FILE_PATH}
          sed -i "" "s/mailto:hujian06@meituan.com/H.J/g" ${FILE_PATH}
       fi
     fi
   else
     echo "the file path is empty, skip this file."
   fi
}

#
# 循环遍历当前目录下的文件，给java文件加上文件头
function scanJavaFileAndAppendHeader() {
   echo "scan:${1}"
   CUR_DIR=$1
   if [[ -f ${CUR_DIR} ]]; then
     # this is a file
     appendFileHeader ${CUR_DIR}
   elif [[ -d ${CUR_DIR} ]]; then
     # this is a directory
     for f in ${CUR_DIR}/*
     do
       scanJavaFileAndAppendHeader ${f}
     done
   fi
}

# 执行文件头替换

BASE_DIR="$( cd "$( dirname "$0" )" && pwd )"

# 不要重复执行

time scanJavaFileAndAppendHeader ${BASE_DIR}/../