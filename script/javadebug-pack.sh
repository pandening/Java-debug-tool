#!/usr/bin/env bash

#  Date      : 2019-04-18  23:00
#  Auth      : Hu Jian
#  Version   : 1.0
#  Function  : 用于打包的脚本，执行该脚本即可实现同时打agent包和core包

BASEDIR="$( cd "$( dirname "$0" )" && pwd )"

# target pack dir
TARGET_PACKAGE_BASE_DIR=${BASEDIR}/..

# core jar
TARGET_PACKAGE_CORE_JAR_DIR=${TARGET_PACKAGE_BASE_DIR}/core/target/

# core pom path
CORE_POM_PATH=${TARGET_PACKAGE_BASE_DIR}/core/pom.xml

# agent jar
TARGET_PACKAGE_AGENT_JAR_DIR=${TARGET_PACKAGE_BASE_DIR}/agent/target

# agent pom path
AGENT_POM_PATH=${TARGET_PACKAGE_BASE_DIR}/agent/pom.xml

# target jar dir
TARGET_JAR_DIR=${BASEDIR}/../bin/lib

# target shell dir
TARGET_SHELL_DIR=${BASEDIR}/../bin

# the third-party tool dir
#THIRD_PARTY_DIR=${BASEDIR}/../bin/third-party

# async-profiler
ASYNC_PROFILER_DIR_TARGET_DIR=${BASEDIR}/../bin
ASYNC_PROFILER_ORIGIN_DIR=${BASEDIR}/../async-profiler

# git user name
USER_NAME=pandening

function mkdirDir() {
    if [ ! -d $1 ]; then
        echo "target jar dir not exists : $1, create it"
        mkdir $1
    else
        echo "target jar dir exists : $1"
    fi
}
function ps() {
    echo $1
}

function pack() {

    ps "check dir ..."

    mkdirDir ${TARGET_SHELL_DIR}

    mkdirDir ${TARGET_JAR_DIR}

#    mkdirDir ${THIRD_PARTY_DIR}

    ps "start to pack core jar ..."

    # package core jar
    mvn clean package -Dmaven.test.skip=true -f ${CORE_POM_PATH}

    ps "start to move the core jar to target dir:${TARGET_JAR_DIR}"

    # move to target dir
    cp ${TARGET_PACKAGE_CORE_JAR_DIR}/Java-Debug-Tool-Core-jar-with-dependencies.jar ${TARGET_JAR_DIR}/debug-core.jar

    ps "start to pack agent jar ..."

    # package agent jar
    mvn clean package -Dmaven.test.skip=true -f ${AGENT_POM_PATH}

    ps "start to move the agent jar to target dir:${TARGET_JAR_DIR}"

    # move to target dir
    cp ${TARGET_PACKAGE_AGENT_JAR_DIR}/Java-Debug-Agent-jar-with-dependencies.jar ${TARGET_JAR_DIR}/debug-agent.jar

    # move shell to target dir
    ps "move shells to target dir:{$TARGET_SHELL_DIR}"

    cp ${BASEDIR}/javadebug-agent-launch.sh ${TARGET_SHELL_DIR}/javadebug-agent-launch.sh
    cp ${BASEDIR}/javadebug-agent-attach.sh ${TARGET_SHELL_DIR}/javadebug-agent-attach.sh
    cp ${BASEDIR}/javadebug-client-launch.sh  ${TARGET_SHELL_DIR}/javadebug-client-launch.sh
    cp ${BASEDIR}/javadebug-simple-client.sh  ${TARGET_SHELL_DIR}/javadebug-simple-client.sh
    cp ${BASEDIR}/javadebug-cluster-launch.sh  ${TARGET_SHELL_DIR}/javadebug-cluster-launch.sh
    cp ${BASEDIR}/javadebug-http-server.sh  ${TARGET_SHELL_DIR}/javadebug-http-server.sh
    cp ${BASEDIR}/wrk_auto_benchmark.sh ${TARGET_SHELL_DIR}/wrk_auto_benchmark.sh
    cp ${BASEDIR}/hello.sh  ${TARGET_SHELL_DIR}/hello.sh
    cp ${BASEDIR}/logo  ${TARGET_SHELL_DIR}/logo
    cp ${BASEDIR}/javadebug-unpack.sh  ${TARGET_SHELL_DIR}/javadebug-unpack.sh

    chmod +x ${TARGET_SHELL_DIR}/*.sh

    # copy whole bin directory to spring/bin
    echo "start to copy whole bin directory ${TARGET_SHELL_DIR} to spring/lib/bin"
    cp -r ${TARGET_SHELL_DIR} "${BASEDIR}/../spring/lib/"

#    # move the profile-tool to bin-lib
#    echo "start to move the async-profiler."
#
#    mkdirDir ${THIRD_PARTY_DIR}/async-profiler/
#    mkdirDir ${THIRD_PARTY_DIR}/async-profiler/build/
#
#    cp -r ${BASEDIR}/../profiles-tool/async-profiler/build ${THIRD_PARTY_DIR}/async-profiler/
#    cp ${BASEDIR}/../profiles-tool/async-profiler/profiler.sh ${THIRD_PARTY_DIR}/async-profiler/
#    chmod +x ${THIRD_PARTY_DIR}/async-profiler/

    # move the async-profile to target bin dir
    echo "start to pack the async-profile to target bin dir : ${ASYNC_PROFILER_DIR_TARGET_DIR}"
    cp -r ${ASYNC_PROFILER_ORIGIN_DIR} ${ASYNC_PROFILER_DIR_TARGET_DIR}

    # zip
    echo "start to zip package."
    zip -r ../java-debug-tool.zip ../bin

    # code line.
    echo "start to code statistic..."
    echo $(git log --author="${USER_NAME}" --pretty=tformat: --numstat | awk '{ add += $1; subs += $2; loc += $1 - $2 } END { printf "added lines: %s, removed lines: %s, total lines: %s\n", add, subs, loc }' -)

}

# append the header
ps "append common file header"
chmod +x ./javadebug-source-header.sh
./javadebug-source-header.sh

# run pack
time pack "${@}"
