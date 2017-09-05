/*
 * Copyright (C) 2014 The Android Open Source Project
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
 * limitations under the License.
 */

package com.example.android.bluemote;

import android.app.ActionBar;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.common.logger.Log;

/**
 * This fragment controls Bluetooth to communicate with other devices.
 */
public class BlueMoteFragment extends Fragment  {

    private static final String TAG = "BlueMoteFragment";

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    private static final int REQUEST_ENABLE_BT = 3;

    // Layout Views
    private ListView mConversationView;
    private EditText mOutEditText;
    private Button mSendButton;
    private ImageView mTouchpad;
    private ImageButton mLeftButton;
    private ImageButton mRightButton;
    private ImageButton mSwitchButton;
    private ImageButton mVolOffButton;
    private ImageButton mVolDownButton;
    private ImageButton mVolUpButton;
    private ImageButton mPreviousButton;
    private ImageButton mPausePlayButton;
    private ImageButton mNextButton;
    private TextView mTracker;
    private GestureDetector mDetector;
    private VelocityTracker mVelocityTracker = null;
    /**
     * Name of the connected device
     */
    private String mConnectedDeviceName = null;
    private String message = null;

    /**
     * Array adapter for the commands thread
     */
    private ArrayAdapter<String> mCommandArrayAdapter;

    /**
     * String buffer for outgoing messages
     */
    private StringBuffer mOutStringBuffer;

    /**
     * Local Bluetooth adapter
     */
    private BluetoothAdapter mBluetoothAdapter = null;

