package com.aixi.lv.model.domain;

import lombok.Data;

/**
 * @author Js
 */
@Data
public class Result<T> {

    private T data;

    private Boolean success;

    private String errorMsg;

    public Result(T data, Boolean success, String errorMsg) {
        this.data = data;
        this.success = success;
        this.errorMsg = errorMsg;
    }

    public static Result fail(String errorMsg) {
        return new Result(null, Boolean.FALSE, errorMsg);
    }

    public static <T> Result success(T data) {
        return new Result(data, Boolean.TRUE, null);
    }
}
