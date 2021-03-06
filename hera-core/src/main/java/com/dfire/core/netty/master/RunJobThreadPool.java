package com.dfire.core.netty.master;

import com.dfire.common.config.ExecuteFilter;
import com.dfire.common.config.FilterType;
import com.dfire.common.config.ServiceLoader;
import com.dfire.common.entity.vo.HeraDebugHistoryVo;
import com.dfire.common.entity.vo.HeraJobHistoryVo;
import com.dfire.common.enums.JobStatus;
import com.dfire.common.enums.TriggerTypeEnum;
import com.dfire.common.service.HeraDebugHistoryService;
import com.dfire.common.service.HeraJobHistoryService;
import com.dfire.common.util.BeanConvertUtils;
import com.dfire.common.vo.JobElement;
import com.dfire.logs.ErrorLog;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * desc:
 *
 * @author scx
 * @create 2019/04/24
 */
public class RunJobThreadPool extends ThreadPoolExecutor {

    private static ConcurrentHashMap<Runnable, JobElement> jobEmrType;
    private final HeraJobHistoryService jobHistoryService;
    private final HeraDebugHistoryService debugHistoryService;


    public RunJobThreadPool(MasterContext masterContext, int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory, RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
        jobHistoryService = masterContext.getHeraJobHistoryService();
        debugHistoryService = masterContext.getHeraDebugHistoryService();
        jobEmrType = new ConcurrentHashMap<>(maximumPoolSize);
    }

    public static List<Long> getWaitClusterJob(TriggerTypeEnum... typeEnum) {
        if (jobEmrType == null) {
            return new ArrayList<>(0);
        }
        Set<TriggerTypeEnum> typeSet = new HashSet<>(Arrays.asList(typeEnum));
        return new ArrayList<>(jobEmrType.values())
                .stream()
                .filter(element -> element.getStatus().equals(JobStatus.waitCluster) && typeSet.contains(element.getTriggerType()))
                .map(JobElement::getJobId)
                .collect(Collectors.toList());
    }

    public static boolean cancelJob(Long id, TriggerTypeEnum... typeEnum) {
        if (jobEmrType == null) {
            return false;
        }
        Set<TriggerTypeEnum> typeSet = new HashSet<>(Arrays.asList(typeEnum));
        Optional<JobElement> jobElement = jobEmrType.values().stream()
                .filter(element -> element.getStatus().equals(JobStatus.waitCluster) && typeSet.contains(element.getTriggerType()))
                .filter(element -> element.getJobId().equals(id)).findFirst();
        if (jobElement.isPresent()) {
            jobElement.get().setCancel(true);
            return true;
        }
        return false;
    }

    @Override
    protected void beforeExecute(Thread t, Runnable r) {
        JobElement jobElement = jobEmrType.get(r);
        try {
            if (jobElement.isCancel()) {
                appendCreateLog(jobElement, "??????????????????");
                //???????????????????????????????????????
                throw new IllegalStateException("?????????????????????:" + jobElement.getJobId());
            }
            appendCreateLog(jobElement, "??????????????????");
            jobElement.setStatus(JobStatus.running);
        } catch (Exception e) {
            ErrorLog.error("????????????????????????" + e.getMessage(), e);
        }

        if (jobElement.isCancel()) {
            appendCreateLog(jobElement, "??????????????????");
            //??????????????????????????????
            afterExecute(r, null);
            //?????????????????????????????????
            throw new IllegalStateException("?????????????????????:" + jobElement.getJobId());
        }
        doFilter(FilterType.execute, jobElement);
    }

    private void doFilter(FilterType filterType, JobElement element) {
        List<ExecuteFilter> filters = ServiceLoader.getFilters();
        try {
            switch (filterType) {
                case execute:
                    for (ExecuteFilter filter : filters) {
                        try {
                            filter.onExecute(element);
                        } catch (Exception e) {
                            ErrorLog.error("???????????????????????????", e);
                        }
                    }
                    break;
                case response:
                    for (ExecuteFilter filter : filters) {
                        try {
                            filter.onResponse(element);
                        } catch (Exception e) {
                            ErrorLog.error("???????????????????????????", e);
                        }
                    }
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            ErrorLog.error("???????????????:" + e.getMessage(), e);
        }
    }

    @Override
    protected void afterExecute(Runnable r, Throwable t) {
        JobElement jobElement = jobEmrType.get(r);
        jobEmrType.remove(r);
        jobElement.setStatus(JobStatus.complete);
        doFilter(FilterType.response, jobElement);
    }

    private void appendCreateLog(JobElement element, String log) {
        switch (element.getTriggerType()) {
            case SCHEDULE:
            case MANUAL_RECOVER:
            case MANUAL:
            case AUTO_RERUN:
            case SUPER_RECOVER:
                HeraJobHistoryVo historyVo = BeanConvertUtils.convert(jobHistoryService.findById(element.getHistoryId()));
                historyVo.getLog().appendHera(log);
                jobHistoryService.updateHeraJobHistoryLog(BeanConvertUtils.convert(historyVo));
                break;
            case DEBUG:
                HeraDebugHistoryVo heraDebugHistoryVo = debugHistoryService.findById(element.getHistoryId());
                heraDebugHistoryVo.getLog().appendHera(log);
                debugHistoryService.updateLog(BeanConvertUtils.convert(heraDebugHistoryVo));
                break;
            default:
                ErrorLog.error("?????????????????????:" + element.getTriggerType().toString());
                break;
        }
    }



    @Override
    public List<Runnable> shutdownNow() {
        List<Runnable> alive = super.shutdownNow();
        for (Runnable runnable : alive) {
            jobEmrType.remove(runnable);
        }
        return alive;
    }

    /**
     * ?????????????????????
     *
     * @param command    ???????????????
     * @param jobElement JobElement
     */
    public void execute(Runnable command, JobElement jobElement) {
        jobElement.setStatus(JobStatus.waitCluster);
        jobEmrType.putIfAbsent(command, jobElement);
        super.execute(command);
    }
}
