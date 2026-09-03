# File Service 通用文件服务

`file-service`（默认端口 8002）保存私有文件元数据，并使用阿里云 OSS Java SDK V2 将二进制内容存放到私有 Bucket。

## 能力与安全边界

- 上传、下载、元数据查询和删除；
- UUID 文件 ID、按用户隔离的随机对象键和安全文件名；
- 所有操作校验 `X-User-ID` 对应的所有权，不返回 OSS 公网地址；
- 上传流式计算 SHA-256，不把完整文件加载到内存；
- 元数据写入失败时补偿删除已上传对象；
- OSS 删除失败时保留元数据，便于重试与排查。

## API

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/files` | `multipart/form-data`，字段名 `file` |
| GET | `/api/files/{fileId}/metadata` | 查询当前用户文件元数据 |
| GET | `/api/files/{fileId}` | 代理下载私有对象 |
| DELETE | `/api/files/{fileId}` | 删除对象并逻辑删除元数据 |

## 必需配置

| 环境变量 | 说明 |
| --- | --- |
| `OSS_REGION` | 例如 `cn-hangzhou` |
| `OSS_ENDPOINT` | 例如 `https://oss-cn-hangzhou.aliyuncs.com` |
| `OSS_ACCESS_KEY_ID` | RAM 用户 AccessKey ID |
| `OSS_ACCESS_KEY_SECRET` | RAM 用户 AccessKey Secret |
| `OSS_BUCKET` | 提前创建的私有 Bucket |
| `FILE_MAX_SIZE` | 单文件上限，默认 `20MB` |

服务不会自动创建 Bucket，避免为运行身份授予 Bucket 管理权限。AccessKey 不提供仓库默认值，必须由环境变量或 Nacos 安全注入。

数据库默认为 `file_db`。本地启动前需按项目建表文档手工创建 `file_object`，并准备 PostgreSQL、Nacos 和上述 OSS 配置。
