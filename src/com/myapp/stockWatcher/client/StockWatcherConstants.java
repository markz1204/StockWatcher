package com.myapp.stockWatcher.client;

import com.google.gwt.i18n.client.Constants;

/**
 * Created by MZhang on 1/05/2017.
 */
public interface StockWatcherConstants extends Constants{
    @DefaultStringValue("StockWatcher")
    String stockWatcher();

    @DefaultStringValue("Symbol")
    String symbol();

    @DefaultStringValue("Price")
    String price();

    @DefaultStringValue("Change")
    String change();

    @DefaultStringValue("Remove")
    String remove();

    @DefaultStringValue("Add")
    String add();
}
