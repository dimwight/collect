/*
 * Copyright 2017 Nafundi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.odk.collect.android.activities;

import android.database.Cursor;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;

import androidx.annotation.LayoutRes;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.ContextCompat;
import androidx.core.view.MenuItemCompat;

import org.odk.collect.android.R;
import org.odk.collect.android.database.instances.DatabaseInstanceColumns;
import org.odk.collect.android.formlists.sorting.FormListSortingBottomSheetDialog;
import org.odk.collect.android.formlists.sorting.FormListSortingOption;
import org.odk.collect.android.injection.DaggerUtils;
import org.odk.collect.androidshared.ui.multiclicksafe.MultiClickGuard;
import org.odk.collect.settings.SettingsProvider;
import org.odk.collect.strings.localization.LocalizedActivity;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import javax.inject.Inject;

import timber.log.Timber;

public abstract class AppListActivity extends LocalizedActivity {

    protected static final int LOADER_ID = 0x01;
    private static final String SELECTED_INSTANCES = "selectedInstances";
    private static final String IS_SEARCH_BOX_SHOWN = "isSearchBoxShown";
    private static final String SEARCH_TEXT = "searchText";

    protected CursorAdapter listAdapter;
    protected LinkedHashSet<Long> selectedInstances = new LinkedHashSet<>();
    protected List<FormListSortingOption> sortingOptions;
    protected Integer selectedSortingOrder;
    protected ListView listView;
    protected LinearLayout llParent;
    protected ProgressBar progressBar;

    private String filterText;
    private String savedFilterText;
    private boolean isSearchBoxShown;

    private SearchView searchView;

    @Inject
    SettingsProvider settingsProvider;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        Timber.i("5358_AA onCreate %s", 79);
        super.onCreate(savedInstanceState);
        DaggerUtils.getComponent(this).inject(this);
    }

    // toggles to all checked or all unchecked
    // returns:
    // true if result is all checked
    // false if result is all unchecked
    //
    // Toggle behavior is as follows:
    // if ANY items are unchecked, check them all
    // if ALL items are checked, uncheck them all
    public static boolean toggleChecked(ListView lv) {
        Timber.i("5358_AA toggleChecked %s", 93);
        // shortcut null case
        if (lv == null) {
            return false;
        }

        boolean newCheckState = lv.getCount() > lv.getCheckedItemCount();
        setAllToCheckedState(lv, newCheckState);
        return newCheckState;
    }

    public static void setAllToCheckedState(ListView lv, boolean check) {
        Timber.i("5358_AA setAllToCheckedState %s", 105);
        // no-op if ListView null
        if (lv == null) {
            return;
        }

        for (int x = 0; x < lv.getCount(); x++) {
            lv.setItemChecked(x, check);
        }
    }

    // Function to toggle button label
    public static void toggleButtonLabel(Button toggleButton, ListView lv) {
        Timber.i("5358_AA toggleButtonLabel %s", 118);
        if (lv.getCheckedItemCount() != lv.getCount()) {
            toggleButton.setText(org.odk.collect.strings.R.string.select_all);
        } else {
            toggleButton.setText(org.odk.collect.strings.R.string.clear_all);
        }
    }

    @Override
    public void setContentView(View view) {
        Timber.i("5358_AA setContentView %s", 128);
        super.setContentView(view);
        init();
    }

    @Override
    public void setContentView(@LayoutRes int layoutResID) {
        Timber.i("5358_AA setContentView %s", 135);
        super.setContentView(layoutResID);
        init();
    }

    private void init() {
        Timber.i("5358_AA init %s", 141);
        listView = findViewById(android.R.id.list);
        listView.setOnItemClickListener((AdapterView.OnItemClickListener) this);
        listView.setEmptyView(findViewById(android.R.id.empty));
        progressBar = findViewById(R.id.progressBar);
        llParent = findViewById(R.id.llParent);

        // Use the nicer-looking drawable with Material Design insets.
        listView.setDivider(ContextCompat.getDrawable(this, R.drawable.list_item_divider));
        listView.setDividerHeight(1);

        setSupportActionBar(findViewById(R.id.toolbar));
    }

    @Override
    protected void onResume() {
        super.onResume();
        restoreSelectedSortingOrder();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        Timber.i("5358_AA onSaveInstanceState %s", 163);
        super.onSaveInstanceState(outState);
        outState.putSerializable(SELECTED_INSTANCES, selectedInstances);

        if (searchView != null) {
            outState.putBoolean(IS_SEARCH_BOX_SHOWN, !searchView.isIconified());
            outState.putString(SEARCH_TEXT, String.valueOf(searchView.getQuery()));
        } else {
            Timber.e(new Error("Unexpected null search view (issue #1412)"));
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle state) {
        Timber.i("5358_AA onRestoreInstanceState %s", 177);
        super.onRestoreInstanceState(state);
        selectedInstances = (LinkedHashSet<Long>) state.getSerializable(SELECTED_INSTANCES);
        isSearchBoxShown = state.getBoolean(IS_SEARCH_BOX_SHOWN);
        savedFilterText = state.getString(SEARCH_TEXT);
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        Timber.i("5358_AA onCreateOptionsMenu %s", 186);
        getMenuInflater().inflate(R.menu.form_list_menu, menu);
        final MenuItem sortItem = menu.findItem(R.id.menu_sort);
        final MenuItem searchItem = menu.findItem(R.id.menu_filter);
        searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
        searchView.setQueryHint(getResources().getString(org.odk.collect.strings.R.string.search));
        searchView.setMaxWidth(Integer.MAX_VALUE);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                Timber.i("5358_AA onQueryTextSubmit %s", 197);
                filterText = query;
                updateAdapter();
                searchView.clearFocus();
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                Timber.i("5358_AA onQueryTextChange %s", 205);
                filterText = newText;
                updateAdapter();
                return false;
            }
        });

        MenuItemCompat.setOnActionExpandListener(searchItem, new MenuItemCompat.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                Timber.i("5358_AA onMenuItemActionExpand %s", 215);
                sortItem.setVisible(false);
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                Timber.i("5358_AA onMenuItemActionCollapse %s", 222);
                sortItem.setVisible(true);
                return true;
            }
        });

        if (isSearchBoxShown) {
            searchItem.expandActionView();
            searchView.setQuery(savedFilterText, false);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Timber.i("5358_AA onOptionsItemSelected %s", 237);
        if (!MultiClickGuard.allowClick(getClass().getName())) {
            return true;
        }

        if (item.getItemId() == R.id.menu_sort) {
            new FormListSortingBottomSheetDialog(
                    this,
                    sortingOptions,
                    selectedSortingOrder,
                    selectedOption -> {
                        saveSelectedSortingOrder(selectedOption);
                        updateAdapter();
                    }
            ).show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    protected void checkPreviouslyCheckedItems() {
        Timber.i("5358_AA checkPreviouslyCheckedItems %s", 259);
        listView.clearChoices();
        List<Integer> selectedPositions = new ArrayList<>();
        int listViewPosition = 0;
        Cursor cursor = listAdapter.getCursor();
        if (cursor != null && cursor.moveToFirst()) {
            do {
                long instanceId = cursor.getLong(cursor.getColumnIndex(DatabaseInstanceColumns._ID));
                if (selectedInstances.contains(instanceId)) {
                    selectedPositions.add(listViewPosition);
                }
                listViewPosition++;
            } while (cursor.moveToNext());
        }

        for (int position : selectedPositions) {
            listView.setItemChecked(position, true);
        }
    }

    protected abstract void updateAdapter();

    protected abstract String getSortingOrderKey();

    protected boolean areCheckedItems() {
        Timber.i("5358_AA areCheckedItems %s", 269);
        return getCheckedCount() > 0;
    }

    protected int getCheckedCount() {
        Timber.i("5358_AA getCheckedCount %s", 274);
        return listView.getCheckedItemCount();
    }

    private void saveSelectedSortingOrder(int selectedStringOrder) {
        Timber.i("5358_AA saveSelectedSortingOrder %s", 278);
        selectedSortingOrder = selectedStringOrder;
        settingsProvider.getUnprotectedSettings().save(getSortingOrderKey(), selectedStringOrder);
    }

    protected void restoreSelectedSortingOrder() {
        Timber.i("5358_AA restoreSelectedSortingOrder %s", 284);
        selectedSortingOrder = settingsProvider.getUnprotectedSettings().getInt(getSortingOrderKey());
    }

    protected int getSelectedSortingOrder() {
        Timber.i("5358_AA getSelectedSortingOrder %s", 289);
        if (selectedSortingOrder == null) {
            restoreSelectedSortingOrder();
        }
        return selectedSortingOrder;
    }

    protected CharSequence getFilterText() {
        Timber.i("5358_AA getFilterText %s", 297);
        return filterText != null ? filterText : "";
    }

    protected void clearSearchView() {
        Timber.i("5358_AA clearSearchView %s", 302);
        searchView.setQuery("", false);
    }

    protected void hideProgressBarAndAllow() {
        Timber.i("5358_AA hideProgressBarAndAllow %s", 307);
        hideProgressBar();
    }

    private void hideProgressBar() {
        progressBar.setVisibility(View.GONE);
    }

    protected void showProgressBar() {
        Timber.i("5358_AA showProgressBar %s", 316);
        progressBar.setVisibility(View.VISIBLE);
    }
}
