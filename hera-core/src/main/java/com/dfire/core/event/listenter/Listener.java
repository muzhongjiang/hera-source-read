package com.dfire.core.event.listenter;

import com.dfire.event.AbstractEvent;

/**
 * @author: <a href="mailto:lingxiao@2dfire.com">ει</a>
 * @time: Created in δΈε5:52 2018/4/18
 * @desc
 */
public interface Listener<E extends AbstractEvent> {

    void handleEvent(E event);
}
