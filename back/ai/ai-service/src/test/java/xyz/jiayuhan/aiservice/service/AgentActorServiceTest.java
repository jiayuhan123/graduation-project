package xyz.jiayuhan.aiservice.service;

import org.junit.jupiter.api.Test;
import xyz.jiayuhan.aiapi.model.AgentContracts;
import xyz.jiayuhan.common.core.result.Result;
import xyz.jiayuhan.userapi.client.UserFeignClient;
import xyz.jiayuhan.userapi.model.response.UserPermissionResponse;
import xyz.jiayuhan.userapi.model.response.UserSummaryResponse;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AgentActorServiceTest {

    @Test
    void onlyIssuesToolsBackedByCurrentPermissions() {
        UserFeignClient userClient = mock(UserFeignClient.class);
        when(userClient.getUserSummary(7L)).thenReturn(Result.success(new UserSummaryResponse(
                7L, "teacher", "王老师", null, null, 1,
                List.of("teacher"), null, null)));
        when(userClient.authorizeUser(7L, "teaching:report:query"))
                .thenReturn(Result.success(new UserPermissionResponse(
                        7L, "teaching:report:query", true, true, true)));
        when(userClient.authorizeUser(7L, "teaching:assignment:manage"))
                .thenReturn(Result.success(new UserPermissionResponse(
                        7L, "teaching:assignment:manage", true, true, false)));

        AgentContracts.Actor actor = new AgentActorService(userClient).load(7L, "teacher");

        assertThat(actor.allowedTools()).containsExactly("get_class_dashboard");
    }
}
