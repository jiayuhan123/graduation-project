"""Agent 自有 PostgreSQL 迁移包。

保留这个包文件后，`storage.py` 可以使用 `importlib.resources` 读取相邻 SQL；即使项目被
安装成 Python 包，也不必依赖当前工作目录去寻找迁移文件。
"""
