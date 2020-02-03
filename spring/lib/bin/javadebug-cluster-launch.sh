#!/usr/bin/env bash

#  Date      : 2019-04-29  00:30
#  Auth      : Hu Jian
#  Version   : 1.0
#  Function  : 用于调试多个目标JVM，前提是同一个命令向集群中的所有机器执行


# the base dir
# bin/.....
EXECUTE_BASE_DIR="$( cd "$( dirname "$0" )" && pwd )"

function usage() {

 echo "
       java-debug-cluster-console usage:
       javadebug-cluster-launch.sh [ip:port,ip:port,ip,port]

       ip:port : the target jvm host info, optional

       example:
          ./javadebug-cluster-launch.sh 127.0.0.1:11234,11235
 "


}

# using io.javadebug.core.TcpClient
#
# connect to remote jvm tcpServer to exec debug.
function execConsole() {

   # show the usage
   usage

   JAVA_COMMAND="java"
     if [ ! -z ${JAVA_VERSION} ]; then
        JAVA_COMMAND="/usr/local/${JAVA_VERSION}/bin/java"
     fi

   # run tcpClient

   ${JAVA_COMMAND} \
       -cp ${EXECUTE_BASE_DIR}/lib/debug-core.jar \
       io.javadebug.core.JavaDebugClusterClientLauncher \
       -p "${@}" \

}

execConsole "${@}"