package com.dfire.core.netty.master;

import com.dfire.common.constants.Constants;
import com.dfire.common.constants.LogConstant;
import com.dfire.common.entity.HeraAction;
import com.dfire.common.entity.HeraJob;
import com.dfire.common.entity.HeraJobHistory;
import com.dfire.common.entity.vo.HeraActionVo;
import com.dfire.common.entity.vo.HeraDebugHistoryVo;
import com.dfire.common.entity.vo.HeraJobHistoryVo;
import com.dfire.common.enums.JobScheduleTypeEnum;
import com.dfire.common.enums.StatusEnum;
import com.dfire.common.enums.TriggerTypeEnum;
import com.dfire.common.exception.HeraException;
import com.dfire.common.util.ActionUtil;
import com.dfire.common.util.BeanConvertUtils;
import com.dfire.common.util.NamedThreadFactory;
import com.dfire.common.vo.JobElement;
import com.dfire.config.HeraGlobalEnv;
import com.dfire.core.netty.master.response.MasterExecuteJob;
import com.dfire.event.*;
import com.dfire.logs.*;
import com.dfire.protocol.JobExecuteKind;
import com.dfire.protocol.ResponseStatus;
import com.dfire.protocol.RpcResponse;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


/**
 * desc:
 *
 * @author scx
 * @create 2019/04/23
 */
public class MasterRunJob implements RunJob {

    private Master master;

    private MasterContext masterContext;

    private RunJobThreadPool executeJobPool;

    private int cacheCoreSize;

    public MasterRunJob(MasterContext masterContext, Master master) {
        this.master = master;
        this.masterContext = masterContext;
        this.cacheCoreSize = HeraGlobalEnv.getMaxParallelNum();
        executeJobPool = new RunJobThreadPool(masterContext, cacheCoreSize, cacheCoreSize, 10L, TimeUnit.MINUTES,
                new LinkedBlockingQueue<>(Integer.MAX_VALUE), new NamedThreadFactory("master-execute-job"), new ThreadPoolExecutor.AbortPolicy());
        executeJobPool.allowCoreThreadTimeOut(true);
    }

