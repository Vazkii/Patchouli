package vazkii.patchouli.client.book.gui.button;

import com.mojang.blaze3d.matrix.MatrixStack;

import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;

import vazkii.patchouli.client.base.PersistentData;
import vazkii.patchouli.client.book.BookCategory;
import vazkii.patchouli.client.book.BookEntry;
import vazkii.patchouli.client.book.EntryDisplayState;
import vazkii.patchouli.client.book.gui.GuiBook;
import vazkii.patchouli.common.book.Book;

public class GuiButtonBookMarkRead extends GuiButtonBook {

	private final Book book;

	public GuiButtonBookMarkRead(GuiBook parent, int x, int y) {
		super(parent, x, y, 308, 31, 11, 11, Button::onPress, getTooltip(parent.book));
		this.book = parent.book;
	}

	@Override
	public void renderButton(MatrixStack ms, int mouseX, int mouseY, float partialTicks) {
		int px = x + 1;
		int py = (int) (y + 0.5);
		GuiBook.drawFromTexture(ms, book, x, y, 285, 160, 13, 10);
		GuiBook.drawFromTexture(ms, book, px, py, u, v, width, height);
		if (isHovered()) {
			GuiBook.drawFromTexture(ms, book, px, py, u + 11, v, width, height);
			parent.setTooltip(getTooltip());
		}
		parent.getMinecraft().fontRenderer.drawStringWithShadow(ms, "+", px - 0.5F, py - 0.2F, 0x00FF01);
	}

	@Override
	public void onPress() {
		for (BookEntry entry : this.book.contents.entries.values()) {
			if (isMainPage(this.book)) {
				markEntry(entry);
			} else {
				markCategoryAsRead(entry, entry.getCategory());
			}
		}
	}

	private void markCategoryAsRead(BookEntry entry, BookCategory category) {
		if (category.getName().equals(this.book.contents.getCurrentGui().getTitle())) {
			markEntry(entry);
		} else if (!category.isRootCategory()) {
			markCategoryAsRead(entry, entry.getCategory().getParentCategory());
		}
	}

	private void markEntry(BookEntry entry) {
		boolean dirty = false;
		String key = entry.getId().toString();

		if (!entry.isLocked() && entry.getReadState().equals(EntryDisplayState.UNREAD)) {
			PersistentData.DataHolder.BookData data = PersistentData.data.getBookData(book);

			if (!data.viewedEntries.contains(key)) {
				data.viewedEntries.add(key);
				dirty = true;
				entry.markReadStateDirty();
			}
		}

		if (dirty) {
			PersistentData.save();
		}
	}

	private static ITextComponent getTooltip(Book book) {
		String text = isMainPage(book) ? "patchouli.gui.lexicon.button.mark_all_read" : "patchouli.gui.lexicon.button.mark_category_read";
		return new TranslationTextComponent(text);
	}

	private static boolean isMainPage(Book book) {
		return !book.contents.currentGui.canSeeBackButton();
	}
}
