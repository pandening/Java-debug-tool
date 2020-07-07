#!/usr/bin/env bash

#
# Date          :  2019-11-25 14:33
# Author        :  Pandening
# Version       :  1.0
# Function      :  using wrk to do benchmark, auto collect the benchmark info
#                  by java-debug-tool, the user just provide the target clients
#                  and step increment clients, this shell will auto complete the benchmark;
# Notice        :  first step, you should let the agent launch on the target JVM, then run
#                  this shell to benchmark;
#
#

EXECUTE_BASE_DIR="$( cd "$( dirname "$0" )" && pwd )"

readonly SELF="`basename $0`"

#
#  the remote tcp server host
REMOTE_IP="127.0.0.1"

#
# the remote tcp server listen port
REMOTE_PORT=11234

#
# which port the http will listen on
HTTP_SERVER_LISTEN_PORT=10234

#
# the setup http server shell script
SET_UP_HTTP_SERVER_SHELL=${EXECUTE_BASE_DIR}/javadebug-http-server.sh

#
# the init clients
INIT_CLIENTS=100

#
# the target clients
TARGET_CLIENTS=1000

#
# step by step increment to the target
STEP_INC_CLIENTS=100

#
# the step duration
# unit =>
#    s -> seconds
#    m -> minutes
STEP_DURATION=60s

#
# warm up clients
WARM_UP_CLIENTS=10

#
# warm up times
WARM_UP_SECS=30s

#
# the base url to send the command
BASE_URL=/java-debug-tool/shell/http/

#
# the benchmark url
BENCHMARK_URL=http://localhost:8080/api/perf/v2

#
# the wrk command
WRK_COMMAND=~/benchmark/wrk/./wrk

#
# the benchmark output
WORK_FILE_NAME=benchmark.txt

function info() {
    time=$(date "+%Y-%m-%d %H:%M:%S")
    echo -e "\033[32m${time} [INFO] $1 \033[0m"
}

function error() {
    time=$(date "+%Y-%m-%d %H:%M:%S")
    echo -e "\033[31m${time} [ERROR] $1 \033[0m"
}

#
#  the usage
#
function usage() {
   echo "
      using wrk tool and java-debug-tool to do the benchmark;
      Example:
         ${SELF}
         -b, benchmark url [url] the target benchmark url;
         -i, remote-ip [ip address] the remote java-debug-tool tcp server ip address, the default is localhost;
         -p, remote-port [port number] the remote java-debug-tool tcp server port number, the default is 11234;
         -h, http-port  [http server listen on] the http server listen on, the default is 10234;
         -c, init-clients [an number] the init clients size, the default is 100;
         -t, total-clients [an number] the target clients size, the default is 1000;
         -s, step-clients [an number] the step by step increment clients size, the default is 100;
         -d, duration [an number with unit (s -> seconds| m -> minutes) ref: wrk] step duration;
         -w, wrk-command [the wrk path] the path to the wrk command
         -u, help  print the usage
         -m, warm up clients, default is 10
         -e, [an number with unit (s -> seconds| m -> minutes) ref: wrk] warm up times, default is 30s
         -f, file [the file name] the output file name
   "
   exit 10
}

#
# this function will kill http-server before exit shell
#
function killSelf() {
    lsof -i:${HTTP_SERVER_LISTEN_PORT} | grep "LISTEN" |  awk '{print $2}' | xargs kill -9
}

