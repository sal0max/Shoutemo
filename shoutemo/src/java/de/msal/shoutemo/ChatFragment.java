/*
 * Copyright 2014 Maximilian Salomon.
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

import android.app.Fragment;
import android.app.LoaderManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;

import de.msal.shoutemo.connector.GetPostsService;
import de.msal.shoutemo.connector.SendPostTask;
import de.msal.shoutemo.connector.model.Message;
import de.msal.shoutemo.db.ChatDb;

public class ChatFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    /*  */
    private final static int LOADER_ID_MESSAGES = 0;

    private RecyclerAdapter mListAdapter;

    //
    private BroadcastReceiver receiver;
    /* stuff for the smiley selector  */
    private View emoticonsSpacer;
    private PopupWindow emoticonsPopupWindow;
    private ImageButton keyboardButton;
    private int previousHeightDifference = 0, keyboardHeight;
    private boolean isKeyBoardVisible;

    /**
     * Use this factory method to create a new instance of this fragment using the provided
     * parameters.
     *
     * @return A new instance of fragment LightMeterFragment.
     */
    public static ChatFragment newInstance() {
        ChatFragment fragment = new ChatFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    public ChatFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat, container, false);

        /* find the other views */
        keyboardButton = (ImageButton) view.findViewById(R.id.ib_emoticons);
        emoticonsSpacer = view.findViewById(R.id.chat_emoticons_spacer);
        final LinearLayout parentLayout = (LinearLayout) view.findViewById(R.id.chat_rl_parent);
        final EditText inputField = (EditText) view.findViewById(R.id.et_input);
        final ImageButton sendButton = (ImageButton) view.findViewById(R.id.ib_send);

        /* everything for showing the udpate status */
        final ImageView updateStatus = (ImageView) view.findViewById(R.id.ib_update_indicator);
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                boolean enabled = intent.getBooleanExtra(GetPostsService.INTENT_UPDATE_ENABLED,
                        false);
                if (enabled) {
                    updateStatus.setVisibility(View.VISIBLE);
                } else {
                    updateStatus.setVisibility(View.INVISIBLE);
                }
            }
        };


        /* initially the inputField is empty, so disable the send button */
        sendButton.setVisibility(View.GONE);

        /* Send message when clicked on SEND-BUTTON */
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (inputField.getText() != null && !TextUtils.isEmpty(inputField.getText())) {
                    new SendPostTask(getActivity()).execute(inputField.getText().toString());
                    inputField.setText("");
                }
            }
        });

        inputField.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                keyboardButton.setImageResource(R.drawable.ic_action_emoticon);
                emoticonsPopupWindow.dismiss();
            }
        });

        /* hide send-button, if no text is entered */
        inputField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (sendButton.getVisibility() == View.VISIBLE && s.length() == 0) {
                    Animation pushOut = AnimationUtils.loadAnimation(getActivity(), R.anim.push_out);
                    sendButton.startAnimation(pushOut);
                    sendButton.setVisibility(View.GONE);
                } else if (sendButton.getVisibility() == View.GONE && s.length() >= 1) {
                    Animation pushIn = AnimationUtils.loadAnimation(getActivity(), R.anim.push_in);
                    sendButton.startAnimation(pushIn);
                    sendButton.setVisibility(View.VISIBLE);
                }
            }
        });

        /* Showing and dismissing popup on clicking EMOTICONS-BUTTON */
        keyboardButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (emoticonsPopupWindow.isShowing()) {
                    keyboardButton.setImageResource(R.drawable.ic_action_emoticon);
                    emoticonsPopupWindow.dismiss();
                } else {
                    emoticonsPopupWindow.setHeight(keyboardHeight);

                    if (isKeyBoardVisible) {
                        keyboardButton.setImageResource(R.drawable.ic_action_keyboard);
                        emoticonsSpacer.setVisibility(View.GONE);
                    } else {
                        keyboardButton.setImageResource(R.drawable.ic_action_emoji_down);
                        emoticonsSpacer.setVisibility(View.VISIBLE);
                    }
                    emoticonsPopupWindow.showAtLocation(parentLayout, Gravity.BOTTOM, 0, 0);
                }
            }
        });


        /* listen for layout changes */
        parentLayout.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        Rect r = new Rect();
                        parentLayout.getWindowVisibleDisplayFrame(r);

                        int screenHeight = parentLayout.getRootView().getHeight();
                        int heightDifference = screenHeight - (r.bottom);

                        if (previousHeightDifference - heightDifference > 50) {
                            keyboardButton.setImageResource(R.drawable.ic_action_emoticon);
                            emoticonsPopupWindow.dismiss();
                        }

                        previousHeightDifference = heightDifference;
                        if (heightDifference > 100) {
                            isKeyBoardVisible = true;
                            setEmoticonsKeyboardHeight(heightDifference);
                        } else {
                            isKeyBoardVisible = false;
                        }
                    }
                }
        );

        /* set the adapter for the emoticons */
        final View emoticonsGrid = inflater.inflate(R.layout.emoticons_grid, null);
        ((GridView) emoticonsGrid.findViewById(R.id.emoticons_gridview)).setAdapter(
                new EmoticonsAdapter(getActivity(), new EmoticonsAdapter.OnEmoticonClickListener() {
                    @Override
                    public void onEmoticonClick(String bbcode) {
                        inputField.getText().replace(inputField.getSelectionStart(),
                                inputField.getSelectionEnd(), " " + bbcode + " ");
                    }
                }));
        // create a pop window that works as the emoticons keyboard
        emoticonsPopupWindow = new PopupWindow(emoticonsGrid, LinearLayout.LayoutParams.MATCH_PARENT, keyboardHeight, false);
        // hide the spacer if the popup gets hidden
        emoticonsPopupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                emoticonsSpacer.setVisibility(View.GONE);
            }
        });

        // default: 230dp
        setEmoticonsKeyboardHeight((int) getResources().getDimension(R.dimen.keyboard_height));

        /* receive and handle share intent */
        Intent intent = getActivity().getIntent();
        String type = intent.getType();
        if (intent.getAction().equals(Intent.ACTION_SEND) && type != null) {
            if (type.equals("text/plain")) {
                String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
                if (sharedText != null) {
                    inputField.setText(sharedText);
                }
            }
        }

        /* list stuff */
        mListAdapter = new RecyclerAdapter(getActivity(), null);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        RecyclerView recyclerView = (RecyclerView) view.findViewById(android.R.id.list);
