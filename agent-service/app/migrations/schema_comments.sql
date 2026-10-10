-- 当前教学 Agent 数据库结构的中文元数据注释。
-- 本文件不是一次性版本迁移，由 Database.apply_schema_comments() 在 LangChain 向量表
-- 初始化完成后幂等执行，因此既适用于已有数据库，也适用于全新数据库。

COMMENT ON TABLE public.ai_agent_run IS
'教学 Agent 单次运行审计主表：记录会话、操作者角色、输入输出、模型与提示词标识、工具和 RAG 统计、执行状态、耗时及错误摘要。';
COMMENT ON COLUMN public.ai_agent_run.run_id IS
'本次 Agent 运行的全局唯一 UUID，由 Java 网关在接收聊天请求时生成。';
COMMENT ON COLUMN public.ai_agent_run.conversation_id IS
'前端会话 UUID；同一会话可包含多次运行，并用于关联 LangGraph 多轮上下文。';
COMMENT ON COLUMN public.ai_agent_run.actor_id IS
'发起本次运行的已认证用户 ID，来源于 Java 登录上下文。';
COMMENT ON COLUMN public.ai_agent_run.active_role IS
'用户本次运行采用的角色，例如 teacher 或 admin；不同角色的上下文和权限彼此隔离。';
COMMENT ON COLUMN public.ai_agent_run.status IS
'运行状态：RUNNING 表示执行中，SUCCEEDED 表示成功，FAILED 表示失败，CANCELLED 表示被取消。';
COMMENT ON COLUMN public.ai_agent_run.input_text IS
'脱敏后的用户输入文本，用于运行审计和问题追踪。';
COMMENT ON COLUMN public.ai_agent_run.output_text IS
'脱敏后的 Agent 最终回答；运行尚未完成或未形成回答时为空。';
COMMENT ON COLUMN public.ai_agent_run.model_name IS
'本次运行实际配置的对话模型名称。';
COMMENT ON COLUMN public.ai_agent_run.prompt_version IS
'本次运行使用的系统提示词标识，用于效果对比、回归评测和问题追踪。';
COMMENT ON COLUMN public.ai_agent_run.tool_call_count IS
'本次运行实际尝试的工具调用总数，成功和失败调用都会计入。';
COMMENT ON COLUMN public.ai_agent_run.rag_hit_count IS
'本次运行经相关性筛选后采用的知识库片段总数。';
COMMENT ON COLUMN public.ai_agent_run.error_type IS
'运行失败或取消时记录的异常类型名称；成功运行为空。';
COMMENT ON COLUMN public.ai_agent_run.error_message IS
'运行失败或取消时保存的错误摘要，最大 1000 字符；成功运行为空。';
COMMENT ON COLUMN public.ai_agent_run.started_at IS
'Agent 运行开始时间，使用带时区的 PostgreSQL 时间戳。';
COMMENT ON COLUMN public.ai_agent_run.finished_at IS
'Agent 成功、失败或取消的结束时间；仍在运行时为空。';
COMMENT ON COLUMN public.ai_agent_run.duration_ms IS
'从开始到结束的总耗时，单位为毫秒。';
COMMENT ON COLUMN public.ai_agent_run.metadata IS
'运行级扩展元数据，使用 JSONB 保存当前固定字段以外的信息。';

COMMENT ON TABLE public.ai_feedback IS
'Agent 回答反馈表：保存用户对某次运行的点赞或点踩及可选说明；同一用户对同一次运行最多保留一条反馈。';
COMMENT ON COLUMN public.ai_feedback.id IS
'反馈记录的全局唯一 UUID。';
COMMENT ON COLUMN public.ai_feedback.run_id IS
'被评价的 Agent 运行 UUID，外键关联 ai_agent_run；运行删除时反馈级联删除。';
COMMENT ON COLUMN public.ai_feedback.actor_id IS
'提交反馈的已认证用户 ID；服务会校验该用户就是对应运行的发起者。';
COMMENT ON COLUMN public.ai_feedback.rating IS
'反馈评分：1 表示正向反馈，-1 表示负向反馈。';
COMMENT ON COLUMN public.ai_feedback.comment IS
'用户补充的文字反馈，最多 1000 字符，可以为空。';
COMMENT ON COLUMN public.ai_feedback.created_at IS
'反馈首次创建时间，使用带时区时间戳。';
COMMENT ON COLUMN public.ai_feedback.updated_at IS
'反馈最近更新时间；重复评价会更新原记录而不是新增重复记录。';

