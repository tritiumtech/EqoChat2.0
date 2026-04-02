package com.eqochat.file.client;

import lombok.extern.slf4j.Slf4j;

/**
 * 文件客户端抽象类，提供模板方法，减少子类冗余代码。
 */
@Slf4j
public abstract class AbstractFileClient<Config extends FileClientConfig> implements FileClient {

    private final Long id;
    protected Config config;

    protected AbstractFileClient(Long id, Config config) {
        this.id = id;
        this.config = config;
    }

    /**
     * 初始化客户端
     */
    public final void init() {
        doInit();
        log.debug("[init][配置({}) 初始化完成]", config);
    }

    /**
     * 子类实现具体初始化逻辑
     */
    protected abstract void doInit();

    /**
     * 刷新配置
     */
    public final void refresh(Config config) {
        if (config.equals(this.config)) {
            return;
        }
        log.info("[refresh][配置发生变化，重新初始化]");
        this.config = config;
        this.init();
    }

    @Override
    public Long getId() {
        return id;
    }
}