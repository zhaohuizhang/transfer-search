# Transfer Contact Smart Search System - 部署文档

本文档详细介绍了如何在本地环境从零开始部署和运行本项目，包括数据库、Elasticsearch、Kafka 等基础设施的初始化以及测试数据的导入。

## 1. 环境准备

在开始部署之前，请确保您的机器已安装以下软件：

- **Java 17**: [下载地址](https://www.oracle.com/java/technologies/downloads/#java17)
- **Maven 3.8+**: [下载地址](https://maven.apache.org/download.cgi)
- **Docker & Docker Compose**: [Docker Desktop](https://www.docker.com/products/docker-desktop/) (推荐)

---

## 2. 编译构建

首先需要编译项目并生成可执行的 JAR 包。

```bash
# 进入工程根目录
cd /Users/xiaolulu/Downloads/codeworkspace/transfer-search

# 清理并构建（跳过测试）
mvn clean package -DskipTests
```

构建成功后，将在 `target/` 目录下生成 `transfer-search-0.0.1-SNAPSHOT.jar`。

---

## 3. 一键部署 (Docker Compose)

项目提供了一个完整的 `docker-compose.yml` 文件，可以一键拉起所有依赖环境及应用本身。

```bash
# 启动所有服务（构建镜像并以后台模式运行）
docker-compose up -d --build
```

### 包含的服务：
- **MySQL (3306)**: 业务主库 (`transfer_db`)
- **Redis (6379)**: 最近联系人缓存
- **Elasticsearch (9200)**: 智能搜索索引库
- **Kafka & Zookeeper (9092, 2181)**: 异步数据同步总线
- **Search Service (8080)**: 本应用

---

## 4. 数据库与 Elasticsearch 初始化

### 4.1 MySQL 初始化
应用启动时，Spring Data JPA 会根据 `entity` 定义自动在 `transfer_db` 中创建所需的表（`contact` 表）。
- **用户名**: `root`
- **密码**: `rootpassword`

### 4.2 Elasticsearch 索引
项目使用了 `sync-contact` 机制。当第一条数据通过应用写入时，`ContactConsumer` 会自动监听 Kafka 消息并创建 Elasticsearch 索引（`transfer_contact_index`）。

---

## 5. 测试数据导入

应用启动后，可以通过以下接口导入测试数据。导入后，数据会自动同步到 Elasticsearch。

### 5.1 导入脚本 (示例)

您可以使用 `curl` 命令导入一些初始数据：

```bash
# 导入张三
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

# 导入李四
curl -X POST http://localhost:8080/contacts \
-H "Content-Type: application/json" \
-d '{
  "userId": 1001,
  "contactName": "李四",
  "contactPinyin": "lisi",
  "contactInitial": "ls",
  "bankName": "工商银行",
  "accountNo": "6212261001113333",
  "phone": "13900139000"
}'
```

---

## 6. 部署验证

### 6.1 查看日志
确认服务是否正常启动且 Kafka 连接成功：
```bash
docker-compose logs -f search-service
```

### 6.2 API 自动文档 (Swagger)
访问以下地址查看所有可用的接口：
[http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)

### 6.3 搜索测试
等待几秒（确保 Kafka 同步完成），尝试搜索：
```bash
# 拼音首字母搜索
curl -X GET "http://localhost:8080/contacts/search?userId=1001&keyword=zs"
```
响应中应包含 `张三` 且 `highlightName` 包含 `<em>` 标签。

---

## 7. 常见问题排查

- **ES 无法连接**: 检查 Docker 日志，确保分配给 ES 的内存足够（默认已在 Compose 中配置 `512MB`）。
- **Kafka 消费失败**: 初始启动时 Kafka 可能准备较慢，应用会自动重试。
- **数据不同步**: 检查 `search-service` 的日志，看是否有 `ContactConsumer` 的报错记录。
