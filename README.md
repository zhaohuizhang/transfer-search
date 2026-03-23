# Transfer Contact Smart Search System

这是一个构建于 **Java 17** 和 **Spring Boot 3.x** 的微服务工程，旨在为银行 APP 提供高性能、智能化的转账联系人（通讯录）搜索功能。

## 系统特性

- **联系人智能搜索**：支持姓名、拼音全拼、拼音首字母、银行名称模糊/精确匹配。
- **联系人手机号搜索**：通过手机号前缀进行快速检索。
- **搜索高亮**：精准反馈匹配的关键信息（对 `contactName` 字段进行 `<em>` 标签高亮）。
- **复合排序策略**：在搜索时，姓名匹配权重最高（5），拼音次之（3），银行名最后（2），并将 **最近联系人** 的召回结果提权。
- **语音搜索支持**：支持解析类似于 "给张三转账" 的自然语言文本，并触发搜索逻辑。
- **最近联系人缓存**：利用 Redis ZSET 缓存用户的最近使用联系人名单，提高复用的效率及搜索排序权重。
- **异构数据最终一致性**：引入了 **Kafka**，当通过 MySQL 写入或更改联系人数据后，将产生异步事件以通知 **Elasticsearch** 建立索引，保障搜索的高性能并解耦核心服务。
- **一键运行测试**：自带完整的 `docker-compose.yml` 及 `Dockerfile` ，通过几个命令即可在本地搭建起所有基础设施并运行服务。

## 架构

  APP端请求
     ↓
[Search Service (Spring Boot/8080)]  --> [Redis (7.0/6379)] (缓存近期联系人)
     ↓
     +---> [MySQL (8.0/3306)] (主数据存储)
     |
     +---> (Kafka Producer: 发送 `contact-sync` 事件) --> [Kafka (3.4/9092) & Zookeeper (3.8/2181)]
               ↓
               (Kafka Consumer: 异步监听拉取)
               ↓
          [Elasticsearch (8.10/9200)] (写入 Document / 供 Search Service 深度检索)

## 快速启动与构建

本项目依赖于 Docker 环境来部署所有的基础设施模块（MySQL, Redis, Zookeeper, Kafka, Elasticsearch）。

### 1. 编译构建
```bash
# 进入工程目录
cd /path/to/transfer-search

# 跳过测试构建 Jar 包
mvn clean package -DskipTests
```

### 2. 构建镜像并启动容器群
```bash
# 启动所有后台服务依赖并且拉起 search-service 本身。
docker-compose up -d --build

# 查看运行日志确认启动状态
docker-compose logs -f search-service
```

## 测试 API 接口示例

启动完成后，应用挂载于本地 `8080` 端口。

### 1. 新增联系人
```bash
curl -X POST http://localhost:8080/contacts \
-H "Content-Type: application/json" \
-d '{
  "userId": 1001,
  "contactName": "张三",
  "contactPinyin": "zhangsan",
  "contactInitial": "zs",
  "bankName": "招商银行",
  "accountNo": "6222021001112222",
  "phone": "13800138000"
}'
```

*(新增完成后，后台会通过 Kafka 异步同步写入 Elasticsearch。)*

### 2. 搜索联系人
（例如按拼音首字母 `zs` 搜索）：
```bash
curl -X GET "http://localhost:8080/contacts/search?userId=1001&keyword=zs"
```
*(响应中会包含联系人信息及 `highlightName`。)*

### 3. 语音/文本语义搜索
```bash
curl -X POST http://localhost:8080/voice/search \
-H "Content-Type: application/json" \
-d '{
  "userId": 1001,
  "text": "给张三转账"
}'
```

### 4. 获取最近联系人
```bash
curl -X GET "http://localhost:8080/contacts/recent?userId=1001"
```

## 目录与代码结构规划

- `controller`: 包含 `ContactController`, `VoiceController` 暴露 RESTFul 接口。
- `service`: `ContactServiceImpl`, `VoiceServiceImpl` 提供核心业务与复合检索逻辑封装。
- `repository`: JPA `ContactRepository`。
- `search`: 基于 `ElasticsearchOperations` 的聚合检索 `ContactSearchRepository`，实现权重配比（function_score）与组装亮显高亮语法。
- `kafka`: 消息生产者 `ContactProducer` 和协同索引的消费者 `ContactConsumer`。
- `entity/dto/mapper`: 数据持久化及展现层模型转换。
