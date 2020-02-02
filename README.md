## Java-debug-tool

### what is Java-debug-tool

Java-debug-tool is a dynamic debugging tool. it provides some debugging commands to debug your code in runtime.

### require

* java 8 +
* Linux / Mac
* libc


### install Java-debug-tool

#### download & install the Java-debug-tool

```bash
wget https://github.com/pandening/storm-ml/releases/download/8.0/javadebug-tool-install.sh
sh javadebug-tool-install.sh
```

#### attach to target jvm

```bash
./javadebug-agent-launch.sh pid@ip:port 
./javadebug-agent-launch.sh pid@ip
./javadebug-agent-launch.sh pid
```
the default port is 11234.


#### debug client launch

```bash
./javadebug-client-launch.sh 
./javadebug-client-launch.sh ip:port
```

### usage

[usage](usage.md)

### develop

#### the module / dir

* agent

the agent code, do attach work.


* core

the java-debug-tool's biz codes, including netty-handler, debug-client / debug-server, command handler, etc;


* spring

do not edit;

* test

the test module, test codes write here;

* script

the java-debug-tool's script dir;

* bin

the target bin dir;


#### pack 

```bash
 
 cd script
 ./javadebug-pack.sh

```

enjoy !

### License

```text

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

```