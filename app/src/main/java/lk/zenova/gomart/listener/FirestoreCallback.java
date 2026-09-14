package lk.zenova.gomart.listener;

public interface FirestoreCallback<T> {
    void onCallback(T data);
}
