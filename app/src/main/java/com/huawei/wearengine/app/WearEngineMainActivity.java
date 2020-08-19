/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2020-2020. All rights reserved.
 */

package com.huawei.wearengine.app;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.huawei.hmf.tasks.OnFailureListener;
import com.huawei.hmf.tasks.OnSuccessListener;
import com.huawei.wearengine.HiWear;
import com.huawei.wearengine.app.utils.SelectFileManager;
import com.huawei.wearengine.auth.AuthCallback;
import com.huawei.wearengine.auth.Permission;
import com.huawei.wearengine.device.Device;
import com.huawei.wearengine.device.DeviceClient;
import com.huawei.wearengine.monitor.MonitorClient;
import com.huawei.wearengine.monitor.MonitorData;
import com.huawei.wearengine.monitor.MonitorItem;
import com.huawei.wearengine.monitor.MonitorListener;
import com.huawei.wearengine.p2p.Message;
import com.huawei.wearengine.p2p.P2pClient;
import com.huawei.wearengine.p2p.PingCallback;
import com.huawei.wearengine.p2p.Receiver;
import com.huawei.wearengine.p2p.SendCallback;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * WearEngine CodeLab Sample Code Application Entrance
 *
 * @since 2020-08-05
 */
public class WearEngineMainActivity extends AppCompatActivity {
    private static final String TAG = "WearEngine_MainActivity";

    private static final String DEVICE_NAME_OF = "'s ";

    private static final String SEND_MESSAGE_TO = "Send message to ";

    private static final String FAILURE = " task failure";

    private static final String SUCCESS = " task success";

    private static final String STRING_RESULT = " result:";

    private static final String STRING_PING = " Ping ";

    private static final String MONITOR_INT_DATA = "], int data[";

    private static final String MONITOR_BOOLEAN_DATA = "], boolean data[";

    private static final String[] PERMISSIONS_STORAGE = {Manifest.permission.READ_EXTERNAL_STORAGE};

    private static final int SELECT_FILE_CODE = 1;

    private static final int REQUEST_EXTERNAL_STORAGE = 1;

    private RadioGroup devicesRadioGroup;

    private EditText messageEditText;

    private TextView logOutputTextView;

    private EditText peerPkgNameEditText;

    private P2pClient p2pClient;

    private MonitorClient monitorClient;

    private DeviceClient deviceClient;

    private List<Device> deviceList = new ArrayList<>();

    private Device selectedDevice;

    private Message sendMessage;

    private Map<String, Device> deviceMap = new HashMap<>();

    private int index = 0;

    private String peerPkgName;

    private MonitorItem monitorItemType = MonitorItem.MONITOR_ITEM_CONNECTION;

    private Receiver receiver = new Receiver() {
        @Override
        public void onReceiveMessage(Message message) {
            if (message != null) {
                String data = new String(message.getData());
                printOperationResult("ReceiveMessage is:" + data);
            } else {
                printOperationResult("Receiver Message is null");
            }
        }
    };

    private MonitorListener monitorListener = new MonitorListener() {
        @Override
        public void onChanged(int resultCode, MonitorItem monitorItem, MonitorData monitorData) {
            if (monitorData != null && monitorItem != null) {
                printOperationResult(
                    "MonitorListener result is: resultCode:" + resultCode + "string data[" + monitorData.asString()
                        + MONITOR_INT_DATA + monitorData.asInt() + MONITOR_BOOLEAN_DATA + monitorData.asBool() + " ]");
            } else {
                printOperationResult("monitorItem is null or monitorData is null!");
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        initData();
        addViewListener();
        verifyStoragePermissions();
    }

    /**
     * Initialization data
     */
    private void initData() {


    }

    /**
     * Set the name of the device-side application package specified by the third-party application for communication.
     *
     */
    private void setPeerPkgName(Editable editable) {


    }

    /**
     * send message to device
     */
    private void sendMessage(){


    }

    /**
     * Applying for the Read Permission on External Storage
     */
    private void verifyStoragePermissions() {
        int permission = ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, PERMISSIONS_STORAGE, REQUEST_EXTERNAL_STORAGE);
        }
    }


    /**
     * Initialization view
     */
    private void initView() {
        devicesRadioGroup = findViewById(R.id.device_radio_group);
        logOutputTextView = findViewById(R.id.log_output_text_view);
        messageEditText = findViewById(R.id.message_edit_text);
        peerPkgNameEditText = findViewById(R.id.peer_pkg_name_edit_text);
    }