    /**
     * Member object for the command services
     */
    private BlueMoteService mRemoteService = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        MyGestureDetector customGestureDetector = new MyGestureDetector();
        mDetector = new GestureDetector(getActivity(),customGestureDetector);



        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            FragmentActivity activity = getActivity();
            Toast.makeText(activity, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            activity.finish();
        }
    }


    @Override
    public void onStart() {
        super.onStart();
        // If BT is not on, request that it be enabled.
        // setupRemote() will then be called during onActivityResult
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            // Otherwise, setup the chat session
        } else if (mRemoteService == null) {
            setupRemote();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mRemoteService != null) {
            mRemoteService.stop();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (mRemoteService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mRemoteService.getState() == BlueMoteService.STATE_NONE) {
                // Start the Bluetooth chat services
                mRemoteService.start();
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_bluemote, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        //mConversationView = (ListView) view.findViewById(R.id.in);
        mOutEditText = (EditText) view.findViewById(R.id.edit_text_out);
        mSendButton = (Button) view.findViewById(R.id.button_send);
        mLeftButton = (ImageButton) view.findViewById(R.id.key_left);
        mRightButton = (ImageButton) view.findViewById(R.id.key_right);
        mSwitchButton = (ImageButton) view.findViewById(R.id.key_switch);
        mVolOffButton = (ImageButton) view.findViewById(R.id.volume_off);
        mVolDownButton = (ImageButton) view.findViewById(R.id.volume_down);
        mVolUpButton = (ImageButton) view.findViewById(R.id.volume_up);
        mPreviousButton = (ImageButton) view.findViewById(R.id.skip_prev);
        mPausePlayButton = (ImageButton) view.findViewById(R.id.pause_play);
        mNextButton = (ImageButton) view.findViewById(R.id.skip_next);
        mTracker = (TextView) getView().findViewById(R.id.tracker);
        mTouchpad = (ImageView) getView().findViewById(R.id.touchpad);


    }

    /**
     * Set up the UI and background operations for remote control.
     */
    private void setupRemote() {
        Log.d(TAG, "setupRemote()");

        // Initialize the array adapter for the conversation thread
        mCommandArrayAdapter = new ArrayAdapter<String>(getActivity(), R.layout.message);

        // Initialize the compose field with a listener for the return key
        mOutEditText.setOnEditorActionListener(mWriteListener);

        // Initialize the send button with a listener that for click events
        mSendButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Send a message using content of the edit text widget
                View view = getView();
                if (null != view) {
                    TextView textView = (TextView) view.findViewById(R.id.edit_text_out);
                    String message = textView.getText().toString();
                    sendMessage(message);
                }
            }
        });
        mLeftButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                    String message = "key_left";
                    sendMessage(message);

            }
        });
        mRightButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                    String message = "key_right";
                    sendMessage(message);
            }
        });
        mSwitchButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                    String message = "key_switch";
                    sendMessage(message);
            }
        });
        mVolOffButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                    String message = "mute";
                    sendMessage(message);
            }
        });
        mVolDownButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                    String message = "volumeDown";
                    sendMessage(message);
            }
        });
        mVolUpButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Send a message using content of the edit text widget
                View view = getView();
                if (null != view) {
                    String message = "volumeUp";
                    sendMessage(message);
                }
            }
        });
        mPreviousButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Send a message using content of the edit text widget
                View view = getView();
                if (null != view) {
                    String message = "previous";
                    sendMessage(message);
                }
            }
        });
        mPausePlayButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Send a message using content of the edit text widget
                View view = getView();
                if (null != view) {
                    String message = "playPause";
                    sendMessage(message);
                }
            }
        });
        mNextButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Send a message using content of the edit text widget
                View view = getView();
                if (null != view) {
                    String message = "next";
                    sendMessage(message);
                }
            }
        });
        //Setting a listener to detect gestures on the touchpad
        mTouchpad.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View view,MotionEvent event) {
                int index = event.getActionIndex();
                int action = event.getActionMasked();
                int pointerId = event.getPointerId(index);

                mDetector.onTouchEvent(event);
                switch(action) {
                    case MotionEvent.ACTION_DOWN:
                        if(mVelocityTracker == null) {
                            // Retrieve a new VelocityTracker object to watch the velocity of a motion.
                            mVelocityTracker = VelocityTracker.obtain();
                        }
                        else {
                            // Reset the velocity tracker back to its initial state.
                            mVelocityTracker.clear();
                        }
                        // Add a user's movement to the tracker.
                        mVelocityTracker.addMovement(event);

                        break;
                    case MotionEvent.ACTION_MOVE:
                        mVelocityTracker.addMovement(event);
                        // When you want to determine the velocity, call
                        // computeCurrentVelocity(). Then call getXVelocity()
                        // and getYVelocity() to retrieve the velocity for each pointer ID.
                        mVelocityTracker.computeCurrentVelocity(1000);
                        // Log velocity of pixels per second
                        // Best practice to use VelocityTrackerCompat where possible.
                        Log.d("", "X velocity: " + mVelocityTracker.getXVelocity(pointerId));
                        Log.d("", "Y velocity: " +mVelocityTracker.getYVelocity(pointerId));
                        if (mDetector.onTouchEvent(event))
                            {
                                message = ("mouse_click");
                                mTracker.setText("Click detected");
                                sendMessage(message);
                                message = "";
                            }
                            else {

                            if(message != null) sendMessage(message);
                            mTracker.setText("X velocity: " + mVelocityTracker.getXVelocity(pointerId) + "; Y velocity : " + mVelocityTracker.getYVelocity(pointerId));
                            message = "mouse_move(" + mVelocityTracker.getXVelocity(pointerId) + "," + mVelocityTracker.getYVelocity(pointerId) + ")";

                            }

                        break;
                    case MotionEvent.ACTION_UP:

                        break;

                    case MotionEvent.ACTION_CANCEL:
                        // Return a VelocityTracker object back to be re-used by others.
                        mVelocityTracker.recycle();
                        mVelocityTracker = null;
                        break;
                }
                return true;
            }
        });
        // Initialize the BlueMoteService to perform bluetooth connections
        mRemoteService = new BlueMoteService(getActivity(), mHandler);

        // Initialize the buffer for outgoing messages
        mOutStringBuffer = new StringBuffer("");
    }



    /**
     * Makes this device discoverable for 300 seconds (5 minutes).
     */
    private void ensureDiscoverable() {
        if (mBluetoothAdapter.getScanMode() !=
                BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }

    /**
     * Sends a message.
     *
     * @param message A string of text to send.
     */
    private void sendMessage(String message) {
        // Check that we're actually connected before trying anything
        if (mRemoteService.getState() != BlueMoteService.STATE_CONNECTED) {
            Toast.makeText(getActivity(), R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }

        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the BlueMoteService to write.
            // The '$' character indicates end of the string.
            message+="$";
            byte[] send = message.getBytes();
            mRemoteService.write(send);

            // Reset out string buffer to zero
            mOutStringBuffer.setLength(0);
        }
    }

    /**
     * The action listener for the EditText widget, to listen for the return key
     */
    private TextView.OnEditorActionListener mWriteListener
            = new TextView.OnEditorActionListener() {
        public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
            // If the action is a key-up event on the return key, send the message
            if (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_UP) {
                String message = view.getText().toString();
                //message+="$";
                sendMessage(message);
            }
            return true;
        }
    };

    /**
     * Updates the status on the action bar.
     *
     * @param resId a string resource ID
     */
    private void setStatus(int resId) {
        FragmentActivity activity = getActivity();
        if (null == activity) {
            return;
        }
        final ActionBar actionBar = activity.getActionBar();
        if (null == actionBar) {
            return;
        }
        actionBar.setSubtitle(resId);
    }

    /**
     * Updates the status on the action bar.
     *
     * @param subTitle status
     */
    private void setStatus(CharSequence subTitle) {
        FragmentActivity activity = getActivity();
        if (null == activity) {
            return;
        }
        final ActionBar actionBar = activity.getActionBar();
        if (null == actionBar) {
            return;
        }
        actionBar.setSubtitle(subTitle);
    }

    /**
     * The Handler that gets information back from the BlueMoteService
     */
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            FragmentActivity activity = getActivity();
            switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BlueMoteService.STATE_CONNECTED:
                            setStatus(getString(R.string.title_connected_to, mConnectedDeviceName));
                            mCommandArrayAdapter.clear();
                            break;
                        case BlueMoteService.STATE_CONNECTING:
                            setStatus(R.string.title_connecting);
                            break;
                        case BlueMoteService.STATE_LISTEN:
                        case BlueMoteService.STATE_NONE:
                            setStatus(R.string.title_not_connected);
                            break;
                    }
                    break;
                case Constants.MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);
                    mCommandArrayAdapter.add("Me:  " + writeMessage);
                    break;

                case Constants.MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(Constants.DEVICE_NAME);
                    if (null != activity) {
                        Toast.makeText(activity, "Connected to "
                                + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    }
                    break;
                case Constants.MESSAGE_TOAST:
                    if (null != activity) {
                        Toast.makeText(activity, msg.getData().getString(Constants.TOAST),
                                Toast.LENGTH_SHORT).show();
                    }
                    break;
            }
        }
    };

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE_SECURE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data);
                }
                break;

            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    // Bluetooth is now enabled, so set up the remote
                    setupRemote();
                } else {
                    // User did not enable Bluetooth or an error occurred
                    Log.d(TAG, "BT not enabled");
                    Toast.makeText(getActivity(), R.string.bt_not_enabled_leaving,
                            Toast.LENGTH_SHORT).show();
                    getActivity().finish();
                }
        }
    }

    /**
     * Establish connection with other device
     *
     * @param data   An {@link Intent} with {@link DeviceListActivity#EXTRA_DEVICE_ADDRESS} extra.
     */
    private void connectDevice(Intent data) {
        // Get the device MAC address
        String address = data.getExtras()
                .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
        // Get the BluetoothDevice object
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        // Attempt to connect to the device
        mRemoteService.connect(device);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.bluetooth_chat, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.connect_scan: {
                // Launch the DeviceListActivity to see devices and do scan
                Intent serverIntent = new Intent(getActivity(), DeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE);
                return true;
            }

            case R.id.discoverable: {
                // Ensure this device is discoverable by others
                ensureDiscoverable();
                return true;
            }
        }
        return false;
    }
    class MyGestureDetector extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onSingleTapConfirmed (MotionEvent event) {
            Log.d("","Single Tap Confirmed");
            String message = ("mouse_click");
            mTracker.setText("Click detected");
            sendMessage(message);

            return true;
        }
        @Override
        public boolean onDown(MotionEvent event) {
            return true;
        }
    }
}