COMMENT ON TABLE public.ai_knowledge_document IS
'RAG 知识文档主表：保存文档来源、内容校验值、脱敏原文、向量模型信息、分块数量及索引生命周期状态。';
COMMENT ON COLUMN public.ai_knowledge_document.id IS
'知识文档的全局唯一 UUID，也是所有向量分块归属文档的业务标识。';
COMMENT ON COLUMN public.ai_knowledge_document.title IS
'知识文档标题，最长 200 字符。';
COMMENT ON COLUMN public.ai_knowledge_document.source_uri IS
'文档来源的唯一标识，可为网页地址或业务自定义 URI；相同来源执行更新而非重复新增。';
COMMENT ON COLUMN public.ai_knowledge_document.source_type IS
'文档来源类型：TEXT、MARKDOWN、POLICY、LESSON 或 TEXTBOOK。';
COMMENT ON COLUMN public.ai_knowledge_document.checksum IS
'标题、来源类型、正文和 metadata 规范化后计算的 SHA-256，用于判断内容是否变化。';
COMMENT ON COLUMN public.ai_knowledge_document.status IS
'索引状态：INDEXING 表示处理中，ACTIVE 表示可检索，FAILED 表示失败，DELETED 表示已软删除。';
COMMENT ON COLUMN public.ai_knowledge_document.metadata IS
'文档的可扩展业务标签和附加属性，使用 JSONB 保存。';
COMMENT ON COLUMN public.ai_knowledge_document.created_by IS
'发起知识入库的已认证用户 ID。';
COMMENT ON COLUMN public.ai_knowledge_document.created_at IS
'知识文档主记录首次创建时间。';
COMMENT ON COLUMN public.ai_knowledge_document.updated_at IS
'文档内容、元数据或索引状态最近更新时间。';
COMMENT ON COLUMN public.ai_knowledge_document.raw_content IS
'脱敏后的完整文档原文，用于重新切分和重新建立向量索引。';
COMMENT ON COLUMN public.ai_knowledge_document.chunk_count IS
'当前文档成功写入向量库的有效分块数量。';
COMMENT ON COLUMN public.ai_knowledge_document.embedding_model IS
'当前文档建立向量索引时使用的 Embedding 模型名称。';
COMMENT ON COLUMN public.ai_knowledge_document.embedding_dimensions IS
'当前文档向量的维度，必须与 ai_knowledge_chunk.embedding 的 VECTOR 维度一致。';
COMMENT ON COLUMN public.ai_knowledge_document.indexed_at IS
'文档最近一次成功完成全部向量分块写入的时间。';

COMMENT ON TABLE public.ai_knowledge_chunk IS
'LangChain PGVectorStore 管理的 RAG 知识分块向量表：保存片段正文、Embedding、文档归属及生成引用所需的来源信息。';
COMMENT ON COLUMN public.ai_knowledge_chunk.id IS
'知识片段 UUID，由文档 UUID 和分块序号稳定生成；相同文档相同序号可得到相同 ID。';
COMMENT ON COLUMN public.ai_knowledge_chunk.content IS
'切分后的知识片段正文，是生成 Embedding 和提供模型引用的原始内容。';
COMMENT ON COLUMN public.ai_knowledge_chunk.embedding IS
'知识片段的 pgvector 向量，用于余弦距离语义检索和 MMR 候选选择。';
COMMENT ON COLUMN public.ai_knowledge_chunk.document_id IS
'所属知识文档 UUID 的字符串形式；由应用维护与 ai_knowledge_document 的逻辑关联。';
COMMENT ON COLUMN public.ai_knowledge_chunk.chunk_index IS
'片段在原文中的零基序号，用于保持顺序并稳定生成片段 ID。';
COMMENT ON COLUMN public.ai_knowledge_chunk.title IS
'冗余保存的文档标题，使检索结果无需回表即可生成可读引用。';
COMMENT ON COLUMN public.ai_knowledge_chunk.source_uri IS
'冗余保存的文档来源 URI，用于在 RAG 结果中返回可追溯来源。';
COMMENT ON COLUMN public.ai_knowledge_chunk.source_type IS
'冗余保存的文档来源类型，用于检索过滤和结果展示。';
COMMENT ON COLUMN public.ai_knowledge_chunk.metadata IS
'片段扩展元数据，保存除固定来源字段之外的业务标签。';

COMMENT ON TABLE public.ai_schema_migration IS
'Agent 服务自有数据库结构变更记录表：保存已执行 SQL 脚本标识和校验值，防止重复执行或脚本被静默修改。';
COMMENT ON COLUMN public.ai_schema_migration.version IS
'已执行数据库脚本的唯一文件标识。';
COMMENT ON COLUMN public.ai_schema_migration.checksum IS
'脚本替换配置占位符后计算的 SHA-256，用于检测已执行脚本内容是否变化。';
COMMENT ON COLUMN public.ai_schema_migration.applied_at IS
'数据库脚本成功执行并登记的时间。';

