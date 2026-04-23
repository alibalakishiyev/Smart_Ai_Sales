package com.DiscountMarket.store;


import com.DiscountMarket.model.ArazProduct;

import java.util.List;

public class ArazMarketResponse {
    private String status;
    private int code;
    private String message;
    private List<ArazProduct> data;
    private int errors;

    public String getStatus() { return status; }
    public int getCode() { return code; }
    public String getMessage() { return message; }
    public List<ArazProduct> getData() { return data; }
    public int getErrors() { return errors; }

    public boolean isSuccess() {
        return "success".equals(status) && code == 200;
    }
}