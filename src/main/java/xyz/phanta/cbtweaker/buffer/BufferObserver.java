package xyz.phanta.cbtweaker.buffer;

public interface BufferObserver {

    BufferObserver NOOP = new BufferObserver() {
        @Override
        public void onIngredientsChanged() {
            // NO-OP
        }

        @Override
        public void onComponentsChanged() {
            // NO-OP
        }
    };

    void onIngredientsChanged();

    void onComponentsChanged();

}