COMMENT ON TABLE public.ai_tool_call IS
'Agent 工具调用审计表：逐次记录工具名称、脱敏参数、脱敏结果、执行状态和耗时，并关联所属 Agent 运行。';
COMMENT ON COLUMN public.ai_tool_call.id IS
'工具调用审计记录的全局唯一 UUID。';
COMMENT ON COLUMN public.ai_tool_call.run_id IS
'所属 Agent 运行 UUID，外键关联 ai_agent_run；运行删除时工具记录级联删除。';
COMMENT ON COLUMN public.ai_tool_call.tool_name IS
'实际调用的 LangChain 工具名称。';
COMMENT ON COLUMN public.ai_tool_call.arguments IS
'模型提交给工具的参数；保存前会递归脱敏并限制最大审计体积。';
COMMENT ON COLUMN public.ai_tool_call.result IS
'工具返回的数据或失败摘要；保存前会递归脱敏并限制最大审计体积。';
COMMENT ON COLUMN public.ai_tool_call.status IS
'工具调用状态：SUCCEEDED 表示成功，FAILED 表示失败。';
COMMENT ON COLUMN public.ai_tool_call.duration_ms IS
'单次工具调用从开始到返回或抛错的耗时，单位为毫秒。';
COMMENT ON COLUMN public.ai_tool_call.created_at IS
'工具调用审计记录的创建时间。';

COMMENT ON TABLE public.checkpoints IS
'LangGraph 工作流检查点主表：保存每条 Agent 会话线程的完整图状态快照、父快照关系及运行元数据，用于多轮上下文恢复和中断续跑。';
COMMENT ON COLUMN public.checkpoints.thread_id IS
'LangGraph 线程标识；本项目格式为用户ID:会话UUID:当前角色，用于隔离不同用户、会话和角色的状态。';
COMMENT ON COLUMN public.checkpoints.checkpoint_ns IS
'检查点命名空间，用于区分主图与嵌套子图；主图通常使用空字符串。';
COMMENT ON COLUMN public.checkpoints.checkpoint_id IS
'当前状态快照在所属线程和命名空间内的唯一标识。';
COMMENT ON COLUMN public.checkpoints.parent_checkpoint_id IS
'父状态快照 ID，用于连接前后检查点并还原图状态演进链；首个快照可以为空。';
COMMENT ON COLUMN public.checkpoints.type IS
'checkpoint 字段采用的 LangGraph 序列化格式标识。';
COMMENT ON COLUMN public.checkpoints.checkpoint IS
'LangGraph 图状态快照的 JSONB 内容，包含通道版本和恢复执行所需的状态信息。';
COMMENT ON COLUMN public.checkpoints.metadata IS
'与状态快照关联的 LangGraph 运行元数据，例如步骤、来源和父级配置信息。';

COMMENT ON TABLE public.checkpoint_blobs IS
'LangGraph 检查点通道数据表：按线程、命名空间、通道和版本保存从主检查点拆出的二进制状态值，减少重复快照的数据体积。';
COMMENT ON COLUMN public.checkpoint_blobs.thread_id IS
'所属 LangGraph 线程标识，与 checkpoints.thread_id 使用相同隔离规则。';
COMMENT ON COLUMN public.checkpoint_blobs.checkpoint_ns IS
'所属检查点命名空间，用于区分主图与嵌套子图。';
COMMENT ON COLUMN public.checkpoint_blobs.channel IS
'LangGraph 状态通道名称，例如 messages 等图状态字段对应的内部通道。';
COMMENT ON COLUMN public.checkpoint_blobs.version IS
'该通道值的版本标识；只有通道内容变化时才产生新的版本。';
COMMENT ON COLUMN public.checkpoint_blobs.type IS
'blob 内容采用的序列化类型标识，供 LangGraph 反序列化恢复原始 Python 值。';
COMMENT ON COLUMN public.checkpoint_blobs.blob IS
'序列化后的通道二进制数据；对空值或特殊占位类型可以为空。';

