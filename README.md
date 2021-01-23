# easy-tool

你是否在烦恼如何快速构建一个Scala应用程序，因为你不得不重复做一些Coding，例如配置，日志，工具类等，你不得不针对每个项目写一些仅适用于该项目的工具类，无法做到重复利用。本项目提供了一系列初始化应用的常见工具，方面你快速构建你的Scala应用。

### easy-conf

快速将你的配置文件封装到Scala Map中，支持不同的优先级配置，不同的profile配置。

#### 如何使用

```scala
val conf = Config(args, configs=Seq("app01", "app02"), noArgConfigs=Seq("app03"))
val app01 = conf.getConfig("app01")
val app02 = conf.getConfig("app02")
val app03 = conf.getConfig("app03")

val configValue = app01.getProp("your config key")
```

Config类需要三个参数：

1. args: 命令行参数数组
2. configs: 想要自动配置的文件（不包含文件后缀）列表，默认Seq("app")
3. noArgConfigs: 想要自动配置的文件（不包含文件后缀）列表, 命令行参数配置不会覆盖该文件列表配置，默认Seq()

支持的配置文件类型

1. yaml配置文件: .yml or .yaml (推荐使用)
2. properties配置文件： .properties

#### 优先级（由高到低）

1. nacos配置中心
2. 命令行参数
3. 指定的jar包外配置文件
4. 指定profle的jar包外配置文件
5. jar包外配置文件
6. 指定profile的classpath下配置文件
7. classpath下配置文件

#### 占位符解析

通常配置文件中需要引用其它配置项的值，该模块支持将类似的占位符形式（${conf01:default_value}）解析为配置的实际值。例如配置

```yaml
app:
  conf01: c01
  conf02: ${app.conf01}
  conf03: ${app.conf01:c011}
  conf04: ${app.conf041:c041}
  conf05: "${ app.conf051 : c051  }"
```

将转化如下：

```yaml
app:
  conf01: c01
  conf02: c01
  conf03: c01
  conf04: c041
  conf05: c051
```

#### 样例类绑定

Props类提供绑定scala样例类方法，如下

```scala
def bind[T <: Product](
      formats: Formats = DefaultFormats
  )(implicit mf: scala.reflect.Manifest[T]): T
```

formats用法可参考json4s，主要用于注册枚举类的转换，例如：

```scala
import java.util

//case class
case class Application(
                        name: String,
                        sourceField: Seq[String],
                        sourceConfig: SourceConfig,
                        status: Status = FINISHED
                      )

case class SourceConfig(source: String, sink: String)

object Status extends util.Enumeration {
  type Status = Value
  val RUNNING, FINISHED = Value
}

//usage
val conf = Config(Array(), Seq("bind01"))
val bind01 = conf.getConfig("bind01")
val formats = DefaultFormats + new EnumNameSerializer(Status)
val application = bind01.bind[Application](formats)
```

