package com.dfire.event;


/**
 * @author: <a href="mailto:lingxiao@2dfire.com">ει</a>
 * @time: Created in δΈε5:08 2018/4/18
 * @desc
 */
public class ApplicationEvent extends AbstractEvent {

    private Object data;

    public ApplicationEvent(EventType type) {
        super(type);
    }

    public ApplicationEvent(EventType type, Object data) {
        super(type);
        this.data = data;
    }

}
