package com.myapp.stockWatcher.client;

import java.io.Serializable;

/**
 * Created by MZhang on 1/05/2017.
 */
public class StockPrice implements Serializable {
    private String symbol;
    private double price;
    private double change;

    public StockPrice(){}

    public StockPrice(String symbol, double price, double change) {
        this.symbol = symbol;
        this.price = price;
        this.change = change;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public double getChange(){
        return this.change;
    }

    public double getChangePercentage() {
        return 100.0 * change/this.price;
    }

    public void setChange(double change) {
        this.change = change;
    }
}
