package lk.zenova.gomart.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.PropertyName;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Notification {

    private String notifId;
    private String title;
    private String message;
    private String type;
    private String orderId;
    private Timestamp timestamp;

    @PropertyName("isRead")
    private boolean isRead;

    public Notification(String title, String message,
                        String type, String orderId) {
        this.title     = title;
        this.message   = message;
        this.type      = type;
        this.orderId   = orderId;
        this.isRead    = false;
        this.timestamp = Timestamp.now();
    }

    @PropertyName("isRead")
    public boolean isRead() { return isRead; }

    @PropertyName("isRead")
    public void setRead(boolean read) { this.isRead = read; }
}