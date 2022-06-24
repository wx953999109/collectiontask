package com.wh.business.collectiontask.domain;

import lombok.Data;

/**
 * @author: wh
 * @date:2022/6/23 17:54
 * @Description:
 */
@Data
public class R {
    int code;
    Object data;
    String msg;

    private R(int code, Object data, String msg) {
        this.code = code;
        this.data = data;
        this.msg = msg;
    }

    public static R success(Object data, String msg) {
        return new R(200, data, msg);
    }

    public static R error(Object data, String msg) {
        return new R(400, data, msg);
    }

    public static R error(String msg) {
        return new R(400, null, msg);
    }
}
