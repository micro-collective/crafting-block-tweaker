package xyz.phanta.cbtweaker.coremod;

import io.github.phantamanta44.libnine.util.nullity.Reflected;
import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.*;

@Reflected
public class ClassTransformerWorld implements IClassTransformer {

    @Override
    public byte[] transform(String name, String transformedName, byte[] code) {
        if (!transformedName.equals("net.minecraft.world.World")) {
            return code;
        }
        ClassReader reader = new ClassReader(code);
        ClassWriter writer = new ClassWriter(reader, 0);
        reader.accept(new TransformClassWorld(Opcodes.ASM5, writer), 0);
        return writer.toByteArray();
    }

    private static class TransformClassWorld extends ClassVisitor {

        public TransformClassWorld(int api, ClassVisitor cv) {
            super(api, cv);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
            if ((name.equals("func_180501_a") || name.equals("setBlockState"))
                    && desc.equals("(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/state/IBlockState;I)Z")) {
                return new TransformMethodSetBlockState(
                        api, super.visitMethod(access, name, desc, signature, exceptions));
            }
            return super.visitMethod(access, name, desc, signature, exceptions);
        }

    }

    private static class TransformMethodSetBlockState extends MethodVisitor {

        private boolean seenGetBlockState = false;
        private int oldStateVar = -1;

        public TransformMethodSetBlockState(int api, MethodVisitor mv) {
            super(api, mv);
        }

        @Override
        public void visitVarInsn(int opcode, int var) {
            super.visitVarInsn(opcode, var);
            if (seenGetBlockState && opcode == Opcodes.ASTORE) {
                oldStateVar = var;
                seenGetBlockState = false;
            }
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
            super.visitMethodInsn(opcode, owner, name, desc, itf);
            if (!seenGetBlockState && oldStateVar == -1) {
                if (opcode == Opcodes.INVOKEVIRTUAL && owner.equals("net/minecraft/world/World")
                        && (name.equals("func_180495_p") || name.equals("getBlockState"))
                        && desc.equals("(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/block/state/IBlockState;")) {
                    seenGetBlockState = true;
                }
            } else if (oldStateVar != -1 && opcode == Opcodes.INVOKEVIRTUAL && owner.equals("net/minecraft/world/World")
                    && name.equals("markAndNotifyBlock")
                    && desc.equals("(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/world/chunk/Chunk;Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/block/state/IBlockState;I)V")) {
                super.visitVarInsn(Opcodes.ALOAD, 0);
                super.visitVarInsn(Opcodes.ALOAD, 1);
                super.visitVarInsn(Opcodes.ALOAD, oldStateVar);
                super.visitVarInsn(Opcodes.ALOAD, 2);
                super.visitMethodInsn(Opcodes.INVOKESTATIC, "xyz/phanta/cbtweaker/coremod/CbtCoreHooks", "onBlockStateChanged",
                        "(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/block/state/IBlockState;)V",
                        false);
            }
        }

    }

}
