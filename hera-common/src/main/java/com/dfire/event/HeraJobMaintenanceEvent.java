package com.dfire.event;

import lombok.Getter;

/**
 * @author: <a href="mailto:lingxiao@2dfire.com">ει</a>
 * @time: Created in δΈε4:23 2018/4/19
 * @desc
 */
public class HeraJobMaintenanceEvent extends ApplicationEvent {

    @Getter
    private final Long id;

    public HeraJobMaintenanceEvent(EventType type, Long id) {
        super(type);
        this.id = id;
    }
}
