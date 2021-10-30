package xyz.phanta.cbtweaker.util.item;

public interface CustomModelItem {

    void collectItemModels(ModelAcceptor modelAcceptor);

    @FunctionalInterface
    interface ModelAcceptor {

        void accept(int meta, String path, String variant);

        default void accept(int meta, String path) {
            accept(meta, path, "inventory");
        }

    }

}
