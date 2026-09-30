package com.oaixdog.loginserver.service;

import com.oaixdog.loginserver.config.ApplicationProperties;
import com.zfoo.protocol.util.StringUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class LoginTicketService {

    private static final String AES_GCM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final ApplicationProperties properties;

    public String encrypt(String json) {
        try {
            if (StringUtils.isBlank(json)) {
                throw new IllegalArgumentException("登录票据内容不能为空");
            }
            byte[] keyBytes = Base64.getDecoder().decode(properties.getLoginTicketKey());
            byte[] iv = new byte[GCM_IV_LENGTH];
            SECURE_RANDOM.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(AES_GCM);
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(keyBytes, "AES"), new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] encrypted = cipher.doFinal(json.getBytes(StandardCharsets.UTF_8));
            byte[] ticket = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, ticket, 0, iv.length);
            System.arraycopy(encrypted, 0, ticket, iv.length, encrypted.length);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(ticket);
        } catch (GeneralSecurityException | IllegalArgumentException exception) {
            throw new IllegalStateException("生成登录票据失败", exception);
        }
    }

    public String decrypt(String loginTicket) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(properties.getLoginTicketKey());
            byte[] ticket = Base64.getUrlDecoder().decode(loginTicket);
            if (ticket.length <= GCM_IV_LENGTH + GCM_TAG_LENGTH / Byte.SIZE) {
                throw new IllegalArgumentException("登录票据格式错误");
            }
            byte[] iv = Arrays.copyOfRange(ticket, 0, GCM_IV_LENGTH);
            byte[] encrypted = Arrays.copyOfRange(ticket, GCM_IV_LENGTH, ticket.length);
            Cipher cipher = Cipher.getInstance(AES_GCM);
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(keyBytes, "AES"), new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException exception) {
            throw new IllegalArgumentException("登录票据解密失败", exception);
        }
    }
}
