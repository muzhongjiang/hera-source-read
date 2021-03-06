package com.dfire.common.exception;

import com.dfire.logs.ErrorLog;

/**
 * @author: <a href="mailto:lingxiao@2dfire.com">ει</a>
 * @time: Created in δΈε3:23 2018/6/12
 * @desc
 */
public class HeraCaughtExceptionHandler implements Thread.UncaughtExceptionHandler {

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        ErrorLog.error("Thread pool caught thread exception " + e);
        throw new RuntimeException(e);
    }
}
