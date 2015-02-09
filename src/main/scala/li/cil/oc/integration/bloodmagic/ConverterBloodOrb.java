package li.cil.oc.integration.bloodmagic;

import WayofTime.alchemicalWizardry.api.items.interfaces.IBloodOrb;
import li.cil.oc.api.driver.Converter;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.Map;

public class ConverterBloodOrb implements Converter {
    @Override
    public void convert(final Object value, final Map<Object, Object> output) {
        if (value instanceof ItemStack) {
            final Item item = ((ItemStack) value).getItem();

            if (item instanceof IBloodOrb) {
                final IBloodOrb bloodOrb = (IBloodOrb) item;

                output.put("maxEssence", bloodOrb.getMaxEssence());
                output.put("orbTier", bloodOrb.getOrbLevel());
            }
        }
    }
}
