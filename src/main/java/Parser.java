import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Parser {
    ExecutorService executorService = Executors.newCachedThreadPool();

    public String parsingFoundElement(String url) throws ExecutionException, InterruptedException {
        Document doc;

        doc = executorService.submit(() -> ConnectionURL.parsePage(url)).get();
        //Document doc = parsePage(url);

        int result = Integer.parseInt(doc.select("span.record-count-lbl").text());
        int pageCount = (int) Math.ceil((double) result / 25);

        Element cheapestElement = null;
        double lowestPrice = Double.MAX_VALUE;


        for (int i = 0; i <= pageCount; i++) {
            String parseUrl = url + "&pg=" + i;
            Document parseDocument = executorService.submit(() -> ConnectionURL.parsePage(parseUrl)).get();

            // Извлечение строк tr
            Elements rows = parseDocument.select("#SearchResultsGrid_grid > tbody > tr");

            for (Element row : rows) {

                Elements pricingRows = row.select("table.search-pricing-table");

                for (Element price : pricingRows) {
                    String priceText = price.text();
                    Map<String, String> priceMap = extractPrices(priceText);

                    if (priceMap.containsKey("100")) {
                        double priceHundred = Double.parseDouble(priceMap.get("100"));
                        if (priceHundred < lowestPrice) {
                            lowestPrice = priceHundred;
                            cheapestElement = row;
                        }
                    }

                }

            }

        }


        // Вывод элемента с наименьшей ценой за 100 штук
        if (cheapestElement != null) {
            Element finalCheapestElement = cheapestElement;
            String partNumberRows = executorService.submit(() -> getPartNumber(finalCheapestElement)).get();
            String quantityRows = executorService.submit(() -> getQuantity(finalCheapestElement)).get();
            String priceRows = executorService.submit(() -> getPrice(finalCheapestElement)).get();
            return "\n<b>Найден самый дешевый элемент (по цене за 100 штук):</b>\n" + partNumberRows + quantityRows + priceRows;
        } else {
            return "Не найдено ни одного элемента не найдено.";
        }

    }


    public String getPartNumber(Element row) {
        Elements partNumberRows = row.select("div.mfr-part-num.hidden-xs > a");

        for (Element cell : partNumberRows) {
            // Извлекаем текст внутри элемента <a> (номер детали)
            String partNumber = cell.text();
            // Извлекаем значение атрибута href (ссылка)
            String linkElement = cell.attr("href");

            if (!(linkElement.isEmpty())) {
                return "<blockquote> <u>Part Number:</u> \n" + "<a href=\"" + "https://eu.mouser.com" + linkElement + "\">" + partNumber + " (ссылка)" + "</a> </blockquote>";
            }
            return "<blockquote> <u>Part Number:</u> " + partNumber + "</blockquote>";
        }

        return "";
    }

    public String getPrice(Element row) {
        Elements pricingRows = row.select("table.search-pricing-table");

        for (Element price : pricingRows) {
            String priceText = price.text();
            Map<String, String> priceMap = extractPrices(priceText);

            // Пример использования priceMap
            return "\n<blockquote><u>Цена за:</u>\n" + "• 1 шт: " + priceMap.get("1") + " €\n" + "• 10 шт: " + priceMap.get("10") + " €\n" + "• 100 шт: " + priceMap.get("100") + " €\n" + "• 1000 шт: " + priceMap.get("1000") + " €</blockquote>";


        }
        return "";
    }

    public String getQuantity(Element row) {
        Elements quantityRows = row.select("span.available-amount");
        Set<String> uniqueQuantity = new HashSet<>();

        for (Element quantity : quantityRows) {
            uniqueQuantity.add(quantity.text().replace(".", ""));
        }
        String[] uniqueQuantityString = uniqueQuantity.toArray(new String[uniqueQuantity.size()]);

        if (uniqueQuantity.size() == 2) {
            return "\n<blockquote><u>В наличии:</u> \n" + uniqueQuantityString[1] + " шт.\n" + "Под заказ: " + uniqueQuantityString[0] + "  шт.</blockquote>";
        } else {
            return "\n<blockquote><u>В наличии:</u> \n" + uniqueQuantityString[0] + " шт.</blockquote>";
        }
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
