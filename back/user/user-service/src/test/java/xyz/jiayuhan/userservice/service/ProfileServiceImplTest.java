package xyz.jiayuhan.userservice.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import xyz.jiayuhan.common.core.exception.BusinessException;
import xyz.jiayuhan.common.core.exception.ResultCode;
import xyz.jiayuhan.userservice.entity.StudentProfile;
import xyz.jiayuhan.userservice.mapper.StudentProfileMapper;
import xyz.jiayuhan.userservice.mapper.TeacherProfileMapper;
import xyz.jiayuhan.userservice.service.impl.ProfileServiceImpl;
import xyz.jiayuhan.userservice.service.impl.UserDataSupport;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileServiceImplTest {

    @Mock
    private StudentProfileMapper studentProfileMapper;
    @Mock
    private TeacherProfileMapper teacherProfileMapper;
    @Mock
    private UserDataSupport userDataSupport;

    @Test
    void createsStudentProfileWithLearningContext() {
        ProfileServiceImpl service = new ProfileServiceImpl(
                studentProfileMapper, teacherProfileMapper, userDataSupport);
        when(userDataSupport.trimToNull("S001")).thenReturn("S001");
        when(studentProfileMapper.selectById(10L)).thenReturn(null).thenAnswer(invocation -> {
            StudentProfile profile = new StudentProfile();
            profile.setUserId(10L);
            profile.setStudentNo("S001");
            profile.setGradeLevel(3);
            profile.setSemester(2);
            return profile;
        });
        doAnswer(invocation -> 1).when(studentProfileMapper).insert(any(StudentProfile.class));

        service.upsertStudentProfile(10L, "S001", 3, 2);

        ArgumentCaptor<StudentProfile> captor = ArgumentCaptor.forClass(StudentProfile.class);
        verify(studentProfileMapper).insert(captor.capture());
        assertThat(captor.getValue().getGradeLevel()).isEqualTo(3);
        assertThat(captor.getValue().getSemester()).isEqualTo(2);
    }

    @Test
    void rejectsStudentAndTeacherRoleCombination() {
        ProfileServiceImpl service = new ProfileServiceImpl(
                studentProfileMapper, teacherProfileMapper, userDataSupport);

        assertThatThrownBy(() -> service.validateRoleProfiles(
                10L, List.of("student", "teacher")))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getCode())
                                .isEqualTo(ResultCode.INVALID_ROLE_COMBINATION.getCode()));
    }
}
