package jei;

import com.xeno.goo.GooMod;
import com.xeno.goo.setup.Registry;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IAdvancedRegistration;
import mezz.jei.api.registration.IModIngredientRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.fluid.Fluid;
import net.minecraft.util.ResourceLocation;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@JeiPlugin
public class GooJeiPlugin implements IModPlugin {

	private static final ResourceLocation pluginId = new ResourceLocation(GooMod.MOD_ID, "jei_plugin");
	@Override
	public ResourceLocation getPluginUid() {
		return pluginId;
	}

	@Override
	public void registerCategories(IRecipeCategoryRegistration registration) {
		registration.addRecipeCategories(
				new SolidifierRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
				new GooifierRecipeCategory(registration.getJeiHelpers().getGuiHelper())
		);
	}

	@Override
	public void registerAdvanced(IAdvancedRegistration registration) {
		registration.addRecipeManagerPlugin(GooRecipeManager.instance);
	}

	@Override
	public void registerIngredients(IModIngredientRegistration registration) {

		List<GooIngredient> fluids = Registry.FluidSuppliers.values().stream().map((e) ->
			new GooIngredient(e.get().getRegistryName())
		).collect(Collectors.toList());
		registration.register(GooIngredient.GOO, fluids, new GooIngredientHelper(), new GooIngredientRenderer());
	}
}
