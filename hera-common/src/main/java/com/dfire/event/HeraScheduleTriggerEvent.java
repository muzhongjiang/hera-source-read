package com.dfire.event;

import lombok.Builder;
import lombok.Data;

/**
 * @author: <a href="mailto:lingxiao@2dfire.com">ει</a>
 * @time: Created in δΈε4:32 2018/4/19
 * @desc
 */
@Builder
@Data
public class HeraScheduleTriggerEvent extends ApplicationEvent {

    private final Long actionId;

    public HeraScheduleTriggerEvent(Long actionId) {
        super(Events.ScheduleTrigger);
        this.actionId = actionId;
    }
}
