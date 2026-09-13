package xyz.jiayuhan.aiapi;

/** AI Agent 对外接口与内部工具契约常量。 */
public final class AiApiConstants {

    public static final String INTERNAL_TOOLS_PATH = "/internal/ai/tools";
    public static final String TOOL_TOKEN_HEADER = "X-Agent-Tool-Token";
    public static final String BRIDGE_TOKEN_HEADER = "X-AI-Bridge-Token";

    private AiApiConstants() {
    }
}
