import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Parser {
    ExecutorService executorService = Executors.newCachedThreadPool();
    private Document doc;
    private int result;
    private int pageCount;
    private String cheapestElement;
    private double lowestPrice = Double.MAX_VALUE;
    private Map<String, Capacitor> capacitorMap;
    private Capacitor capacitor = new Capacitor();

    public String parsingFoundElement(String url) throws ExecutionException, InterruptedException {
        doc = executorService.submit(() -> ConnectionURL.parsePage(url)).get();
        result = Integer.parseInt(doc.select("span.record-count-lbl").text());
        pageCount = (int) Math.ceil((double) result / 25);
        cheapestElement = null;
        capacitorMap = new HashMap<>();
        capacitor = new Capacitor();

        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (int i = 0; i <= pageCount; i++) {
            String parseUrl = url + "&pg=" + i;
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    Document parseDocument = ConnectionURL.parsePage(parseUrl);
                    processDocument(parseDocument);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }, executorService);
            futures.add(future);
        }

        // Wait for all futures to complete
        CompletableFuture<Void> allOf = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        allOf.get();

        return printCheapestCapacitor(cheapestElement, capacitorMap);
    }

    private void processDocument(Document parseDocument) {
        if (parseDocument.body().text().contains(" See an Error?")) {
            synchronized (this) {
                capacitorMap.put(parseOneElement(doc).getPartNumber(), parseOneElement(doc));
            }
        } else {
            // Извлечение строк tr
            Elements rows = parseDocument.select("#SearchResultsGrid_grid > tbody > tr");
            for (Element row : rows) {
                Elements pricingRows = row.select("table.search-pricing-table");
                String partNumber = getPartNumber(row);
                String partNumberLink = getPartNumberLink(row);
                Map<String, String> prices = getPrice(row);
                Map<String, String> inStock = getInStock(row);

                Capacitor capacitor = new Capacitor();
                capacitor.setPartNumber(partNumber);
                capacitor.setLinkPartNumber(partNumberLink);
                capacitor.setPrices(prices);
                capacitor.setInStock(inStock);

                synchronized (this) {
                    capacitorMap.put(partNumber, capacitor);
                    if (prices.containsKey("100")) {
                        double priceHundred = Double.parseDouble(prices.get("100"));
                        if (priceHundred < lowestPrice) {
                            lowestPrice = priceHundred;
                            cheapestElement = partNumber;
                        }
                    }
                }
            }
        }
    }

    public String printCheapestCapacitor(String cheapestElement, Map<String, Capacitor> capacitorMap) {
        Capacitor capacitor = new Capacitor();

        if (cheapestElement != null) {
            capacitor = capacitorMap.get(cheapestElement);

            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("\n<b>Найден самый дешевый элемент (по цене за 100 штук):</b>\n");
            stringBuilder.append("<blockquote><u>Part Number:</u> \n" + "<a href=\"" + "https://eu.mouser.com" + capacitor.getLinkPartNumber() + "\">" + capacitor.getPartNumber() + " (ссылка)" + "</a></blockquote>");
            stringBuilder.append("<blockquote>");
            stringBuilder.append("В наличии: " + capacitor.getInStock().get("inStock") + " шт.");
            if (capacitor.getInStock().get("onOrder") != null) {
                stringBuilder.append("\nПод заказ: " + capacitor.getInStock().get("onOrder") + " шт.");
            } else if (capacitor.getInStock().get("expected") != null) {
                stringBuilder.append("\nОжидается: " + capacitor.getInStock().get("expected") + " шт.");
            }
            stringBuilder.append("</blockquote>");
            stringBuilder.append("\n<blockquote><u>Цена за:</u>\n• 1 шт: " + capacitor.getPrices().get("1") + " €\n" + "• 10 шт: " + capacitor.getPrices().get("10") + " €\n" + "• 100 шт: " + capacitor.getPrices().get("100") + " €\n" + "• 1000 шт: " + capacitor.getPrices().get("1000") + " €</blockquote>");

            return stringBuilder.toString();

        } else {
            return "Не найдено ни одного элемента не найдено.";
        }

    }


    public Capacitor parseOneElement(Document doc) {
        Capacitor capacitor = new Capacitor();
        Element canonicalLink = doc.head().selectFirst("link[rel=canonical]");
        String partNumberLink = canonicalLink.attr("href");
        String partNumber = doc.body().select("div.panel-heading.pdp-product-card-header > h1").text();
        Map<String, String> inStockElements = new HashMap<>();
        String inStock;
        String onOrder;
        String expected;
        if(doc.body().text().contains("In Stock:")) {
            inStock = doc.body().select("div.panel-title.pdp-pricing-header").text().replaceAll(".*:", "").replace(".", "").replace(" ", "");
            inStockElements.put("inStock", inStock);
        } else if (doc.body().text().contains("On Order:")) {
            onOrder = doc.body().select("#content-onOrderShipments > div.onOrderLine > div.col-xs-3.onOrderQuantity").text().replace(".", "");
            inStockElements.put("onOrder", onOrder);
        } else if (doc.body().text().contains("Expected:")) {
            expected = doc.body().select("#content-onOrderShipments > div.onOrderLine > div.col-xs-3.onOrderQuantity").text().replace(".", "");
            inStockElements.put("expected", expected);
        }

        Map<String, String> prices = new HashMap<>();

        Elements priceElements = doc.select("table.pricing-table > tbody > tr");
        for (Element price : priceElements) {
            prices = extractPrices(price.text());
        }
        capacitor.setLinkPartNumber(partNumberLink);
        capacitor.setPartNumber(partNumber);
        capacitor.setInStock(inStockElements);
        capacitor.setPrices(prices);

        return capacitor;
    }


    public String getPartNumberLink(Element row) {
        Elements partNumberRows = row.select("div.mfr-part-num.hidden-xs > a");
        String linkElement = null;
        for (Element cell : partNumberRows) {
            linkElement = cell.attr("href");
        }

        return linkElement;
    }

    public String getPartNumber(Element row) {
        Elements partNumberRows = row.select("div.mfr-part-num.hidden-xs > a");
        String partNumber = null;
        for (Element cell : partNumberRows) {
            partNumber = cell.text();
        }
        return partNumber;
    }

    public Map<String, String> getPrice(Element row) {
        Elements pricingRows = row.select("table.search-pricing-table");
        Map<String, String> priceMap = new HashMap<>();
        for (Element price : pricingRows) {
            String priceText = price.text();
            priceMap = extractPrices(priceText);
        }
        return priceMap;
    }

    public Map<String, String> getInStock(Element row) {
        // Находим все элементы span с классом avail-status, содержащие текст "In Stock"
        Elements inStockElement = row.select("span.avail-status:contains(In Stock)");
        Elements onOrderElement = row.select("span.avail-status:contains(On Order)");
        Elements expectedElement = row.select("span.avail-status:contains(Expected)");
        Map<String, String> order = new HashMap<>();

        // Перебираем найденные элементы
        for (Element inStockStatusElement : inStockElement) {
            // Получаем предыдущий элемент span с классом available-amount
            Element inStockAmountElement = inStockStatusElement.previousElementSibling();
            if (inStockAmountElement != null && inStockAmountElement.hasClass("available-amount")) {
                String value = inStockAmountElement.text().replace(".", "").replace(" ", "");
                order.put("inStock", value);
                break; // Если нашли первое значение, выходим из цикла
            }
        }
        for (Element onOrderStatusElement : onOrderElement) {
            // Получаем предыдущий элемент span с классом available-amount
            Element onOrderAmountElement = onOrderStatusElement.previousElementSibling();
            if (onOrderAmountElement != null && onOrderAmountElement.hasClass("available-amount")) {
                String value = onOrderAmountElement.text().replace(".", "").replace(" ", "");
                order.put("onOrder", value);
                break; // Если нашли первое значение, выходим из цикла
            }
        }
        for (Element expectedStatusElement : expectedElement) {
            // Получаем предыдущий элемент span с классом available-amount
            Element expectedAmountElement = expectedStatusElement.previousElementSibling();
            if (expectedAmountElement != null && expectedAmountElement.hasClass("available-amount")) {
                String value = expectedAmountElement.text();
                order.put("expected", value);
                break; // Если нашли первое значение, выходим из цикла
            }
        }
        /*
        
        
        Elements quantityRows = row.select("span.available-amount");
        Set<String> uniqueQuantity = new HashSet<>();
        Map<String, String> order = new HashMap<>();

        for (Element quantity : quantityRows) {
            uniqueQuantity.add(quantity.text().replace(".", ""));
        }
        String[] uniqueQuantityString = uniqueQuantity.toArray(new String[uniqueQuantity.size()]);

        if (uniqueQuantity.size() == 2) {
            order.put("inStock", uniqueQuantityString[0]);
            order.put("onOrder", uniqueQuantityString[1]);
        }*/
        return order;
    }

    public Map<String, String> extractPrices(String priceText) {
        Map<String, String> priceMap = new HashMap<>();

        // Измененное регулярное выражение для обработки цен с тысячами
        Pattern pattern = Pattern.compile("(\\d[\\.\\d]*):\\s([\\d,\\.]+)\\s€");
        Matcher matcher = pattern.matcher(priceText);

        while (matcher.find()) {
            String quantityStr = matcher.group(1).replace(".", "");
            String price = matcher.group(2).replace(",", "."); // Заменяем запятую на точку для десятичных дробей
            priceMap.put(quantityStr, price);
        }

        return priceMap;
    }

}

