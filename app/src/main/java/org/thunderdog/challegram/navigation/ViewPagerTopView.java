/*
 * This file is a part of Telegram X
 * Copyright © 2014-2022 (tgx-android@pm.me)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * File created on 26/12/2016
 */
package org.thunderdog.challegram.navigation;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.text.Layout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import androidx.annotation.Dimension;
import androidx.annotation.DrawableRes;
import androidx.annotation.FloatRange;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;

import org.thunderdog.challegram.R;
import org.thunderdog.challegram.U;
import org.thunderdog.challegram.component.sticker.TGStickerObj;
import org.thunderdog.challegram.core.Lang;
import org.thunderdog.challegram.data.TGReaction;
import org.thunderdog.challegram.loader.ComplexReceiver;
import org.thunderdog.challegram.loader.ImageReceiver;
import org.thunderdog.challegram.support.RippleSupport;
import org.thunderdog.challegram.telegram.TGLegacyManager;
import org.thunderdog.challegram.theme.Theme;
import org.thunderdog.challegram.theme.ThemeColorId;
import org.thunderdog.challegram.tool.Drawables;
import org.thunderdog.challegram.tool.Paints;
import org.thunderdog.challegram.tool.Screen;
import org.thunderdog.challegram.tool.Views;
import org.thunderdog.challegram.util.DrawableProvider;
import org.thunderdog.challegram.util.text.Counter;
import org.thunderdog.challegram.util.text.Text;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;

import me.vkryl.android.widget.FrameLayoutFix;
import me.vkryl.core.ColorUtils;
import me.vkryl.core.MathUtils;
import me.vkryl.core.StringUtils;
import me.vkryl.core.lambda.Destroyable;

public class ViewPagerTopView extends FrameLayoutFix implements RtlCheckListener, View.OnClickListener, View.OnLongClickListener, Destroyable, TGLegacyManager.EmojiLoadListener {
  public static final @Dimension(unit = Dimension.DP) float SELECTION_HEIGHT = 2f;

  public static class Item {
    public final CharSequence string;
    public final boolean needFakeBold;
    public final @DrawableRes int iconRes;
    public ImageReceiver imageReceiver;
    public int imageReceiverSize = 0;
    public float imageReceiverScale = 0f;
    public TGReaction reaction;
    public final Counter counter;
    public final DrawableProvider provider;
    public final boolean hidden;

    public Item (CharSequence string) {
      this(string, 0, null, null, false);
    }

    public Item (@DrawableRes int iconRes) {
      this(null, iconRes, null, null, false);
    }

    public Item (@DrawableRes int iconRes, Counter counter) {
      this(null, iconRes, counter, null, false);
    }

    public Item (CharSequence string, Counter counter) {
      this(string, 0, counter, null, false);
    }

    public Item (CharSequence string, @DrawableRes int iconRes, Counter counter) {
      this(string, iconRes, counter, null, false);
    }

    public Item (Counter counter, DrawableProvider provider, int addWidth) {
      this(null, 0, counter, provider, false);
      this.addWidth = addWidth;
    }

    public Item (TGReaction reaction, Counter counter, DrawableProvider provider, int addWidth) {
      this(null, 0, counter, provider, false);
      this.addWidth = addWidth;
      this.reaction = reaction;
    }

    public Item () {
      this(null, 0, null, null, true);
    }

    private Item (CharSequence string, @DrawableRes int iconRes, Counter counter, DrawableProvider provider, boolean hidden) {
      this.string = string;
      this.needFakeBold = string != null && Text.needFakeBold(string);
      this.iconRes = iconRes;
      this.counter = counter;
      this.provider = provider;
      this.hidden = hidden;
    }

    private Drawable icon;

    public Drawable getIcon () {
      if (icon == null && iconRes != 0)
        icon = Drawables.get(iconRes);
      return icon;
    }

    @Override
    public boolean equals (Object obj) {
      return obj instanceof Item && ((Item) obj).iconRes == iconRes && StringUtils.equalsOrBothEmpty(((Item) obj).string, string) && (((Item) obj).counter == counter);
    }

    private int width;
    private int addWidth = 0;
    private int staticWidth = -1;
    private int translationX = 0;

    public void setStaticWidth (int staticWidth) {
      this.staticWidth = staticWidth;
    }

