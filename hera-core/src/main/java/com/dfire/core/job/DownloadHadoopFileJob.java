package com.dfire.core.job;


import com.alibaba.fastjson.JSONObject;
import com.dfire.config.HeraGlobalEnv;
import com.dfire.logs.MonitorLog;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author: <a href="mailto:lingxiao@2dfire.com">凌霄</a>
 * @time: Created in 下午5:18 2018/5/1
 * @desc
 */
public class DownloadHadoopFileJob extends ProcessJob {

    private String hadoopPath;
    private String localPath;

    public DownloadHadoopFileJob(JobContext jobContext, String hadoopPath, String localPath) {
        super(jobContext);
        this.hadoopPath = hadoopPath;
        this.localPath = localPath;
    }

    @Override
    public List<String> getCommandList() {
        List<String> commands = new ArrayList<>();
        if (StringUtils.isNotBlank(HeraGlobalEnv.getKerberosKeytabPath()) && StringUtils.isNotBlank(HeraGlobalEnv.getKerberosPrincipal())) {
            commands.add("kinit -kt " + HeraGlobalEnv.getKerberosKeytabPath() + " " + HeraGlobalEnv.getKerberosPrincipal());
        }
        commands.add("hadoop fs -copyToLocal " + hadoopPath + " " + localPath);
        MonitorLog.debug("组装后的命令为:" + JSONObject.toJSONString(commands));
        return commands;
    }

}
