package xyz.jiayuhan.userservice.service;

/** 基于 Redis 的登录失败计数，降低口令暴力破解风险。 */
public interface LoginAttemptService {

    /**
     * 检查账号是否仍允许尝试登录。
     *
     * @param account 已规范化的登录账号
     */
    void checkAllowed(String account);

    /**
     * 记录一次凭证校验失败。
     *
     * @param account 已规范化的登录账号
     */
    void recordFailure(String account);

    /**
     * 登录成功后清除失败计数。
     *
     * @param account 已规范化的登录账号
     */
    void clear(String account);
}
