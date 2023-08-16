// This is a Flexxgram source code file.
// Flexxgram is not a trademark of Telegram and Telegram X.
// Flexxgram is an open-source and freely distributed modification of Telegram X.
//
// Copyright (C) 2023 Flexxteam.

package com.flexxteam.messenger.ui.controllers;

import android.content.Context;
import android.view.View;

import org.thunderdog.challegram.BuildConfig;
import org.thunderdog.challegram.R;
import org.thunderdog.challegram.U;
import org.thunderdog.challegram.component.base.SettingView;
import org.thunderdog.challegram.core.Lang;
import org.thunderdog.challegram.telegram.Tdlib;
import org.thunderdog.challegram.telegram.TdlibUi;
import org.thunderdog.challegram.tool.UI;
import org.thunderdog.challegram.ui.ListItem;
import org.thunderdog.challegram.ui.RecyclerViewController;
import org.thunderdog.challegram.ui.SettingsAdapter;
import org.thunderdog.challegram.v.CustomRecyclerView;

import java.util.ArrayList;

public class FlexxController extends RecyclerViewController<Void> implements View.OnClickListener, View.OnLongClickListener {

  public FlexxController (Context context, Tdlib tdlib) {
    super(context, tdlib);
  }

  @Override public CharSequence getName () {
    return Lang.getString(R.string.FlexxController);
  }

  @Override public int getId () {
    return R.id.controller_flexx;
  }

  @Override public void onClick (View v) {
    int viewId = v.getId();
    if (viewId == R.id.btn_flexx) {
      // navigateTo(new FlexxUpdatesController(context, tdlib));
    } else if (viewId == R.id.btn_generalSettingsController) {
      navigateTo(new GeneralSettingsController(context, tdlib));
    } else if (viewId == R.id.btn_interfaceSettingsController) {
      navigateTo(new InterfaceSettingsController(context, tdlib));
    } else if (viewId == R.id.btn_chatsSettingsController) {
      navigateTo(new ChatsSettingsController(context, tdlib));
    } else if (viewId == R.id.btn_experimentalSettingsController) {
      navigateTo(new ExperimentalSettingsController(context, tdlib));
    } else if (viewId == R.id.btn_tgch) {
      tdlib.ui().openUrl(this, Lang.getStringSecure(R.string.TgChannelLink), new TdlibUi.UrlOpenParameters().forceInstantView());
    } else if (viewId == R.id.btn_crowdin) {
      tdlib.ui().openUrl(this, Lang.getStringSecure(R.string.TranslateLink), new TdlibUi.UrlOpenParameters().forceInstantView());
    } else if (viewId == R.id.btn_sources) {
      tdlib.ui().openUrl(this, Lang.getStringSecure(R.string.SourcesLink), new TdlibUi.UrlOpenParameters().forceInstantView());
    } else if (viewId == R.id.btn_donate) {
      tdlib.ui().openUrl(this, Lang.getStringSecure(R.string.DonateLink), new TdlibUi.UrlOpenParameters().forceInstantView());
    }
  }

  @Override
  public boolean onLongClick (View v) {
    int viewId = v.getId();
    if (viewId == R.id.btn_flexx) {
      UI.copyText(Lang.getStringSecure(R.string.FlexxVersion) + " (" + BuildConfig.COMMIT + ")\n", R.string.CopiedText);
    }
    return false;
  }

  @Override protected void onCreateView (Context context, CustomRecyclerView recyclerView) {
    SettingsAdapter adapter = new SettingsAdapter(this) {
      @Override protected void setValuedSetting (ListItem item, SettingView view, boolean isUpdate) {
        view.setDrawModifier(item.getDrawModifier());
        int itemId = item.getId();
        if (itemId == R.id.btn_flexx) {
          view.setData(Lang.getStringSecure(R.string.FlexxVersion) + " (" + BuildConfig.COMMIT + ")\n");
        } else if (itemId == R.id.btn_tgch) {
          view.setData(R.string.TgChannelDesc);
        } else if (itemId == R.id.btn_crowdin) {
          view.setData(R.string.TranslateDesc);
        } else if (itemId == R.id.btn_sources) {
          view.setData(R.string.SourcesDesc);
        } else if (itemId == R.id.btn_donate) {
          view.setData(R.string.DonateDesc);
        }
      }
    };

    ArrayList<ListItem> items = new ArrayList<>();

    items.add(new ListItem(ListItem.TYPE_EMPTY_OFFSET_SMALL));
    items.add(new ListItem(ListItem.TYPE_HEADER, 0, 0, R.string.FlexxUpdates));

    items.add(new ListItem(ListItem.TYPE_SHADOW_TOP));
    items.add(new ListItem(ListItem.TYPE_VALUED_SETTING_COMPACT, R.id.btn_flexx, R.drawable.baseline_system_update_24, R.string.Flexx));
    items.add(new ListItem(ListItem.TYPE_SHADOW_BOTTOM));

    items.add(new ListItem(ListItem.TYPE_HEADER, 0, 0, R.string.FlexxSettings));

    items.add(new ListItem(ListItem.TYPE_SHADOW_TOP));
    items.add(new ListItem(ListItem.TYPE_SETTING, R.id.btn_generalSettingsController, R.drawable.baseline_widgets_24, R.string.GeneralSettingsController));
    items.add(new ListItem(ListItem.TYPE_SEPARATOR));
    items.add(new ListItem(ListItem.TYPE_SETTING, R.id.btn_interfaceSettingsController, R.drawable.baseline_palette_24, R.string.InterfaceSettingsController));
    items.add(new ListItem(ListItem.TYPE_SEPARATOR));
    items.add(new ListItem(ListItem.TYPE_SETTING, R.id.btn_chatsSettingsController, R.drawable.baseline_chat_bubble_24, R.string.ChatsSettingsController));
    items.add(new ListItem(ListItem.TYPE_SEPARATOR));
    items.add(new ListItem(ListItem.TYPE_SETTING, R.id.btn_experimentalSettingsController, R.drawable.baseline_science_24, R.string.ExperimentalSettingsController));
    items.add(new ListItem(ListItem.TYPE_SHADOW_BOTTOM));

    items.add(new ListItem(ListItem.TYPE_HEADER, 0, 0, R.string.FlexxAbout));

    items.add(new ListItem(ListItem.TYPE_SHADOW_TOP));
    items.add(new ListItem(ListItem.TYPE_VALUED_SETTING_COMPACT, R.id.btn_tgch, R.drawable.baseline_help_24, R.string.TgChannel));
    items.add(new ListItem(ListItem.TYPE_SEPARATOR));
    items.add(new ListItem(ListItem.TYPE_VALUED_SETTING_COMPACT, R.id.btn_crowdin, R.drawable.baseline_translate_24, R.string.Translate));
    items.add(new ListItem(ListItem.TYPE_SEPARATOR));
    items.add(new ListItem(ListItem.TYPE_VALUED_SETTING_COMPACT, R.id.btn_sources, R.drawable.baseline_github_24, R.string.Sources));
    items.add(new ListItem(ListItem.TYPE_SEPARATOR));
    items.add(new ListItem(ListItem.TYPE_VALUED_SETTING_COMPACT, R.id.btn_donate, R.drawable.baseline_paid_24, R.string.Donate));
    items.add(new ListItem(ListItem.TYPE_SHADOW_BOTTOM));

    adapter.setItems(items, true);
    recyclerView.setAdapter(adapter);
  }
}
