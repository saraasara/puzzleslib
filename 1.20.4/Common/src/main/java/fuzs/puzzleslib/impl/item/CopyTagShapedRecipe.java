package fuzs.puzzleslib.impl.item;

import net.minecraft.core.RegistryAccess;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;

public class CopyTagShapedRecipe extends ShapedRecipe implements CopyTagRecipe {
    private final RecipeSerializer<?> recipeSerializer;
    private final Ingredient copyFrom;

    public CopyTagShapedRecipe(String modId, ShapedRecipe shapedRecipe, Ingredient copyFrom) {
        this(CopyTagRecipe.getModSerializer(modId, CopyTagRecipe.SHAPED_RECIPE_SERIALIZER_ID), shapedRecipe, copyFrom);
    }

    public CopyTagShapedRecipe(RecipeSerializer<?> recipeSerializer, ShapedRecipe shapedRecipe, Ingredient copyFrom) {
        super(shapedRecipe.getGroup(), shapedRecipe.category(), shapedRecipe.pattern, shapedRecipe.getResultItem(RegistryAccess.EMPTY), shapedRecipe.showNotification());
        this.recipeSerializer = recipeSerializer;
        this.copyFrom = copyFrom;
    }

    @Override
    public ItemStack assemble(CraftingContainer craftingContainer, RegistryAccess registryAccess) {
        ItemStack itemStack = super.assemble(craftingContainer, registryAccess);
        CopyTagRecipe.super.tryCopyTagToResult(itemStack, craftingContainer);
        return itemStack;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return this.recipeSerializer;
    }

    @Override
    public Ingredient getCopyTagSource() {
        return this.copyFrom;
    }
}
