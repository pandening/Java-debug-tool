#!/usr/bin/env bash


# java-debug-tool.zip name
ZIP_NAME=java-debug-tool.zip

# version
JAVA_DEBUG_TOOL_VERSION=8.0

# the zip url
DOWNLOAD_URL=https://github.com/pandening/storm-ml/releases/download

# work dir
WORK_DIR=java-debug-tool

function download() {
    #JAVA_DEBUG_TOOL_VERSION=$1

    echo "start to download release java-debug-tool.zip:${DOWNLOAD_URL}/${JAVA_DEBUG_TOOL_VERSION}/${ZIP_NAME}"

    wget ${DOWNLOAD_URL}/${JAVA_DEBUG_TOOL_VERSION}/${ZIP_NAME}

    # unzip
    echo "start to unzip the release zip"
    mkdir ${WORK_DIR}

    unzip ${ZIP_NAME} -d ${WORK_DIR}

    # test shell path
    HELLO_WORLD_SHELL_PATH=${WORK_DIR}/bin/hello.sh

    sh ${HELLO_WORLD_SHELL_PATH}
}

download

