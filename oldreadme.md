## Java-debug-tool

### Java-debug-tool解决什么问题

Java-debug-tool是为了解决日常问题排查的痛点而设计的，问题排查分成两个主要阶段，问题定位和问题修复，问题定位是说找到问题的原因，问题修复是说将问题解决，使得系统恢复正常运行。
对于问题定位来说，我们的需求是：
* 能知道方法入参和返回值，或者抛出的异常信息
* 当方法有多个出口的时候，可以知道方法是从什么地方退出的，或者是从什么地方抛出异常的；
* 一次方法调用的执行路径是怎么样的，每一行代码的耗时又是多少；
* 获取到方法执行过程中的局部变量信息；

本质上，问题定位的需求是实现单步调试，因为这样是最容易发现问题出在什么地方的，但是对于java来说，单步调试技术会停顿整个JVM，所以只能在测试的时候使用这种技术，对于生产环境来说就不能使用了，所以对于线上问题排查来说，基本可以不用考虑单步调试，但是如果集群有流量摘除等功能的话，倒是可以使用；java-debug-tool解决了这个问题，可以模拟单步调试的同时不会停顿正在运，使用行的JVM，下面会介绍Java-debug-tool到底实现了一些什么功能。

找到了问题出现的原因，接着就是问题修复，问题修复最大的痛点其实是恢复生产，对于java来说，恢复生产意味着需要重启JVM，这样就会造成问题修复时间变长，Java-debug-tool为此提供了技术支持Java Instrumentation技术，可以在运行时的JVM中替换类的字节码，实现热修复。

***

### Java-debug-tool不能解决什么问题

* （1）如果需要做性能优化分析，Java-debug-tool可能支持的力度很小，虽然可以通过Java-debug-tool观察到每一行代码的执行耗时，但是也仅仅是观察，所以性能问题还是需要其他专业的工具来进行；
* （2）非JVM自身问题，比如机器CPU、磁盘I/O等问题，Java-debug-tool就无能为力了，Java-debug-tool专注于解决JVM自身的问题；
* （3）Java-debug-tool仅支持方法级别的观察，无法观察到整体的调用链路，后续可能会支持多级方法链路的观察，但可能性不大，因为要支持这种方法间调用链路追踪，就得增强多个方法，而增强方法是对运行时有一定损耗的，如果一个方法调用链路特别长（对于java来说一般调用链路都很长），那么就悲剧了；
* （4）Java-debug-tool不支持递归方法的观察，这个功能实现起来也是非常麻烦，而且极其不可控，所以千万不要用Java-debug-tool去观察一个递归方法，切记；

***

### 如何使用

### 使用

首先需要下载安装脚本：

```java
wget https://github.com/pandening/storm-ml/releases/download/2/javadebug-tool-install.sh -P path
```

之后执行：

```java
sh javadebug-tool-install.sh ${version}
```

#### ${version} releaseNote

* 1.0

基础动态调试功能，可能存在不可控的bug；推荐使用最新版本；
   
* 2.0

增强多种功能，修复若干1.0版本的bug；

* 3.0

（1）修复若干2.0版本bug；
（2）支持打印没有重写toString方法的类对象


如果看到屏幕输出：

```java
welcome to use java-debug-tool
```

就说明安装成功了，可以使用工具了！


### 开发

Java-debug-tool使用Java开发，下面介绍如何使用Java-debug-tool进行问题排查；

* （1）下载Java-debug-tool代码；
* （2）进入script目录，执行*javadebug-pack.sh*脚本执行编译打包，要求*JDK 1.8 +*，并且一定要执行*javadebug-pack.sh*脚本之后再使用；
* （3）如果是Spring项目，则只需要将下面的bean配置到项目中即可实现JVM启动之后Java-debug-tool Agent自动attach到目标JVM上的功能，如果不是Spring项目，请看（4）
```java
    <!-- dynamic debug bean -->
    <bean id = "javaDebugInitializer" class="io.javadebug.spring.JavaDebugInitializer" factory-method="initializer" destroy-method="destroy" lazy-init="false"/>
```

