package com.dfire.event;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author: <a href="mailto:lingxiao@2dfire.com">ει</a>
 * @time: Created in δΈε4:56 2018/4/18
 * @desc
 */
@Data
@NoArgsConstructor
public class AbstractEvent {

    private boolean cancelled;
    private Object source;
    private EventType type;

    public AbstractEvent(EventType type) {
        this.type = type;
    }

    public AbstractEvent(Object source) {
        this.source = source;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }


}