#
#  parse the benchmark data
#
function parse_benchmark_result() {

    awk 'BEGIN {
             print "connecton qps avg_cost_ms per_cpu cpu_usage usr_usage sys_usage";
                     connection="";qps="";avg_cost="";per_cpu="";cpu_usage="";usr_usage="";sys_usage="";

                 }

                    {    if (match($0, "threads and")) {connection=$4};
                         if (match($0,"Requests/sec:")){
                         qps=$2;
                         };

                         if(match($0,"Latency   ")){avg_cost=substr($2,0,length($2)-2)};

                         if(match($0,"Total cpu usage :")){cpu_usage = $5};

                         if(match($0,"time;usr_ms;sys_ms;"))
                         {
                           getline;
                           split($0,arr,";");
                           usr_usage=arr[4];
                           sys_usage=arr[5];
                           cpu_usage=arr[4] + arr[5];
                        };

                         if(connection != "" && qps != "" && avg_cost != "" && cpu_usage != "" && usr_usage != "" && sys_usage != "")
                                 {printf "%s %s %s %.2f %s %s %s\n", connection, qps, avg_cost, cpu_usage/qps, cpu_usage, usr_usage, sys_usage
                                     connection="";qps="";avg_cost="";per_cpu="";cpu_usage="";usr_usage="";sys_usage="";
                     }

                 }
         ' $*

}

#
#  start to benchmark
#
function benchmark() {

   echo "start to setup the http server on port : ${HTTP_SERVER_LISTEN_PORT}"

   sh ${SET_UP_HTTP_SERVER_SHELL} ${REMOTE_IP} ${REMOTE_PORT} ${HTTP_SERVER_LISTEN_PORT}

   seq=1
   LEFT_CLIENTS=`expr ${TARGET_CLIENTS} - ${INIT_CLIENTS}`
   total_circle=`expr ${LEFT_CLIENTS} / ${STEP_INC_CLIENTS}`
   echo "start benchmark, init clients ${INIT_CLIENTS}, step increment clients ${STEP_INC_CLIENTS} with ${total_circle} times
    each round hold ${STEP_DURATION}"

    benchmark_url="http://127.0.0.1:${HTTP_SERVER_LISTEN_PORT}${BASE_URL}?command=ct%20-o%20csv"
    echo "the benchmark data info url is : ${benchmark_url}"

    # warm up
    error "try to warm-up target jvm with connection: ${WARM_UP_CLIENTS} , hold on ${WARM_UP_SECS}"
    ${WRK_COMMAND} -t1 -c${WARM_UP_CLIENTS} -L -d${WARM_UP_SECS} ${BENCHMARK_URL}
    error "warm-up target jvm done, start to do benchmark now"
    sleep 20

    echo "" > ${WORK_FILE_NAME}
    local seq=0
    clients=${INIT_CLIENTS}
    for ((times = 0; times <= ${total_circle}; times ++)) do
       dalt=`expr ${times} \* ${STEP_INC_CLIENTS}`
       clients=`expr ${INIT_CLIENTS} + ${dalt}`
       echo "this round will use ${clients} clients, hold ${STEP_DURATION}"

       ${WRK_COMMAND} -t1 -c${clients} -L -d${STEP_DURATION} ${BENCHMARK_URL} >> ${WORK_FILE_NAME} &
       error  "sleep 20 seconds"
       sleep 20

       seq=`expr ${seq} + 1`
       url="${benchmark_url}&seq=${seq}"
       error "whole benchmark url : ${url}, try to get the profile data"
       curl ${url} >> ${WORK_FILE_NAME} &
       sleep 40
    done

    echo "benchmark done, please check the ${WORK_FILE_NAME} file to get the benchmark info"
    echo "" >> ${WORK_FILE_NAME}

    # wait the file is write done ~
    sleep 10

    # parse the benchmark result, and print out to console
    parse_benchmark_result ${WORK_FILE_NAME}

    # kill http server
    killSelf

}

