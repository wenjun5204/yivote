# 咦投票后端（backend/）

Spring Boot + Java 21，给小程序提供全部 API。**这份文档面向前端同学**：不用懂 Java，照着抄命令就能跑起来。

## 一、本地调试（前端联调用）

### 1. 启动后端

```bash
cd /Users/liuwenjun/project/yivote/backend
./mvnw spring-boot:run
```

第一次启动会下载依赖，等 1-2 分钟；看到这行就是成功了：

```
Started BackendApplication in X.XXX seconds
Tomcat started on port 8080
```

> `./mvnw` 是项目自带的 Maven 启动器，**不需要你装 Java/Maven**……前提是装过 Java 21。没装的话：`brew install openjdk@21`，或者问后端同学。

### 2. 确认活着

浏览器开 `http://127.0.0.1:8080/api/ping`，看到 `{"status":"ok"}` 就 OK。

### 3. 小程序连它

- 微信开发者工具导入 `miniprogram/` 目录
- **详情 → 本地设置 → 勾选「不校验合法域名、web-view（业务域名）、TLS 版本以及 HTTPS 证书」**（必须勾，否则 `wx.request` 请求本地 http 会被拦截，报 `url not in domain list`）
- 网络层配置在 [miniprogram/utils/request.js](../miniprogram/utils/request.js)：
  - `USE_CLOUD = false`（现状）→ 直连 `http://127.0.0.1:8080`
  - 登录是自动的：首次进入小程序静默登录，本地用随机 `dev-xxx` 当 openid（每个设备一个身份，开发者工具「清除缓存」会换人——可以用来模拟多用户）

### 4. 常见报错对照表

| 报错 | 原因 | 解法 |
|---|---|---|
| `net::ERR_CONNECTION_REFUSED` | **后端没启动**（最常见） | 按上面第 1 步起后端 |
| `url not in domain list` | 没勾「不校验合法域名」 | 本地设置里勾上 |
| 改了后端代码不生效 | spring-boot:run 没自动重启 | `Ctrl+C` 停掉再 `./mvnw spring-boot:run` |
| 真机预览连不上 | 手机访问不到你 Mac 的 127.0.0.1 | 同一 WiFi，把 `request.js` 的 `BASE_URL` 临时改成 `http://你Mac的局域网IP:8080`（`ipconfig getifaddr en0` 查 IP） |

### 5. 日常命令速查

```bash
# 启动（前台跑,Ctrl+C 停）
./mvnw spring-boot:run

# 后台挂着(日志进 /tmp)
nohup ./mvnw spring-boot:run > /tmp/yivote-backend.log 2>&1 &

# 停止
lsof -ti :8080 | xargs kill

# 看实时日志
tail -f /tmp/yivote-backend.log

# 看 API 有哪些(浏览器打开)
http://127.0.0.1:8080/swagger-ui.html
```

## 二、数据说明

本地**零配置**：默认用 H2 文件数据库，数据存在 `backend/data/` 下。

- 后端重启**数据不丢**；想清空重来：停掉后端，`rm -rf backend/data/`，再启动
- 表是 JPA 按实体自动建的（`ddl-auto=update`），**不需要手工建表**；后端同学改了实体重启即生效
- `backend/data/` 已在 `.gitignore`，不会误提交

## 三、部署

### 路径 A：微信云托管（推荐，原设计）

后端就是按云托管设计的（[Dockerfile](Dockerfile) 已就位）：

1. 微信云托管控制台「新建服务」→ 接入方式选**代码库/本地 Dockerfile**，指到 `backend/Dockerfile`
2. 平台自动构建镜像（Maven 多阶段构建 + JRE 21 运行，监听 80）
3. 同环境开一个云托管 MySQL——平台自动注入 `MYSQL_ADDRESS / MYSQL_USERNAME / MYSQL_PASSWORD` 环境变量，[application-cloud.properties](src/main/resources/application-cloud.properties) 直接吃，**不需要改任何代码**
4. 拿到环境 ID 和服务名，改 [miniprogram/utils/request.js](../miniprogram/utils/request.js)：
   ```js
   const USE_CLOUD = true
   const CLOUD_ENV = '云托管环境ID'
   const SERVICE_NAME = '服务名'
   ```
5. 云托管通道 `wx.cloud.callContainer` 会自动注入真实 openid（`X-WX-OPENID`），小程序**天然合法域名豁免**，正式发布无需备案域名配置 request 合法域名

> 云托管收费参考：按量计费（CPU/内存/流量），个人主体可开，有免费额度。

### 路径 B：自有服务器（如阿里云 ECS）

适合内测自测，**正式发布受小程序合法域名限制**（需已备案域名 + HTTPS + 微信后台配置）：

```bash
# 服务器上(装好 docker + docker compose)
docker build -t yivote-backend ./backend
docker run -d -p 8080:80 \
  -e MYSQL_ADDRESS=你的MySQL地址:3306 \
  -e MYSQL_USERNAME=xxx -e MYSQL_PASSWORD=xxx \
  -e MYSQL_DATABASE=yivote \
  yivote-backend
```

不接 MySQL 也可以先用 H2 跑通：去掉 MYSQL 环境变量、`-e SPRING_PROFILES_ACTIVE=default` 即可（容器内 H2 文件在 `/app/data`，记得挂卷持久化）。

### 部署前检查清单

- [ ] `cd backend && ./mvnw test` 全绿（AGENTS.md 约定）
- [ ] 契约没漂移：`api/openapi.yaml` 与代码一致（运行时 `/v3/api-docs` 可比对）
- [ ] 云托管路径：request.js 三个常量已回填
- [ ] 自有服务器路径：HTTPS 证书 + 备案域名 + 微信后台 request 合法域名已配

## 四、代码结构（想找接口实现时看这里）

```
backend/src/main/java/com/yivote/backend/
├── controller/ApiController.java  # 全部接口入口(对应 openapi.yaml 的每个 path)
├── service/                        # 业务逻辑(每个域一个)
│   ├── AuthService / VoteService / CollectService / TodoService / GroupService
├── entity/                         # 数据表(一个类一张表)
├── repository/                     # 数据访问
└── dto/                            # 接口出入参(与 openapi.yaml schema 一一对应)
```

前端对接口时：**以 [api/openapi.yaml](../api/openapi.yaml) 为准**（SSOT），出入参与 schema 严格对齐；改接口的流程是「先改 yaml → 再改后端 → 最后小程序」。
