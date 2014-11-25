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

package de.msal.shoutemo.ui.chat;

import android.app.Activity;
import android.app.Fragment;
import android.app.LoaderManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;

import de.msal.shoutemo.R;
import de.msal.shoutemo.connector.GetPostsService;
import de.msal.shoutemo.connector.SendPostTask;
import de.msal.shoutemo.connector.model.Message;
import de.msal.shoutemo.db.ChatDb;
import de.msal.shoutemo.ui.TitleSetListener;

public class ChatFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private final static int LOADER_ID_MESSAGES = 0;
    private ListAdapter mListAdapter;
    private BroadcastReceiver mReceiver;
    private TitleSetListener mCallback;
    private GridView mEmoticonGrid;
    private ImageButton mKeyboardButton;
    private EditText mInputField;
    private ImageButton mSendButton;

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
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mCallback = (TitleSetListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnHeadlineSelectedListener");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat, container, false);

        /* set the fragment title of the toolbar */
        mCallback.setTitle(getString(R.string.app_name));

        /* INPUT FIELD WHERE THE MESSAGE IS COMPOSED */
        mInputField = (EditText) view.findViewById(R.id.et_input);

        /* display a blinking dot to show the refresh status */
        final ImageView updateStatus = (ImageView) view.findViewById(R.id.ib_update_indicator);
        mReceiver = new BroadcastReceiver() {
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

        /* SEND MESSAGE BUTTON */
        mSendButton = (ImageButton) view.findViewById(R.id.ib_send);
        // initially the mInputField is empty, so disable the send button
        mSendButton.setVisibility(View.GONE);
        // Send message when clicked on SEND-BUTTON
        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mInputField.getText() != null && !TextUtils.isEmpty(mInputField.getText())) {
                    new SendPostTask(getActivity()).execute(mInputField.getText().toString());
                    mInputField.setText("");
                }
            }
        });

        mEmoticonGrid = (GridView) view.findViewById(R.id.emoticons_grid);
        /* set the adapter for the emoticons */
        mEmoticonGrid.setAdapter(
                new EmoticonsAdapter(getActivity(), new EmoticonsAdapter.OnEmoticonClickListener() {
                    @Override
                    public void onEmoticonClick(String bbcode) {
                        mInputField.getText().replace(mInputField.getSelectionStart(),
                                mInputField.getSelectionEnd(), " " + bbcode + " ");
                    }
                }));

        /* A BUTTON WHICH SWITCHES BETWEEN SOFT KEYBOARD AND EMOTICON SELECTOR */
        mKeyboardButton = (ImageButton) view.findViewById(R.id.ib_emoticons);
        /* Showing and dismissing popup on clicking EMOTICONS-BUTTON */
        mKeyboardButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mEmoticonGrid.getVisibility() == View.VISIBLE) {
                    mKeyboardButton.setImageResource(R.drawable.ic_action_keyboard_alt_white_24dp);
                    mEmoticonGrid.setVisibility(View.GONE);
                } else {
                    mKeyboardButton.setImageResource(R.drawable.ic_action_keyboard_arrow_down_white_24dp);
                    mEmoticonGrid.setVisibility(View.VISIBLE);
                    hideKeyboard();
                }
            }
        });

        /* LIST STUFF */
        mListAdapter = new ListAdapter(getActivity(), null, 0);
        ListView listView = (ListView) view.findViewById(android.R.id.list);
        listView.setVerticalScrollBarEnabled(true);
        listView.setAdapter(mListAdapter);
        this.getLoaderManager().initLoader(LOADER_ID_MESSAGES, null, this);

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

       /* receive and handle share intent */
        Intent intent = getActivity().getIntent();
        String type = intent.getType();
        if (intent.getAction().equals(Intent.ACTION_SEND) && type != null) {
            if (type.equals("text/plain")) {
                String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
                if (sharedText != null) {
                    mInputField.setText(sharedText);
                }
            }
        }

       /* hide send-button, if no text is entered */
        mInputField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (mSendButton.getVisibility() == View.VISIBLE && s.length() == 0) {
                    Animation pushOut = AnimationUtils
                            .loadAnimation(getActivity(), R.anim.push_out);
                    mSendButton.startAnimation(pushOut);
                    mSendButton.setVisibility(View.GONE);
                } else if (mSendButton.getVisibility() == View.GONE && s.length() >= 1) {
                    Animation pushIn = AnimationUtils.loadAnimation(getActivity(), R.anim.push_in);
                    mSendButton.startAnimation(pushIn);
                    mSendButton.setVisibility(View.VISIBLE);
                }
            }
        });

       /*  */
        mInputField.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mKeyboardButton.setImageResource(R.drawable.ic_action_keyboard_alt_white_24dp);
                mEmoticonGrid.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver((mReceiver),
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
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mReceiver);
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

    private void hideKeyboard() {
        InputMethodManager inputManager = (InputMethodManager) getActivity().getSystemService(
                Context.INPUT_METHOD_SERVICE);

        // check if no view has focus:
        View view = getActivity().getCurrentFocus();
        if (view != null) {
            inputManager.hideSoftInputFromWindow(view.getWindowToken(),
                    InputMethodManager.HIDE_NOT_ALWAYS);
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
