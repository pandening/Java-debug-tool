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


### Ability

Java-debug-tool including 2-part ability, debugging and profiling.

#### debugging

debug your code in runtime, the main command list :

* mt (method trace)
* fc (find class)

the simplest usage of 'mt' command like this : 

```bash

  mt -c [class name with package] -m [method name]

```

there are many other options for mt command:

* timeout   : set command execute timeout (seconds unit)
* t         : the debug event type
    * return : stop debug when method exit
    * throw  : stop debug when method throw exception
        *       e  ： the target exception class name [with package]
        *       tl :  set the target invoked line, ref [line]
        *       te :  set the target line's expression ref [line]   
    * watch  : stop debug when the params match
        *       i  : the spring expression
        *       tl :  set the target invoked line, ref [line]
        *       te :  set the target line's expression ref [line]   
    * custom : call method & debug itself 
        *       i  : the params
        *       tl :  set the target invoked line, ref [line]
        *       te :  set the target line's expression ref [line]    
    * line   : stop debug when special line is invoked(and the spring expression is true). 
        *       tl :  set the target invoked line
        *       te :  set the target line's expression
    * record : record some method calls  
        *       u  : show the record method call with index [from 0 inc]
        *       n  : the record count limit
        *       time : timeout for recording
* l       :  just show the target line's debug info

for 'fc' command, it's very easy to use:

```bash

[case 1] you know the class name and want to find the class 
   fc -class [class name]
[case 2] you just want to find some classes by the regex
   fc -r [regex to match class]   


```

the 'l' option can set the output class limit. like this:

```bash

fc -r regex -l 10

```

#### profiling


java-debug-tool can profiling your code, there are some command can get the performance data in run time;

* thread
* monitor
* cputime

the thread command can get the cpu usage of each thread, and the call stack, and you can get the topN busy thread by this command:

thread command options:

* top       : [topN] get the topN busy thread info
* status    : [R(runnable)|W(waiting)|TW(timed waiting)|B(blocking)] get the target status' thread info
* tid      : [thread id] get the thread info by thread id

monitor (aka collect) command options :
* t  : the monitor event type, multi type can split with ',' like thread,mem
    * thread : the thread statistic info
    * mem    : the mem info
    * class  : the class info
    * gc     : the gc info
* i  : the interval (secs)

the default event is thread, the default interval is 5 seconds

the cputime command can get the cpu usage of the target jvm, the interval is 30 seconds, the base usage of this command is :

```bash

ct -o csv

```

the 'o' option set the output format, you can set one from [csv,json]; the csv format is recommend, you need to wait 30 seconds
before the command execute successfully. the output like this:

```text

---------------------------------------------------------------------------------------------
Command            	：ct
Round              	：1
ClientId           	：10000
ClientType         	：console:1
Version            	：version:1
CommandCost        	：32125 (ms)
STW_Cost           	：0 (ms)
Time:              	: Mon Feb 03 17:33:10 CST 2020
---------------------------------------------------------------------------------------------
Start Time : Mon Feb 03 17:32:38 CST 2020
Stop Time : Mon Feb 03 17:33:10 CST 2020
Total cpu usage : 35.425249999999984 ms

time;usr_ms;sys_ms;avg_usr_ms;avg_sys_ms;nivc_switch_per_sec;nvc_switch_per_sec
0;22.729;13.790;20.770;14.655;514;11
1;18.769;11.816;20.770;14.655;499;18
2;19.048;12.215;20.770;14.655;483;13
3;22.882;14.352;20.770;14.655;483;9
4;49.963;17.214;20.770;14.655;479;18
5;23.305;15.979;20.770;14.655;456;16
6;22.382;13.520;20.770;14.655;491;8
7;25.801;11.920;20.770;14.655;543;7
8;18.927;12.211;20.770;14.655;509;14
9;22.440;9.026;20.770;14.655;541;6
10;20.290;13.212;20.770;14.655;472;13
11;19.866;15.524;20.770;14.655;456;5
12;24.528;16.648;20.770;14.655;447;19
13;19.002;16.981;20.770;14.655;457;11
14;20.882;16.202;20.770;14.655;452;15
15;20.041;15.652;20.770;14.655;484;16
16;17.661;10.187;20.770;14.655;507;4
17;15.381;10.135;20.770;14.655;498;12
18;18.671;16.080;20.770;14.655;446;13
19;27.149;17.041;20.770;14.655;481;14
20;18.231;13.681;20.770;14.655;502;14
21;17.777;13.696;20.770;14.655;480;12
22;18.266;16.266;20.770;14.655;450;17
23;20.510;16.447;20.770;14.655;480;13
24;19.699;16.250;20.770;14.655;474;14
25;19.966;15.105;20.770;14.655;455;24
26;25.839;15.825;20.770;14.655;447;15
27;19.736;16.556;20.770;14.655;455;17
28;18.673;16.246;20.770;14.655;440;18
29;18.499;16.801;20.770;14.655;447;16

---------------------------------------------------------------------------------------------

```

* time                  : the time line
* usr_ms                : user time mills
* sys_ms                : system time mills
* avg_usr_ms            : the avg user time mills
* avg_sys_ms            : the avg system time mills
* nivc_switch_per_sec   : the nivc switch per sec
* nvc_switch_per_sec    ：the nvc switch per sec

```text
        
        |
        |
        |     
        |
        |
    val |
        |
        |
        |
        |
        |_ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _
               time line [0 - 29] 

```

for profiling, you can choose [async-profiler](bin/async-profiler) which already including in bin path of java-debug-tool.

### Demo 

we will use a simple case to show the usage of java-debug-tool:

```java
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class Demo {

    private void say(String word, boolean tag, int rdm) {
        if (word == null) {
            word = "test say";
        }
        int length = word.length();
        if (tag) {
            length += 1;
        } else {
            length -= 1;
        }
        word += "@" + length;
        System.out.println(word);
        if (rdm > 5) {
            throw new IllegalStateException("test exception");
        }
    }

    private static final String[] list = {"a", "ab", "abc", "abcd"};

    public static void main(String[] args) {
        Demo demo = new Demo();
        Random random = new Random(47);
        new Thread(new Runnable() {
            @Override
            public void run() {
                for(;;) {
                    try {
                        demo.say(list[random.nextInt(4)], random.nextBoolean(), random.nextInt(10));
                        TimeUnit.MILLISECONDS.sleep(100);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }, "demo-thread").start();
    }

}

```

the 'mt' command (aka method trace) is the main command of java-debug-tool, this command 
can offer many runtime info of target method:

* Action time
* caller thread
* method params
* line cost
* invoked line number
* line variables
* return val
* exception
* call stack

![mt command detail](https://p1.meituan.net/travelcube/f5ac2f15e1c8794448ed980fe6eed91397685.png)


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

* [Netty](https://github.com/netty/netty) : Transport support.
* [JLine](https://github.com/jline/jline2) : Console support.
* [jna](https://github.com/java-native-access/jna) : java native access support.
* [ASM](https://github.com/llbit/ow2-asm) :  java bytecode weaver.
* [arthas](https://github.com/alibaba/arthas) : java-debut-tool reference arthas.
* [async-profiler](https://github.com/jvm-profiling-tools/async-profiler) : profiling support. 

Thanks for the excellent work in these projects!
