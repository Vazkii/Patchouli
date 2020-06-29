package vazkii.patchouli.client.book.template.component;

import com.google.gson.annotations.SerializedName;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;

import vazkii.patchouli.api.IVariable;
import vazkii.patchouli.client.book.BookEntry;
import vazkii.patchouli.client.book.BookPage;
import vazkii.patchouli.client.book.template.TemplateComponent;
import vazkii.patchouli.common.util.ItemStackUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

public class ComponentItemStack extends TemplateComponent {

	public IVariable item;

	private boolean framed;
	@SerializedName("link_recipe") private boolean linkedRecipe;

	private transient List<ItemStack> items;

	@Override
	public void build(BookPage page, BookEntry entry, int pageNum) {
		if (linkedRecipe) {
			for (ItemStack stack : items) {
				entry.addRelevantStack(stack, pageNum);
			}
		}
	}

	@Override
	public void onVariablesAvailable(UnaryOperator<IVariable> lookup) {
		super.onVariablesAvailable(lookup);
		items = lookup.apply(item).asStreamOrSingleton().map(x -> x.as(ItemStack.class)).collect(Collectors.toList());
	}

	@Override
	public void render(MatrixStack ms, BookPage page, int mouseX, int mouseY, float pticks) {
		if (items.isEmpty()) {
			return;
		}

		if (framed) {
			RenderSystem.enableBlend();
			RenderSystem.color4f(1F, 1F, 1F, 1F);
			page.mc.getTextureManager().bindTexture(page.book.craftingTexture);
			DrawableHelper.drawTexture(ms, x - 4, y - 4, 83, 71, 24, 24, 128, 128);
		}

		page.parent.renderItemStack(ms, x, y, mouseX, mouseY, items.get((page.parent.ticksInBook / 20) % items.size()));
	}

}
