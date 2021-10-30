package xyz.phanta.cbtweaker.recipe;

import io.github.phantamanta44.libnine.util.function.INullableSupplier;
import xyz.phanta.cbtweaker.buffer.BufferGroup;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Map;

public interface RecipeExecutor<R, E> {

    Map<String, BufferGroup> getBufferGroups();

    @Nullable
    Run<R, E> getCurrentRecipe();

    interface Run<R, E> {

        R getRecipe();

        E getExecution();

    }

    class OfNullable<R, E> implements RecipeExecutor<R, E> {

        private final INullableSupplier<RecipeExecutor<R, E>> delegate;

        public OfNullable(INullableSupplier<RecipeExecutor<R, E>> delegate) {
            this.delegate = delegate;
        }

        @Override
        public Map<String, BufferGroup> getBufferGroups() {
            RecipeExecutor<R, E> executor = delegate.get();
            return executor != null ? executor.getBufferGroups() : Collections.emptyMap();
        }

        @Nullable
        @Override
        public Run<R, E> getCurrentRecipe() {
            RecipeExecutor<R, E> executor = delegate.get();
            return executor != null ? executor.getCurrentRecipe() : null;
        }

    }

}
