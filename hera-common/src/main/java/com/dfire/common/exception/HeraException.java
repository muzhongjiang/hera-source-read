package com.dfire.common.exception;

/**
 * @author: <a href="mailto:lingxiao@2dfire.com">ει</a>
 * @time: Created in δΈε3:18 2018/4/25
 * @desc
 */
public class HeraException extends Exception {

    public HeraException(){
        super();
    }

    public HeraException(String message){
        super(message);
    }

    public HeraException(Throwable e){
        super(e);
    }

    public HeraException(String msg,Throwable e){
        super(msg, e);
    }
}
