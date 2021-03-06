package com.dfire.monitor.domain;

import lombok.Data;


/**
 * @author: <a href="mailto:lingxiao@2dfire.com">ει</a>
 * @time: Created in δΈε3:42 2018/8/15
 * @desc
 */
@Data
public class JobHistoryVo {

    private String jobId;

    //private String actionId;

    private String startTime;

    private String endTime;

    private String executeHost;

    private String status;

    private String operator;

    private String description;

    private String jobName;
    
    private String groupId;

    private String groupName;

    private Integer times;
    
    private String bizLabel;
}
