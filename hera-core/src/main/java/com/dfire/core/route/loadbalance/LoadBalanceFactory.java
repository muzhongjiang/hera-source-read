package com.dfire.core.route.loadbalance;

import com.dfire.config.HeraGlobalEnv;
import com.dfire.core.route.loadbalance.impl.RandomLoadBalance;
import com.dfire.core.route.loadbalance.impl.RoundRobinLoadBalance;
import lombok.extern.slf4j.Slf4j;


/**
 * 负载均衡实例工厂
 *
 * @author xiaosuda
 */
@Slf4j
public class LoadBalanceFactory {

    public static LoadBalance getLoadBalance() {
        LoadBalance loadBalance;
        switch (HeraGlobalEnv.getLoadBalance()) {
            case RoundRobinLoadBalance.NAME:
                loadBalance = new RoundRobinLoadBalance();
                break;
            case RandomLoadBalance.NAME:
                loadBalance = new RandomLoadBalance();
                break;
            default:
                loadBalance = new RoundRobinLoadBalance();
                log.warn("配置LoadBalance有误，使用默认值 RoundRobinLoadBalance");
        }
        return loadBalance;
    }

}