* （4）Java-debug-tool不要求在目标JVM启动的时候就必须attach到目标JVM上，可以动态attach，在目录 */bin*下有多个可用的脚本，方便用于动态attach到目标JVM上，无论如何，你都需要首先知道目标JVM的进程id，然后执行一个脚本就可以动态attach到目标JVM上：

```shell
./javadebug-agent-launch.sh PID
```
这样就可以在目标JVM上启动一个tcp服务，默认地址为：127.0.0.1:11234，如果你想要指定其他的地址，可以使用下面的命令:

```shell
./javadebug-agent-launch.sh PID@IP:PORT
```
之后就可以在IP:PORT启动tcpServer，attach到目标JVM上之后，就可以连接目标JVM进行动态调试了，连接到目标JVM只需要执行下面的命令即可：

```shell
 ./javadebug-client-launch.sh

```
默认就是连接 127.0.0.1:11234，如果attach目标JVM的时候指定的地址不是这个，需要显示指定地址:

```shell
 ./javadebug-client-launch.sh IP:PORT

```


***

### 命令详解

Java-debug-tool目前支持的命令不多，下面分别介绍一下当前支持的核心命令。首先介绍一下命令输出界面信息介绍：

```java

---------------------------------------------------------------------------------------------
命令            	：mt
命令执行Round    	：1
客户端ID         	：10000
客户端类型        	：client:1
协议版本          	：version:1
命令耗时         	：179 (ms)
STW时间          	：45 (ms)
---------------------------------------------------------------------------------------------
[ReturnTest.getIntVal] with params
[1]
[0 ms] (37)
[0 ms] (43) [startTime = 1559358148073]
[0 ms] (44) [strTag = the return/throw line test tag]
[0 ms] (45)
[0 ms] (47)
[0 ms] (51)
[3 ms] (52) [paramModel = 1.1]
[0 ms] (53)
return value:[101]  at line:53 with cost:5 ms

---------------------------------------------------------------------------------------------
```

每个输出字段都介绍一下：

字段 | 含义 | 值
-|-|-
命令 | 本次输出执行的命令是什么 | 就是你输入的命令名称
命令执行Round | 这个调试客户端和目标JVM交互了几次 | 交互次数
客户端ID | 每个客户端首次连接服务端都会被分配一个ContextId，后续的交互都需要将这个ID带上 | 唯一ID
客户端类型 | 这是一个保留字段，Java-debug-tool认为第一个连接到目标JVM的调试客户端应该是一个Master Client，权限最高 | |
| 协议版本 | 防伪，只有是从服务端拿到的协议才能继续交互 | |
| 命令耗时 | 命令的执行耗时，从命令输入处理开始计算，到命令结果展示出来结束，所以是客户端耗时 + 服务端耗时 | |
| STW时间 | 动态增强字节码涉及到JVM字节码替换，会造成STW，这个时间就记录到底STW了多长时间，这个时间会比实际STW的时间长，只是一个粗略的时间| 如果一个方法被一个client增强过了，后续的client就不能增强了，除非增强该方法的client退出，其他client才能继续增强；同时，一个client增强过的方法，其他client可以共享 |

接着就是具体方法的执行路径信息，比如上面这个例子，说明本次观察的方法执行是 *"ReturnTest.getIntVal"*，方法入参是1，方法执行路径是37-43-44-45-47-51-52-53，最终从53行退出，其中第52行耗时3ms，其他行耗时小于1ms，所以无法收集到，最终方法的执行结果是101，本次方法耗时5ms，并且可以看到43、44、52行都有变量赋值信息，格式为 varName = varVal.toString()，需要注意的是，varName可能是错误的，但是varVal是正确的，如果有多个，按照赋值顺序展示；这是方法正常返回的结果展示，下面看一个方法抛出异常的结果展示：

