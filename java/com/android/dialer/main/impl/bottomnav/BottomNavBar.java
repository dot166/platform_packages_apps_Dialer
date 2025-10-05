/*
 * Copyright (C) 2018 The Android Open Source Project
 * Copyright (C) 2023 The LineageOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package com.android.dialer.main.impl.bottomnav;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.dialer.R;
import com.android.dialer.common.Assert;
import com.android.dialer.common.LogUtil;
import com.android.dialer.main.impl.MainActivity;

import com.google.android.material.bottomnavigation.BottomNavigationItemView;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;

/** Dialer Bottom Nav Bar for {@link MainActivity}. */
public final class BottomNavBar extends BottomNavigationView {

  /** Index for each tab in the bottom nav. */
  @Retention(RetentionPolicy.SOURCE)
  @IntDef({
    TabIndex.NONE,
    TabIndex.SPEED_DIAL,
    TabIndex.CALL_LOG,
    TabIndex.CONTACTS,
    TabIndex.VOICEMAIL,
  })
  public @interface TabIndex {
    int NONE = -1;
    int SPEED_DIAL = 0;
    int CALL_LOG = 1;
    int CONTACTS = 2;
    int VOICEMAIL = 3;
  }

  private final List<OnBottomNavTabSelectedListener> listeners = new ArrayList<>();

  private BottomNavigationItemView speedDial;
  private BottomNavigationItemView callLog;
  private BottomNavigationItemView contacts;
  private BottomNavigationItemView voicemail;
  private @TabIndex int selectedTab;

  public BottomNavBar(Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
    inflateMenu(R.menu.bottom_nav);
  }

  @Override
  protected void onFinishInflate() {
    super.onFinishInflate();
    speedDial = findViewById(R.id.speed_dial_tab);
    callLog = findViewById(R.id.call_log_tab);
    contacts = findViewById(R.id.contacts_tab);
    voicemail = findViewById(R.id.voicemail_tab);

    setOnItemSelectedListener(new OnItemSelectedListener() {
      @Override
      public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
        switch (menuItem.getItemId()) {
          case R.id.speed_dial_tab:
            selectTab(TabIndex.SPEED_DIAL);
            return true;
          case R.id.call_log_tab:
            selectTab(TabIndex.CALL_LOG);
            return true;
          case R.id.contacts_tab:
            selectTab(TabIndex.CONTACTS);
            return true;
          case R.id.voicemail_tab:
            selectTab(TabIndex.VOICEMAIL);
            return true;
          default:
            return false;
        }
      }
    });
  }

  private void setSelected(View view) {
    speedDial.setChecked(view == speedDial);
    callLog.setChecked(view == callLog);
    contacts.setChecked(view == contacts);
    voicemail.setChecked(view == voicemail);
  }

  /**
   * Select tab for uesr and non-user click.
   *
   * @param tab {@link TabIndex}
   */
  public void selectTab(@TabIndex int tab) {
    if (tab == TabIndex.SPEED_DIAL) {
      selectedTab = TabIndex.SPEED_DIAL;
      setSelected(speedDial);
    } else if (tab == TabIndex.CALL_LOG) {
      selectedTab = TabIndex.CALL_LOG;
      setSelected(callLog);
    } else if (tab == TabIndex.CONTACTS) {
      selectedTab = TabIndex.CONTACTS;
      setSelected(contacts);
    } else if (tab == TabIndex.VOICEMAIL) {
      selectedTab = TabIndex.VOICEMAIL;
      setSelected(voicemail);
    } else {
      throw new IllegalStateException("Invalid tab: " + tab);
    }

    updateListeners(selectedTab);
  }

  /**
   * Displays or hides the voicemail tab.
   *
   * <p>In the event that the voicemail tab was earlier visible but is now no longer visible, we
   * move to the speed dial tab.
   *
   * @param showTab whether to hide or show the voicemail
   */
  public void showVoicemail(boolean showTab) {
    LogUtil.i("OldMainActivityPeer.showVoicemail", "showing Tab:%b", showTab);
    int voicemailpreviousVisibility = voicemail.getVisibility();
    voicemail.setVisibility(showTab ? View.VISIBLE : View.GONE);
    int voicemailcurrentVisibility = voicemail.getVisibility();

    if (voicemailpreviousVisibility != voicemailcurrentVisibility
        && voicemailpreviousVisibility == View.VISIBLE
        && getSelectedTab() == TabIndex.VOICEMAIL) {
      LogUtil.i("OldMainActivityPeer.showVoicemail", "hid VM tab and moved to speed dial tab");
      selectTab(TabIndex.SPEED_DIAL);
    }
  }

  public void setNotificationCount(@TabIndex int tab, int count) {
    if (tab == TabIndex.SPEED_DIAL) {
      if (count > 0) {
        getOrCreateBadge(R.id.speed_dial_tab).setNumber(count);
      } else {
        getOrCreateBadge(R.id.speed_dial_tab).clearNumber();
        removeBadge(R.id.speed_dial_tab);
      }
    } else if (tab == TabIndex.CALL_LOG) {
      if (count > 0) {
        getOrCreateBadge(R.id.call_log_tab).setNumber(count);
      } else {
        getOrCreateBadge(R.id.call_log_tab).clearNumber();
        removeBadge(R.id.call_log_tab);
      }
    } else if (tab == TabIndex.CONTACTS) {
      if (count > 0) {
        getOrCreateBadge(R.id.contacts_tab).setNumber(count);
      } else {
        getOrCreateBadge(R.id.contacts_tab).clearNumber();
        removeBadge(R.id.contacts_tab);
      }
    } else if (tab == TabIndex.VOICEMAIL) {
      if (count > 0) {
        getOrCreateBadge(R.id.voicemail_tab).setNumber(count);
      } else {
        getOrCreateBadge(R.id.voicemail_tab).clearNumber();
        removeBadge(R.id.voicemail_tab);
      }
    } else {
      throw new IllegalStateException("Invalid tab: " + tab);
    }
  }

  public void addOnTabSelectedListener(OnBottomNavTabSelectedListener listener) {
    listeners.add(listener);
  }

  private void updateListeners(@TabIndex int tabIndex) {
    for (OnBottomNavTabSelectedListener listener : listeners) {
      switch (tabIndex) {
        case TabIndex.SPEED_DIAL:
          listener.onSpeedDialSelected();
          break;
        case TabIndex.CALL_LOG:
          listener.onCallLogSelected();
          break;
        case TabIndex.CONTACTS:
          listener.onContactsSelected();
          break;
        case TabIndex.VOICEMAIL:
          listener.onVoicemailSelected();
          break;
        default:
          throw Assert.createIllegalStateFailException("Invalid tab: " + tabIndex);
      }
    }
  }

  @TabIndex
  public int getSelectedTab() {
    return selectedTab;
  }

  /** Listener for bottom nav tab's on click events. */
  public interface OnBottomNavTabSelectedListener {

    /** Speed dial tab was clicked. */
    void onSpeedDialSelected();

    /** Call Log tab was clicked. */
    void onCallLogSelected();

    /** Contacts tab was clicked. */
    void onContactsSelected();

    /** Voicemail tab was clicked. */
    void onVoicemailSelected();
  }
}
