package com.dfire.core.job;

import com.dfire.common.constants.Constants;
import com.dfire.common.enums.JobRunTypeEnum;
import com.dfire.common.enums.OperatorSystemEnum;
import com.dfire.common.util.HierarchyProperties;
import com.dfire.config.HeraGlobalEnv;
import com.dfire.logs.HeraLog;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;

import java.util.UUID;

/**
 * @author: <a href="mailto:lingxiao@2dfire.com">凌霄</a>
 * @time: Created in 16:49 2018/1/10
 * @desc
 */
public abstract class AbstractJob implements Job {

    protected JobContext jobContext;
    protected boolean canceled = false;

    public AbstractJob(JobContext jobContext) {
        this.jobContext = jobContext;
    }


    @Override
    public boolean isCanceled() {
        return canceled;
    }

    @Override
    public JobContext getJobContext() {
        return jobContext;
    }

    public HierarchyProperties getProperties() {
        return jobContext.getProperties();
    }

    protected String getProperty(String key, String defaultValue) {
        String val;
        return StringUtils.isBlank(val = jobContext.getProperties().getProperty(key)) ? defaultValue : val;
    }

    protected String getProperty(String key) throws NullPointerException {
        String val;
        if ((val = jobContext.getProperties().getProperty(key)) == null) {
            throw new NullPointerException("找不到" + key + "的值");
        }
        return val;
    }

    protected String getJobPrefix() {
        if (HeraGlobalEnv.getSystemEnum() == OperatorSystemEnum.MAC || !HeraGlobalEnv.isSudoUser()) {
            return " ";
        }
        String user = getUser();
        if (StringUtils.isBlank(user)) {
            return null;
        }
        if (HeraGlobalEnv.isMacOS()) {
            return "sudo -u " + user;
        }
        return "sudo -s -E -u " + user;
    }

    protected String generateRunCommand(JobRunTypeEnum runTypeEnum, String prefix, String jobPath) throws Exception {
        StringBuilder command = new StringBuilder();
        // emr集群
        command.append(generateRunCommandBizCore(runTypeEnum, prefix, jobPath));
        return command.toString();
    }

    /**
     * 业务核心代码的脚本内容
     *
     * @param runTypeEnum
     * @param prefix
     * @param runPath
     * @return
     */
    public static String generateRunCommandBizCore(JobRunTypeEnum runTypeEnum, String prefix, String runPath) {

        String uuid = UUID.randomUUID().toString();
        String bizBefore = "bash /etc/profile" + Constants.NEW_LINE
                + "curDir=$(cd `dirname $0`; pwd)" + Constants.NEW_LINE
                + "scriptName=`basename $0`" + Constants.NEW_LINE
                + "cd ${curDir}" + Constants.NEW_LINE
                + "log_file=" + uuid + ".log" + Constants.NEW_LINE
                + "echo \"调度作业的日志文件:[${curDir}/${log_file}]\"" + Constants.NEW_LINE
                + "runtime=`date '+%Y-%m-%d %H:%M:%S'`" + Constants.NEW_LINE
                + "echo \"作业执行开始,时间[$runtime]\"" + Constants.NEW_LINE;

        String bizAfter = "if [ ${PIPESTATUS[0]}  != 0 ]" + Constants.NEW_LINE
                + "then" + Constants.NEW_LINE
                + "    runtime=`date '+%Y-%m-%d %H:%M:%S'`" + Constants.NEW_LINE
                + "    echo \"作业执行失败,时间[$runtime]\"" + Constants.NEW_LINE
                + "    exit -1" + Constants.NEW_LINE
                + "else" + Constants.NEW_LINE
                + "    runtime=`date '+%Y-%m-%d %H:%M:%S'`" + Constants.NEW_LINE
                + "    #'此处可设置web或FTP服务,如上传日志文件，以达到网页端可查看完成日志功能' " + Constants.NEW_LINE
                + "    echo \"作业执行成功,时间[$runtime]\"" + Constants.NEW_LINE
                + "fi" + Constants.NEW_LINE;

        StringBuilder cmd = new StringBuilder();
        cmd.append(bizBefore).append(Constants.NEW_LINE);


        //以下是业务脚本
        switch (runTypeEnum) {
            case Spark:
                cmd.append(HeraGlobalEnv.getJobSparkSqlBin()).append(prefix).append(" -f ").append(runPath);
                cmd.append("  2>&1|tee -a ${log_file} ");
                break;
            case Hive:
                cmd.append(HeraGlobalEnv.getJobHiveBin()).append(" -f ").append(runPath);
                cmd.append("  2>&1|tee -a ${log_file} ");
                break;
            case Shell:
                cmd.append("bash ").append(runPath);
                cmd.append("  2>&1|tee -a ${log_file} ");
                break;
            default:
                break;
        }

        cmd.append(Constants.NEW_LINE).append(bizAfter);

        return cmd.toString();

    }


