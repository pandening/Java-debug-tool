#!/usr/bin/env bash

#  Date      : 2019-04-18  23:00
#  Auth      : Hu Jian
#  Version   : 1.0
#  Function  : 用于调试目标JVM的客户端，请在本机启动该脚本连接远程JVM进行调试


# the base dir
# bin/.....
EXECUTE_BASE_DIR="$( cd "$( dirname "$0" )" && pwd )"

# define the default ip
DEFAULT_TARGET_IP=127.0.0.1

# define the default port
DEFAULT_TARGET_PORT=11234

# java version
JAVA_VERSION=java8

function usage() {

 echo "
       java-debug-console usage:
       javadebug-console-launch.sh [ip:port]

       ip:port : the target jvm host info, optional

       example:
          ./javadebug-console-launch.sh 127.0.0.1:11234
 "


}
# parse the argument
function parse_arguments()
{

    TARGET_IP=$(echo ${1}|awk '/^[0-9]*@/;/^[^@]+:/{print "@"$1};/^[0-9]+$/'|awk -F "@|:" '{print $2}');
    TARGET_PORT=$(echo ${1}|awk '/^[0-9]*@/;/^[^@]+:/{print "@"$1};/^[0-9]+$/'|awk -F ":"   '{print $2}');

#    # check pid
#    if [ -z ${TARGET_IP} ];then
#        echo "illegal arguments, the <ip> is required."
#        exit 1
#    fi
#
#    # check pid
#    if [ -z ${TARGET_PORT} ];then
#        echo "illegal arguments, the <port> is required."
#        exit 1
#    fi

    # set default ip
    [[ -z ${TARGET_IP} ]] && TARGET_IP=${DEFAULT_TARGET_IP}

    # set default port
    [[ -z ${TARGET_PORT} ]] && TARGET_PORT=${DEFAULT_TARGET_PORT}

    return 0

}

# using io.javadebug.core.TcpClient
#
# connect to remote jvm tcpServer to exec debug.
function execConsole() {

   # show the usage
   usage

   echo "start to connect to remote jvm tcp server ${TARGET_IP}:${TARGET_PORT}"

   parse_arguments "${@}"

   JAVA_COMMAND="/usr/local/${JAVA_VERSION}/bin/java"

   # run tcpClient

   ${JAVA_COMMAND} \
       -cp ${EXECUTE_BASE_DIR}/lib/debug-core.jar \
       io.javadebug.core.JavaDebugClientLauncher \
       -ip ${TARGET_IP} \
       -port ${TARGET_PORT}

}

execConsole "${@}"