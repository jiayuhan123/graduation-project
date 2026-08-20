# Vue 管理端

基于 Vue 3、TypeScript、Vite、Vue Router、Pinia、Axios 和 Element Plus 的管理端基础工程。

## 环境要求

- Node.js `^20.19.0 || >=22.12.0`
- npm（使用仓库中的 `package-lock.json`）
- 本地 Gateway 默认运行在 `http://localhost:10000`

## 启动

```bash
cp .env.example .env.local
npm ci
npm run dev
```

`.env.local` 不提交到 Git。所有 `VITE_` 变量都会进入浏览器构建产物，禁止写入密码、Token 或 AccessKey。

## 常用命令

```bash
npm run lint
npm run lint:style
npm run format
npm run type-check
npm run test:unit
npm run build
npm run check
```

`npm run check` 是提交前的完整质量门禁。

## 基础能力

- `src/utils/request.ts`：Result 解包、统一异常、401 单次处理、TraceId、上传和下载；
- `src/stores/user.ts`：登录、服务端退出、会话恢复、角色与权限；
- `src/router`：模块化路由、登录守卫、RBAC、403/404 和安全回跳；
- `src/directives/permission.ts`：按钮权限指令 `v-permission`；
- `src/utils/error-reporter.ts`：统一异常上报入口；
- `src/config`、`src/constants`：运行配置和公共常量；
- `*.spec.ts`：Vitest 单元测试。

前端权限只用于界面展示和导航控制，不能替代后端鉴权。
