package com.eqochat.framework.common;

import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceAware;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class I18nUtil implements MessageSourceAware {
    
    private static MessageSource messageSource;
    
    @Override
    public void setMessageSource(MessageSource messageSource) {
        I18nUtil.messageSource = messageSource;
    }
    
    public static String get(String code, Object... args) {
        if (messageSource == null) {
            return code;
        }
        Locale locale = LocaleContextHolder.getLocale();
        return messageSource.getMessage(code, args, code, locale);
    }
    
    public static String getOrDefault(String code, String defaultMessage) {
        if (messageSource == null) {
            return defaultMessage;
        }
        Locale locale = LocaleContextHolder.getLocale();
        return messageSource.getMessage(code, null, defaultMessage, locale);
    }
    
    public static String getLocaleTag() {
        Locale locale = LocaleContextHolder.getLocale();
        if (locale == null) {
            return "zh-CN";
        }
        String tag = locale.toLanguageTag();
        return tag == null || tag.isBlank() ? "zh-CN" : tag;
    }
}
