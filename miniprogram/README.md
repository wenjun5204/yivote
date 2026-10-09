# 咦投票（yivote）

微信群协作工具：投票凑时间（MVP P0）→ 收集填表（v1.1）→ 群待办（v1.2）→ 群日历（v1.3）。

## 目录

```
yivote/
├── miniprogram/   # 微信小程序（原生框架 + 官方组件；三 tab：我的事/群板/我的，发起从首页 hero 进入）
├── backend/       # Spring Boot + Java 21（包名 com.yivote.backend；云托管部署同 fe_backend_mini 链路）
├── api/openapi.yaml  # 前后端契约 SSOT（fe-be-contract-harness 驱动）
└── docs/
    ├── PRD-咦投票.md          # v1.4 定稿（唯一版本）
    └── prototype/             # 交互原型（index.html v1 定稿基底；index-optimized.html 为小程序对齐的交互 SSOT）
```

## 开发流程（fe-be-contract-harness 契约驱动）

1. 改接口先改 `api/openapi.yaml`，再走 Stage 2 checkpoint
2. 契约校验：`python3 lib/contract/cli.py verify api/openapi.yaml --stack java`（在 fe-be-contract-harness 仓）
3. 后端本地跑：`cd backend && ./mvnw spring-boot:run`（H2，端口 8080）——详见 [backend/README.md](../backend/README.md)（前端友好的后端调试/部署指南）
4. 小程序联调：开发者工具导入 `miniprogram/`，勾选「不校验合法域名」（USE_CLOUD=false）
5. 部署：微信云托管（新 AppID 新环境），Dockerfile 已就位；`contract cli live` 契约校验

## 硬约束（PRD）

- 个人主体：不碰资金（AA 已砍，D5）、AI 功能迁主体后（D4）
- 合规：UGC 过内容安全接口；订阅消息 ≤1 条/人/日；催办话术走召唤语气
