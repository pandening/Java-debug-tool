## Java-debug-tool 4.0 features list

* 一种字节码增强框架 *"ByteDance"* ,需要支持如下特性：
    * 支持任意位置插入代码，包括方法调用前、方法执行前、方法退出前、方法退出后、每一行代码执行之前、每一行代码执行之后：
        * beforeInvoke
        * beforeDoInvoke
        * beforeInvokeLine
        * afterInvokeLine
        * beforeReturn
        * beforeThrow
        * afterInvoke
        
注：4.0是一个新的里程碑，ByteDance框架包名不再使用 io.javadebug.*的命令规则，使用
io.bytedance.*作为该框架的包命令规则；        


## Timeline Scheduling

* 整体方案设计：2019-9月中旬（0.5M）
* 搭建整体代码框架，做好抽象工作：2019-9月底（0.5M）
* 完成所有通知开发：2019-11月底（2M）

4.0开发周期依然为3个月，任务比较困难，但是考虑到3.0可能提前完成，所以最终可能也会在11月底发布4.0；