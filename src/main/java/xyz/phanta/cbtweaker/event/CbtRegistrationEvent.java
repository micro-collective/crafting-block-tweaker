package xyz.phanta.cbtweaker.event;

import net.minecraftforge.fml.common.eventhandler.GenericEvent;

import java.util.function.Consumer;

public class CbtRegistrationEvent<T> extends GenericEvent<T> {

    private final Consumer<T> callback;

    public CbtRegistrationEvent(Class<T> type, Consumer<T> callback) {
        super(type);
        this.callback = callback;
    }

    public void register(T object) {
        callback.accept(object);
    }

}
