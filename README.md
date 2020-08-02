# 商品实时推荐系统

### 1. 系统架构  v2.0

- **1.1 系统架构图**

  <div align=center><img src="resources/pic/v2.0架构.png" width="100%" height="100%"></div>

- **1.2模块说明**

- a.在日志数据模块(flink-2-hbase)中,又主要分为6个Flink任务:

  - **日志导入 `src/main/java/com/demo/task/LogTask.java`**

    - 目的：从Kafka接收的数据直接导入进Hbase事实表

    - 描述：保存完整的日志log,日志中包含了用户Id,用户操作的产品id,操作时间,行为(如购买,点击,推荐等). 数据按时间窗口统计数据大屏需要的数据,返回前段展示

    - 输出：数据存储在Hbase的con表
    
      | userid | productid | time | action      |
      | ------ | --------- | ---- | ----------- |
      | xxxx   | xxxx      | ts   | 收藏/点击等 |

  - **产品画像记录 src/main/java/com/demo/task/ProductProtaritTask.java**

    - 目的： 实现基于标签的推荐逻辑

    - 描述： 当用户对产品产生交互行为时， 查找`mysql`数据库获取用户性别、年龄两个字段。 将年龄进行分组， 分成10-20， 20-30， 30-40， 40-50， 50-60， 60+ 6个年龄组。 将Hbase`prod`表中关于此产品id的用性别，年龄组分别+1

    - 输出结果：数据存储在Hbase的prod表

      before:

      |               | **sex** |        | **age** |      |      |      |      |      |      |
      | ------------- | ------- | ------ | ------- | ---- | ---- | ---- | ---- | ---- | ---- |
      | **productid** | male    | female | 10s     | 20s  | 30s  | 40s  | 50s  | 60s  | 0s   |
      | 1             | 0       | 0      | 0       | 0    | 0    | 0    | 0    | 0    | 0    |

      after:

      |               | **sex** |        | **age** |      |      |      |      |      |      |
      | ------------- | ------- | ------ | ------- | ---- | ---- | ---- | ---- | ---- | ---- |
      | **productid** | male    | female | 10s     | 20s  | 30s  | 40s  | 50s  | 60s  | 0s   |
      | 1             | 1       | 0      | 1       | 0    | 0    | 0    | 0    | 0    | 0    |

  - **事实热度榜 src/main/java/com/demo/task/TopProductTask.java**

    - 目的： 实现基于热度的推荐逻辑 
    - 描述：通过Flink时间窗口机制,统计当前时间的实时热度,并将数据缓存在Redis中；通过Flink的窗口机制计算实时热度,使用ListState保存一次热度榜

    - 输出结果：数据存储在redis中,按照时间戳存储list

  - **用户-产品浏览历史 src/main/java/com/demo/task/UserHistoryTask.java**

    - 目的：实现基于协同过滤的推荐逻辑 

    - 描述：通过Flink去记录用户浏览过这个类目下的哪些产品,为后面的基于Item的协同过滤做准备
      实时的记录用户的评分到Hbase中,为后续离线处理做准备.

    - 数据存储在Hbase的p_history以及u_history表. 假设用户2浏览了产品3：

      before:

      u_history

      |      | p    |      |      |      |
      | ---- | ---- | ---- | ---- | ---- |
      |      | 1    | 2    | 3    | …    |
      | 1    | 0    | 0    | 0    | 0    |
      | 2    | 0    | 0    | 0    | 0    |
      | 3    | 0    | 0    | 0    | 0    |

      p_history

      |      | u    |      |      |      |
      | ---- | ---- | ---- | ---- | ---- |
      |      | 1    | 2    | 3    | …    |
      | 1    | 0    | 0    | 0    | 0    |
      | 2    | 0    | 0    | 0    | 0    |

      after:

      u_history

      |      | p    |      |      |      |
      | ---- | ---- | ---- | ---- | ---- |
      |      | 1    | 2    | 3    | …    |
      | 1    | 0    | 0    | 0    | 0    |
      | 2    | 0    | 0    | 0    | 0    |
      | 3    | 0    | 1    | 0    | 0    |

      p_history

      |      | u    |      |      |      |
      | ---- | ---- | ---- | ---- | ---- |
      |      | 1    | 2    | 3    | …    |
      | 1    | 0    | 0    | 0    | 0    |
      | 2    | 0    | 0    | 1    | 0    |

  - **用户-兴趣 src/main/java/com/demo/task/UserInterestTask.java**

    - 目的：实现基于上下文的推荐逻辑
    - 根据用户对同一个产品的操作计算兴趣度,计算规则通过操作间隔时间(购物 - 浏览 < 100s)则判定为一次兴趣事件。通过Flink的ValueState实现,如果用户的操作Action=3(收藏),则清除这个产品的state,如果超过100s没有出现Action=3的事件,也会清除这个state

    数据存储在Hbase的u_interest表

  - 用户画像计算 -> 实现基于标签的推荐逻辑

    v1.0按照三个维度去计算用户画像,分别是用户的颜色兴趣,用户的产地兴趣,和用户的风格兴趣.根据日志不断的修改用户画像的数据,记录在Hbase中.

    数据存储在Hbase的user表

  - 

    

