package xyz.phanta.cbtweaker.integration.jei;

import xyz.phanta.cbtweaker.buffer.BufferType;

import javax.annotation.Nullable;
import java.util.*;

public class JeiBufferGroup {

    private final Map<BufferType<?, ?, ?, ?>, List<?>> bufTable = new HashMap<>();

    @SuppressWarnings("unchecked")
    @Nullable
    public <JB> List<JB> getBuffers(BufferType<?, ?, JB, ?> bufType) {
        List<JB> bufs = (List<JB>)bufTable.get(bufType);
        return bufs != null ? bufs : Collections.emptyList();
    }

    @SuppressWarnings("unchecked")
    public <JB> void addBuffer(BufferType<?, ?, JB, ?> bufType, JB buffer) {
        ((List<JB>)bufTable.computeIfAbsent(bufType, k -> new ArrayList<>())).add(buffer);
    }

    public void forEach(Visitor visitor) {
        for (Map.Entry<BufferType<?, ?, ?, ?>, List<?>> entry : bufTable.entrySet()) {
            visitUnchecked(visitor, entry.getKey(), entry.getValue());
        }
    }

    @SuppressWarnings("unchecked")
    private static <JB, JA> void visitUnchecked(Visitor visitor, BufferType<?, ?, JB, JA> bufType, List<?> buffers) {
        visitor.visit(bufType, (List<JB>)buffers);
    }

    public interface Visitor {

        <JB, JA> void visit(BufferType<?, ?, JB, JA> bufType, List<JB> buffers);

    }

}
