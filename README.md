# Transfer Contact Smart Search System

这是一个构建于 **Java 17** 和 **Spring Boot 3.x** 的微服务工程，旨在为银行 APP 提供高性能、智能化的转账联系人（通讯录）搜索功能。

## 系统特性

- **联系人智能搜索**：支持姓名、拼音全拼、拼音首字母、前缀及中英混输。
- **搜索增强**：返回包含匹配字段 (`matchedFields`)、高亮名称、原始分数及生成的 DSL 语句。
- **自动补全 (Suggest)**：基于 Elasticsearch Completion Suggester 实现毫秒级联想。
- **分析器调试**：提供 `/contacts/analyze` 接口，可视化拼音分词过程。
- **复合排序策略**：姓名匹配权重 (5.0) > 首字母 (4.0) > 拼音前缀 (3.0) > 银行名 (1.0)，结合 Redis 缓存实现最近转账联系人提权。
- **异构数据最终一致性**：利用 Kafka 实现 MySQL 到 Elasticsearch 的异步近实时同步。
- **大规模数据验证**：系统已通过 10,000 条真实分布数据的压力测试与性能验证。

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

## 智能搜索原理 (Banking-Level Pinyin Search)

本系统的 Pinyin 搜索实现遵循以下核心机制：

1. **拼音预处理 (Pinyin Generation)**：
   存储阶段利用 `pinyin4j` 将中文姓名自动转化为**全拼**（如 `zhangsan`）和**首字母**（如 `zs`）字段。

2. **边缘 N-gram 分词 (Edge N-gram)**：
   配置自定义分析器 `pinyin_analyzer`。通过 `edge_ngram` 将拼音片段化（如 `z`, `zh`, `zha`...），支持输入部分拼音即时匹配。

3. **编辑距离算法 (Fuzzy Matching)**：
   引入 `fuzzy: AUTO` 查询，基于 **Levenshtein Distance** 允许 1-2 个字符的拼音输入误差（如 `zagn` 匹配 `zhang`）。

4. **自动补全 (Completion Suggester)**：
   独立映射 `contactSuggest` 字段为 `completion` 类型，利用内存 FST 结构实现极低延迟的联想词推荐。

5. **复合加权 (Function Score)**：
   使用 `bool` 查询组合多维度命中，通过 `function_score` 动态调整权重，确保“姓名精确匹配”和“活跃联系人”始终排在首位。

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
