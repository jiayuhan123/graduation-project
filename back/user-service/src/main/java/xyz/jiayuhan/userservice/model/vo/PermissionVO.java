package xyz.jiayuhan.userservice.model.vo;

/**
 * 权限字典项。
 *
 * @param id 权限 ID
 * @param code 程序鉴权使用的稳定权限编码
 * @param name 权限名称
 * @param description 权限说明
 */
public record PermissionVO(Long id, String code, String name, String description) {
}
