/*
 * Copyright 2013 Maximilian Salomon.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 */

package de.msal.shoutemo;

import android.app.ListActivity;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;

import java.lang.reflect.Field;

import de.msal.shoutemo.connector.GetPostsService;
import de.msal.shoutemo.connector.SendPostTask;
import de.msal.shoutemo.db.ChatDb;

public class ChatActivity extends ListActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    /*  */
    private final static int NO_OF_EMOTICONS = 55;
    private final static int LOADER_ID_MESSAGES = 0;
    private ListAdapter listAdapter;
    //
    /* stuff for the smiley selector  */
    private View emoticonsCover, popUpView;
    private int previousHeightDiffrence = 0, keyboardHeight;
    private PopupWindow emoticonsPopupWindow;
    private boolean isKeyBoardVisible;
    private Drawable[] emoticons;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        popUpView = getLayoutInflater().inflate(R.layout.emoticons_popup, null);
        emoticonsCover = findViewById(R.id.chat_ll_popup_parent);
        final RelativeLayout parentLayout = (RelativeLayout) findViewById(R.id.chat_rl_parent);
        final EditText editText = (EditText) findViewById(R.id.et_input);
        final ImageButton btnSend = (ImageButton) findViewById(R.id.ib_send);
        final ImageView smileyButton = (ImageView) findViewById(R.id.ib_smileys);

        listAdapter = new ListAdapter(this, null, CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
        // Defining default height of keyboard which is equal to 230 dip
        final int popUpheight = (int) getResources().getDimension(R.dimen.keyboard_height);

        editText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (emoticonsPopupWindow.isShowing()) {
                    emoticonsPopupWindow.dismiss();
                }
            }
        });
        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (editText.getText() != null && !TextUtils.isEmpty(editText.getText())) {
                    new SendPostTask(getApplicationContext())
                            .execute(editText.getText().toString());
                    editText.setText("");
                }
            }
        });
        /* Showing and dismissing popup on clicking emoticons button */
        smileyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!emoticonsPopupWindow.isShowing()) {
                    emoticonsPopupWindow.setHeight(keyboardHeight);

                    if (isKeyBoardVisible) {
                        emoticonsCover.setVisibility(LinearLayout.GONE);
                    } else {
                        emoticonsCover.setVisibility(LinearLayout.VISIBLE);
                    }
                    emoticonsPopupWindow.showAtLocation(parentLayout, Gravity.BOTTOM, 0, 0);

                } else {
                    emoticonsPopupWindow.dismiss();
                }
            }
        });

        changeKeyboardHeight(popUpheight);
        getEmoticons();
        enablePopUpView();
        checkKeyboardHeight(parentLayout);

        /* list stuff */
        setListAdapter(this.listAdapter);
        getLoaderManager().initLoader(LOADER_ID_MESSAGES, null, this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        /* retrieve data */
        startService(new Intent(this, GetPostsService.class));
    }

    @Override
    protected void onPause() {
        super.onPause();
        /* stop retrieving data */
        stopService(new Intent(this, GetPostsService.class));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.chat, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_about:
                Intent intent = new Intent(this, MyPreferenceActivity.class);
                startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Reading all emoticons in local cache
     */
    private void getEmoticons() {
        emoticons = new Drawable[NO_OF_EMOTICONS];
        int i = 0;
        Field[] drawables = de.msal.shoutemo.R.drawable.class.getDeclaredFields();
        for (Field f : drawables) {
            if (f.getName().startsWith("smil_")) {
                emoticons[i] = getResources().getDrawable(getResources().getIdentifier(f.getName(),
                        "drawable", getPackageName()));
                i++;
            }
        }
    }

    /**
     * Overriding onKeyDown for dismissing keyboard on key down
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (emoticonsPopupWindow.isShowing()) {
            emoticonsPopupWindow.dismiss();
            return false;
        } else {
            return super.onKeyDown(keyCode, event);
        }
    }

    private void checkKeyboardHeight(final View parentLayout) {
        parentLayout.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        Rect r = new Rect();
                        parentLayout.getWindowVisibleDisplayFrame(r);

                        int screenHeight = parentLayout.getRootView()
                                .getHeight();
                        int heightDifference = screenHeight - (r.bottom);

                        if (previousHeightDiffrence - heightDifference > 50) {
                            emoticonsPopupWindow.dismiss();
                        }

                        previousHeightDiffrence = heightDifference;
                        if (heightDifference > 100) {
                            isKeyBoardVisible = true;
                            changeKeyboardHeight(heightDifference);
                        } else {
                            isKeyBoardVisible = false;
                        }

                    }
                });
    }

    /**
     * change height of emoticons keyboard according to height of actual keyboard
     *
     * @param height minimum height by which we can make sure actual keyboard is open or not
     */
    private void changeKeyboardHeight(int height) {
        if (height > 100) {
            keyboardHeight = height;
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.MATCH_PARENT, keyboardHeight);
            emoticonsCover.setLayoutParams(params);
        }
    }

    /**
     * Defining all components of emoticons keyboard
     */
    private void enablePopUpView() {
        final GridView grid = (GridView) popUpView.findViewById(R.id.emoticons_gridview);
        grid.setAdapter(new EmoticonsAdapter(this, emoticons));

        // Creating a pop window for emoticons keyboard
        emoticonsPopupWindow = new PopupWindow(popUpView, LinearLayout.LayoutParams.MATCH_PARENT,
                keyboardHeight, false);

        emoticonsPopupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                emoticonsCover.setVisibility(LinearLayout.GONE);
            }
        });
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case LOADER_ID_MESSAGES:
                return new CursorLoader(this, ChatDb.Posts.CONTENT_URI, null, null, null, null);
            default:
                return null;
        }

    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        listAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        listAdapter.swapCursor(null);
    }

}
