package opencontacts.open.com.opencontacts.interfaces;

public interface DataStoreChangeListener<T> {
    int ADDITION = 1;
    int DELETION = 2;
    int UPDATION = 3;
    int REFRESH = 4;

    void onUpdate(T t);

    void onRemove(T t);

    void onAdd(T t);

    void onStoreRefreshed();
}