//        RecyclerView.ItemDecoration dividerItemDecoration = new SpacingItemDecoration(12, 12);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        layoutManager.setStackFromEnd(true);
//        mRecyclerView.addItemDecoration(dividerItemDecoration);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setHasFixedSize(true);
        recyclerView.setVerticalScrollBarEnabled(true);
        recyclerView.setAdapter(mListAdapter);

        this.getLoaderManager().initLoader(LOADER_ID_MESSAGES, null, this);

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver((receiver),
                new IntentFilter(GetPostsService.INTENT_UPDATE));
    }

    @Override
    public void onResume() {
        super.onResume();
        /* retrieve data */
        getActivity().startService(new Intent(getActivity(), GetPostsService.class));
    }

    @Override
    public void onPause() {
        super.onPause();
        /* stop retrieving data */
        getActivity().stopService(new Intent(getActivity(), GetPostsService.class));
    }

    @Override
    public void onStop() {
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(receiver);
        super.onStop();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.chat, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Bundle args = new Bundle();
        Intent intent;

        switch (item.getItemId()) {
            /* filters */
            case R.id.menu_filter_all:
                getLoaderManager().restartLoader(LOADER_ID_MESSAGES, null, this);

                item.setChecked(true);
                break;
            case R.id.menu_filter_shouts:
                args.putString("WHERE",
                        ChatDb.Messages.COLUMN_NAME_TYPE + " = '" + Message.Type.SHOUT + "'");
                getLoaderManager().restartLoader(LOADER_ID_MESSAGES, args, this);

                item.setChecked(true);
                break;
            case R.id.menu_filter_global:
                args.putString("WHERE",
                        ChatDb.Messages.COLUMN_NAME_TYPE + " = '" + Message.Type.GLOBAL + "'");
                getLoaderManager().restartLoader(LOADER_ID_MESSAGES, args, this);

                item.setChecked(true);
                break;
            case R.id.menu_filter_competitions:
                args.putString("WHERE",
                        ChatDb.Messages.COLUMN_NAME_TYPE + " = '" + Message.Type.COMPETITION + "'");
                getLoaderManager().restartLoader(LOADER_ID_MESSAGES, args, this);

                item.setChecked(true);
                break;
            case R.id.menu_filter_awards:
                args.putString("WHERE",
                        ChatDb.Messages.COLUMN_NAME_TYPE + " = '" + Message.Type.AWARD + "'");
                getLoaderManager().restartLoader(LOADER_ID_MESSAGES, args, this);

                item.setChecked(true);
                break;
            case R.id.menu_filter_promotions:
                args.putString("WHERE",
                        ChatDb.Messages.COLUMN_NAME_TYPE + " = '" + Message.Type.PROMOTION + "'");
                getLoaderManager().restartLoader(LOADER_ID_MESSAGES, args, this);

                item.setChecked(true);
                break;
            case R.id.menu_filter_threads:
                args.putString("WHERE",
                        ChatDb.Messages.COLUMN_NAME_TYPE + " = '" + Message.Type.THREAD + "'");
                getLoaderManager().restartLoader(LOADER_ID_MESSAGES, args, this);

                item.setChecked(true);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    //TODO
//    /**
//     * override onKeyDown for dismissing the emoticons keyboard on key down
//     */
//    @Override
//    public boolean onKeyDown(int keyCode, KeyEvent event) {
//        if (emoticonsPopupWindow.isShowing()) {
//            keyboardButton.setImageResource(R.drawable.ic_action_emoticon);
//            emoticonsPopupWindow.dismiss();
//            return false;
//        } else {
//            return super.onKeyDown(keyCode, event);
//        }
//    }

    /**
     * change the height of the emoticons keyboard matching to height of actual keyboard
     *
     * @param height minimum height by which we can make sure actual keyboard is open or not
     */
    private void setEmoticonsKeyboardHeight(int height) {
        if (height > 100) {
            keyboardHeight = height;
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, keyboardHeight);
            emoticonsSpacer.setLayoutParams(params);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case LOADER_ID_MESSAGES:
                String where = null;
                if (args != null) {
                    where = args.getString("WHERE");
                }
                return new CursorLoader(getActivity(), ChatDb.Posts.CONTENT_URI, null, where, null, null);
            default:
                return null;
        }

    }

    @Override
    public void onLoadFinished(Loader<Cursor> objectLoader, Cursor data) {
        mListAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mListAdapter.swapCursor(null);
    }

}
