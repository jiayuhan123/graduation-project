package xyz.jiayuhan.userservice.model.response.admin;

/**
 * 用户详情中的简要角色信息。
 *
 * @param id 角色 ID
 * @param code 程序鉴权使用的稳定角色编码
 * @param name 角色名称
 */
public record RoleSummaryResponse(Long id, String code, String name) {
}
