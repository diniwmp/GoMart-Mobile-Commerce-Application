package lk.zenova.gomart.model;

public class SuggestionItem {

    public static final int TYPE_PRODUCT  = 1;
    public static final int TYPE_CATEGORY = 2;
    public static final int TYPE_BRAND    = 3;

    private String displayName;
    private int    type;
    private Object data;

    public SuggestionItem(String displayName, int type, Object data) {
        this.displayName = displayName;
        this.type        = type;
        this.data        = data;
    }

    public String getDisplayName() { return displayName; }
    public int    getType()        { return type; }
    public Object getData()        { return data; }

    @Override
    public String toString() { return displayName; }
}