#
# this function will check your args, if any arg is invalid, this program will die;
#
function check_args() {

    if [[ -z ${REMOTE_IP} ]]; then
        error " The Remote IP address is empty, you must provide an valid ip address "
        exit 100
    fi

    if [[ ${REMOTE_PORT} -gt 65535 ]]; then
        error " invalid remote port ${REMOTE_PORT} > 65535"
        exit 200
    fi

    if [[ ${REMOTE_PORT} -lt 1000 ]]; then
        error " invalid remote port ${REMOTE_PORT} < 1000"
        exit 300
    fi

    if [[ ${HTTP_SERVER_LISTEN_PORT} -gt 65535 ]]; then
        error " invalid http listen port ${HTTP_SERVER_LISTEN_PORT} > 65535"
        exit 400
    fi

    if [[ ${HTTP_SERVER_LISTEN_PORT} -lt 1000 ]]; then
        error " invalid http listen port ${HTTP_SERVER_LISTEN_PORT} < 1000"
        exit 500
    fi

    if [[ ${INIT_CLIENTS} -lt 1 ]]; then
        error " invalid init clients ${INIT_CLIENTS} <= 0"
        exit 600
    fi

    if [[ ${TARGET_CLIENTS} -lt ${INIT_CLIENTS} ]]; then
        error " invalid target clients ${TARGET_CLIENTS} < ${INIT_CLIENTS} (init clients)"
        exit 700
    fi

    if [[ ${STEP_INC_CLIENTS} -gt `expr ${TARGET_CLIENTS} - ${INIT_CLIENTS}` ]]; then
        error " invalid step increment clients ${STEP_INC_CLIENTS} > `expr ${TARGET_CLIENTS} - ${INIT_CLIENTS}` (target - init)"
        exit 800
    fi

    if [[ -z ${STEP_DURATION} ]]; then
        error " The Step Duration is empty, you must provide an valid duration"
        exit 900
    fi

    if [[ -z ${WRK_COMMAND} ]]; then
        error " The wrk home is empty, you must provide an valid wrk command path "
        exit 1000
    fi

    if [[ ! -f ${WRK_COMMAND} ]]; then
        error " The wrk home is not invalid, the file ${WORK_FILE_NAME} is not existed !"
        exit 1000
    fi

    if [[ -z ${WORK_FILE_NAME} ]]; then
        error " The output file is empty, you must provide an valid output path "
        exit 1100
    fi

    if [[ -z ${BENCHMARK_URL} ]]; then
        error " The benchmark url is empty, you must provide an valid benchmark url "
        exit 1100
    fi

    info "the args check successfully !!! "
}


#
#  this function will display the benchmark env info, including all of
#  the configs in this shell scope
#
function display_env_info() {
cat <<EOF
    Remote Tcp Server IP        : ${REMOTE_IP}
    Remote Tcp Server Port      : ${REMOTE_PORT}
    HttpServer Listen Port      : ${HTTP_SERVER_LISTEN_PORT}
    Init Connections            : ${INIT_CLIENTS}
    Target Total Connections    : ${TARGET_CLIENTS}
    Step Increment Connections  : ${STEP_INC_CLIENTS}
    Step Duration               : ${STEP_DURATION}
    Wrk Command                 : ${WRK_COMMAND}
    Output file                 : ${WORK_FILE_NAME}
    Target Benchmark URL        : ${BENCHMARK_URL}
    Warm-up Clients             : ${WARM_UP_CLIENTS}
    Warm-up Times               : ${WARM_UP_SECS}
EOF
}


############################
#   parse the params       #
############################

while getopts "b:i:p:h:c:t:s:d:w:f:m:u" opt; do
  case ${opt} in
    m)
      WARM_UP_CLIENTS=$OPTARG
      ;;
    e)
      WARM_UP_SECS=$OPTARG
      ;;
    i)
      REMOTE_IP=$OPTARG
      ;;
    b)
      BENCHMARK_URL=$OPTARG
      ;;
    p)
      REMOTE_PORT=$OPTARG
      ;;
    h)
      HTTP_SERVER_LISTEN_PORT=$OPTARG
      ;;
    c)
      INIT_CLIENTS=$OPTARG
      ;;
    t)
      TARGET_CLIENTS=$OPTARG
      ;;
    s)
      STEP_INC_CLIENTS=$OPTARG
      ;;
    d)
      STEP_DURATION=$OPTARG
      ;;
    w)
      WRK_COMMAND=$OPTARG
      ;;
    f)
      WORK_FILE_NAME=$OPTARG
    ;;
    u)
      usage
      ;;
    \?)
      usage
      ;;
    :)
      echo "Option -$OPTARG requires an argument" >&2
      exit 1
      ;;
  esac
done

# display the config
display_env_info

# check args
check_args

# benchmark
benchmark
