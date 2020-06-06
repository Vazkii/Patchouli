package vazkii.patchouli.client.book.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.text.StringTextComponent;

import vazkii.patchouli.client.book.BookCategory;
import vazkii.patchouli.client.book.BookEntry;
import vazkii.patchouli.client.book.gui.button.GuiButtonCategory;
import vazkii.patchouli.common.base.PatchouliConfig;
import vazkii.patchouli.common.book.Book;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class GuiBookCategory extends GuiBookEntryList {

	BookCategory category;

	public GuiBookCategory(Book book, BookCategory category) {
		super(book, new StringTextComponent(category.getName()));
		this.category = category;
	}

	@Override
	protected String getDescriptionText() {
		return category.getDescription();
	}

	@Override
	protected Collection<BookEntry> getEntries() {
		return category.getEntries();
	}

	@Override
	protected void addSubcategoryButtons() {
		int i = 0;
		List<BookCategory> categories = new ArrayList<>(book.contents.categories.values());
		Collections.sort(categories);

		for (BookCategory ocategory : categories) {
			if (ocategory.getParentCategory() != category || ocategory.shouldHide()) {
				continue;
			}

			int x = LEFT_PAGE_X + 10 + (i % 4) * 24;
			int y = TOP_PADDING + PAGE_HEIGHT - (!book.advancementsEnabled() ? 46 : 68);

			Button button = new GuiButtonCategory(this, x, y, ocategory, this::handleButtonCategory);
			addButton(button);
			dependentButtons.add(button);

			i++;
		}
	}

	@Override
	protected boolean doesEntryCountForProgress(BookEntry entry) {
		return entry.getCategory() == category;
	}

	@Override
	public boolean equals(Object obj) {
		return obj == this || (obj instanceof GuiBookCategory && ((GuiBookCategory) obj).category == category && ((GuiBookCategory) obj).spread == spread);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(category) * 31 + Objects.hashCode(spread);
	}

	@Override
	public boolean canBeOpened() {
		return !category.isLocked() && !equals(Minecraft.getInstance().currentScreen);
	}

}