- b. web模块

  - 前台用户界面

    该页面返回给用户推荐的产品list

  - 后台监控页面

    该页面返回给管理员指标监控



### 2.推荐引擎逻辑说明

- **2.1 基于热度的推荐逻辑**

  现阶段推荐逻辑图

<div align=center><img src="resources/pic/v2.0用户推荐流程.png" width="80%" height="100%"></div>

​    根据用户特征，重新排序热度榜，之后根据两种推荐算法计算得到的产品相关度评分，为每个热度榜中的产品推荐几个关联的产品

- **2.2 基于产品画像的产品相似度计算方法**

  基于产品画像的推荐逻辑依赖于产品画像和热度榜两个维度,产品画像有三个特征,包含color/country/style三个角度,通过计算用户对该类目产品的评分来过滤热度榜上的产品

  <div align=center><img src="resources/pic/基于产品画像的推荐逻辑.png" width="80%" height="100%"></div>

  在已经有产品画像的基础上,计算item与item之间的关联系,通过**余弦相似度**来计算两两之间的评分,最后在已有物品选中的情况下推荐关联性更高的产品.

| 相似度 | A    | B    | C    |
| ------ | ---- | ---- | ---- |
| A      | 1    | 0.7  | 0.2  |
| B      | 0.7  | 1    | 0.6  |
| C      | 0.2  | 0.6  | 1    |

  

- **2.3 基于协同过滤的产品相似度计算方法**

  根据产品用户表（Hbase） 去计算公式得到相似度评分：

  <img src="resources/pic/%E5%9F%BA%E4%BA%8E%E7%89%A9%E5%93%81%E7%9A%84%E5%8D%8F%E5%90%8C%E8%BF%87%E6%BB%A4%E5%85%AC%E5%BC%8F.svg" width="30%" height="30%">
                 

### 3. 前台推荐页面

 当前推荐结果分为3列,分别是热度榜推荐,协同过滤推荐和产品画像推荐

<div align=center><img src="resources/pic/推荐页面.png" width="80%" height="100%"></div>

### 4. 后台数据大屏

 **在后台上显示推荐系统的实时数据**,数据来自其他Flink计算模块的结果.目前包含热度榜和1小时日志接入量两个指标. 
真实数据位置在resource/database.sql

<div align=center><img src="resources/pic/后台数据.png" width="80%" height="100%"></div>

### 5. 部署说明 
>以下的部署均使用Docker，对于搭建一套复杂的系统，使用docker来部署各种服务中间件再合适不过了。这里有一套简单的[Docker入门系列](https://blog.csdn.net/qqHJQS/column/info/33078)

详细的部署说明已经写了一篇文章来说明了，按照流程即可运行项目，无需自己搭建任何组件。
[文章地址](https://xinze.fun/2019/11/19/%E4%BD%BF%E7%94%A8Docker%E9%83%A8%E7%BD%B2Flink%E5%A4%A7%E6%95%B0%E6%8D%AE%E9%A1%B9%E7%9B%AE/)

Hbase部署说明-》[使用Docker搭建伪分布式Hbase(外置Zookeeper)](https://www.jianshu.com/p/3aabe3a152a8)
Kafka部署说明-》[使用Docker部署Kafka时的网络应该如何配置](https://www.jianshu.com/p/52a505354bbc)





#### docker-compose

#### 1. hbase

之后是构建 hbase 表结构

```shell
[root@docker-linux recommend]# docker exec -ti hbase bash
root@docker-linux:/opt/hbase# hbase shell /opt/hbase_ini.sql
```

通过 list 命令可以看到我们创建的表都已成功

```shell
hbase(main):001:0> list
TABLE                                                             ··· 
8 row(s) in 0.1390 seconds
=> ["con", "p_history", "prod", "ps", "px", "u_history", "u_interest", "user"]
```

#### 2. 生成redis热点数据

进入redis，创建10个热度数据

```shell
[root@docker-linux recommend]# docker exec -ti redis bash
root@7735aff1391f:/bin# redis-cli
```

启动客户端后，之后按照如下格式创建 set 0 123之类的创建从0-9的10条数据即可，最后一位为商品id，在0-999内任意取值。

#### 3. kafka 消息生成器

### 3. 启动 Kafka 消息生成器

进入kafka容器并启动shell脚本即可，脚本会按照每秒一次的频率发送message到log这个topic里。

```shell
[root@docker-linux recommend]# docker exec -ti kafka bash
bash-4.4# sh /opt/generator.sh
```

验证成功发送消息的方法是重新开一个连接进入kafka容器并启动Consumer，注意kafka内部端口号配置的是9093

```shell
bash-4.4# $KAFKA_HOME/bin/kafka-console-consumer.sh --bootstrap-server kafka:9093 --topic con
```

正确看到如下格式的消息即可

```shell
558,559,1574180802,1
576,576,1574180819,3
585,585,1574180828,3
594,594,1574180837,3
603,603,1574180846,3
613,613,1574180856,1
```

