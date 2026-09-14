package lk.zenova.gomart.model;

public class SearchResultItem {

    public static final int TYPE_HEADER   = 0;
    public static final int TYPE_PRODUCT  = 1;
    public static final int TYPE_CATEGORY = 2;
    public static final int TYPE_BRAND    = 3;

    private int type;
    private String headerTitle;
    private Object data;

    public SearchResultItem(String headerTitle) {
        this.type        = TYPE_HEADER;
        this.headerTitle = headerTitle;
    }

    public SearchResultItem(int type, Object data) {
        this.type = type;
        this.data = data;
    }

    public int    getType()        { return type; }
    public String getHeaderTitle() { return headerTitle; }
    public Object getData()        { return data; }
}