# 微信小程序前端

这是小学数学平台的原生微信小程序前端，使用 TypeScript、WXML、WXSS 和 TDesign Miniprogram。学生、教师和管理员共用同一个小程序账号体系；同一账号拥有多个角色时，可以在首页和个人中心切换当前身份。

## 目录结构

```text
front/miniprogram/
├─ project.config.json         # 微信开发者工具项目配置与 TS 编译开关
├─ project.private.config.json # 本机私有配置，由开发者工具生成且不提交
├─ package.json                # 类型检查、Lint 和格式化命令
├─ package-lock.json           # npm 依赖锁文件
├─ tsconfig.json               # TypeScript 严格模式配置
├─ eslint.config.mjs           # ESLint 配置
├─ .prettierrc.json            # Prettier 配置
├─ .gitignore
└─ miniprogram/                # 小程序运行源码根目录
   ├─ app.ts                   # 应用入口与全局生命周期
   ├─ app.json                 # 页面、窗口、分包等全局配置
   ├─ app.wxss                 # 全局样式
   ├─ sitemap.json             # 微信索引规则
   ├─ pages/                   # 页面；每页由 ts/json/wxml/wxss 四个文件组成
   ├─ components/              # 可复用原生组件
   ├─ behaviors/               # 组件间复用的行为
   ├─ api/                     # 按后端资源划分的接口模块
   ├─ services/                # 请求、认证、上传等基础服务
   ├─ types/                   # 后端响应和页面数据的 TypeScript 契约
   ├─ config/                  # 环境和运行配置
   ├─ constants/               # 跨模块常量与枚举
   ├─ utils/                   # 无业务状态的通用函数
   ├─ styles/                  # 公共样式、变量和主题
   └─ assets/                  # 图片、图标等静态资源
```

当前阶段已经实现：

- 微信登录和账号密码登录；
- 多角色展示、切换与本地记忆；
- 个人资料、私有头像、学生资料和教师资料维护；
- 首次设置密码、修改密码以及安全绑定或解绑当前微信；
- 管理员按权限查看用户目录、角色和权限；
- 登录态存储、统一请求和接口错误提示。
- 学生按本人年级和学期浏览已发布内容模块、选择知识点和练习难度；
- 学生从数据库固定题库创建自主练习，逐题立即判题、答错重试，并查看错题、掌握度变化和个性化推荐；
- 学生凭班级码加入或退出班级，查看已分配作业和提交状态；
- 学生可从作业列表幂等创建学习任务并直接进入答题；作答期间只保存答案，整份提交后统一反馈且不可修改；
- 教师创建、归档班级并维护学生，可布置统一或分层作业、逐学生分层发布、结束作业、查看提交并维护评语；
- 拍照批改明确标记为暂未开放，只保留数据库已有记录的本人查询入口；
- 所有后端雪花 ID 均按字符串接收和传递，避免 JavaScript 整数精度丢失。

当前学习功能只覆盖人教版小学数学计算类知识点，只使用数据库中已经审核启用的固定题目，不调用大模型生成题目。教学端支持统一、分层作业及发布时逐学生分层；游戏页面仍保留目录但不开放入口。

## 本地打开

1. 在本目录执行 `npm install`。
2. 使用微信开发者工具导入 `front/miniprogram`，在“设置 → 安全设置”中开启服务端口，然后执行“工具 → 构建 npm”，生成 TDesign 小程序组件。
3. 开发者工具会根据 `project.config.json` 编译 TypeScript；当前 AppID 已配置为 `wxa69fc7e3c9ecd0d4`。
4. 本地开发接口默认是 `http://127.0.0.1:10000`，配置位于 `miniprogram/config/environment.ts`。开发者工具调试时需关闭合法域名校验，并确保网关与用户服务已经启动；上传和展示私有头像时还需启动文件服务。
5. 真机不能访问电脑自己的 `127.0.0.1`；真机联调时需改成电脑局域网 IP，并保证手机和电脑处在同一网络。
6. 后端微信登录还需配置与 AppID 对应的 `WECHAT_APP_ID`、`WECHAT_APP_SECRET`，并启用微信登录配置。只测试账号密码登录时可以先不配置。
7. 开发者工具会生成 `project.private.config.json`，该文件只保存本机设置，不提交到 Git。

## 工程检查

```shell
npm run typecheck
npm run lint
npm run format:check
npm run check
```

## 后续约定

- 页面路径使用 `pages/<页面名>/index.*`，逻辑文件统一使用 TypeScript。
- 页面私有组件放在页面目录内的 `components/`，跨页面组件放在全局 `components/`。
- `api/` 只描述接口调用，登录态、请求头、错误归一化等放入 `services/`。
- 主包只保留导航、登录和个人中心；学习、教学和游戏页面按领域分包。
- 除小程序原生能力不能满足需求外，不引入 Taro、uni-app 等跨端框架。
