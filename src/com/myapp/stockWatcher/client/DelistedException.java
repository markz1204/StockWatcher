package com.myapp.stockWatcher.client;

import java.io.Serializable;

/**
 * Created by MZhang on 1/05/2017.
 */
public class DelistedException extends Exception implements Serializable {
    private String symbol;

    public DelistedException() {
    }

    public DelistedException(String symbol) {
        this.symbol = symbol;
    }

    public String getSymbol() {
        return this.symbol;
    }
}
