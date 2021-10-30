package xyz.phanta.cbtweaker.integration.jei;

import xyz.phanta.cbtweaker.buffer.BufferType;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class JeiAccumulatorMap {

    private final Map<String, Group> groupTable;

    public JeiAccumulatorMap(Map<String, JeiBufferGroup> bufGroups) {
        this.groupTable = bufGroups.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> new Group(e.getValue())));
    }

    @Nullable
    public Group getGroup(String groupId) {
        return groupTable.get(groupId);
    }

    public static class Group {

        private final Map<BufferType<?, ?, ?, ?>, Object> accumTable = new HashMap<>();

        public Group(JeiBufferGroup bufGroup) {
            bufGroup.forEach(new JeiBufferGroup.Visitor() {
                @Override
                public <JB, JA> void visit(BufferType<?, ?, JB, JA> bufType, List<JB> buffers) {
                    JA accum = bufType.createJeiAccumulator();
                    for (JB buffer : buffers) {
                        bufType.jeiAccumulate(accum, buffer);
                    }
                    accumTable.put(bufType, accum);
                }
            });
        }

        @SuppressWarnings("unchecked")
        @Nullable
        public <JA> JA getAccumulator(BufferType<?, ?, ?, JA> bufType) {
            return (JA)accumTable.get(bufType);
        }

    }

}