    /**
     * ??????????????????????????????
     *
     * @param selectWork ????????????
     * @param debugId    debugId
     */
    private void runDebugJob(MasterWorkHolder selectWork, Long debugId) {
        HeraDebugHistoryVo history = masterContext.getHeraDebugHistoryService().findById(debugId);
        history.getLog().append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + " ????????????");
        masterContext.getHeraDebugHistoryService().update(BeanConvertUtils.convert(history));
        Exception exception = null;
        RpcResponse.Response response = null;
        Future<RpcResponse.Response> future = null;
        try {
            future = new MasterExecuteJob().executeJob(masterContext, selectWork, TriggerTypeEnum.DEBUG, debugId);
            response = future.get(HeraGlobalEnv.getTaskTimeout(), TimeUnit.HOURS);
        } catch (Exception e) {
            exception = e;
            if (future != null) {
                future.cancel(true);
            }
            DebugLog.error(String.format("debugId:%s run failed", debugId), e);
        }
        boolean success = response != null && response.getStatusEnum() == ResponseStatus.Status.OK;
        if (!success) {
            exception = new HeraException(String.format("fileId:%s run failed ", history.getFileId()), exception);
            TaskLog.info("8.Master: debug job error");
            history = masterContext.getHeraDebugHistoryService().findById(debugId);
            HeraDebugFailEvent failEvent = HeraDebugFailEvent.builder()
                    .debugHistory(BeanConvertUtils.convert(history))
                    .throwable(exception)
                    .fileId(history.getFileId())
                    .build();
            masterContext.getDispatcher().forwardEvent(failEvent);
        } else {
            TaskLog.info("7.Master: debug success");
            HeraDebugSuccessEvent successEvent = HeraDebugSuccessEvent.builder()
                    .fileId(history.getFileId())
                    .history(BeanConvertUtils.convert(history))
                    .build();
            masterContext.getDispatcher().forwardEvent(successEvent);
        }
    }

    /**
     * ?????????????????????????????????????????????master???channel???manual??????????????????
     *
     * @param selectWork selectWork ????????????
     * @param actionId   actionId
     */
    private void runManualJob(MasterWorkHolder selectWork, Long actionId, TriggerTypeEnum triggerType) {
        SocketLog.info("start run manual job, actionId = {}", actionId);
        HeraAction heraAction = masterContext.getHeraJobActionService().findById(actionId);
        HeraJobHistory history = masterContext.getHeraJobHistoryService().findById(heraAction.getHistoryId());
        HeraJobHistoryVo historyVo = BeanConvertUtils.convert(history);
        historyVo.getLog().append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + " ????????????");
        heraAction.setStatus(StatusEnum.RUNNING.toString());
        historyVo.setStatusEnum(StatusEnum.RUNNING);
        HeraAction cacheAction = master.getHeraActionMap().get(actionId);
        if (cacheAction != null) {
            cacheAction.setStatus(StatusEnum.RUNNING.toString());
            cacheAction.setHistoryId(heraAction.getHistoryId());
        }
        masterContext.getHeraJobHistoryService().updateHeraJobHistoryLogAndStatus(BeanConvertUtils.convert(historyVo));

        Exception exception = null;
        RpcResponse.Response response = null;
        Future<RpcResponse.Response> future = null;
        try {
            future = new MasterExecuteJob().executeJob(masterContext, selectWork,
                    triggerType, actionId);
            response = future.get();
        } catch (Exception e) {
            exception = e;
            if (future != null) {
                future.cancel(true);
            }
            ErrorLog.error("manual job run error {}", e);
        }
        boolean success = response != null && response.getStatusEnum() != null && response.getStatusEnum() == ResponseStatus.Status.OK;
        if (response != null) {
            ScheduleLog.info("actionId ????????????" + actionId + "---->" + response.getStatusEnum());
        }
        ApplicationEvent event;
        if (!success) {
            if (exception != null) {
                HeraException heraException = new HeraException(exception);
                ErrorLog.error("manual actionId = {} error, {}", history.getActionId(), heraException.getMessage());
            }
            ScheduleLog.info("actionId = {} manual execute failed", history.getActionId());
            heraAction.setStatus(StatusEnum.FAILED.toString());
            HeraJobHistory jobHistory = masterContext.getHeraJobHistoryService().findById(history.getId());
            if (LogConstant.CANCEL_JOB_LOG.equals(jobHistory.getIllustrate())) {
                event = null;
            } else {
                HeraJobHistoryVo jobHistoryVo = BeanConvertUtils.convert(jobHistory);
                event = new HeraJobFailedEvent(history.getActionId(), jobHistoryVo.getTriggerType(), jobHistoryVo);
            }
        } else {
            heraAction.setStatus(StatusEnum.SUCCESS.toString());
            event = new HeraJobSuccessEvent(history.getActionId(), historyVo.getTriggerType(), historyVo);
        }
        updateCacheAction(actionId, heraAction.getStatus());
        heraAction.setStatisticEndTime(new Date());
        masterContext.getHeraJobActionService().update(heraAction);
        if (event != null) {
            masterContext.getDispatcher().forwardEvent(event);
        }
    }

    /**
     * ?????????????????????????????????????????????????????????????????????????????????
     *
     * @param selectWork  ????????????
     * @param actionId    actionId
     * @param triggerType
     */
    private void runScheduleJob(MasterWorkHolder selectWork, Long actionId, TriggerTypeEnum triggerType) {
        int runCount = 0;
        int retryCount = 0;
        int retryWaitTime = 1;
        HeraActionVo heraActionVo = masterContext.getHeraJobActionService().findHeraActionVo(actionId).getSource();
        Map<String, String> properties = heraActionVo.getConfigs();
        if (properties != null && properties.size() > 0) {
            retryCount = Integer.parseInt(properties.get("roll.back.times") == null ? "0" : properties.get("roll.back.times"));
            retryWaitTime = Integer.parseInt(properties.get("roll.back.wait.time") == null ? "0" : properties.get("roll.back.wait.time"));
        }
        runScheduleJobContext(selectWork, actionId, runCount, retryCount, retryWaitTime, triggerType);
    }

    /**
     * ??????????????????????????????????????????master??????channel???????????????????????????
     *
     * @param selectWork    selectWork
     * @param actionId      actionId
     * @param runCount      runCount
     * @param retryCount    retryCount
     * @param retryWaitTime retryWaitTime
     * @param triggerType
     */
    private void runScheduleJobContext(MasterWorkHolder selectWork, Long actionId, int runCount, int retryCount, int retryWaitTime, TriggerTypeEnum triggerType) {
        DebugLog.info("???????????????{},???????????????{},actionId:{}", retryCount, retryWaitTime, actionId);
        runCount++;
        boolean isCancelJob = false;
        if (runCount > 1) {
            HeraJob memJob = masterContext.getHeraJobService().findMemById(ActionUtil.getJobId(actionId));
            //???????????????????????????????????????
            if (memJob == null || memJob.getAuto() != 1) {
                ScheduleLog.info("??????{}????????????????????????:{}", actionId, runCount);
                return;
            }
            if (master.checkJobExists(HeraJobHistoryVo.builder()
                            .actionId(actionId)
                            .jobId(memJob.getId())
                            .triggerType(triggerType)
                            .build()
                    , true)) {
                ScheduleLog.info("--------------------------{}???????????????????????????--------------------------", actionId);
                return;
            }
            HeraJobHistory lastHistory = masterContext.getHeraJobHistoryService().findNewest(ActionUtil.getJobId(actionId));
            boolean checkCancel = lastHistory.getActionId().equals(actionId) &&
                    (lastHistory.getTriggerType().equals(TriggerTypeEnum.MANUAL_RECOVER.getId())
                            || lastHistory.getTriggerType().equals(TriggerTypeEnum.SCHEDULE.getId()));
            //???????????????????????????????????????????????????
            if (checkCancel) {
                //?????????????????????????????????????????? ????????????
                if (StatusEnum.RUNNING.toString().equals(lastHistory.getStatus()) || StatusEnum.SUCCESS.toString().equals(lastHistory.getStatus())) {
                    ScheduleLog.info(String.format("cancel job  {} retry, there is another task running or success", actionId));
                    return;
                }
            }

            DebugLog.info("????????????????????????{}??????", retryWaitTime);
            try {
                TimeUnit.MINUTES.sleep(retryWaitTime);
            } catch (InterruptedException e) {
                ErrorLog.error("sleep interrupted", e);
            }
        }
        HeraJobHistoryVo heraJobHistoryVo;
        HeraJobHistory heraJobHistory;
        HeraAction heraAction;
        if (runCount == 1) {
            heraAction = masterContext.getHeraJobActionService().findById(actionId);
            heraJobHistory = masterContext.getHeraJobHistoryService().
                    findById(heraAction.getHistoryId());
            heraJobHistoryVo = BeanConvertUtils.convert(heraJobHistory);
            triggerType = heraJobHistoryVo.getTriggerType();
            heraJobHistoryVo.getLog().append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + " ????????????");

        } else {
            heraAction = masterContext.getHeraJobActionService().findById(actionId);
            HeraJob heraJob = masterContext.getHeraJobService().findMemById(heraAction.getJobId());
            heraJobHistory = HeraJobHistory.builder()
                    .illustrate(LogConstant.FAIL_JOB_RETRY)
                    .triggerType(TriggerTypeEnum.SCHEDULE.getId())
                    .jobId(heraAction.getJobId())
                    .actionId(heraAction.getId())
                    .operator(heraAction.getOwner())
                    .hostGroupId(heraAction.getHostGroupId())
                    .batchId(heraAction.getBatchId())
                    .bizLabel(heraJob.getBizLabel())
                    .build();
            masterContext.getHeraJobHistoryService().insert(heraJobHistory);
            heraAction.setHistoryId(heraJobHistory.getId());
            heraAction.setStatus(StatusEnum.RUNNING.toString());
            masterContext.getHeraJobActionService().update(heraAction);
            heraJobHistoryVo = BeanConvertUtils.convert(heraJobHistory);
            heraJobHistoryVo.getLog().append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + " ???" + (runCount - 1) + "???????????????\n");
            triggerType = heraJobHistoryVo.getTriggerType();
        }
        HeraAction cacheAction = master.getHeraActionMap().get(actionId);
        if (cacheAction != null) {
            cacheAction.setStatus(StatusEnum.RUNNING.toString());
            cacheAction.setHistoryId(heraJobHistory.getId());
        }
        heraJobHistoryVo.setStatusEnum(StatusEnum.RUNNING);
        masterContext.getHeraJobHistoryService().updateHeraJobHistoryLogAndStatus(BeanConvertUtils.convert(heraJobHistoryVo));
        RpcResponse.Response response = null;
        Future<RpcResponse.Response> future = null;
        try {
            future = new MasterExecuteJob().executeJob(masterContext, selectWork,
                    triggerType, actionId);
            response = future.get(HeraGlobalEnv.getTaskTimeout(), TimeUnit.HOURS);
        } catch (Exception e) {
            ErrorLog.error("schedule job run error :" + actionId, e);
            if (future != null) {
                future.cancel(true);
            }
            heraAction.setStatus(StatusEnum.FAILED.toString());
            heraJobHistoryVo.setStatusEnum(StatusEnum.FAILED);
            masterContext.getHeraJobHistoryService().updateHeraJobHistoryStatus(BeanConvertUtils.convert(heraJobHistoryVo));
        }
        boolean success = response != null && response.getStatusEnum() == ResponseStatus.Status.OK;
        ScheduleLog.info("job_id ????????????" + actionId + "---->" + (response == null ? "?????????" : response.getStatusEnum().toString()));
        ApplicationEvent event;
        if (!success) {
            heraAction.setStatus(StatusEnum.FAILED.toString());
            HeraJobHistory history = masterContext.getHeraJobHistoryService().findById(heraJobHistoryVo.getId());
            HeraJobHistoryVo jobHistory = BeanConvertUtils.convert(history);
            event = new HeraJobFailedEvent(actionId, triggerType, jobHistory);
            ((HeraJobFailedEvent) event).setRollBackTime(retryWaitTime);
            ((HeraJobFailedEvent) event).setRunCount(runCount);
            ((HeraJobFailedEvent) event).setRetryCount(retryCount);
            if (Constants.CANCEL_JOB_MESSAGE.equals(jobHistory.getIllustrate()) || StatusEnum.WAIT.toString().equals(history.getStatus())) {
                isCancelJob = true;
                ScheduleLog.info("???????????????????????????????????????:{}", jobHistory.getActionId());
            }
        } else {
            //????????????????????? ????????????
            if (JobScheduleTypeEnum.Dependent.getType().equals(heraAction.getScheduleType())) {
                heraAction.setReadyDependency("{}");
            }
            heraAction.setStatus(StatusEnum.SUCCESS.toString());
            event = new HeraJobSuccessEvent(actionId, triggerType, heraJobHistoryVo);
        }
        updateCacheAction(actionId, heraAction.getStatus());
        heraAction.setStatisticEndTime(new Date());
        masterContext.getHeraJobActionService().update(heraAction);
        masterContext.getDispatcher().forwardEvent(event);
        if (runCount < (retryCount + 1) && !success && !isCancelJob) {
            DebugLog.info("--------------------------???????????????????????????--------------------------");
            runScheduleJobContext(selectWork, actionId, runCount, retryCount, retryWaitTime, triggerType);
        }
    }

    private void updateCacheAction(Long actionId, String status) {
        HeraAction cacheAction = master.getHeraActionMap().get(actionId);
        if (cacheAction != null) {
            cacheAction.setStatus(status);
        }
    }

    /**
     * ??????????????????????????? ????????????????????????????????????????????????
     *
     * @return
     */
    public boolean isTaskLimit() {
        //????????????apollo??????????????????????????????limit?????????????????????
        setCoreSize(HeraGlobalEnv.getMaxParallelNum());
        return executeJobPool.getActiveCount() >= HeraGlobalEnv.getMaxParallelNum();
    }

    public void setCoreSize(int size) {
        if (size > 0 && size != cacheCoreSize) {
            cacheCoreSize = size;
            executeJobPool.setMaximumPoolSize(size);
            executeJobPool.setCorePoolSize(size);
        }
    }

    public void printThreadPoolLog() {
        String sb = "?????????????????????" + "[ActiveCount: " + executeJobPool.getActiveCount() + "," +
                "CompletedTaskCount???" + executeJobPool.getCompletedTaskCount() + "," +
                "PoolSize:" + executeJobPool.getPoolSize() + "," +
                "LargestPoolSize:" + executeJobPool.getLargestPoolSize() + "," +
                "TaskCount:" + executeJobPool.getTaskCount() + "]";
        ScheduleLog.info(sb);
    }

    @Override
    public void run(MasterWorkHolder workHolder, JobElement element) {
        switch (element.getTriggerType()) {
            case SCHEDULE:
            case SUPER_RECOVER:
            case MANUAL_RECOVER:
                executeJobPool.execute(() -> runScheduleJob(workHolder, element.getJobId(), element.getTriggerType()), element);
                break;
            case MANUAL:
            case AUTO_RERUN:
                executeJobPool.execute(() -> runManualJob(workHolder, element.getJobId(), element.getTriggerType()), element);
                break;
            case DEBUG:
                executeJobPool.execute(() -> runDebugJob(workHolder, element.getJobId()), element);
                break;
            default:
                ErrorLog.error("?????????????????????:" + element.getTriggerType().toString());
                break;
        }
    }

    public Integer getRunningTaskNum() {
        return executeJobPool.getActiveCount();
    }
}
