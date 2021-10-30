package xyz.phanta.cbtweaker.structure.block;

import xyz.phanta.cbtweaker.hatch.HatchTileEntity;

import javax.annotation.Nullable;

public abstract class StructureBlockMatch {

    public static StructureBlockMatch maybeComponent(@Nullable String compId) {
        return compId != null ? new Component(compId) : Normal.INSTANCE;
    }

    private StructureBlockMatch() {
        // NO-OP
    }

    public static class Normal extends StructureBlockMatch {

        public static final Normal INSTANCE = new Normal();

        private Normal() {
            // NO-OP
        }

    }

    public static class Hatch extends StructureBlockMatch {

        private final String groupId;
        private final HatchTileEntity hatch;

        public Hatch(String groupId, HatchTileEntity hatch) {
            this.groupId = groupId;
            this.hatch = hatch;
        }

        public String getGroupId() {
            return groupId;
        }

        public HatchTileEntity getHatch() {
            return hatch;
        }

    }

    public static class Component extends StructureBlockMatch {

        private final String compId;

        public Component(String compId) {
            this.compId = compId;
        }

        public String getComponentId() {
            return compId;
        }

    }

}
