package com.eqochat.business.world.api.dto.response;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorldTopicResponse {
    private String id;
    private String name;
    private int posts;
    private int followers;
    private boolean favorite;
    private boolean followed;
}
