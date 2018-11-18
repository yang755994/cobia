package lam.cobia.cluster;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lam.cobia.core.model.RegistryData;
import lam.cobia.core.util.ParamConstant;
import lam.cobia.loadbalance.LoadBalance;
import lam.cobia.rpc.Consumer;
import lam.cobia.rpc.Invocation;
import lam.cobia.rpc.Result;

import java.util.List;
import java.util.Random;

/**
 * @description: RandomLoadBalanceCluster
 * @author: linanmiao
 * @date: 2018/7/22 0:30
 * @version: 1.0
 */
public class FailoverCluster<T> extends AbstractCluster<T>{

    private static Logger LOGGER = LoggerFactory.getLogger(FailoverCluster.class);

    private int retryTime = 2;

    public FailoverCluster() {
        super.name = "failover";
    }

    public FailoverCluster(Class<T> interfaceClass, List<Consumer<T>> consumers, LoadBalance loadBalance) {
        super(interfaceClass, consumers, loadBalance);
        super.name = "failover";
    }

    @Override
    public Result doInvoke(Invocation invocation) {
        Consumer<T> selectedConsumer = null;
        Consumer<T> consumer = null;
        for (int invokeTime = 0; invokeTime <= retryTime; invokeTime++) {
            consumer = select(getConsumers(), selectedConsumer, invocation);
            try {
                selectedConsumer = consumer;
                Result result = consumer.invoke(invocation);
                return result;
            } catch (Exception e) {
                LOGGER.error("[doInvoke] interface:{}, method:{}, parameterTypes:{}, arguments:{}",
                       invocation.getInterface(), invocation.getMethod(), invocation.getParameterTypes(), invocation.getArguments(), e);
            }
        }
        //shouldn't happen.
        throw new IllegalStateException("invoke fail");
    }

    @Override
    public <T> void onProvidersChanges(Class<T> clazz, List<RegistryData> registryDatas) {
        super.reloadConsumers(clazz.getName(), registryDatas);
    }

    @Override
    public <T> void onProviderChanges(Class<T> clazz, RegistryData registryData) {
        super.reloadConsumer(clazz.getName(), registryData);
    }
}