    public int calculateWidth (TextPaint paint) {
      final int width;
      if (staticWidth != -1) {
        width = staticWidth;
      } else if (counter != null) {
        if (string != null) {
          width = (int) (U.measureEmojiText(string, paint) + counter.getScaledWidth(Screen.dp(6f))) + (iconRes != 0 ? Screen.dp(24f) + Screen.dp(6f) : 0);
        } else if (imageReceiver != null) {
          width = (int) counter.getWidth() + imageReceiverSize;
        } else if (iconRes != 0) {
          width = Screen.dp(24f) + (int) counter.getScaledWidth(Screen.dp(6f));
        } else {
          width = (int) counter.getWidth() + Screen.dp(6f);
        }
      } else if (string != null) {
        width = (int) U.measureEmojiText(string, paint) + (iconRes != 0 ? Screen.dp(24f) + Screen.dp(6f) : 0);
      } else if (iconRes != 0) {
        width = Screen.dp(24f)/* + Screen.dp(6f)*/; // ???
      } else {
        width = 0;
      }
      this.width = width + addWidth;
      return this.width;
    }

    public void setTranslationX (int translationX) {
      this.translationX = translationX;
    }

    private Layout ellipsizedStringLayout;
    private int actualWidth;

    public void trimString (int availWidth, TextPaint paint) {
      if (string != null) {
        CharSequence ellipsizedString = TextUtils.ellipsize(string, paint, availWidth, TextUtils.TruncateAt.END);
        ellipsizedStringLayout = U.createLayout(ellipsizedString, availWidth, paint);
        actualWidth = ellipsizedStringLayout.getWidth();
      } else {
        ellipsizedStringLayout = null;
        actualWidth = width;
      }
    }

    public void untrimString (TextPaint paint) {
      if (string != null) {
        ellipsizedStringLayout = U.createLayout(string, (int) Math.ceil(U.measureEmojiText(string, paint)), paint);
      } else {
        ellipsizedStringLayout = null;
      }
      actualWidth = width;
    }
  }
  private List<Item> items;
  private int maxItemWidth;

  private int textPadding;
  private final ComplexReceiver complexReceiver;
  private CounterAlphaProvider counterAlphaProvider = DEFAULT_COUNTER_ALPHA_PROVIDER;

  private @ThemeColorId int fromTextColorId, toTextColorId = R.id.theme_color_headerText;
  private @ThemeColorId int selectionColorId;

  public ViewPagerTopView (Context context) {
    super(context);
    this.textPadding = Screen.dp(19f);
    this.complexReceiver = new ComplexReceiver(this);
    setWillNotDraw(false);
    TGLegacyManager.instance().addEmojiListener(this);
  }

  @Override
  public void checkRtl () {
    int count = getChildCount();
    for (int i = 0; i < count; i++) {
      View view = getChildAt(i);
      if (view != null) {
        view.requestLayout();
      }
    }
  }

  public void setTextPadding (int textPadding) {
    this.textPadding = textPadding;
  }

  private boolean fitsParentWidth;

  public void setFitsParentWidth (boolean fits) {
    this.fitsParentWidth = fits;
  }

  private boolean drawSelectionAtTop;

  public void setDrawSelectionAtTop (boolean drawSelectionAtTop) {
    this.drawSelectionAtTop = drawSelectionAtTop;
    invalidate();
  }

  public boolean isDrawSelectionAtTop () {
    return drawSelectionAtTop;
  }

  private OnItemClickListener listener;

  public void setOnItemClickListener (OnItemClickListener listener) {
    this.listener = listener;
  }

  public OnItemClickListener getOnItemClickListener () {
    return listener;
  }

  @Override
  public void onClick (View v) {
    if (listener != null && v instanceof BackgroundView) {
      int i = ((BackgroundView) v).index;
      listener.onPagerItemClick(i);
    }
  }

  private boolean isDark;

  public void setUseDarkBackground () {
    isDark = true;
  }

  @Retention(RetentionPolicy.SOURCE)
  @IntDef({SLIDE_OFF_DIRECTION_TOP, SLIDE_OFF_DIRECTION_BOTTOM})
  public @interface SlideOffDirection { }

  public static final int SLIDE_OFF_DIRECTION_TOP = -1;
  public static final int SLIDE_OFF_DIRECTION_BOTTOM = 1;

  private @SlideOffDirection int slideOffDirection = SLIDE_OFF_DIRECTION_BOTTOM;

  public void setSlideOffDirection (@SlideOffDirection int slideOffDirection) {
    this.slideOffDirection = slideOffDirection;
  }

