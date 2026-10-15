package com.oaixdog.loginserver.pojo;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class LoginTicketData {

    private Long userId;
    private String anchorOpenId;
    private Integer zoneId;

    public LoginTicketData(Long userId, String anchorOpenId, Integer zoneId) {
        this.userId = userId;
        this.anchorOpenId = anchorOpenId;
        this.zoneId = zoneId;
    }

    public static LoginTicketData valueOf(Long userId, String anchorOpenId, Integer zoneId) {
        return new LoginTicketData(userId, anchorOpenId, zoneId);
    }

}
