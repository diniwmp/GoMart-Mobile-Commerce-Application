package lk.zenova.gomart.model;

import com.google.firebase.Timestamp;

public class Message {
    private String messageId;
    private String text;
    private String senderId;
    private String senderName;
    private Timestamp timestamp;
    private boolean isAdmin;

    public Message() {}

    public Message(String text, String senderId,
                   String senderName, boolean isAdmin) {
        this.text       = text;
        this.senderId   = senderId;
        this.senderName = senderName;
        this.isAdmin    = isAdmin;
        this.timestamp  = Timestamp.now();
    }

    public String getMessageId()          { return messageId; }
    public void setMessageId(String id)   { this.messageId = id; }
    public String getText()               { return text; }
    public void setText(String text)      { this.text = text; }
    public String getSenderId()           { return senderId; }
    public void setSenderId(String id)    { this.senderId = id; }
    public String getSenderName()         { return senderName; }
    public void setSenderName(String n)   { this.senderName = n; }
    public Timestamp getTimestamp()       { return timestamp; }
    public void setTimestamp(Timestamp t) { this.timestamp = t; }
    public boolean isAdmin()              { return isAdmin; }
    public void setAdmin(boolean admin)   { this.isAdmin = admin; }
}