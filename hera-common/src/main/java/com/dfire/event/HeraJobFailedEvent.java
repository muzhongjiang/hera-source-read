package com.dfire.event;

import com.dfire.common.entity.HeraJob;
import com.dfire.common.entity.vo.HeraJobHistoryVo;
import com.dfire.common.enums.TriggerTypeEnum;
import lombok.Data;

/**
 * @author: <a href="mailto:lingxiao@2dfire.com">ει</a>
 * @time: Created in δΈε4:11 2018/4/19
 * @desc
 */
@Data
public class HeraJobFailedEvent extends ApplicationEvent {

    private final HeraJobHistoryVo heraJobHistory;
    private final Long actionId;
    private final TriggerTypeEnum triggerType;
    private HeraJob heraJob;
    private int runCount = 0;
    private int rollBackTime = 0;
    private int retryCount = 0;

    public HeraJobFailedEvent(Long jobId, TriggerTypeEnum triggerType, HeraJobHistoryVo heraJobHistory) {
        super(Events.JobFailed);
        this.actionId = jobId;
        this.triggerType = triggerType;
        this.heraJobHistory = heraJobHistory;
    }

    public void setRollBackTime(int value) {
        switch (triggerType) {
            case SCHEDULE:
            case MANUAL_RECOVER: {
                this.rollBackTime = value;
            }
        }
    }


}
