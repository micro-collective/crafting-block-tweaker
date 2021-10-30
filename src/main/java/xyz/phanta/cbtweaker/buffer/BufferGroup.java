package xyz.phanta.cbtweaker.buffer;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import xyz.phanta.cbtweaker.integration.jei.JeiBufferGroup;

import javax.annotation.Nullable;
import java.util.*;

public class BufferGroup {

    private final SortedMap<BufferType<?, ?, ?, ?>, List<?>> bufTable
            = new TreeMap<>(Comparator.comparing(BufferType::getId));

    @SuppressWarnings("unchecked")
    @Nullable
    public <B> List<B> getBuffers(BufferType<B, ?, ?, ?> bufType) {
        List<B> bufs = (List<B>)bufTable.get(bufType);
        return bufs != null ? bufs : Collections.emptyList();
    }

    @SuppressWarnings("unchecked")
    public <B, A> A accumulateIngredients(BufferType<B, A, ?, ?> bufType) {
        A acc = bufType.createAccumulator();
        List<B> bufs = (List<B>)bufTable.get(bufType);
        if (bufs != null) {
            for (B buf : bufs) {
                bufType.accumulate(acc, buf);
            }
        }
        return acc;
    }

    @SuppressWarnings("unchecked")
    public <B> void addBuffer(BufferType<B, ?, ?, ?> bufType, B buffer) {
        ((List<B>)bufTable.computeIfAbsent(bufType, k -> new ArrayList<>())).add(buffer);
    }

    public void forEach(Visitor visitor) {
        for (Map.Entry<BufferType<?, ?, ?, ?>, List<?>> entry : bufTable.entrySet()) {
            visitUnchecked(visitor, entry.getKey(), entry.getValue());
        }
    }

    @SuppressWarnings("unchecked")
    private static <B> void visitUnchecked(Visitor visitor, BufferType<B, ?, ?, ?> bufType, List<?> buffers) {
        visitor.visit(bufType, (List<B>)buffers);
    }

    public interface Visitor {

        <B> void visit(BufferType<B, ?, ?, ?> bufType, List<B> buffers);

    }

    public static class Factory {

        private final Map<BufferType<?, ?, ?, ?>, List<? extends BufferFactory<?, ?>>> factoryTable = new HashMap<>();

        @SuppressWarnings("unchecked")
        public <B> void addFactory(BufferType<B, ?, ?, ?> bufType, BufferFactory<B, ?> factory) {
            ((List<BufferFactory<B, ?>>)factoryTable.computeIfAbsent(bufType, k -> new ArrayList<>())).add(factory);
        }

        public BufferGroup createBufferGroup(World world, BlockPos pos, @Nullable BufferObserver observer) {
            BufferGroup bufGroup = new BufferGroup();
            for (Map.Entry<BufferType<?, ?, ?, ?>, List<? extends BufferFactory<?, ?>>> entry
                    : factoryTable.entrySet()) {
                createBuffersUnchecked(bufGroup, entry.getKey(), entry.getValue(), world, pos, observer);
            }
            return bufGroup;
        }

        @SuppressWarnings("unchecked")
        private static <B> void createBuffersUnchecked(BufferGroup bufGroup, BufferType<B, ?, ?, ?> bufType,
                                                       List<? extends BufferFactory<?, ?>> factories,
                                                       World world, BlockPos pos, @Nullable BufferObserver observer) {
            for (BufferFactory<B, ?> factory : (List<BufferFactory<B, ?>>)factories) {
                bufGroup.addBuffer(bufType, factory.createBuffer(world, pos, observer));
            }
        }

        public JeiBufferGroup createJeiBufferGroup() {
            JeiBufferGroup bufGroup = new JeiBufferGroup();
            for (Map.Entry<BufferType<?, ?, ?, ?>, List<? extends BufferFactory<?, ?>>> entry
                    : factoryTable.entrySet()) {
                createJeiBuffersUnchecked(bufGroup, entry.getKey(), entry.getValue());
            }
            return bufGroup;
        }

        @SuppressWarnings("unchecked")
        private static <JB> void createJeiBuffersUnchecked(JeiBufferGroup bufGroup, BufferType<?, ?, JB, ?> bufType,
                                                           List<? extends BufferFactory<?, ?>> factories) {
            for (BufferFactory<?, JB> factory : (List<BufferFactory<?, JB>>)factories) {
                bufGroup.addBuffer(bufType, factory.createJeiBuffer());
            }
        }

    }

}
