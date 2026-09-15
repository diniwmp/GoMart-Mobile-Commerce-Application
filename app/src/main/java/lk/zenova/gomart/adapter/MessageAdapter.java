package lk.zenova.gomart.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import lk.zenova.gomart.R;
import lk.zenova.gomart.model.Message;

public class MessageAdapter extends
        RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_SENT     = 1;
    private static final int VIEW_TYPE_RECEIVED = 2;

    private final List<Message> messages;
    private final String currentUserId;

    public MessageAdapter(List<Message> messages, String currentUserId) {
        this.messages      = messages;
        this.currentUserId = currentUserId;
    }

    @Override
    public int getItemViewType(int position) {
        Message msg = messages.get(position);
        return msg.getSenderId().equals(currentUserId)
                ? VIEW_TYPE_SENT
                : VIEW_TYPE_RECEIVED;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == VIEW_TYPE_SENT) {
            View v = inflater.inflate(
                    R.layout.item_message_sent, parent, false);
            return new SentViewHolder(v);
        } else {
            View v = inflater.inflate(
                    R.layout.item_message_received, parent, false);
            return new ReceivedViewHolder(v);
        }
    }

    @Override
    public void onBindViewHolder(
            @NonNull RecyclerView.ViewHolder holder, int position) {
        Message msg = messages.get(position);
        String time = "";
        if (msg.getTimestamp() != null) {
            time = new SimpleDateFormat("hh:mm a", Locale.getDefault())
                    .format(new Date(
                            msg.getTimestamp().toDate().getTime()));
        }

        if (holder instanceof SentViewHolder) {
            SentViewHolder h = (SentViewHolder) holder;
            h.msgText.setText(msg.getText());
            h.msgTime.setText(time);
        } else if (holder instanceof ReceivedViewHolder) {
            ReceivedViewHolder h = (ReceivedViewHolder) holder;
            h.msgText.setText(msg.getText());
            h.msgTime.setText(time);
        }
    }

    @Override
    public int getItemCount() { return messages.size(); }


    static class SentViewHolder extends RecyclerView.ViewHolder {
        TextView msgText, msgTime;
        SentViewHolder(@NonNull View v) {
            super(v);
            msgText = v.findViewById(R.id.msg_text);
            msgTime = v.findViewById(R.id.msg_time);
        }
    }

    static class ReceivedViewHolder extends RecyclerView.ViewHolder {
        TextView msgText, msgTime, msgSender;
        ReceivedViewHolder(@NonNull View v) {
            super(v);
            msgText   = v.findViewById(R.id.msg_text);
            msgTime   = v.findViewById(R.id.msg_time);
            msgSender = v.findViewById(R.id.msg_sender);
        }
    }
}