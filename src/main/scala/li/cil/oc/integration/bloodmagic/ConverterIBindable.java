package li.cil.oc.integration.bloodmagic;

import WayofTime.alchemicalWizardry.api.items.interfaces.IBindable;
import WayofTime.alchemicalWizardry.common.items.EnergyItems;
import li.cil.oc.api.driver.Converter;
import net.minecraft.item.ItemStack;

import java.util.Map;

public class ConverterIBindable implements Converter {
    @Override
    public void convert(final Object value, final Map<Object, Object> output) {
        if (value instanceof ItemStack) {
            final ItemStack stack = (ItemStack) value;

            if (stack.getItem() instanceof IBindable) {
                output.put("ownerName", EnergyItems.getOwnerName(stack));
            }
        }
    }
}
