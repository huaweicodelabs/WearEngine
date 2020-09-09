/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2020-2020. All rights reserved.
 */

package com.huawei.wearengine.app;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;

import com.huawei.hmf.tasks.OnFailureListener;
import com.huawei.hmf.tasks.OnSuccessListener;
import com.huawei.wearengine.HiWear;
import com.huawei.wearengine.app.utils.FileManager;
import com.huawei.wearengine.auth.AuthCallback;
import com.huawei.wearengine.auth.Permission;
import com.huawei.wearengine.device.Device;
import com.huawei.wearengine.device.DeviceClient;
import com.huawei.wearengine.p2p.Message;
import com.huawei.wearengine.p2p.P2pClient;
import com.huawei.wearengine.p2p.PingCallback;
import com.huawei.wearengine.p2p.Receiver;
import com.huawei.wearengine.p2p.SendCallback;

import java.io.File;
import java.io.IOException;
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

    private static final String HASH_CODE = " , hashcode is: ";

    private static final String PEER_PKG_NAME = "com.watch.wearengine";

    private static final String[] PERMISSIONS = {Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.CAMERA};

    private static final int TAKE_PHOTO = 2;

    private static final int SCROLL_HIGH = 50;

    private RadioGroup devicesRadioGroup;

    private TextView logOutputTextView;

    private P2pClient p2pClient;

    private DeviceClient deviceClient;

    private List<Device> deviceList = new ArrayList<>();

    private Device selectedDevice;

    private Message sendMessage;

    private Map<String, Device> deviceMap = new HashMap<>();

    private int index = 0;

    // 申请相机、存储权限的requestCode
    private static final int PERMISSION_CAMERA_STORAGE_CODE = 0x00000012;

    /**
     * 是否是Android 10以上手机
     */
    private boolean isAndroidQ = Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q;

    /**
     * 用于保存拍照图片的uri
     */
    private Uri mCameraUri;

    /**
     * 用于保存图片的文件路径，Android 10以下使用图片路径访问图片
     */
    private String mCameraImagePath;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        initData();
        checkPermissionStorageAndCamera();
        addViewListener();
    }

    /**
     * Applying for the Read Permission on External Storage
     */
    private void checkPermissionStorageAndCamera() {
        int permissionStorage = ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
        int permissionCamera = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        if (permissionStorage != PackageManager.PERMISSION_GRANTED
                || permissionCamera != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_CAMERA_STORAGE_CODE);
        }
    }

    /**
     * Initialization data
     */
    private void initData() {

    }

    /**
     * Initialization view
     */
    private void initView() {
        devicesRadioGroup = findViewById(R.id.device_radio_group);
        logOutputTextView = findViewById(R.id.log_output_text_view);
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
        if (!checkSelectedDevice()) {
            return;
        }
        p2pClient.ping(selectedDevice, new PingCallback() {
            @Override
            public void onPingResult(int result) {
                printOperationResult(Calendar.getInstance().getTime() + STRING_PING + selectedDevice.getName()
                        + DEVICE_NAME_OF + PEER_PKG_NAME + STRING_RESULT + result);
            }
        }).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void result) {
                printOperationResult(STRING_PING + selectedDevice.getName() + DEVICE_NAME_OF + PEER_PKG_NAME + SUCCESS);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                printOperationResult(STRING_PING + selectedDevice.getName() + DEVICE_NAME_OF + PEER_PKG_NAME + FAILURE);
            }
        });
    }

    /**
     * send message to device
     *
     * @param view UI object
     */
    public void sendMessage(View view) {
        String sendMessageStr = "Hello Watch! This is HuaWei Phone.";
        sendMessageResult(sendMessageStr);
    }

    /**
     * send file to device
     *
     * @param sendFilePath file path
     */
    public void sendFile(String sendFilePath) {

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == TAKE_PHOTO && resultCode == RESULT_OK) {
            String compressedFilePath = FileManager.getPathAfterCompressed(this, mCameraUri);
            printOperationResult("take photo success，getPathAfterCompressed file path is " + compressedFilePath);
            sendFile(compressedFilePath);
        }
    }

    /**
     * register message listener for message from device
     *
     * @param view UI object
     */
    public void receiveMessage(View view) {
        if (!checkSelectedDevice()) {
            return;
        }
        Receiver receiver = new Receiver() {
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
        int receiverPid = android.os.Process.myPid();
        int receiverHashCode = System.identityHashCode(receiver);
        Log.d(TAG, "receiveMessageButtonOnClick receiver pid is:" + receiverPid + HASH_CODE + receiverHashCode);
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


    private void sendMessageResult(String message) {
        if (!checkSelectedDevice()) {
            return;
        }
        if (message.length() > 0) {
            Message.Builder builder = new Message.Builder();
            try {
                builder.setPayload(message.getBytes("UTF-8"));
            } catch (UnsupportedEncodingException e) {
                Log.e(TAG, "set sendMessageStr UnsupportedEncodingException");
            }
            sendMessage = builder.build();
        }
        if (sendMessage == null || sendMessage.getData().length == 0) {
            printOperationResult("please input message for send!");
            return;
        }

        SendCallback sendCallback = new SendCallback() {
            @Override
            public void onSendResult(int resultCode) {
                printOperationResult(Calendar.getInstance().getTime() + SEND_MESSAGE_TO + selectedDevice.getName()
                        + DEVICE_NAME_OF + PEER_PKG_NAME + STRING_RESULT + resultCode);
            }

            @Override
            public void onSendProgress(long progress) {
                printOperationResult(Calendar.getInstance().getTime() + SEND_MESSAGE_TO + selectedDevice.getName()
                        + DEVICE_NAME_OF + PEER_PKG_NAME + " progress:" + progress);
            }
        };
        p2pClient.send(selectedDevice, sendMessage, sendCallback).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void result) {
                printOperationResult(
                        SEND_MESSAGE_TO + selectedDevice.getName() + DEVICE_NAME_OF + PEER_PKG_NAME + SUCCESS);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                printOperationResult(
                        SEND_MESSAGE_TO + selectedDevice.getName() + DEVICE_NAME_OF + PEER_PKG_NAME + FAILURE);
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
            logOutputTextView.scrollTo(0, offset - logOutputTextView.getHeight() + SCROLL_HIGH);
        }
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

    private boolean checkSelectedDevice() {
        if (selectedDevice == null) {
            printOperationResult("please select the target device!");
            return false;
        }
        return true;
    }

    /**
     * use camera to take photo
     *
     * @param view UI object
     */
    public void takePhoto(View view) {
        if (!checkSelectedDevice()) {
            return;
        }
        Intent captureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // 判断是否有相机
        if (captureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            Uri photoUri = null;
            if (isAndroidQ) {
                // 适配android 10
                photoUri = FileManager.createImageUri(this);
                Log.e(TAG, "camera come here0");
            } else {
                try {
                    photoFile = FileManager.createImageFile(this);
                    Log.e(TAG, "camera come here1");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (photoFile != null) {
                    mCameraImagePath = photoFile.getAbsolutePath();
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        // 适配Android 7.0文件权限，通过FileProvider创建一个content类型的Uri
                        photoUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", photoFile);
                    } else {
                        photoUri = Uri.fromFile(photoFile);
                    }
                }
            }
            mCameraUri = photoUri;
            if (photoUri != null) {
                captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                captureIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                startActivityForResult(captureIntent, TAKE_PHOTO);
            }
        }
    }
}