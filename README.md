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

using 'list' command to show all of the commands, then use 'help' command to get the usage of the command;

```bash

  list
  h -cmd list
  h -cmd mt
  h -cmd monitor
  ...

```

![list command](https://p1.meituan.net/travelcube/70b00b644f4d9b5408df122b7c226631140733.png)
```text
127.0.0.1:11234>list

---------------------------------------------------------------------------------------------
Command            	：list
Round              	：2
ClientId           	：10000
ClientType         	：console:1
Version            	：version:1
CommandCost        	：4 (ms)
STW_Cost           	：0 (ms)
Time:              	: Sun Feb 02 17:22:20 CST 2020
---------------------------------------------------------------------------------------------
fc
help
list
exit
alive
redefine
rollback
lock
set
info
trace
mt
ct
thread
monitor
btrace

---------------------------------------------------------------------------------------------
```

![help command](https://p1.meituan.net/travelcube/d4a8eaeaa070396cf6f472e53b9685a9111432.png)
```text
127.0.0.1:11234>h -cmd list

---------------------------------------------------------------------------------------------
Command            	：h
Round              	：3
ClientId           	：10000
ClientType         	：console:1
Version            	：version:1
CommandCost        	：7 (ms)
STW_Cost           	：0 (ms)
Time:              	: Sun Feb 02 17:22:39 CST 2020
---------------------------------------------------------------------------------------------
Command    	: list | all
Function   	: list commands
Usage      	: list | all
Type       	: COMPUTE

---------------------------------------------------------------------------------------------
```

![monitor command](https://p0.meituan.net/travelcube/74c50cdff72994f8ac6f02d922262528158297.png)
```text
--------------------------------------------------------------------------------
                Thread Dynamic Statistic
--------------------------------------------------------------------------------
-----      -----                                              -----      -----
tid        name                                                 s        cpu%
-----      -----                                              -----      -----
11         test-test-R-worker                                   TW       95%
20         Java-Debug-Tool-WebSocket-Server-Worker              R        5%

Total Thread : 13, New : 0, Runnable : 7, Blocked : 0, Waiting : 4, Timed Waiting : 2, Terminated : 0
--- System Load ---
AvailableProcessors                     8
SystemLoadAverage              3.18652343
ProcessCpuLoad                 0.01164025


--------------------------------------------------------------------------------
q
```

![method trace command](https://p0.meituan.net/travelcube/e19e52ee5dc4aa941d9be79c8e2dc0aa262152.png)
```text
127.0.0.1:11234>mt -c R -m call

---------------------------------------------------------------------------------------------
Command            	：mt
Round              	：6
ClientId           	：10000
ClientType         	：console:1
Version            	：version:1
CommandCost        	：411 (ms)
STW_Cost           	：68 (ms)
Time:              	: Sun Feb 02 17:23:52 CST 2020
---------------------------------------------------------------------------------------------
[R.call] invoke by Thread:Thread[test-test-R-worker,5,main]
with params
[
[0] @class:java.lang.Integer -> 6,
[1] @class:java.lang.Integer -> 6,
[2] @class:C -> {"@class":"C","a":0},
[3] @unknown -> NULL,
[4] @unknown -> NULL,
[5] @unknown -> NULL,
[6] @unknown -> NULL,
[7] @class:java.util.ArrayList -> ["java.util.ArrayList",[{"@class":"R$MMM","data":"test"}]]
]
[0 ms] (73) [mmmList1 = ["java.util.ArrayList",[{"@class":"R$MMM","data":"test"}]]]
[0 ms] (74)
[0 ms] (75)
[0 ms] (79)
[0 ms] (83) [el = -1]
[0 ms] (84)
[0 ms] (89)
[0 ms] (93)
[0 ms] (97)
[0 ms] (99) [sa = 1]
[0 ms] (100)
[1 ms] (101) [ii = 0]
[0 ms] (102) [ij = 0]
[0 ms] (103) [jk = 1]
[0 ms] (104) [f = 1.0]
[0 ms] (105) [d = 0.123]
[0 ms] (106) [name = "hello6,6"]
[0 ms] (107) [list = ["java.util.ArrayList",[6,7]]]
[0 ms] (108)
[0 ms] (109)
[0 ms] (110)
[0 ms] (112)
[0 ms] (115)
[0 ms] (118)
[0 ms] (119)
throw exception:[java.lang.IllegalArgumentException:  i + j <= 10:66]  at line:119 with cost:3 ms
 -R.call
  -R$1.run at line:152
   -java.lang.Thread.run at line:748

---------------------------------------------------------------------------------------------
```

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

### Thanks

* [Netty Project](https://github.com/netty/netty) : Transport support.
* [JLine](https://github.com/jline/jline2) : Console support.
* [jna](https://github.com/java-native-access/jna) : java native access support.
* [ASM](https://github.com/llbit/ow2-asm) :  java bytecode weaver.
* [arthas](https://github.com/alibaba/arthas) : java-debut-tool reference arthas.
* [async-profiler](https://github.com/jvm-profiling-tools/async-profiler) : profile support. 

Thanks for the excellent work in these projects !

### NOTICE

```text

   DO NOT FORK UNTIL YOU CAN NOT SEE THIS NOTICE.
   DO NOT FORK UNTIL YOU CAN NOT SEE THIS NOTICE.
   DO NOT FORK UNTIL YOU CAN NOT SEE THIS NOTICE.
   DO NOT FORK UNTIL YOU CAN NOT SEE THIS NOTICE.
   DO NOT FORK UNTIL YOU CAN NOT SEE THIS NOTICE.
   DO NOT FORK UNTIL YOU CAN NOT SEE THIS NOTICE.
   DO NOT FORK UNTIL YOU CAN NOT SEE THIS NOTICE.
   DO NOT FORK UNTIL YOU CAN NOT SEE THIS NOTICE.
   DO NOT FORK UNTIL YOU CAN NOT SEE THIS NOTICE.
   DO NOT FORK UNTIL YOU CAN NOT SEE THIS NOTICE.
   DO NOT FORK UNTIL YOU CAN NOT SEE THIS NOTICE.
   DO NOT FORK UNTIL YOU CAN NOT SEE THIS NOTICE.
   DO NOT FORK UNTIL YOU CAN NOT SEE THIS NOTICE.
   DO NOT FORK UNTIL YOU CAN NOT SEE THIS NOTICE.
   DO NOT FORK UNTIL YOU CAN NOT SEE THIS NOTICE.
   DO NOT FORK UNTIL YOU CAN NOT SEE THIS NOTICE.
   DO NOT FORK UNTIL YOU CAN NOT SEE THIS NOTICE.
   DO NOT FORK UNTIL YOU CAN NOT SEE THIS NOTICE.
   DO NOT FORK UNTIL YOU CAN NOT SEE THIS NOTICE.
   DO NOT FORK UNTIL YOU CAN NOT SEE THIS NOTICE.
   DO NOT FORK UNTIL YOU CAN NOT SEE THIS NOTICE.
   DO NOT FORK UNTIL YOU CAN NOT SEE THIS NOTICE.
   DO NOT FORK UNTIL YOU CAN NOT SEE THIS NOTICE.
   DO NOT FORK UNTIL YOU CAN NOT SEE THIS NOTICE.
  
   
```