```java

---------------------------------------------------------------------------------------------
命令            	：mt
命令执行Round    	：1
客户端ID         	：10001
客户端类型        	：client:0
协议版本          	：version:1
命令耗时         	：75 (ms)
STW时间          	：0 (ms)
---------------------------------------------------------------------------------------------
[ReturnTest.getIntVal] with params
[7]
[0 ms] (37)
[0 ms] (43) [startTime = 1559358921527]
[0 ms] (44) [strTag = the return/throw line test tag]
[0 ms] (45)
[0 ms] (47)
[0 ms] (51)
[0 ms] (54)
[0 ms] (59)
[0 ms] (73) [paramModel = ParamModel{intVal=0, doubleVal='0.0'}]
[0 ms] (74)
[0 ms] (75)
[0 ms] (76) [subVal = 200]
[0 ms] (78)
[0 ms] (82)
throw exception:[java.lang.IllegalStateException: error occ with in:7]  at line:82 with cost:0 ms

---------------------------------------------------------------------------------------------
```
可以看到本次方法执行路径，参数为7，在82行抛出了异常，其他信息和正常返回时类似，就不做过多解释了。

#### methodTrace命令

就像命令名称一样，这个命令是用于观察方法执行路径的，可以使用mt来替代命令，该命令参数较多，但是大部分都是可选的，下面先介绍每一个参数的含义，然后再介绍如何实现具体的功能。

命令基本格式：
mt -c <class> -m <method>

可选参数：
* -d ：如果目标类中的目标方法是重载方法，那么你需要提供这个参数，比如int a(int a) => desc = "(I)I"；
* -t：选择具体的功能类型，可选项为：
  * return：当方法正常退出的时候，获取到一次方法链路信息；
  * throw：方方法抛出异常的时候，获取到一次方法链路信息；
  * record：记录方法调用信息，用于回放流量；
  * custom：用于实现用户自己输入参数观察，或者回放record的流量进行观察，当然，如果只是想发生一次请求也是可以的；
  * watch：等待特定的参数，使用Spring表达式进行参数匹配，当匹配到目标参数之后，会返回方法链路信息，如果Spring表达式有误，那么会直接在第一次方法调用之后返回；  

* -i：用于接收用户的参数输入，比如当t=custom的时候，i参数就是用户指定的参数，这个参数是通过特殊处理的json字符串，java-debug-tool将提供工具接口来生成这个字符串，当t=watch的时候，i参数就是用于匹配参数的Spring表达式。

* -n：当t=record的时候，n参数的含义就是需要录制的流量数量，当前仅允许录制10个以内；

* -time：当t=record的时候，该参数的含义是录制的时间限制，超出则停止录制；

* -u：当t=custom的时候，如果提供了u参数，那么i参数将被忽略，u代表record的流量下标，从0开始，如果u参数获取到了具体的流量，那么本次custom输入的参数就会从u参数取出来的流量中拿到参数，如果t=record，并且u参数合法，那么就不会进行录制，而是会从录制好的流量中取出代表u下标的流量，用户可以查看具体的流量信息（包括该流量的方法链路）；

* -e：如果t=throw，那么如果-e内容合法，那么该参数就代表需要等待的目标异常，如果参数不合法，只要遇到一个异常，本次观察就会结束；当t=custom的时候，该参数用于匹配自定义输入，也就是说，如果你希望观察自定义输入的执行路径，你需要在custom类型下指定-e参数，内容是用于匹配输入的Spring表达式；

* -s：有些情况下，你可能只需要看方法调用的路径，不需要耗时信息，或者不需要变量信息，那么这个参数有很有用，因为可能有些变量很长，展示出来很难看，而有些时候你只需要看看方法到底是从哪里退出来的，这个参数有很有帮助。可以是"line"/"cost"中的一个，前者表示只需要给我方法链路信息，后者其实是"line" + "cost"；

* -l：这个参数很有用，当某个方法很长，那么链路追踪信息打印出来会很难看，你可能只关心某一行的相关信息，比如就想看看某一行的代码执行耗时，以及这一行相关的变量信息，那么这个参数就可以派上用场，值就是具体的行号（对照源码）；

下面根据上面的参数来实现不同的观察功能，首先是用于测试的Java类：

