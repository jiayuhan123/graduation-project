package xyz.jiayuhan.gameapi;

/** 游戏闯关服务内部调用使用的稳定基础常量。 */
public final class GameApiConstants {

    public static final String SERVICE_NAME = "game-service";
    public static final String INTERNAL_PATH = "/internal/games";
    public static final String INTERNAL_TOKEN_HEADER = "X-Internal-Token";

    private GameApiConstants() {
    }
}
