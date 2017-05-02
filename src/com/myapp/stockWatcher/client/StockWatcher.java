package com.myapp.stockWatcher.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsonUtils;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.http.client.*;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.jsonp.client.JsonpRequestBuilder;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;

import java.util.*;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class StockWatcher implements EntryPoint {

    private static final int REFRESH_INTERVAL = 5000;
    private VerticalPanel mainPanel = new VerticalPanel();
    private FlexTable stocksFlexTable = new FlexTable();
    private HorizontalPanel addPanel = new HorizontalPanel();
    private TextBox newSymbolTextBox = new TextBox();
    private Label lastUpdatedLabel = new Label();
    private List<String> stocks = new ArrayList<>();
    private StockWatcherConstants constants = GWT.create(StockWatcherConstants.class);
    private StockWatcherMessages messages = GWT.create(StockWatcherMessages.class);
    private StockPriceServiceAsync stockPriceServiceAsync = GWT.create(StockPriceService.class);
    private Label errorMsgLabel = new Label();
    private static final String JSON_URL = GWT.getModuleBaseURL() + "jsonStockPrices?q=";
    private static final String REMOTE_JSON_URL = "http://127.0.0.1:8000/?q=";

    /**
     * This is the entry point method.
     */
    public void onModuleLoad() {

        Window.setTitle(constants.stockWatcher());
        RootPanel.get("appTitle").add(new Label(constants.stockWatcher()));

        Button addStockButton = new Button(constants.add());

        stocksFlexTable.setText(0, 0, "Symbol");
        stocksFlexTable.setText(0, 1, "Price");
        stocksFlexTable.setText(0, 2, "Change");
        stocksFlexTable.setText(0, 3, "Remove");

        stocksFlexTable.addStyleName("watchList");
        stocksFlexTable.getRowFormatter().addStyleName(0, "watchListHeader");
        stocksFlexTable.getCellFormatter().addStyleName(0, 1, "watchListNumericColumn");
        stocksFlexTable.getCellFormatter().addStyleName(0, 2, "watchListNumericColumn");
        stocksFlexTable.getCellFormatter().addStyleName(0, 3, "watchListRemoveColumn");

        addPanel.add(newSymbolTextBox);
        addPanel.add(addStockButton);
        addPanel.addStyleName("addPanel");

        errorMsgLabel.setStyleName("errorMessage");
        errorMsgLabel.setVisible(false);

        mainPanel.add(errorMsgLabel);
        mainPanel.add(stocksFlexTable);
        mainPanel.add(addPanel);
        mainPanel.add(lastUpdatedLabel);

        RootPanel.get("stocklist").add(mainPanel);

        newSymbolTextBox.setFocus(true);

        Timer refreshTimer = new Timer() {

            @Override
            public void run() {
                refreshWatchList();
            }
        };
        refreshTimer.scheduleRepeating(REFRESH_INTERVAL);

        addStockButton.addClickHandler(clickEvent -> addStock());

        newSymbolTextBox.addKeyDownHandler(event -> {
            if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER) {
                addStock();
            }
        });
    }

    private void refreshWatchList() {
        getJsonPricesFromRemote();
    }

    private void displayError(String error) {
        errorMsgLabel.setText("Error: " + error);
        errorMsgLabel.setVisible(true);
    }

    private void getJsonPricesFromRemote() {
        if (stocks.size() == 0) {
            return;
        }

        String url = REMOTE_JSON_URL;

        // Append watch list stock symbols to query URL.
        Iterator<String> iter = stocks.iterator();
        while (iter.hasNext()) {
            url += iter.next();
            if (iter.hasNext()) {
                url += "+";
            }
        }

        url = URL.encode(url);

        JsonpRequestBuilder builder = new JsonpRequestBuilder();
        builder.requestObject(url, new AsyncCallback<JsArray<StockData>>() {
            public void onFailure(Throwable caught) {
                displayError("Couldn't retrieve JSON");
            }

            public void onSuccess(JsArray<StockData> data) {
                if (data == null) {
                    displayError("Couldn't retrieve JSON");
                    return;
                }

                updateTable(data);
            }
        });
    }

    private void getJsonPrices() {
        if (stocks.size() == 0) {
            return;
        }

        String url = JSON_URL;

        // Append watch list stock symbols to query URL.
        Iterator<String> iter = stocks.iterator();
        while (iter.hasNext()) {
            url += iter.next();
            if (iter.hasNext()) {
                url += "+";
            }
        }

        url = URL.encode(url);

        RequestBuilder builder = new RequestBuilder(RequestBuilder.GET, url);

        try {
            Request request = builder.sendRequest(null, new RequestCallback() {
                public void onError(Request request, Throwable exception) {
                    displayError("Couldn't retrieve JSON");
                }

                public void onResponseReceived(Request request, Response response) {
                    if (200 == response.getStatusCode()) {
                        updateTable(JsonUtils.<JsArray<StockData>>safeEval(response.getText()));
                    } else {
                        displayError("Couldn't retrieve JSON (" + response.getStatusText()
                                + ")");
                    }
                }
            });
        } catch (RequestException e) {
            displayError("Couldn't retrieve JSON");
        }

    }

    private void asyncGetPrices() {
        if (stockPriceServiceAsync == null) {
            stockPriceServiceAsync = GWT.create(StockPriceService.class);
        }

        stockPriceServiceAsync.getPrices(stocks.toArray(new String[0]), new AsyncCallback<StockPrice[]>() {
            @Override
            public void onFailure(Throwable caught) {
                String details = caught.getMessage();
                if (caught instanceof DelistedException) {
                    details = "Company '" + ((DelistedException) caught).getSymbol() + "' was delisted";
                }

                errorMsgLabel.setText("Error: " + details);
                errorMsgLabel.setVisible(true);
            }

            @Override
            public void onSuccess(StockPrice[] result) {
                updateTable(result);
            }
        });
    }

    private void updateTable(StockPrice[] prices) {
        Arrays.stream(prices).forEach(this::updateTable);

        lastUpdatedLabel.setText(messages.lastUpdate(new Date()));

        errorMsgLabel.setVisible(false);
    }

    private void updateTable(JsArray<StockData> prices) {
        for (int i = 0; i < prices.length(); i++) {
            updateTable(prices.get(i));
        }

        // Display timestamp showing last refresh.
        lastUpdatedLabel.setText("Last update : " +
                DateTimeFormat.getMediumDateTimeFormat().format(new Date()));

        // Clear any errors.
        errorMsgLabel.setVisible(false);
    }

    private void updateTable(StockData price) {

        if (!stocks.contains(price.getSymbol())) {
            return;
        }

        int row = stocks.indexOf(price.getSymbol()) + 1;

        String priceText = NumberFormat.getFormat("#,##0.00").format(price.getPrice());
        NumberFormat changeFormat = NumberFormat.getFormat("+#,##0.00;-#,##0.00");
        String changeText = changeFormat.format(price.getChange());
        String changePercentText = changeFormat.format(price.getChangePercent());

        // Populate the Price and Change fields with new data.
        stocksFlexTable.setText(row, 1, priceText);
        Label changeWidget = (Label) stocksFlexTable.getWidget(row, 2);
        changeWidget.setText(changeText + " (" + changePercentText + "%)");

        String changeStyleName = "noChange";
        if (price.getChangePercent() < -0.1f) {
            changeStyleName = "negativeChange";
        } else if (price.getChangePercent() > 0.1f) {
            changeStyleName = "positiveChange";
        }

        changeWidget.setStyleName(changeStyleName);
    }


    private void updateTable(StockPrice price) {

        if (!stocks.contains(price.getSymbol())) {
            return;
        }

        int row = stocks.indexOf(price.getSymbol()) + 1;

        String priceText = NumberFormat.getFormat("#,##0.00").format(price.getPrice());
        NumberFormat changeFormat = NumberFormat.getFormat("+#,##0.00;-#,##0.00");
        String changeText = changeFormat.format(price.getChange());
        String changePercentText = changeFormat.format(price.getChangePercentage());

        // Populate the Price and Change fields with new data.
        stocksFlexTable.setText(row, 1, priceText);
        Label changeWidget = (Label) stocksFlexTable.getWidget(row, 2);
        changeWidget.setText(changeText + " (" + changePercentText + "%)");

        String changeStyleName = "noChange";
        if (price.getChangePercentage() < -0.1f) {
            changeStyleName = "negativeChange";
        } else if (price.getChangePercentage() > 0.1f) {
            changeStyleName = "positiveChange";
        }

        changeWidget.setStyleName(changeStyleName);
    }


    private void addStock() {
        final String symbol = newSymbolTextBox.getText().toUpperCase().trim();
        newSymbolTextBox.setFocus(true);

        if (!symbol.matches("^[0-9A-Z\\.]{1,10}$")) {
            Window.alert(messages.invalidSymbol(symbol));
            newSymbolTextBox.selectAll();
            return;
        }

        if (stocks.contains(symbol))
            return;

        int row = stocksFlexTable.getRowCount();
        stocks.add(symbol);

        stocksFlexTable.setText(row, 0, symbol);
        stocksFlexTable.setWidget(row, 2, new Label());
        stocksFlexTable.getCellFormatter().addStyleName(row, 1, "watchListNumericColumn");
        stocksFlexTable.getCellFormatter().addStyleName(row, 2, "watchListNumericColumn");
        stocksFlexTable.getCellFormatter().addStyleName(row, 3, "watchListRemoveColumn");

        Button removeStockButton = new Button("x");
        removeStockButton.addStyleDependentName("remove");
        removeStockButton.addClickHandler(clickEvent -> {
            int removeIndex = stocks.indexOf(symbol);
            stocks.remove(removeIndex);
            stocksFlexTable.removeRow(removeIndex + 1);
        });
        stocksFlexTable.setWidget(row, 3, removeStockButton);

        newSymbolTextBox.setText("");
        refreshWatchList();
    }
}
