package org.example.back.common.util;

import org.example.back.common.exception.BusinessException;
import org.springframework.util.StringUtils;

import java.util.regex.Pattern;
//密码策略工具类：提供密码强度校验功能，确保用户设置的密码符合安全要求。
public final class PasswordPolicyUtil {

    private static final int MIN_LENGTH = 8;
    private static final Pattern LETTER_PATTERN = Pattern.compile("[A-Za-z]");
    private static final Pattern DIGIT_PATTERN = Pattern.compile("\\d");

    private PasswordPolicyUtil() {
    }
    /**
     * 检验新密码的要求
     * @param password
     * @param fieldLabel
     */
    public static void validateUserPassword(String password, String fieldLabel) {
        String label = StringUtils.hasText(fieldLabel) ? fieldLabel.trim() : "密码";
        if (!StringUtils.hasText(password)) {
            throw BusinessException.validateFail(label + "不能为空");
        }
        if (password.length() < MIN_LENGTH
                || !LETTER_PATTERN.matcher(password).find()
                || !DIGIT_PATTERN.matcher(password).find()) {
            throw BusinessException.validateFail(label + "至少8位，且需同时包含字母和数字");
        }
    }
}