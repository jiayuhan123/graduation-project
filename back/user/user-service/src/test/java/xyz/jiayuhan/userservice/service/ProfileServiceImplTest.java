package xyz.jiayuhan.userservice.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import xyz.jiayuhan.userservice.entity.StudentProfile;
import xyz.jiayuhan.userservice.entity.TeacherProfile;
import xyz.jiayuhan.userservice.mapper.StudentProfileMapper;
import xyz.jiayuhan.userservice.mapper.TeacherProfileMapper;
import xyz.jiayuhan.userservice.service.impl.ProfileServiceImpl;
import xyz.jiayuhan.userservice.service.impl.UserDataSupport;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
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
    void acceptsStudentAndTeacherRolesWhenBothProfilesExist() {
        ProfileServiceImpl service = new ProfileServiceImpl(
                studentProfileMapper, teacherProfileMapper, userDataSupport);
        when(studentProfileMapper.selectById(10L)).thenReturn(new StudentProfile());
        when(teacherProfileMapper.selectById(10L)).thenReturn(new TeacherProfile());

        service.validateRoleProfiles(10L, List.of("student", "teacher"));

        verify(studentProfileMapper).selectById(10L);
        verify(teacherProfileMapper).selectById(10L);
    }
}