    protected String getLoginCmd() {
        return "localhost";
    }

    protected String getOwner() {
        return jobContext.getHeraJobHistory().getOperator();
    }

    protected String getIp(String owner) {
        return "localhost";
    }

//    private void uploadFile(String loginCmd, String targetPath, String parentPath) throws Exception {
//        Pair<String, String> pair = CommandUtils.parseCmd(loginCmd);
//        UploadEmrFileJob uploadJob = new UploadEmrFileJob(pair.fst(),
//                parentPath, targetPath, pair.snd(), jobContext);
//        uploadJob.run();
//    }


    /**
     * 获得固定集群的ip
     *
     * @return boolean
     */
    protected String getFixedHost() {
        return getProperty(HeraGlobalEnv.getArea() + "." + Constants.HERA_EMR_FIXED_HOST, HeraGlobalEnv.emrFixedHost).trim();
    }


    protected String getUser() {
        if (jobContext.getRunType() == JobContext.SCHEDULE_RUN || jobContext.getRunType() == JobContext.MANUAL_RUN) {
            return jobContext.getHeraJobHistory().getOperator();
        } else if (jobContext.getRunType() == JobContext.DEBUG_RUN) {
            return jobContext.getDebugHistory().getOwner();
        } else if (jobContext.getRunType() == JobContext.SYSTEM_RUN) {
            return "";
        } else {
            log("没有RunType=" + jobContext.getRunType() + " 的执行类别");
        }
        return null;
    }


    protected String dosToUnix(String script) {
        return script.replace("\r\n", "\n");
    }

    protected boolean checkDosToUnix(String filePath) {
        String[] excludeFile = HeraGlobalEnv.excludeFile.split(Constants.SEMICOLON);
        if (!ArrayUtils.isEmpty(excludeFile)) {
            String lowCaseShellPath = filePath.toLowerCase();
            for (String exclude : excludeFile) {
                if (lowCaseShellPath.endsWith(Constants.POINT + exclude)) {
                    return false;
                }
            }
        }
        return true;
    }

    protected void logConsole(String log) {
        if (jobContext.getHeraJobHistory() != null) {
            jobContext.getHeraJobHistory().getLog().appendConsole(log);
        } else if (jobContext.getDebugHistory() != null) {
            jobContext.getDebugHistory().getLog().appendConsole(log);
        } else {
            HeraLog.info(log);
        }
    }

    protected void log(String log) {
        if (jobContext.getHeraJobHistory() != null) {
            jobContext.getHeraJobHistory().getLog().appendHera(log);
        } else if (jobContext.getDebugHistory() != null) {
            jobContext.getDebugHistory().getLog().appendHera(log);
        } else {
            HeraLog.warn(log);
        }
    }

    protected void log(Exception e) {
        if (jobContext.getHeraJobHistory() != null) {
            jobContext.getHeraJobHistory().getLog().appendHeraException(e);
        } else if (jobContext.getDebugHistory() != null) {
            jobContext.getDebugHistory().getLog().appendHeraException(e);
        } else {
            HeraLog.error(e.getMessage(), e);
        }
    }
}
