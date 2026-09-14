package xyz.jiayuhan.notificationservice.service;

import com.baomidou.mybatisplus.annotation.TableField;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import xyz.jiayuhan.common.mq.event.UserRegisteredEvent;
import xyz.jiayuhan.notificationservice.entity.Notification;
import xyz.jiayuhan.notificationservice.mapper.NotificationMapper;

import java.time.Instant;
import java.lang.reflect.Field;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 验证注册事件生成通知时的字段映射与消费幂等。 */
@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationMapper notificationMapper;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    @Test
    void shouldMapReadPropertyToIsReadColumn() throws NoSuchFieldException {
        Field field = Notification.class.getDeclaredField("read");

        assertThat(field.getAnnotation(TableField.class).value()).isEqualTo("is_read");
    }

    @Test
    void shouldCreateWelcomeNotificationForNewEvent() {
        UserRegisteredEvent event = event();
        when(notificationMapper.exists(any())).thenReturn(false);

        notificationService.createWelcomeNotification(event);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationMapper).insert(captor.capture());
        Notification saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(42L);
        assertThat(saved.getSourceEventId()).isEqualTo(event.eventId());
        assertThat(saved.getRead()).isFalse();
        assertThat(saved.getContent()).contains("demo");
    }

    @Test
    void shouldIgnoreAlreadyConsumedEvent() {
        when(notificationMapper.exists(any())).thenReturn(true);

        notificationService.createWelcomeNotification(event());

        verify(notificationMapper, never()).insert(any(Notification.class));
    }

    private UserRegisteredEvent event() {
        return new UserRegisteredEvent(
                UUID.fromString("00000000-0000-4000-8000-000000000001"),
                UserRegisteredEvent.TYPE,
                Instant.parse("2026-08-19T12:00:00Z"),
                "user-service",
                42L,
                "demo",
                "demo@example.com");
    }
}