    /**
     * Add view listener
     */
    private void addViewListener() {
        logOutputTextView.setMovementMethod(ScrollingMovementMethod.getInstance());
        devicesRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                Log.d(TAG, "onCheckedChanged:" + checkedId);
                selectedDevice = deviceList.get(checkedId);
            }
        });
        peerPkgNameEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (editable == null) {
                    Log.e(TAG, "peerPkgNameEditText After message text changed editable is null");
                    return;
                }
                Log.d(TAG, "After package text changed" + editable);
                setPeerPkgName(editable);
            }
        });
    }

    /**
     * Get Bound Devices List
     *
     * @param view UI object
     */
    public void getBoundDevices(View view) {
        deviceClient.getBondedDevices().addOnSuccessListener(new OnSuccessListener<List<Device>>() {
            @Override
            public void onSuccess(List<Device> devices) {
                if (devices == null || devices.size() == 0) {
                    printOperationResult("getBondedDevices list is null or list size is 0");
                    return;
                }
                printOperationResult("getBondedDevices onSuccess! devices list size = " + devices.size());
                updateDeviceList(devices);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                printOperationResult("getBondedDevices task submission error");
            }
        });
    }

    /**
     * ping bound watch device
     *
     * @param view UI object
     */
    public void pingBoundDevices(View view) {
        if (!checkDevice() || !checkPackageName()) {
            return;
        }
        p2pClient.ping(selectedDevice, new PingCallback() {
            @Override
            public void onPingResult(int result) {
                printOperationResult(Calendar.getInstance().getTime() + STRING_PING + selectedDevice.getName()
                    + DEVICE_NAME_OF + peerPkgName + STRING_RESULT + result);
            }
        }).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void result) {
                printOperationResult(STRING_PING + selectedDevice.getName() + DEVICE_NAME_OF + peerPkgName + SUCCESS);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                printOperationResult(STRING_PING + selectedDevice.getName() + DEVICE_NAME_OF + peerPkgName + FAILURE);
            }
        });
    }

    /**
     * send message to device
     *
     * @param view UI object
     */
    public void sendMessageToDevice(View view) {
        if (!checkDevice() || !checkPackageName()) {
            return;
        }

        // Build the request param message
        String sendMessageStr = messageEditText.getText().toString();
        if (sendMessageStr.length() > 0) {
            Message.Builder builder = new Message.Builder();
            try {
                builder.setPayload(sendMessageStr.getBytes("UTF-8"));
            } catch (UnsupportedEncodingException e) {
                Log.e(TAG, "set sendMessageStr UnsupportedEncodingException");
            }
            sendMessage = builder.build();
        }
        if (sendMessage == null || sendMessage.getData().length == 0) {
            printOperationResult("please input message for send!");
            return;
        }

        sendMessage();
    }

    /**
     * select file to send device
     *
     * @param view UI object
     */
    public void selectFileAndSend(View view) {
        if (!checkDevice() || !checkPackageName()) {
            return;
        }
        try {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(intent, SELECT_FILE_CODE);
        } catch (ActivityNotFoundException e) {
            logOutputTextView.append("ActivityNotFoundException" + System.lineSeparator());
        }
    }

    /**
     * send file to device
     *
     * @param sendFilePath file path
     */
    public void sendFile(String sendFilePath) {
        if (TextUtils.isEmpty(sendFilePath)) {
            printOperationResult("selectFilePath is invalid");
            return;
        }
        printOperationResult("selectFilePath is:" + sendFilePath);
        File sendFile = new File(sendFilePath);
        if (!sendFile.exists()) {
            printOperationResult("file is not exist");
            return;
        }
        Message.Builder builder = new Message.Builder();
        builder.setPayload(sendFile);
        Message fileMessage = builder.build();
        if (fileMessage == null) {
            printOperationResult("fileMessage is null");
            return;
        }
        p2pClient.send(selectedDevice, fileMessage, new SendCallback() {
            @Override
            public void onSendResult(int resultCode) {
                printOperationResult(Calendar.getInstance().getTime() + SEND_MESSAGE_TO + selectedDevice.getName()
                    + DEVICE_NAME_OF + peerPkgName + STRING_RESULT + resultCode);
            }

            @Override
            public void onSendProgress(long progress) {
                printOperationResult(Calendar.getInstance().getTime() + SEND_MESSAGE_TO + selectedDevice.getName()
                    + DEVICE_NAME_OF + peerPkgName + " progress:" + progress);
            }
        }).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void succussVoid) {
                printOperationResult(
                    SEND_MESSAGE_TO + selectedDevice.getName() + DEVICE_NAME_OF + peerPkgName + SUCCESS);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                printOperationResult(
                    SEND_MESSAGE_TO + selectedDevice.getName() + DEVICE_NAME_OF + peerPkgName + FAILURE);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SELECT_FILE_CODE && resultCode == RESULT_OK) {
            if (data == null) {
                Log.e(TAG, "Invalid file data");
                return;
            }
            Uri selectFileUri = data.getData();
            String selectFilePath = SelectFileManager.getFilePath(this, selectFileUri);
            sendFile(selectFilePath);
        }
    }

    /**
     * register message listener for message from device
     *
     * @param view UI object
     */
    public void receiveMessage(View view) {
        if (!checkDevice() || !checkPackageName()) {
            return;
        }
        int receiverPid = android.os.Process.myPid();
        int receiverHashCode = System.identityHashCode(receiver);
        Log.d(TAG, "receiveMessageButtonOnClick receiver pid is:" + receiverPid + ", hashcode is:" + receiverHashCode);
        p2pClient.registerReceiver(selectedDevice, receiver).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void avoid) {
                printOperationResult("register receiver listener" + SUCCESS);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                printOperationResult("register receiver listener" + FAILURE);
            }
        });
    }

    /**
     * unregister message listener for message from device
     *
     * @param view UI object
     */
    public void cancelReceiveMessage(View view) {
        int receiverPid = android.os.Process.myPid();
        int receiverHashCode = System.identityHashCode(receiver);
        Log.d(TAG, "cancelReceiveMessage receiver pid is:" + receiverPid + ", hashcode is:" + receiverHashCode);
        p2pClient.unregisterReceiver(receiver).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void succussVoid) {
                printOperationResult("cancel receive message" + SUCCESS);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                printOperationResult("cancel receive message" + FAILURE);
            }
        });
    }

    /**
     * register status listener for connectionStatus
     *
     * @param view UI object
     */
    public void registerEventStatus(View view) {
        if (!checkDevice()) {
            return;
        }
        monitorListener = new MonitorListener() {
            @Override
            public void onChanged(int errorCode, MonitorItem monitorItem, MonitorData monitorData) {
                String result = "ReceiveMonitorMessage is: string data[" + monitorData.asString() + MONITOR_INT_DATA
                    + monitorData.asInt() + MONITOR_BOOLEAN_DATA + monitorData.asBool() + "]";
                printOperationResult(result);
            }
        };

        int receiverPid = android.os.Process.myPid();
        int receiverHashCode = System.identityHashCode(monitorListener);
        Log.d(TAG, "registerEventStatus receiver pid is:" + receiverPid + ", hashcode is:" + receiverHashCode);
        monitorClient.register(selectedDevice, monitorItemType, monitorListener)
            .addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void avoid) {
                    printOperationResult("register status event listener " + SUCCESS);
                }
            })
            .addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(Exception e) {
                    printOperationResult("register status event listener " + FAILURE);
                }
            });
    }

    /**
     * unregister status listener for connectionStatus
     *
     * @param view UI object
     */
    public void unRegisterEventStatus(View view) {
        int receiverPid = android.os.Process.myPid();
        int receiverHashCode = System.identityHashCode(monitorListener);
        Log.d(TAG, "cancelReceiveMessage receiver pid is:" + receiverPid + ", hashcode is:" + receiverHashCode);
        monitorClient.unregister(monitorListener).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void avoid) {
                printOperationResult("cancel register status event listener " + SUCCESS);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                printOperationResult("cancel register status event listener " + FAILURE);
            }
        });
    }

    /**
     * Clear OutputTextView
     *
     * @param view UI object
     */
    public void clearOutputTextView(View view) {
        logOutputTextView.setText("");
        logOutputTextView.scrollTo(0, 0);
    }

    /**
     * Send the operation result logs to the logcat and TextView control on the UI
     *
     * @param string indicating the log string
     */
    private void printOperationResult(String string) {
        Log.i(TAG, string);
        logOutputTextView.append(string + System.lineSeparator());
        int offset = logOutputTextView.getLineCount() * logOutputTextView.getLineHeight();
        if (offset > logOutputTextView.getHeight()) {
            logOutputTextView.scrollTo(0, offset - logOutputTextView.getHeight());
        }
    }

    /**
     * check selected device
     *
     * @return true/false
     */
    private boolean checkDevice() {
        if (selectedDevice == null) {
            printOperationResult("please select a target device!");
            return false;
        }
        return true;
    }

    /**
     * check input target app packageName
     *
     * @return true/false
     */
    private boolean checkPackageName() {
        if (TextUtils.isEmpty(peerPkgName)) {
            printOperationResult("please input target app packageName!");
            return false;
        }
        return true;
    }

    /**
     * update device list for devicesRadioGroup
     *
     * @param devices devices list
     */
    private void updateDeviceList(List<Device> devices) {
        for (Device device : devices) {
            printOperationResult("device Name: " + device.getName());
            printOperationResult("device connect status:" + device.isConnected());
            if (deviceMap.containsKey(device.getUuid())) {
                continue;
            }
            deviceList.add(device);
            deviceMap.put(device.getUuid(), device);
            RadioButton deviceRadioButton = new RadioButton(this);
            setRaidButton(deviceRadioButton, device.getName(), index);
            devicesRadioGroup.addView(deviceRadioButton);
            index++;
        }
    }

    /**
     * Set device list
     */
    private void setRaidButton(final RadioButton radioButton, String text, int id) {
        radioButton.setChecked(false);
        radioButton.setId(id);
        radioButton.setText(text);
    }
}