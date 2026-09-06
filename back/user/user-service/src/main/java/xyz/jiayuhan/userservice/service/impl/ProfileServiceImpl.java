package xyz.jiayuhan.userservice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.jiayuhan.common.core.exception.BusinessException;
import xyz.jiayuhan.common.core.exception.ResultCode;
import xyz.jiayuhan.userservice.entity.StudentProfile;
import xyz.jiayuhan.userservice.entity.TeacherProfile;
import xyz.jiayuhan.userservice.mapper.StudentProfileMapper;
import xyz.jiayuhan.userservice.mapper.TeacherProfileMapper;
import xyz.jiayuhan.userservice.model.request.account.UpdateStudentProfileRequest;
import xyz.jiayuhan.userservice.model.request.account.UpdateTeacherProfileRequest;
import xyz.jiayuhan.userservice.model.response.account.StudentProfileResponse;
import xyz.jiayuhan.userservice.model.response.account.TeacherProfileResponse;
import xyz.jiayuhan.userservice.security.RoleCodes;
import xyz.jiayuhan.userservice.service.ProfileService;

import java.util.List;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/** 角色资料实现；工号与学号的唯一性由业务校验和数据库唯一索引双重保证。 */
@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {

    private final StudentProfileMapper studentProfileMapper;
    private final TeacherProfileMapper teacherProfileMapper;
    private final UserDataSupport userDataSupport;

    @Override
    public StudentProfileResponse studentProfile(Long userId) {
        return toStudent(studentProfileMapper.selectById(userId));
    }

    @Override
    public TeacherProfileResponse teacherProfile(Long userId) {
        return toTeacher(teacherProfileMapper.selectById(userId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StudentProfileResponse updateOwnStudentProfile(Long userId, UpdateStudentProfileRequest request) {
        requireStudentProfile(userId);
        studentProfileMapper.update(null, new LambdaUpdateWrapper<StudentProfile>()
                .eq(StudentProfile::getUserId, userId)
                .set(StudentProfile::getGradeLevel, request.gradeLevel())
                .set(StudentProfile::getSemester, request.semester())
                .set(StudentProfile::getUpdatedAt, now()));
        return studentProfile(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TeacherProfileResponse updateOwnTeacherProfile(Long userId, UpdateTeacherProfileRequest request) {
        requireTeacherProfile(userId);
        teacherProfileMapper.update(null, new LambdaUpdateWrapper<TeacherProfile>()
                .eq(TeacherProfile::getUserId, userId)
                .set(TeacherProfile::getTitle, userDataSupport.trimToNull(request.title()))
                .set(TeacherProfile::getIntroduction, userDataSupport.trimToNull(request.introduction()))
                .set(TeacherProfile::getUpdatedAt, now()));
        return teacherProfile(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StudentProfileResponse upsertStudentProfile(
            Long userId, String studentNo, Integer gradeLevel, Integer semester) {
        String normalizedStudentNo = userDataSupport.trimToNull(studentNo);
        ensureStudentNoUnique(normalizedStudentNo, userId);
        StudentProfile existing = studentProfileMapper.selectById(userId);
        if (existing == null) {
            StudentProfile profile = new StudentProfile();
            profile.setUserId(userId);
            profile.setStudentNo(normalizedStudentNo);
            profile.setGradeLevel(gradeLevel);
            profile.setSemester(semester);
            profile.setVersion(0);
            studentProfileMapper.insert(profile);
        } else {
            studentProfileMapper.update(null, new LambdaUpdateWrapper<StudentProfile>()
                    .eq(StudentProfile::getUserId, userId)
                    .set(StudentProfile::getStudentNo, normalizedStudentNo)
                    .set(StudentProfile::getGradeLevel, gradeLevel)
                    .set(StudentProfile::getSemester, semester)
                    .set(StudentProfile::getUpdatedAt, now()));
        }
        return studentProfile(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TeacherProfileResponse upsertTeacherProfile(
            Long userId, String teacherNo, String title, String introduction) {
        String normalizedTeacherNo = userDataSupport.trimToNull(teacherNo);
        ensureTeacherNoUnique(normalizedTeacherNo, userId);
        TeacherProfile existing = teacherProfileMapper.selectById(userId);
        if (existing == null) {
            TeacherProfile profile = new TeacherProfile();
            profile.setUserId(userId);
            profile.setTeacherNo(normalizedTeacherNo);
            profile.setTitle(userDataSupport.trimToNull(title));
            profile.setIntroduction(userDataSupport.trimToNull(introduction));
            profile.setVersion(0);
            teacherProfileMapper.insert(profile);
        } else {
            teacherProfileMapper.update(null, new LambdaUpdateWrapper<TeacherProfile>()
                    .eq(TeacherProfile::getUserId, userId)
                    .set(TeacherProfile::getTeacherNo, normalizedTeacherNo)
                    .set(TeacherProfile::getTitle, userDataSupport.trimToNull(title))
                    .set(TeacherProfile::getIntroduction, userDataSupport.trimToNull(introduction))
                    .set(TeacherProfile::getUpdatedAt, now()));
        }
        return teacherProfile(userId);
    }

    @Override
    public void validateRoleProfiles(Long userId, List<String> roleCodes) {
        if (roleCodes.contains(RoleCodes.STUDENT) && studentProfileMapper.selectById(userId) == null) {
            throw new BusinessException(ResultCode.STUDENT_PROFILE_REQUIRED);
        }
        if (roleCodes.contains(RoleCodes.TEACHER) && teacherProfileMapper.selectById(userId) == null) {
            throw new BusinessException(ResultCode.TEACHER_PROFILE_REQUIRED);
        }
    }

    private void ensureStudentNoUnique(String studentNo, Long userId) {
        if (studentNo != null && studentProfileMapper.exists(new LambdaQueryWrapper<StudentProfile>()
                .eq(StudentProfile::getStudentNo, studentNo)
                .ne(StudentProfile::getUserId, userId))) {
            throw new BusinessException(ResultCode.STUDENT_NO_EXISTS);
        }
    }

    private void ensureTeacherNoUnique(String teacherNo, Long userId) {
        if (teacherNo != null && teacherProfileMapper.exists(new LambdaQueryWrapper<TeacherProfile>()
                .eq(TeacherProfile::getTeacherNo, teacherNo)
                .ne(TeacherProfile::getUserId, userId))) {
            throw new BusinessException(ResultCode.TEACHER_NO_EXISTS);
        }
    }

    private StudentProfile requireStudentProfile(Long userId) {
        StudentProfile profile = studentProfileMapper.selectById(userId);
        if (profile == null) {
            throw new BusinessException(ResultCode.STUDENT_PROFILE_REQUIRED);
        }
        return profile;
    }

    private TeacherProfile requireTeacherProfile(Long userId) {
        TeacherProfile profile = teacherProfileMapper.selectById(userId);
        if (profile == null) {
            throw new BusinessException(ResultCode.TEACHER_PROFILE_REQUIRED);
        }
        return profile;
    }

    private StudentProfileResponse toStudent(StudentProfile profile) {
        return profile == null ? null : new StudentProfileResponse(
                profile.getUserId(),
                profile.getStudentNo(),
                profile.getGradeLevel(),
                profile.getSemester(),
                profile.getUpdatedAt());
    }

    private TeacherProfileResponse toTeacher(TeacherProfile profile) {
        return profile == null ? null : new TeacherProfileResponse(
                profile.getUserId(),
                profile.getTeacherNo(),
                profile.getTitle(),
                profile.getIntroduction(),
                profile.getUpdatedAt());
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(ZoneOffset.UTC);
    }
}
