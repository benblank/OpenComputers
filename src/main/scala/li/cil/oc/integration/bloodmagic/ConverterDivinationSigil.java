package li.cil.oc.integration.bloodmagic;

import WayofTime.alchemicalWizardry.api.soulNetwork.SoulNetworkHandler;
import WayofTime.alchemicalWizardry.common.items.EnergyItems;
import WayofTime.alchemicalWizardry.common.items.sigil.DivinationSigil;
import li.cil.oc.api.driver.Converter;
import net.minecraft.item.ItemStack;

import java.util.Map;

public class ConverterDivinationSigil implements Converter {
    @Override
    public void convert(final Object value, final Map<Object, Object> output) {
        if (value instanceof ItemStack) {
            final ItemStack stack = (ItemStack) value;

            if (stack.getItem() instanceof DivinationSigil) {
                final String ownerName = EnergyItems.getOwnerName(stack);
                final int networkEssence = SoulNetworkHandler.getCurrentEssence(ownerName);

                final int maxOrbTier = SoulNetworkHandler.getCurrentMaxOrb(ownerName);
                final int maxNetworkEssence = SoulNetworkHandler.getMaximumForOrbTier(maxOrbTier);
                final double networkFilledRatio = (double) networkEssence / maxNetworkEssence;

                output.put("networkEssence", networkEssence);
                output.put("networkFilledRatio", networkFilledRatio);
            }
        }
    }
}
