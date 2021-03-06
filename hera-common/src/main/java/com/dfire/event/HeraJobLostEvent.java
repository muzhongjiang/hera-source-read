package com.dfire.event;

import lombok.Data;

/**
 * @author: <a href="mailto:lingxiao@2dfire.com">ει</a>
 * @time: Created in δΈε4:20 2018/4/19
 * @desc
 */
@Data
public class HeraJobLostEvent extends ApplicationEvent {

    private final Long jobId;
    public HeraJobLostEvent(EventType type, Long jobId){
        super(type);
        this.jobId=jobId;
    }
}
