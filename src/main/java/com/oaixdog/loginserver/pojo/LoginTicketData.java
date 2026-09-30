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

    public LoginTicketData(Long userId, String anchorOpenId) {
        this.userId = userId;
        this.anchorOpenId = anchorOpenId;
    }

    public static LoginTicketData valueOf(Long userId, String anchorOpenId) {
        return new LoginTicketData(userId, anchorOpenId);
    }

}
