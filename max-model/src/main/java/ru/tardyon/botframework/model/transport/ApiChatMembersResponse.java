package ru.tardyon.botframework.model.transport;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * MAX API transport members/admins list response.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ApiChatMembersResponse(
        @JsonProperty("members") List<ApiChatMember> members,
        @JsonProperty("admins") List<ApiChatMember> admins,
        @JsonProperty("marker") Long marker
) {
    public ApiChatMembersResponse {
        members = members == null ? List.of() : List.copyOf(members);
        admins = admins == null ? List.of() : List.copyOf(admins);
    }
}
