package com.eqochat.file.client;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * 文件客户端配置接口。
 * 不同实现的客户端需要不同的配置，通过子类来定义。
 * Jackson 多态序列化支持。
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
public interface FileClientConfig {
}