#!/usr/bin/env bash

#  Date      : 2019-04-18  23:00
#  Auth      : Hu Jian
#  Version   : 1.0
#  Function  : 用于挂载到目标jvm上，该命令仅挂载到jvm，并且打开tcpServer，要求将
#              java安装到 /usr/local/${JAVA_VERSION}目录下，${JAVA_VERSION}表示
#              目标jvm运行时使用的java版本，不要随意使用该脚本，通用attach脚本是同目录下
#              的 javadebug-agent-launch.sh

# the base dir
# bin/.....
EXECUTE_BASE_DIR="$( cd "$( dirname "$0" )" && pwd )"

# define default target ip
DEFAULT_TARGET_IP="127.0.0.1"

# define default target port
DEFAULT_TARGET_PORT="11234"

# the java version
JAVA_VERSION="java8"

# parse the argument
function parse_arguments()
{

    TARGET_PID=$(echo ${1}|awk '/^[0-9]*@/;/^[^@]+:/{print "@"$1};/^[0-9]+$/'|awk -F "@"   '{print $1}');
    TARGET_IP=$(echo ${1}|awk '/^[0-9]*@/;/^[^@]+:/{print "@"$1};/^[0-9]+$/'|awk -F "@|:" '{print $2}');
    TARGET_PORT=$(echo ${1}|awk '/^[0-9]*@/;/^[^@]+:/{print "@"$1};/^[0-9]+$/'|awk -F ":"   '{print $2}');

    # check pid
    if [[ -z ${TARGET_PID} ]];then
        echo "illegal arguments, the <PID> is required." 1>&2
        exit 1
    fi

    # set default ip
    [[ -z ${TARGET_IP} ]] && TARGET_IP=${DEFAULT_TARGET_IP}

    # set default port
    [[ -z ${TARGET_PORT} ]] && TARGET_PORT=${DEFAULT_TARGET_PORT}

    return 0

}

# attach to target jvm, call this function after
# parse the input param.
function attachTargetJvm() {

   echo "start to attach target jvm ${TARGET_PID} ${TARGET_IP}:${TARGET_PORT}"

   if [[ ! -z ${TARGET_PID} ]]; then
      JAVA_COMMAND="/usr/local/${JAVA_VERSION}/bin/java"
   fi

   # tools jar path
   BOOT_CLASSPATH=-Xbootclasspath/a:/usr/local/${JAVA_VERSION}/lib/tools.jar

   # show the version
   ${JAVA_COMMAND} -version

   # run the agent
   ${JAVA_COMMAND} ${BOOT_CLASSPATH} \
       -jar     ${EXECUTE_BASE_DIR}/lib/debug-core.jar \
       -pid     ${TARGET_PID} \
       -ip      ${TARGET_IP} \
       -port    ${TARGET_PORT} \
       -core    "${EXECUTE_BASE_DIR}/lib/debug-core.jar" \
       -agent   "${EXECUTE_BASE_DIR}/lib/debug-agent.jar" \

}

# using io.javadebug.core.TcpClient
#
# connect to remote jvm tcpServer to exec debug.
function execConsole() {

   echo "start to connect to remote jvm tcp server ${TARGET_IP}:${TARGET_PORT}"

   #  telnet ${TARGET_IP} ${TARGET_PORT}

      #JAVA_COMMAND="java"
      JAVA_COMMAND="/usr/local/${JAVA_VERSION}/bin/java"

   # run tcpClient

   ${JAVA_COMMAND} \
       -cp ${EXECUTE_BASE_DIR}/lib/debug-core.jar \
       io.javadebug.core.JavaDebugClientLauncher \
       -ip ${TARGET_IP} \
       -port ${TARGET_PORT}

}

function main() {

    # check permission
    [[ ! -w ${EXECUTE_BASE_DIR} ]] \
        && echo "you have no permission to write the dir:${EXECUTE_BASE_DIR}" | exit 1

    # parse input params
    parse_arguments "${@}"

    # start to attach target jvm
    attachTargetJvm

    # show the logo
    cat ./logo

}

# run the main function
time main "${@}"