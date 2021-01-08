package com.example.redwechat;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.text.TextUtils;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import androidx.annotation.RequiresApi;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.security.MessageDigest;
import java.util.List;

public class MyAccessibilityService extends AccessibilityService {

    private final static String WECHAT_UI = "com.tencent.mm.ui.LauncherUI";
    private final static String WECHAT_RED = "com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyNotHookReceiveUI";
    private final static String WECHAT_RED_DETAIL = "com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyDetailUI";
    private final static String WECHAT_RED_BUTTON = "android.widget.Button";
    private final static String WECHAT_RED_NAME = "微信红包";
    private final static String WECHAT = "微信";
    private final static String IS_GET_WECHAT_RED_NAME = "已领取";
    private final static String IS_ALL_GET_WECHAT_RED_NAME = "已被领完";
    private final static String WECHAT_UI_2 = "android.widget.FrameLayout";
    private final static int NUM = 4096;
    private final static int DELAY_TIME = 100;//ms
    private final static int DELAY_DETAIL_TIME = 300;//ms
    private boolean isOpenRP = false;
    private boolean isOpenDetail = false;
    private Handler handler = new Handler();
    private long mValue = 0;


    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        int eventType = event.getEventType();
        switch (eventType) {
            case AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED:
                List<CharSequence> texts = event.getText();
                for (CharSequence text : texts) {
                    String content = text.toString();
                    if (!TextUtils.isEmpty(content)) {
                        if (content.contains(WECHAT)) {
                            isOpenRP = false;
                        }
                    }
                }
                break;
            case AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED:
            case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:
                final String className = event.getClassName().toString();
                AccessibilityNodeInfo rootNode = getRootInActiveWindow();
                if (WECHAT_UI.equals(className) || WECHAT_UI_2.equals(className)) {
                    findRedPacket(rootNode);
                }
                if (WECHAT_RED.equals(className)) {
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            openRedPacket();
                            isOpenDetail = true;

                        }
                    }, DELAY_TIME);
                }

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (isOpenDetail && WECHAT_RED_DETAIL.equals(className)) {
                            MyAccessibilityService.this.performGlobalAction(1);
                            isOpenDetail = false;
                            release();
                        }
                    }
                }, DELAY_DETAIL_TIME);
                break;
        }
    }

    private void release() {
        isOpenRP = false;
        isOpenDetail = false;
    }

    /**
     * 遍历查找红包
     */
    private void findRedPacket(AccessibilityNodeInfo accessibilityNodeInfo) {
        if (accessibilityNodeInfo != null) {
            for (int i = accessibilityNodeInfo.getChildCount() - 1; i >= 0; i--) {
                AccessibilityNodeInfo accessibilityNodeInfoChild = accessibilityNodeInfo.getChild(i);
                if (accessibilityNodeInfoChild == null) {
                    continue;
                }
                CharSequence text = accessibilityNodeInfoChild.getText();
                if (text != null && text.toString().equals(WECHAT_RED_NAME)) {
                    AccessibilityNodeInfo parent = accessibilityNodeInfoChild.getParent();
                    while (parent != null) {
                        if (parent.isClickable()) {
                            if (!parent.getChild(1).getText().equals(IS_GET_WECHAT_RED_NAME) &&
                                    !parent.getChild(1).getText().equals(IS_ALL_GET_WECHAT_RED_NAME)) {
                                parent.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                                isOpenRP = true;
                            }
                            break;
                        }
                        parent = parent.getParent();
                    }
                }
                findRedPacket(accessibilityNodeInfoChild);
                if (isOpenRP) {
                    break;
                } else {
                    findRedPacket(accessibilityNodeInfoChild);
                }

            }
        }
    }


    /**
     * 开始打开红包
     */
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private void openRedPacket() {
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        for (int i = 0; i < rootNode.getChildCount(); i++) {
            if (rootNode.getChildCount() == 0) {
                continue;
            }
            AccessibilityNodeInfo accessibilityNodeInfo = rootNode.getChild(i);
            accessibilityNodeInfo.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            if (WECHAT_RED_BUTTON.equals(accessibilityNodeInfo.getClassName())) {
                accessibilityNodeInfo.performAction(AccessibilityNodeInfo.ACTION_CLICK);
//                execByRuntime(" pm clear com.example.redwechat");
                break;
            } else {
                openRedPacket(accessibilityNodeInfo);
            }
        }
    }

    private void openRedPacket(AccessibilityNodeInfo rootNode) {
        if (rootNode != null) {
            int childCount = rootNode.getChildCount();
            for (int i = 0; i < childCount; i++) {
                AccessibilityNodeInfo tempNodeInfo = rootNode.getChild(i);
                if (tempNodeInfo.getChildCount() > 0) {
                    openRedPacket(tempNodeInfo);
                } else if (i + 1 < childCount) {
                    AccessibilityNodeInfo node1 = rootNode.getChild(i);
                    AccessibilityNodeInfo node2 = rootNode.getChild(i + 1);
                    if (node1.getText() != null && node2.getText() != null) {
                        if (getString(R.string.yuan).equals(node2.getText().toString())) {
                            this.mValue = (long) (Float.valueOf(node1.getText().toString()).floatValue() * 100.0f);
                            return;
                        }
                    } else {
                        return;
                    }
                } else {
                    continue;
                }
            }
        }

    }

    @Override
    public void onInterrupt() {
    }

    /**
     * 模拟Cmd debug
     */
    public static String execByRuntime(String cmd) {
        Process process = null;
        BufferedReader bufferedReader = null;
        InputStreamReader inputStreamReader = null;
        try {
            process = Runtime.getRuntime().exec("su");
            process = Runtime.getRuntime().exec(cmd);
            inputStreamReader = new InputStreamReader(process.getInputStream());
            bufferedReader = new BufferedReader(inputStreamReader);

            int read;
            char[] buffer = new char[NUM];
            StringBuilder output = new StringBuilder();
            while ((read = bufferedReader.read(buffer)) > 0) {
                output.append(buffer, 0, read);
            }
            return output.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if (null != inputStreamReader) {
                try {
                    inputStreamReader.close();
                } catch (Throwable t) {

                }
            }
            if (null != bufferedReader) {
                try {
                    bufferedReader.close();
                } catch (Throwable t) {

                }
            }
            if (null != process) {
                try {
                    process.destroy();
                } catch (Throwable t) {

                }
            }
        }
    }

    public String md5Encode(String inStr) {
        try {
            byte[] md5Bytes = MessageDigest.getInstance("MD5").digest(inStr.getBytes("UTF-8"));
            StringBuffer hexValue = new StringBuffer();
            for (byte b : md5Bytes) {
                int val = b & 255;
                if (val < 16) {
                    hexValue.append("0");
                }
                hexValue.append(Integer.toHexString(val));
            }
            return hexValue.toString();
        } catch (Exception e) {
            System.out.println(e.toString());
            e.printStackTrace();
            return "";
        }
    }

}
