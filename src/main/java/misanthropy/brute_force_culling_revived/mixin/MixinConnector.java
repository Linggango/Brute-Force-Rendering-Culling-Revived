package misanthropy.brute_force_culling_revived.mixin;

import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.mixin.connect.IMixinConnector;

public class MixinConnector implements IMixinConnector {

    @Override
    public void connect() {
        Mixins.addConfigurations("mixins.bfrc.json");
    }
}