  private BackgroundView newBackgroundView (int i) {
    BackgroundView backgroundView = new BackgroundView(getContext());
    if (isDark) {
      RippleSupport.setTransparentBlackSelector(backgroundView);
    } else {
      RippleSupport.setTransparentWhiteSelector(backgroundView);
    }
    backgroundView.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT));
    backgroundView.setOnClickListener(this);
    backgroundView.setOnLongClickListener(this);
    backgroundView.setBoundView(this);
    backgroundView.setIndex(i);
    return backgroundView;
  }

  @Override
  public boolean onLongClick (View v) {
    if (listener != null && v instanceof BackgroundView) {
      int i = ((BackgroundView) v).index;
      return listener.onPagerItemLongClick(i);
    }
    return false;
  }

  private int totalWidth;

  public void setItems (String[] stringItems) {
    List<Item> items = new ArrayList<>(stringItems.length);
    for (String stringItem : stringItems) {
      items.add(new Item(stringItem));
    }
    setItems(items);
  }

  public void setItems (int[] iconItems) {
    List<Item> items = new ArrayList<>(iconItems.length);
    for (int iconItem : iconItems) {
      items.add(new Item(iconItem));
    }
    setItems(items);
  }

  public void setItemAt (int index, String text) {
    setItemAt(index, new Item(text));
  }

  public void setItemAt (int index, Item item) {
    Item oldItem = this.items.get(index);
    this.items.set(index, item);
    onUpdateItems();
    totalWidth -= oldItem.width + textPadding * 2;

    int textColor = Theme.headerTextColor();
    TextPaint paint = Paints.getViewPagerTextPaint(textColor, item.needFakeBold);
    item.calculateWidth(paint);
    totalWidth += item.width + textPadding * 2;
    maxItemWidth = totalWidth / items.size();

    this.lastMeasuredWidth = 0;
    requestLayout();
    invalidate();
  }

  public void setItemTranslationX (int index, int x) {
    if (index < getItemsCount()) {
      this.items.get(index).setTranslationX(x);
      invalidate();
    }
  }

  public int getItemsCount () {
    return items.size();
  }

  public void setItems (@NonNull List<Item> items) {
    if (this.items != null && this.items.size() == items.size()) {
      boolean foundDiff = false;
      int i = 0;
      for (Item item : items) {
        if (!item.equals(this.items.get(i++))) {
          foundDiff = true;
          break;
        }
      }
      if (!foundDiff) {
        return;
      }
    }
    removeAllViews();
    this.items = items;
    onUpdateItems();

    this.totalWidth = 0;
    this.lastMeasuredWidth = 0;
    int i = 0;
    int textColor = Theme.headerTextColor();
    for (Item item : items) {
      TextPaint paint = Paints.getViewPagerTextPaint(textColor, item.needFakeBold);
      item.calculateWidth(paint);
      totalWidth += item.width + textPadding * 2;
      addView(newBackgroundView(i));
      i++;
    }
    maxItemWidth = items.isEmpty() ? 0 : totalWidth / items.size();
  }

  public void addItem (String item) {
    addItemAtIndex(new Item(item), -1);
  }

  public void addItemAtIndex (String item, int index) {
    addItemAtIndex(new Item(item), index);
  }

  public void addItem (int item) {
    addItemAtIndex(new Item(item), -1);
  }

  public void addItemAtIndex (int item, int index) {
    addItemAtIndex(new Item(item), index);
  }

  public void addItemAtIndex (Item item, int index) {
    if (index == -1) {
      index = items.size();
    }
    boolean append = index == items.size();
    if (append) {
      items.add(item);
    } else {
      items.add(index, item);
    }

    onUpdateItems();
    int textColor = Theme.headerTextColor();
    TextPaint paint = Paints.getViewPagerTextPaint(textColor, item.needFakeBold);

    item.calculateWidth(paint);
    int width = item.width;
    totalWidth += width + textPadding * 2;
    maxItemWidth = totalWidth / items.size();

    commonItemWidth = calculateCommonItemWidth(width);

    if (index <= (int) selectionFactor) {
      selectionFactor++;
    }

    final int availTextWidth = commonItemWidth - textPadding * 2;
    if (!shouldWrapContent() && width < availTextWidth) {
      item.trimString(availTextWidth, paint);
    } else {
      item.untrimString(paint);
    }
    addView(newBackgroundView(items.size() - 1));
    invalidate();
  }

  public void removeLastItem () {
    if (!items.isEmpty()) {
      removeItemAt(items.size() - 1);
    }
  }

  public void removeItemAt (int index) {
    if (index < 0 || index >= items.size()) {
      throw new IllegalArgumentException(index + " is out of range 0.." + items.size());
    }

    items.remove(index);
    onUpdateItems();

    if ((int) selectionFactor >= items.size()) {
      selectionFactor--;
    }

    removeViewAt(index);
    invalidate();
  }

  private boolean shouldWrapContent () {
    return getLayoutParams().width == ViewGroup.LayoutParams.WRAP_CONTENT;
  }

  private int getTotalWidth () {
    int sum = 0;
    for (Item item : items) {
      sum += item.width;
    }
    return sum;
  }

  private void onUpdateItems () {
    for (Item item : items) {
      if (item.reaction != null) {
        TGReaction reaction = item.reaction;
        TGStickerObj stickerObj = reaction.centerAnimationSicker();
        item.imageReceiver = complexReceiver.getImageReceiver(reaction.getId());
        item.imageReceiver.requestFile(stickerObj.getImage());
        item.imageReceiverScale = stickerObj.getDisplayScale();
        item.imageReceiverSize = Screen.dp(34);
      }
    }
  }

  public void requestItemLayoutAt (int index) {
    if (index >= 0 && index < items.size()) {
      setItemAt(index, items.get(index));
    }
  }

  @Override
  protected void onMeasure (int widthMeasureSpec, int heightMeasureSpec) {
    if (shouldWrapContent()) {
      int totalWidth = textPadding * 2 * items.size() + getTotalWidth();
      super.onMeasure(MeasureSpec.makeMeasureSpec(totalWidth, MeasureSpec.EXACTLY), heightMeasureSpec);
      layout(totalWidth, true);
    } else {
      super.onMeasure(widthMeasureSpec, heightMeasureSpec);
      layout(getMeasuredWidth(), false);
    }
  }

  private int lastMeasuredWidth;
  private int commonItemWidth;

  private int calculateCommonItemWidth (int parentWidth) {
    int itemWidth = Math.min(parentWidth / items.size(), maxItemWidth);
    if (parentWidth - itemWidth * items.size() < itemWidth / 2) {
      itemWidth = parentWidth / items.size();
    } else if (fitsParentWidth) {
      itemWidth = Math.max(itemWidth, Math.min((int) ((float) itemWidth * 2f), parentWidth / items.size()));
    }
    return itemWidth;
  }

  private void layout (int width, boolean wrapContent) {
    if (width == 0 || lastMeasuredWidth == width || items == null) {
      return;
    }

    boolean relayout = lastMeasuredWidth != 0;

    lastMeasuredWidth = width;
    commonItemWidth = calculateCommonItemWidth(width);
    int textColor = Theme.headerTextColor();

    final int availTextWidth = commonItemWidth - textPadding * 2;

    for (Item item : items) {
      TextPaint textPaint = Paints.getViewPagerTextPaint(textColor, item.needFakeBold);
      if (!wrapContent && item.width < availTextWidth) {
        item.trimString(availTextWidth, textPaint);
      } else {
        item.untrimString(textPaint);
      }
    }

    if (relayout) {
      postDelayed(() -> {
        recalculateSelection(selectionFactor, true);
        invalidate();
      }, 10);
    } else {
      recalculateSelection(selectionFactor, true);
    }
  }

  private float selectionFactor;

  private void recalculateSelection (float selectionFactor, boolean set) {
    if (items == null || items.isEmpty()) {
      return;
    }
    int selectionWidth, selectionLeft;
    if (shouldWrapContent()) {
      float remainFactor = selectionFactor - (float) ((int) selectionFactor);
      if (remainFactor == 0f) {
        int selectionIndex = Math.max(0, Math.min(items.size() - 1, (int) selectionFactor));
        selectionWidth = items.get(selectionIndex).actualWidth + textPadding * 2;
      } else {
        int fromWidth = items.get((int) selectionFactor).actualWidth + textPadding * 2;
        int toWidth = items.get((int) selectionFactor + 1).actualWidth + textPadding * 2;
        selectionWidth = fromWidth + (int) ((float) (toWidth - fromWidth) * remainFactor);
      }
      selectionLeft = 0;
      for (int i = 0; i < (int) selectionFactor; i++) {
        selectionLeft += items.get(i).actualWidth + textPadding * 2;
      }
      if (remainFactor != 0f) {
        selectionLeft += (int) ((float) (items.get((int) selectionFactor).actualWidth + textPadding * 2) * remainFactor);
      }
    } else {
      selectionLeft = (int) (selectionFactor * (float) commonItemWidth);
      selectionWidth = commonItemWidth;
    }

    boolean callListener;
    if (set) {
      if (this.selectionLeft != selectionLeft || this.selectionWidth != selectionWidth) {
        this.selectionLeft = selectionLeft;
        this.selectionWidth = selectionWidth;
      }
      callListener = (fromIndex == -1 && toIndex == -1) || (fromIndex != -1 && toIndex != -1 && Math.abs(toIndex - fromIndex) == 1);
    } else {
      callListener = fromIndex != -1 && toIndex != -1 && Math.abs(toIndex - fromIndex) > 1;
    }
    float totalFactor = items.size() > 1 ? selectionFactor / (float) (items.size() - 1) : 0;
    if (callListener && selectionChangeListener != null && (lastCallSelectionLeft != selectionLeft || lastCallSelectionWidth != selectionWidth || lastCallSelectionFactor != totalFactor)) {
      selectionChangeListener.onSelectionChanged(lastCallSelectionLeft = selectionLeft, lastCallSelectionWidth = selectionWidth, items.get(0).actualWidth, items.get(items.size() - 1).actualWidth, lastCallSelectionFactor = totalFactor, !set);
    }
  }

  /*public void resendSectionChangeEvent (boolean animated) {
    if (items != null && !items.isEmpty()) {
      selectionChangeListener.onSelectionChanged(lastCallSelectionLeft, lastCallSelectionWidth, items.get(0).actualWidth, items.get(items.size() - 1).actualWidth, lastCallSelectionFactor, animated);
    }
  }*/

  public interface SelectionChangeListener {
    void onSelectionChanged (int selectionLeft, int selectionRight, int firstItemWidth, int lastItemWidth, float totalFactor, boolean animated);
  }

  private SelectionChangeListener selectionChangeListener;

  public ViewPagerTopView setSelectionChangeListener (SelectionChangeListener selectionChangeListener) {
    this.selectionChangeListener = selectionChangeListener;
    return this;
  }

  public void setSelectionFactor (float factor) {
    if (this.selectionFactor != factor) {
      this.selectionFactor = factor;
      if (toIndex != -1 && (int) factor == toIndex && factor % 1f == 0) {
        fromIndex = toIndex = -1;
      }

      recalculateSelection(selectionFactor, true);
      invalidate();
    }
  }

  private int fromIndex = -1;
  private int toIndex = -1;

  public void setFromTo (int fromIndex, int toIndex) {
    if (fromIndex != toIndex || fromIndex == -1) {
      this.fromIndex = fromIndex;
      this.toIndex = toIndex;
      if (toIndex != -1) {
        recalculateSelection(toIndex, false);
      }
    }
  }

  public boolean setTextFromToColorId (@ThemeColorId int fromColorId, @ThemeColorId int toColorId) {
    if (this.fromTextColorId != fromColorId || this.toTextColorId != toColorId) {
      this.fromTextColorId = fromColorId;
      this.toTextColorId = toColorId;
      invalidate();
      return true;
    }
    return false;
  }

  public boolean setSelectionColorId (@ThemeColorId int colorId) {
    if (this.selectionColorId != colorId) {
      this.selectionColorId = colorId;
      invalidate();
      return true;
    }
    return false;
  }

  private boolean disabled;

  public void setTouchDisabled (boolean disabled) {
    if (this.disabled != disabled) {
      this.disabled = disabled;
      final int childCount = getChildCount();
      for (int i = 0; i < childCount; i++) {
        View view = getChildAt(i);
        if (view != null && view instanceof BackgroundView) {
          view.setEnabled(!disabled);
        }
      }
    }
  }

  private float disabledFactor;

  public void setDisabledFactor (float factor) {
    if (this.disabledFactor != factor) {
      this.disabledFactor = factor;
      invalidate();
    }
  }

  @Override
  public boolean onInterceptTouchEvent (MotionEvent ev) {
    return disabledFactor != 0f;
  }

  @Override
  public boolean onTouchEvent (MotionEvent event) {
    return disabled || disabledFactor != 0f || super.onTouchEvent(event);
  }

  private float overlayFactor;

  public void setOverlayFactor (float factor) {
    if (this.overlayFactor != factor) {
      this.overlayFactor = factor;
      invalidate();
    }
  }

  private int selectionLeft, selectionWidth;
  private int lastCallSelectionLeft, lastCallSelectionWidth;
  private float lastCallSelectionFactor;

  @SuppressLint("WrongConstant")
  @Override
  public void draw (Canvas c) {
    super.draw(c);

    if (items == null) {
      return;
    }

    final int viewHeight = getMeasuredHeight();
    final boolean wrapContent = shouldWrapContent();

    if (overlayFactor != 1f) {
      int textToColor = Theme.getColor(toTextColorId);
      int textFromColor = fromTextColorId != 0 ? Theme.getColor(fromTextColorId) : ColorUtils.alphaColor(Theme.getSubtitleAlpha(), Theme.getColor(R.id.theme_color_headerText));
      int selectionColor = selectionColorId != 0 ? Theme.getColor(selectionColorId) : ColorUtils.alphaColor(.9f, Theme.getColor(R.id.theme_color_headerText));

      boolean rtl = Lang.rtl();

      int selectionHeight = Screen.dp(SELECTION_HEIGHT);
      int selectionLeft = rtl ? this.totalWidth - this.selectionLeft - this.selectionWidth : this.selectionLeft;
      int selectionRight = selectionLeft + this.selectionWidth;
      int selectionTop = this.drawSelectionAtTop ? 0 : viewHeight - selectionHeight;
      int selectionBottom = selectionTop + selectionHeight;

      c.drawRect(selectionLeft, selectionTop, selectionRight, selectionBottom, Paints.fillingPaint(disabledFactor == 0f ? selectionColor : ColorUtils.fromToArgb(selectionColor, textFromColor, disabledFactor)));

      int cx = rtl ? totalWidth : 0;
      int itemIndex = 0;
      int itemCount = items.size();
      for (int i = 0; i < itemCount; i++) {
        Item item = items.get(i);
        boolean hasTranslate = item.translationX != 0;
        if (hasTranslate) {
          c.save();
          c.translate(item.translationX, 0f);
        }

        float factor;
        if (fromIndex != -1 && toIndex != -1) {
          int diff = Math.abs(toIndex - fromIndex);
          if (itemIndex == toIndex) {
            factor = Math.abs(selectionFactor - fromIndex) / (float) diff;
          } else if (itemIndex == fromIndex) {
            factor = 1f - Math.abs(selectionFactor - fromIndex) / (float) diff;
          } else {
            factor = 0f;
          }
        } else {
          float abs = Math.abs(selectionFactor - (float) itemIndex);
          if (abs <= 1f) {
            factor = 1f - abs;
          } else {
            factor = 0f;
          }
        }

        final int itemWidth;
        if (wrapContent) {
          itemWidth = item.actualWidth + textPadding * 2;
        } else {
          itemWidth = commonItemWidth;
        }
        if (rtl)
          cx -= itemWidth;
        if (!item.hidden) {
          int color = ColorUtils.fromToArgb(textFromColor, textToColor, factor * (1f - disabledFactor));
          if (item.counter != null) {
            float alphaFactor = 1f - MathUtils.clamp(Math.abs(selectionFactor - i));
            float imageAlpha = counterAlphaProvider.getDrawableAlpha(item.counter, alphaFactor);
            if (items.get(0).hidden) {
              alphaFactor = Math.max(alphaFactor, 1f - MathUtils.clamp(selectionFactor));
              if (i == 1 && selectionFactor < 1) {
                alphaFactor = 1f;
              }
            }
            float textAlpha = counterAlphaProvider.getTextAlpha(item.counter, alphaFactor);
            float backgroundAlpha = counterAlphaProvider.getBackgroundAlpha(item.counter, alphaFactor);
            if (item.ellipsizedStringLayout != null) {
              int horizontalPadding = Math.max((itemWidth - item.actualWidth) / 2, 0);
              int stringX;
              if (item.iconRes != 0) {
                Drawable drawable = item.getIcon();
                Drawables.draw(c, drawable, cx + horizontalPadding, viewHeight / 2 - drawable.getMinimumHeight() / 2, Paints.getPorterDuffPaint(color));
                stringX = cx + horizontalPadding + Screen.dp(24f) + Screen.dp(6f);
              } else {
                stringX = cx + horizontalPadding;
              }
              int stringY = viewHeight / 2 - item.ellipsizedStringLayout.getHeight() / 2;
              c.translate(stringX, stringY);
              item.ellipsizedStringLayout.getPaint().setColor(color);
              item.ellipsizedStringLayout.draw(c);
              c.translate(-stringX, -stringY);
              item.counter.draw(c, cx + itemWidth - horizontalPadding - item.counter.getWidth() / 2f, viewHeight / 2f, Gravity.CENTER, textAlpha, backgroundAlpha, imageAlpha, item.provider, ThemeColorId.NONE);
            } else if (item.imageReceiver != null) {
              int size = item.imageReceiverSize;
              int imgY = (viewHeight - size) / 2;
              item.imageReceiver.setAlpha(imageAlpha);
              item.imageReceiver.setBounds(cx, imgY, cx + size, imgY + size);
              item.imageReceiver.drawScaled(c, item.imageReceiverScale);
              item.counter.draw(c, cx + size, viewHeight / 2f, Gravity.LEFT, textAlpha, backgroundAlpha, imageAlpha, item.provider, 0);
            } else if (item.iconRes != 0) {
              int horizontalPadding = Math.max((itemWidth - item.actualWidth) / 2, 0);
              Drawable drawable = item.getIcon();
              Drawables.draw(c, drawable, cx + horizontalPadding, viewHeight / 2 - drawable.getMinimumHeight() / 2, Paints.getPorterDuffPaint(color));
              item.counter.draw(c, cx + itemWidth - horizontalPadding - item.counter.getWidth() / 2f, viewHeight / 2f, Gravity.CENTER, textAlpha, backgroundAlpha, imageAlpha, item.provider, ThemeColorId.NONE);
            } else {
              item.counter.draw(c, cx + itemWidth / 2f, viewHeight / 2f, Gravity.CENTER, textAlpha, backgroundAlpha, imageAlpha, item.provider, 0);
            }
          } else if (item.ellipsizedStringLayout != null) {
            int stringX;
            if (item.iconRes != 0) {
              int horizontalPadding = Math.max((itemWidth - item.actualWidth) / 2, 0);
              Drawable drawable = item.getIcon();
              Drawables.draw(c, drawable, cx + horizontalPadding, viewHeight / 2 - drawable.getMinimumHeight() / 2, Paints.getPorterDuffPaint(color));
              stringX = cx + horizontalPadding + Screen.dp(24f) + Screen.dp(6f);
            } else {
              stringX = cx + itemWidth / 2 - item.actualWidth / 2;
            }
            int stringY = viewHeight / 2 - item.ellipsizedStringLayout.getHeight() / 2;
            c.translate(stringX, stringY);
            item.ellipsizedStringLayout.getPaint().setColor(color);
            item.ellipsizedStringLayout.draw(c);
            c.translate(-stringX, -stringY);
          } else if (item.iconRes != 0) {
            Drawable drawable = item.getIcon();
            Drawables.draw(c, drawable, cx + itemWidth / 2 - drawable.getMinimumWidth() / 2, viewHeight / 2 - drawable.getMinimumHeight() / 2, Paints.getPorterDuffPaint(color));
          }
        }
        if (!rtl)
          cx += itemWidth;
        itemIndex++;

        if (hasTranslate) {
          c.restore();
        }
      }
    }

    if (overlayFactor != 0f && overlayFactor != 1f) {
      final int viewWidth = getMeasuredWidth();

      c.save();
      c.translate(0, (float) viewHeight * (1f - overlayFactor));
      c.drawRect(0, 0, viewWidth, viewHeight, Paints.fillingPaint(Theme.fillingColor()));
      c.restore();

      /*float fromX = selectionFactor * itemWidth + itemWidth / 2;
      float fromY = viewHeight / 2;

      float x = fromX + (viewWidth / 2 - fromX) * overlayFactor;
      float y = fromY + (viewHeight / 2 - fromY) * overlayFactor;
      float radius = (float) Math.sqrt(viewWidth * viewWidth + viewHeight * viewHeight) * .5f * overlayFactor;

      c.save();
      c.clipRect(0, 0, viewWidth, viewHeight);
      c.drawRect(0, 0, viewWidth, viewHeight, Paints.fillingPaint(Utils.alphaColor(overlayFactor, TGTheme.fillingColor())));
      c.drawCircle(x, y, radius, Paints.fillingPaint(TGTheme.fillingColor())); // Utils.alphaColor(overlayFactor, TGTheme.fillingColor())
      c.restore();*/
    }
  }

  private static final CounterAlphaProvider DEFAULT_COUNTER_ALPHA_PROVIDER = new CounterAlphaProvider() {
  };

  public interface CounterAlphaProvider {
    default float getTextAlpha (Counter counter, @FloatRange(from = 0f, to = 1f) float alphaFactor) {
      return .5f + .5f * alphaFactor;
    }
    default float getDrawableAlpha (Counter counter, @FloatRange(from = 0f, to = 1f) float alphaFactor) {
      return .5f + .5f * alphaFactor;
    }
    default float getBackgroundAlpha (Counter counter, @FloatRange(from = 0f, to = 1f) float alphaFactor) {
      return .5f + .5f * alphaFactor;
    }
  }

  public void setCounterAlphaProvider (CounterAlphaProvider counterAlphaProvider) {
    this.counterAlphaProvider = counterAlphaProvider;
  }

  public interface OnItemClickListener {
    void onPagerItemClick (int index);
    default boolean onPagerItemLongClick (int index) {
      return false;
    }
  }

  public interface OnSlideOffListener {
    boolean onSlideOffPrepare (View view, MotionEvent event, int index);
    void onSlideOffStart (View view, MotionEvent event, int index);
    void onSlideOffMovement (View view, MotionEvent event, int index);
    void onSlideOffFinish (View view, MotionEvent event, int index, boolean apply);
  }

  private OnSlideOffListener onSlideOffListener;

  public void setOnSlideOffListener (OnSlideOffListener onSlideOffListener) {
    this.onSlideOffListener = onSlideOffListener;
  }

  @Override
  protected void onAttachedToWindow () {
    super.onAttachedToWindow();
    complexReceiver.attach();
  }

  @Override
  protected void onDetachedFromWindow () {
    super.onDetachedFromWindow();
    complexReceiver.detach();
  }

  @Override
  public void performDestroy () {
    complexReceiver.performDestroy();
  }

  private static class BackgroundView extends View {
    public BackgroundView (Context context) {
      super(context);

      Views.setClickable(this);
    }

    private float touchDownY;
    private boolean inSlideOff;
    private ViewParent lockedParent;

    @Override
    public boolean onTouchEvent (MotionEvent e) {
      OnSlideOffListener slideOffListener = topView != null ? topView.onSlideOffListener : null;
      if (slideOffListener == null) {
        return ((View) getParent()).getAlpha() >= 1f && super.onTouchEvent(e);
      }
      super.onTouchEvent(e);
      switch (e.getAction()) {
        case MotionEvent.ACTION_DOWN:
          touchDownY = e.getY();
          break;
        case MotionEvent.ACTION_MOVE: {
          if (lockedParent == null) {
            if (Math.abs(e.getY() - touchDownY) > Screen.getTouchSlop()) {
              if (lockedParent != null) {
                lockedParent.requestDisallowInterceptTouchEvent(false);
                lockedParent = null;
              }
              boolean needSlideOff = slideOffListener.onSlideOffPrepare(this, e, index);
              if (needSlideOff) {
                lockedParent = getParent();
                if (lockedParent != null) {
                  lockedParent.requestDisallowInterceptTouchEvent(true);
                }
              }
            }
          } else {
            int start = getMeasuredHeight();
            boolean inSlideOff = topView.slideOffDirection == SLIDE_OFF_DIRECTION_TOP ? e.getY() <= start : e.getY() >= start;
            if (this.inSlideOff != inSlideOff) {
              this.inSlideOff = inSlideOff;
              if (inSlideOff) {
                slideOffListener.onSlideOffStart(this, e, index);
              } else {
                slideOffListener.onSlideOffFinish(this, e, index, false);
              }
            }
            if (inSlideOff) {
              slideOffListener.onSlideOffMovement(this, e, index);
            }
          }
          break;
        }
        case MotionEvent.ACTION_CANCEL:
          if (inSlideOff) {
            inSlideOff = false;
            slideOffListener.onSlideOffFinish(this, e, index, false);
          }
          if (lockedParent != null) {
            lockedParent.requestDisallowInterceptTouchEvent(false);
            lockedParent = null;
          }
          break;
        case MotionEvent.ACTION_UP:
          if (inSlideOff) {
            inSlideOff = false;
            slideOffListener.onSlideOffFinish(this, e, index, true);
          }
          if (lockedParent != null) {
            lockedParent.requestDisallowInterceptTouchEvent(false);
            lockedParent = null;
          }
          break;
      }
      return true;
    }

    private ViewPagerTopView topView;

    public void setBoundView (ViewPagerTopView topView) {
      this.topView = topView;
    }

    private int index;

    public void setIndex (int index) {
      this.index = index;
    }

    @Override
    protected void onMeasure (int widthMeasureSpec, int heightMeasureSpec) {
      if (topView.shouldWrapContent()) {
        int left = 0;
        for (int i = 0; i < index; i++) {
          left += topView.items.get(i).width + topView.textPadding * 2;
        }
        int itemWidth = topView.items.get(index).width + topView.textPadding * 2;
        if (Lang.rtl()) {
          left = MeasureSpec.getSize(widthMeasureSpec) - left - itemWidth;
        }
        super.onMeasure(MeasureSpec.makeMeasureSpec(itemWidth, MeasureSpec.EXACTLY), heightMeasureSpec);
        setTranslationX(left);
      } else {
        int itemWidth = topView.calculateCommonItemWidth(MeasureSpec.getSize(widthMeasureSpec));
        super.onMeasure(MeasureSpec.makeMeasureSpec(itemWidth, MeasureSpec.EXACTLY), heightMeasureSpec);
        setTranslationX(itemWidth * index);
      }
    }
  }

  @Override
  public void onEmojiUpdated (boolean isPackSwitch) {
    invalidate();
  }
}
