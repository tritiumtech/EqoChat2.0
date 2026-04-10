package com.eqochat.framework.file.client;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractFileClient<Config extends FileClientConfig> implements FileClient {

    private final Long id;
    protected Config config;

    protected AbstractFileClient(Long id, Config config) {
        this.id = id;
        this.config = config;
    }

    public final void init() {
        doInit();
        log.debug("[init][配置({}) 初始化完成]", config);
    }

    protected abstract void doInit();

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
