#!/usr/bin/env bash

#  Date      : 2019-11-24  19:31
#  Auth      : Hu Jian
#  Version   : 1.0
#  Function  : 用于和目标JVM通信的http服务端，可以将命令通过http的方式进行调试

# the base dir
# bin/.....
EXECUTE_BASE_DIR="$( cd "$( dirname "$0" )" && pwd )"

REMOTE_IP="127.0.0.1"

REMOTE_PORT=11234

HTTP_SERVER_LISTEN_PORT=10234

JAVA_COMMAND="java"


#
#  for input some command like mt contains "#", you should
#  convert it to %23, the map table like this:
#  =======================
#  character | trans
#  =======================
#  space     %20
#  "         %22
#  #         %23
#  %         %25
#  &         %26
#  (         %28
#  )         %29
#  +         %2B
#  ,         %2C
#  /         %2F
#  :         %3A
#  ;         %3B
#  <         %3C
#  =         %3D
#  >         %3E
#  ?         %3F
#  @         %40
#  \         %5C
#  |         %7C 
# =======================
#

#
#  to split the ct command's result, using grep,awk command to split the origin result
#  is so easy like this:
#  cat result.txt | grep -E ".*;.*;.*" | awk -F ';' '{print $1,$2,$3,$4}'
#
#

#
#  $1 => remote ip
#  $2 => remote port
#  $3 => http server listen port
#
function http_server() {

   if [[ -n $1 ]]; then
        REMOTE_IP=$1
   fi

   if [[ -n $2 ]]; then
        REMOTE_PORT=$2
   fi

   if [[ -n $3 ]]; then
        HTTP_SERVER_LISTEN_PORT=$3
   fi

   echo "get the remote address : ${REMOTE_IP}:${REMOTE_PORT},
            set up the http server on localhost:${HTTP_SERVER_LISTEN_PORT},
             Open your web browser and navigate to
             http://127.0.0.1:${HTTP_SERVER_LISTEN_PORT}/java-debug-tool/shell/http/"

      ${JAVA_COMMAND} \
       -cp ${EXECUTE_BASE_DIR}/lib/debug-core.jar \
       io.javadebug.core.JavaDebugHttpServerLauncher \
       ${REMOTE_IP} \
       ${REMOTE_PORT} \
       ${HTTP_SERVER_LISTEN_PORT} \
       &

}


# set up the http server
http_server "${@}"
