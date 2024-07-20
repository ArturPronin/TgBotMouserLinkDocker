import java.util.Map;

public class Capacitor {

    private String partNumber;
    /*private String inStock;
    private String onOrder;*/
    private Map<String, String> inStock;
    private Map<String, String> prices;
    private String linkPartNumber;

    public Capacitor() {
    }

    public String getPartNumber() {
        return partNumber;
    }

    public void setPartNumber(String partNumber) {
        this.partNumber = partNumber;
    }

    public Map<String, String> getInStock() {
        return inStock;
    }

    public void setInStock(Map<String, String> inStock) {
        this.inStock = inStock;
    }
    /*public String getInStock() {
        return inStock;
    }

    public void setInStock(String inStock) {
        this.inStock = inStock;
    }

    public String getOnOrder() {
        return onOrder;
    }

    public void setOnOrder(String onOrder) {
        this.onOrder = onOrder;
    }*/

    public Map<String, String> getPrices() {
        return prices;
    }

    public void setPrices(Map<String, String> prices) {
        this.prices = prices;
    }

    public String getLinkPartNumber() {
        return linkPartNumber;
    }

    public void setLinkPartNumber(String linkPartNumber) {
        this.linkPartNumber = linkPartNumber;
    }

    @Override
    public String toString() {
        return "Capacitor{" +
                "partNumber='" + partNumber + '\'' +
                ", inStock=" + inStock +
                ", prices=" + prices +
                ", linkPartNumber='" + linkPartNumber + '\'' +
                '}';
    }
}
