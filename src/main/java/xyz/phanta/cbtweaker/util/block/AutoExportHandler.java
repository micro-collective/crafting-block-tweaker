package xyz.phanta.cbtweaker.util.block;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import xyz.phanta.cbtweaker.buffer.BufferType;
import xyz.phanta.cbtweaker.util.TickModulator;

import java.util.Set;
import java.util.function.Supplier;

public interface AutoExportHandler {

    boolean isAutoExportSupported();

    boolean isAutoExporting();

    void setAutoExporting(boolean exporting);

    void tick();

    void writeToNbt(NBTTagCompound tag, String key);

    void readFromNbt(NBTTagCompound tag, String key);

    class Noop implements AutoExportHandler {

        public static final Noop INSTANCE = new Noop();

        private Noop() {
            // NO-OP
        }

        @Override
        public boolean isAutoExportSupported() {
            return false;
        }

        @Override
        public boolean isAutoExporting() {
            return false;
        }

        @Override
        public void setAutoExporting(boolean exporting) {
            // NO-OP
        }

        @Override
        public void tick() {
            // NO-OP
        }

        @Override
        public void writeToNbt(NBTTagCompound tag, String key) {
            // NO-OP
        }

        @Override
        public void readFromNbt(NBTTagCompound tag, String key) {
            // NO-OP
        }

    }

    class Impl<B> implements AutoExportHandler {

        private final BufferType<B, ?, ?, ?> bufType;
        private final B buffer;
        private final Runnable changeCallback;
        private final Supplier<Set<EnumFacing>> faceGetter;

        private final TickModulator ticker = new TickModulator();
        private boolean exporting;

        public Impl(BufferType<B, ?, ?, ?> bufType, B buffer,
                    Supplier<Set<EnumFacing>> faceGetter, Runnable changeCallback, boolean exporting) {
            this.bufType = bufType;
            this.buffer = buffer;
            this.faceGetter = faceGetter;
            this.changeCallback = changeCallback;
            if (exporting) {
                this.exporting = true;
                ticker.setInterval(1);
            } else {
                this.exporting = false;
            }
        }

        @Override
        public boolean isAutoExportSupported() {
            return true;
        }

        @Override
        public boolean isAutoExporting() {
            return exporting;
        }

        @Override
        public void setAutoExporting(boolean exporting) {
            if (exporting) {
                if (!this.exporting) {
                    this.exporting = true;
                    ticker.setInterval(1);
                    changeCallback.run();
                }
            } else if (this.exporting) {
                this.exporting = false;
                ticker.sleep();
                changeCallback.run();
            }
        }

        @Override
        public void tick() {
            if (exporting && ticker.tick()) {
                bufType.doExport(buffer, faceGetter.get(), ticker);
            }
        }

        @Override
        public void writeToNbt(NBTTagCompound tag, String key) {
            tag.setBoolean(key, exporting);
        }

        @Override
        public void readFromNbt(NBTTagCompound tag, String key) {
            setAutoExporting(tag.getBoolean(key));
        }

    }

}
