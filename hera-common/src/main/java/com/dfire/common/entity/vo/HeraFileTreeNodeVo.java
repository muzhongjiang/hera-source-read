package com.dfire.common.entity.vo;

import lombok.Builder;
import lombok.Data;


/**
 * @author: <a href="mailto:lingxiao@2dfire.com">ει</a>
 * @time: Created in δΈε4:49 2018/4/11
 * @desc
 */
@Data
@Builder
public class HeraFileTreeNodeVo {

    Integer id;
    Integer parent;
    String name;
    boolean isParent;

    public boolean getIsParent() {
        return isParent;
    }

    public void setIsParent(boolean isParent) {
        this.isParent = isParent;
    }

}