```java
public class ReturnTest {

    private TestClass testClass = new TestClass();

    public static void say(int a) {
        int b = a * 10;
        System.out.println("hello:" + b);
        //return b;
    }

    public int getIntVal(int in) {
//        if (in < 7) {
//            System.out.println("in < 7, return");
//            throw new UnsupportedOperationException("test");
//        }

        if (in == 5) {
            String msg = null;
            // produce npe
            in += msg.length();
        }

        long startTime = System.currentTimeMillis() + fibonacci(2);
        String strTag = "the return/throw line test tag";
        if (in < 0) {
            return strTag.charAt(0);
        } else if (in == 0) {
            return 1000;
        }
        // > 0
        if (in < 2) {
            double dbVal = 1.1;
            return (int) (dbVal + 100);
        } else if (in == 2) {
            float fVal = 1.2f;
            return (int) (fVal + 200);
        }
        // > 2
        if (in % 2 == 0) {
            Random random = new Random();
            int rdm = random.nextInt(100);
            if (rdm >= 50) {
                throw new IllegalArgumentException("npe test");
            } else if (rdm <= 20) {
                throw new NullPointerException("< 20");
            }
            // end time
            long end = System.currentTimeMillis();
            long cost = startTime - end;
            int ret = testClass.test(in);
            return (int) (rdm * 10 + ret + (cost / 1000));
        } else {
            ParamModel paramModel = new ParamModel();
            paramModel.setIntVal(in);
            paramModel.setDoubleVal(1.0 * in);
            int subVal = getSubIntVal(paramModel);

            if (subVal == 100) {
                throw new IllegalArgumentException("err occ with in:" + subVal);
            }

            throw new IllegalStateException("error occ with in:" + in);
        }
    }

    /**
     *  不支持递归函数
     *
     * @param n
     * @return
     */
    public int fibonacci(int n) {
        if (n < 0) {
            return -1;
        }
        if (n == 0) {
            return 0;
        }
        if (n <= 2) {
            return 1;
        }
        return fibonacci(n - 1) + fibonacci(n - 2);
    }

    public int getSubIntVal(ParamModel paramModel) {
        if (paramModel == null) {
            return -1;
        }
        if (paramModel.getIntVal() <= 0) {
            return (int) paramModel.getDoubleVal();
        } else if (paramModel.getIntVal() <= 5) {
            return 100;
        } else if (paramModel.getIntVal() <= 8) {
            return 200;
        } else {
            throw new RuntimeException("ill");
        }
    }

    public static void main(String[] args) {
        new Thread(new Runnable() {
            private Random random = new Random();
            private ReturnTest returnTest = new ReturnTest();

            @Override
            public void run() {
                while (true) {
                    try {
                        System.err.println(returnTest.getIntVal(random.nextInt(10)));
                        TimeUnit.MILLISECONDS.sleep(5);
                    } catch (Exception e) {
                        //e.printStackTrace();
                        //System.out.println("error:" + e.getMessage());
                    }
                }
            }
        }).start();
    }
}

public class TestClass {

    Aa aa = new Aa();

    public int test(int in) {

        if (in == 5) {
            return 100;
        }
        String tag = "the in:" + in;
        if (in < 5) {
            in += 2;
        } else {
            in -= 1;
        }

        if (in > 5) {
            throw new IllegalArgumentException("must <= 5");
        }
        if (in <= 3) {
            throw new NullPointerException("must >= 3");
        }

        return in * 100;
    }

}
```

* （1）观察一次方法调用的执行路径

