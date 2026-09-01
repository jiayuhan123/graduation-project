package xyz.jiayuhan.userservice.service;

import xyz.jiayuhan.userservice.model.request.account.UpdateStudentProfileRequest;
import xyz.jiayuhan.userservice.model.request.account.UpdateTeacherProfileRequest;
import xyz.jiayuhan.userservice.model.response.account.StudentProfileResponse;
import xyz.jiayuhan.userservice.model.response.account.TeacherProfileResponse;

import java.util.List;

/** 学生与教师专有资料的查询、维护和角色一致性校验。 */
public interface ProfileService {

    StudentProfileResponse studentProfile(Long userId);

    TeacherProfileResponse teacherProfile(Long userId);

    StudentProfileResponse updateOwnStudentProfile(Long userId, UpdateStudentProfileRequest request);

    TeacherProfileResponse updateOwnTeacherProfile(Long userId, UpdateTeacherProfileRequest request);

    StudentProfileResponse upsertStudentProfile(
            Long userId, String studentNo, Integer gradeLevel, Integer semester);

    TeacherProfileResponse upsertTeacherProfile(
            Long userId, String teacherNo, String title, String introduction);

    void validateRoleProfiles(Long userId, List<String> roleCodes);
}
