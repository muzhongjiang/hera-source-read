package com.dfire.monitor.domain;

import lombok.Data;

/**
 * @author: <a href="mailto:lingxiao@2dfire.com">ει</a>
 * @time: Created in δΈε10:58 2018/8/15
 * @desc
 */
@Data
public class ActionTime {

    private Integer jobId;

    private String actionId;

    private String jobTime;

    private Integer yesterdayTime;
}