![观察一次方法调用执行路径](https://upload-images.jianshu.io/upload_images/7853175-dc5157e2da727b3c.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

上面的图片展示了观察一次 *"ReturnTest.getIntVal"*方法调用的执行路径，本次方法入参是2，返回值是201，是从56行代码退出的，耗时1ms；

* （2）在（1）中只要方法被调用一次，那么观察就会立刻结束，所以观察结果可能是方法正常结束，也可能是抛出了异常，如果只是希望观察方法正常退出，那么就可以指定-t参数为return，这样只有当第一次方法不抛出异常退出才会结束观察；

* （3）和（2）相反的是，如果你希望监控一个异常的执行路径，比如一个方法执行偶尔会抛出某种异常，搞得你很摸不着头脑，那你就可以指定-t参数为throw来观察抛出异常的执行路径：

![观察方法异常退出执行路径](https://upload-images.jianshu.io/upload_images/7853175-bd0acbeb6699ed85.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

当然，如果你想要观察的是某种特定的异常，可以指定-e参数：

![观察指定的异常](https://upload-images.jianshu.io/upload_images/7853175-ae0f21c3236c5172.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

* （4）自定义输入参数进行方法调用，并进行方法执行路径观察：需要注意的是，在执行自定义参数调用之前，Java-debug-tool需要获取到目标对象，或者目标方法是一个静态方法，否则Java-debug-tool命令会一直等待获取目标对象（不会主动创建目标对象）

![观察特定输入](https://upload-images.jianshu.io/upload_images/7853175-b80fd9b98199b75d.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

* （5）记录方法调用请求

![录制方法请求](https://upload-images.jianshu.io/upload_images/7853175-ba5e44968a7b63eb.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

录制完成后可以回放请求：

![观察回放请求](https://upload-images.jianshu.io/upload_images/7853175-11c2281c9d33dd7e.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

* （6）观察特定输入执行路径

![观察特定参数-1](https://upload-images.jianshu.io/upload_images/7853175-e9718f83cb7be470.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

![观察特定参数-2](https://upload-images.jianshu.io/upload_images/7853175-d14f66eb6c4f236a.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)


#### redefineClass命令

redefineClass命令用于替换一个类的字节码，可以简写成rdf，用于快速恢复生产环境，命令的参数没有mt命令复杂，但是需要有几点需要注意：

* 一个client对一个类执行rdf命令，就会锁定这个类，其他client就不能对相同的类执行rdf，直至client退出；

我们把getIntVal方法的开始部分的注释去掉，也就是：

```java
//        if (in < 7) {
//            System.out.println("in < 7, return");
//            throw new UnsupportedOperationException("test");
//        }
```

这一段内容，去掉之后，只要输入的参数小于7，那么就会抛出异常，我们使用mt命令配合custom来验证我们的rdf结果:

![执行rdf命令](https://upload-images.jianshu.io/upload_images/7853175-4a72e4fe17565ec9.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

可以看到，此时输入参数为5的时候抛出了那个期望的异常；

#### rollback命令

rollback命令用于将一个增强过的类恢复到初始状态，可以使用back简写，目前仅支持恢复到初始状态，后续会记录增强stage，然后恢复到上一次增强过的字节码：

![回滚一个类](https://upload-images.jianshu.io/upload_images/7853175-36f101706a53e6df.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

#### findClass命令

是不是曾经出现过因为jar包冲突导致的类加载错误的情况呢？findClass命令用于快速判断一个类是不是在目标JVM加载了，如果加载了，是从哪个jar包中加载的（jar一般都会有版本号，可以看看是不是从期望的jar版本中加载的），是被什么类加载器加载的，还可以仅仅使用类名（不含包名）来匹配，甚至通过正则表达式来匹配，可以使用简写fc:

![查找类信息](https://upload-images.jianshu.io/upload_images/7853175-d884109d8d7c6800.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

#### help命令

如果你不知道怎么使用一个命令，那么可以试试help命令：

![help命令](https://upload-images.jianshu.io/upload_images/7853175-ee35bbad73c18bb8.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

#### 如何重复发生上一次发送的命令

有时候需要重复上一次输入的命令，但是上一次命令输入内容很多，如何快速实现上一次命令的复制呢？下面的一些字符可以快速帮你搞定这件事情：*"p","r","s","go","last"*

![重复命令发送](https://upload-images.jianshu.io/upload_images/7853175-b1352dff32ff44a1.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

后续将支持命令历史记录回放的功能，目前仅支持回放上一次输入。

### 规划中的功能

* （1）线程相关功能，包括当前线程总数、活动线程数等等信息，并能获取到某个线程的调用堆栈等信息；
* （2）GC相关信息；
* （3）将方法执行上传调用链路信息包含进来，使得可以感知到具体是什么方法在调用这个方法，整体执行路径是什么样的；


