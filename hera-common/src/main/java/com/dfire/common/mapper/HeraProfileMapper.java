package com.dfire.common.mapper;

import com.dfire.common.entity.HeraProfile;

/**
 * @author: <a href="mailto:lingxiao@2dfire.com">ει</a>
 * @time: Created in δΈε3:46 2018/5/1
 * @desc
 */
public interface HeraProfileMapper {

    void update(String uid, HeraProfile p);

    public HeraProfile findByUid(String uid);

}
