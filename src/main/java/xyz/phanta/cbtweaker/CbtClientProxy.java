package xyz.phanta.cbtweaker;

import io.github.phantamanta44.libnine.util.nullity.Reflected;

@Reflected
public class CbtClientProxy extends CbtCommonProxy {

    @Override
    protected ItemBlockRegistrar createRegistrar() {
        return new ClientItemBlockRegistrar();
    }

}
