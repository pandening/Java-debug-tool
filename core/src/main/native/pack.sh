#!/usr/bin/env bash

#
# info log with green color
#
function info() {
    time=$(date "+%Y-%m-%d %H:%M:%S")
    echo -e "\033[32m${time} [INFO] $1 \033[0m"
}

#
# error log with red color
#
function error() {
    time=$(date "+%Y-%m-%d %H:%M:%S")
    echo -e "\033[31m${time} [ERROR] $1 \033[0m"
}

#
#  pack to libcuptime.dylib
#
function pack_for_mac() {

  # check the gcc
  if [[ ! -x "/usr/bin/gcc" ]]; then
        error "gcc is require."
        exit 100
  fi
  info "using gcc : /usr/bin/gcc"

   # actual work
   gcc -c cputime.cpp
   gcc -dynamiclib -o libcputime.dylib cputime.o

   # move the target
   info "success to pack for mac, move to resource"

   BASEDIR="$( cd "$( dirname "$0" )" && pwd )"
   info "current dir: ${BASEDIR}"

   DYLIB_TARGET_DIR=${BASEDIR}/../resources
   info "the dylib target dir is: ${DYLIB_TARGET_DIR}"

   mv libcputime.dylib ${DYLIB_TARGET_DIR}/libcputime.dylib

   info "success to move the dylib to target dir: ${DYLIB_TARGET_DIR}/libcputime.dylib"

   # clean object file
   rm cputime.o

}

#
#  pack to libcuptime.so
#
function pack_for_linux() {

  # check the gcc
  if [[ ! -x "/usr/bin/gcc" ]]; then
        error "gcc is require."
        exit 100
  fi
  info "using gcc : /usr/bin/gcc"

   # actual work
   gcc -c cputime.cpp
   gcc -dynamiclib -o libcputime.so cputime.o

   # move the target
   info "success to pack for linux, move to resource"

   BASEDIR="$( cd "$( dirname "$0" )" && pwd )"
   info "current dir: ${BASEDIR}"

   DYLIB_TARGET_DIR=${BASEDIR}/../resources
   info "the dylib target dir is: ${DYLIB_TARGET_DIR}"

   mv libcputime.so ${DYLIB_TARGET_DIR}/libcputime.so

   info "success to move the dylib to target dir: ${DYLIB_TARGET_DIR}/libcputime.so"

   # clean object file
   rm cputime.o
}

# do the  pack work
function pack_entrance() {

    os=`uname -a`
    if [[ ${os} =~ "Darwin" ]]; then
        error "pack for maxOS"
        # pack for mac = > dylib
        pack_for_mac
    else
        error "pack for linux"
        # pack for linux => so
        pack_for_linux
    fi

}

# the pack entrance
pack_entrance