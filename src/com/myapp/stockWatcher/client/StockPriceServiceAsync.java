package com.myapp.stockWatcher.client;

import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * Created by MZhang on 1/05/2017.
 */
public interface StockPriceServiceAsync {
    void getPrices(String[] symbols, AsyncCallback<StockPrice[]> async);
}
