package xyz.phanta.cbtweaker.event;

import net.minecraftforge.fml.common.eventhandler.GenericEvent;
import xyz.phanta.cbtweaker.buffer.BufferType;
import xyz.phanta.cbtweaker.buffer.ingredient.IngredientMatcherType;
import xyz.phanta.cbtweaker.buffer.ingredient.IngredientProviderType;

import java.util.function.Consumer;

// sadly, only one type parameter is supported for generic events
public class CbtIngredientHandlerRegistrationEvent<A, JA> extends GenericEvent<A> {

    private final BufferType<?, A, ?, JA> bufType;
    private final Consumer<IngredientMatcherType<A, JA>> matcherCb;
    private final Consumer<IngredientProviderType<A, JA>> providerCb;

    public CbtIngredientHandlerRegistrationEvent(Class<A> accClass, BufferType<?, A, ?, JA> bufType,
                                                 Consumer<IngredientMatcherType<A, JA>> matcherCb,
                                                 Consumer<IngredientProviderType<A, JA>> providerCb) {
        super(accClass);
        this.bufType = bufType;
        this.matcherCb = matcherCb;
        this.providerCb = providerCb;
    }

    public BufferType<?, A, ?, JA> getBufferType() {
        return bufType;
    }

    public void registerMatcherType(IngredientMatcherType<A, JA> type) {
        matcherCb.accept(type);
    }

    public void registerProviderType(IngredientProviderType<A, JA> type) {
        providerCb.accept(type);
    }

}
