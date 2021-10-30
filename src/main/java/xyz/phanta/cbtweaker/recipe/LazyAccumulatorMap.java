package xyz.phanta.cbtweaker.recipe;

import io.github.phantamanta44.libnine.util.LazyConstant;
import xyz.phanta.cbtweaker.buffer.BufferGroup;
import xyz.phanta.cbtweaker.buffer.BufferType;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public interface LazyAccumulatorMap {

    @Nullable
    Group getGroup(String groupId);

    LazyAccumulatorMap copyAccumulators();

    interface Group {

        <A> Supplier<A> getAccumulator(BufferType<?, A, ?, ?> bufType);

    }

    class Impl implements LazyAccumulatorMap {

        private final Map<String, BufferGroup> bufGroups;
        private final Map<String, BaseGroup> groupCache = new HashMap<>();

        public Impl(Map<String, BufferGroup> bufGroups) {
            this.bufGroups = bufGroups;
        }

        @Nullable
        @Override
        public Group getGroup(String groupId) {
            if (!groupCache.containsKey(groupId)) {
                BufferGroup bufGroup = bufGroups.get(groupId);
                if (bufGroup == null) {
                    groupCache.put(groupId, null);
                    return null;
                } else {
                    BaseGroup group = new BaseGroup(bufGroup);
                    groupCache.put(groupId, group);
                    return group;
                }
            }
            return groupCache.get(groupId);
        }

        @Override
        public LazyAccumulatorMap copyAccumulators() {
            return new CopyingMap(this);
        }

        private static class BaseGroup implements Group {

            private final BufferGroup bufGroup;
            private final Map<BufferType<?, ?, ?, ?>, Supplier<?>> accumCache = new HashMap<>();

            public BaseGroup(BufferGroup bufGroup) {
                this.bufGroup = bufGroup;
            }

            @SuppressWarnings("unchecked")
            @Override
            public <A> Supplier<A> getAccumulator(BufferType<?, A, ?, ?> bufType) {
                return (Supplier<A>)accumCache.computeIfAbsent(bufType, k -> new LazyConstant<>(
                        () -> bufGroup.accumulateIngredients(bufType)));
            }

        }

        private static class CopyingMap implements LazyAccumulatorMap {

            private final LazyAccumulatorMap baseMap;
            private final Map<String, CopyingGroup> groupCache = new HashMap<>();

            public CopyingMap(LazyAccumulatorMap baseMap) {
                this.baseMap = baseMap;
            }

            @Nullable
            @Override
            public Group getGroup(String groupId) {
                if (!groupCache.containsKey(groupId)) {
                    Group baseGroup = baseMap.getGroup(groupId);
                    if (baseGroup == null) {
                        groupCache.put(groupId, null);
                        return null;
                    } else {
                        CopyingGroup group = new CopyingGroup(baseGroup);
                        groupCache.put(groupId, group);
                        return group;
                    }
                }
                return groupCache.get(groupId);
            }

            @Override
            public LazyAccumulatorMap copyAccumulators() {
                return new CopyingMap(this);
            }

            private static class CopyingGroup implements Group {

                private final Group baseGroup;
                private final Map<BufferType<?, ?, ?, ?>, Supplier<?>> accumCache = new HashMap<>();

                public CopyingGroup(Group baseGroup) {
                    this.baseGroup = baseGroup;
                }

                @SuppressWarnings("unchecked")
                @Override
                public <A> Supplier<A> getAccumulator(BufferType<?, A, ?, ?> bufType) {
                    return (Supplier<A>)accumCache.computeIfAbsent(bufType, k -> new LazyConstant<>(
                            () -> bufType.copyAccumulator(baseGroup.getAccumulator(bufType).get())));
                }

            }

        }

    }

}
