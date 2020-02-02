//
//  ========================================================================
//  Copyright (c) 2018-2019 HuJian/Pandening soft collection.
//  ------------------------------------------------------------------------
//  All rights reserved. This program and the accompanying materials
//  are made available under the terms of the #{license} Public License #{version}
//  EG:
//      The Eclipse Public License is available at
//      http://www.eclipse.org/legal/epl-v10.html
//
//      The Apache License v2.0 is available at
//      http://www.opensource.org/licenses/apache2.0.php
//
//  You may elect to redistribute this code under either of these licenses.
//  You should bear the consequences of using the software (named 'java-debug-tool')
//  and any modify must be create an new pull request and attach an text to describe
//  the change detail.
//  ========================================================================
//


package io.javadebug.core.console;

import io.javadebug.core.log.PSLogger;
import io.javadebug.core.transport.NettyTransportClusterClient;
import jline.console.ConsoleReader;
import jline.console.history.FileHistory;
import jline.console.history.History;
import jline.console.history.MemoryHistory;

import java.io.File;
import java.io.IOException;

public class JLineConsole implements CommandSource {

    // the user's home
    private static final String USER_DIR = System.getProperty("user.home");

    // the history directory
    private static final String COMMAND_HISTORY_WORK_DIR = USER_DIR + File.separatorChar + ".java-debug-tool-history" ;

    // the console reader
    private ConsoleReader consoleReader;

    public JLineConsole() throws IOException {

        // create the console reader
        this.consoleReader = new ConsoleReader(System.in, System.out);

        // init the history job
        History history = initCommandHistory();
        this.consoleReader.setHistoryEnabled(true);
        history.moveToEnd();
        this.consoleReader.setHistory(history);

    }

    /**
     *  init the history work type, if the target work dir is not allow to w/r, then
     *  use {@link MemoryHistory}, or use {@link FileHistory}
     *
     * @return the history instance
     * @throws IOException e
     */
    private History initCommandHistory() throws IOException {
        File userWorkDir = new File(USER_DIR);
        File historyPath = new File(COMMAND_HISTORY_WORK_DIR);

        // check permission
        if (!userWorkDir.canRead() || !userWorkDir.canWrite()) {
            return new MemoryHistory();
        }

        // check file
        if (historyPath.exists()) {
            return new FileHistory(historyPath);
        }

        // create the history file
        if (historyPath.createNewFile()) {
            return new FileHistory(historyPath);
        }

        // fallback
        return new MemoryHistory();
    }


    /**
     * 用于生成一条命令输入，比如可以使用命令行，或者从某个队列取，只要能生成
     * 一条命令输入即可;
     * 这个source就像是一个水龙头，只要把水龙头打开，下游就会拿到水，如果你需要监听
     * 命令执行结果，那么可以在 {@link NettyTransportClusterClient}
     * 上面安装一个{@link CommandSink}，sink可以安装多个，同一份结果会广播给所有安装了的sink
     *
     * @return 命令输入，比如 "fc -class String"
     */
    @Override
    public String source() {
        try {
            String line =  this.consoleReader.readLine();
            History history = consoleReader.getHistory();
            if (history instanceof FileHistory) {
                ((FileHistory) history).flush();
            }
            return line;
        } catch (Exception e) {
            PSLogger.error("error when read line from jLine", e);
            return source();
        }
    }

}