COMMENT ON TABLE public.checkpoint_writes IS
'LangGraph 检查点中间写入表：保存图节点任务执行期间尚未合并到正式检查点的通道写入，支持并行节点、失败恢复和中断续跑。';
COMMENT ON COLUMN public.checkpoint_writes.thread_id IS
'所属 LangGraph 线程标识，与 checkpoints.thread_id 对应。';
COMMENT ON COLUMN public.checkpoint_writes.checkpoint_ns IS
'所属检查点命名空间，用于隔离主图和子图的中间写入。';
COMMENT ON COLUMN public.checkpoint_writes.checkpoint_id IS
'这些中间写入基于的检查点 ID。';
COMMENT ON COLUMN public.checkpoint_writes.task_id IS
'产生本次写入的 LangGraph 节点任务唯一标识。';
COMMENT ON COLUMN public.checkpoint_writes.idx IS
'同一任务内写入项的顺序编号，用于稳定保存和重放多个通道结果。';
COMMENT ON COLUMN public.checkpoint_writes.channel IS
'本次中间结果写入的 LangGraph 状态通道名称。';
COMMENT ON COLUMN public.checkpoint_writes.type IS
'blob 内容采用的序列化类型标识。';
COMMENT ON COLUMN public.checkpoint_writes.blob IS
'序列化后的中间写入二进制内容。';
COMMENT ON COLUMN public.checkpoint_writes.task_path IS
'产生写入的节点任务路径，用于在嵌套图和并行任务中确定执行位置与稳定顺序。';

COMMENT ON TABLE public.checkpoint_migrations IS
'LangGraph PostgreSQL Checkpoint 适配器的内部结构迁移记录表；由框架 setup 方法自动维护，业务代码不应手工修改。';
COMMENT ON COLUMN public.checkpoint_migrations.v IS
'已应用的 LangGraph Checkpoint 内部迁移序号。';

COMMENT ON TABLE public.store IS
'LangGraph 长期记忆主表：按 namespace 与 key 保存 JSONB 记忆内容、创建更新时间和 TTL；本项目用于按用户及角色保存可跨会话召回的问答记忆。';
COMMENT ON COLUMN public.store.prefix IS
'记忆 namespace 的持久化前缀；本项目逻辑结构为 agent_memory、用户ID、当前角色。';
COMMENT ON COLUMN public.store.key IS
'namespace 内的记忆唯一键；本项目使用产生该条记忆的 Agent run_id。';
COMMENT ON COLUMN public.store.value IS
'长期记忆的 JSONB 内容，包含脱敏后的问答文本、会话 ID、运行 ID 和记忆类型。';
COMMENT ON COLUMN public.store.created_at IS
'长期记忆首次写入时间。';
COMMENT ON COLUMN public.store.updated_at IS
'长期记忆最近一次更新或覆盖时间。';
COMMENT ON COLUMN public.store.expires_at IS
'长期记忆的到期时间；TTL 清理器会删除已过期记录。';
COMMENT ON COLUMN public.store.ttl_minutes IS
'长期记忆的生存时间，单位为分钟；读取时可根据配置刷新到期时间。';

COMMENT ON TABLE public.store_vectors IS
'LangGraph 长期记忆向量索引表：保存 Store 中指定 JSON 字段的 Embedding，用于在同一 namespace 内按语义相似度召回记忆。';
COMMENT ON COLUMN public.store_vectors.prefix IS
'被向量化记忆的 namespace 前缀，与 store.prefix 共同定位主记录。';
COMMENT ON COLUMN public.store_vectors.key IS
'被向量化记忆在 namespace 内的唯一键，与 store.key 共同组成外键。';
COMMENT ON COLUMN public.store_vectors.field_name IS
'被向量化的 JSON 字段路径；本项目主要索引长期记忆的 content 字段。';
COMMENT ON COLUMN public.store_vectors.embedding IS
'指定记忆字段生成的 pgvector 向量，当前维度为 1024，用于余弦距离语义检索。';
COMMENT ON COLUMN public.store_vectors.created_at IS
'记忆向量首次创建时间。';
COMMENT ON COLUMN public.store_vectors.updated_at IS
'记忆内容变化并重新生成向量后的最近更新时间。';

COMMENT ON TABLE public.store_migrations IS
'LangGraph PostgreSQL Store 主表结构的内部迁移记录；由框架 setup 方法自动维护，业务代码不应手工修改。';
COMMENT ON COLUMN public.store_migrations.v IS
'已应用的 LangGraph Store 主表内部迁移序号。';

COMMENT ON TABLE public.vector_migrations IS
'LangGraph PostgreSQL Store 向量能力的内部迁移记录；用于跟踪 pgvector 表和向量索引结构，由框架自动维护。';
COMMENT ON COLUMN public.vector_migrations.v IS
'已应用的 LangGraph Store 向量结构内部迁移序号。';
