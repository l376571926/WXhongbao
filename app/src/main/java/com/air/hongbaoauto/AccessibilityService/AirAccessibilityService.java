package com.air.hongbaoauto.AccessibilityService;

import android.accessibilityservice.AccessibilityService;
import android.annotation.TargetApi;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.os.Build;
import android.os.PowerManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import com.socks.library.KLog;

import java.util.ArrayList;
import java.util.List;


public class AirAccessibilityService extends AccessibilityService {

    public static boolean ALL = true;
    private List<AccessibilityNodeInfo> parents;
    private boolean auto = false;
    private int lastbagnum;
    private boolean WXMAIN = false;

    private KeyguardManager.KeyguardLock keyguardLock;
    private PowerManager.WakeLock wakeLock = null;

    private static String CHAT_LIST_ITEM = "com.tencent.mm:id/aja";//微信红包，父布局，android.widget.LinearLayout
    private static String CHAT_LIST_ITEM_SUBTITLE = "com.tencent.mm:id/aje";//微信红包，android.view.View
    private static String HB_CLOSE = "com.tencent.mm:id/bmu";//红包界面，关闭按钮
    private static String HB_OPEN = "com.tencent.mm:id/bp6";//红包界面，开红包按钮,第次安全apk好像都会变？
    private static String HB_DETAIL_CLOSE = "com.tencent.mm:id/hd";//红包详情，后退按钮

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        parents = new ArrayList<>();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        int eventType = event.getEventType();
        switch (eventType) {
            //当通知栏发生改变时
            case AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED://2048
                KLog.e("发生事件，窗口内容改变");
                String pubclassName = event.getClassName().toString();
                KLog.e("有2048事件---->" + auto + " " + pubclassName);
                if (!auto && pubclassName.equals("android.widget.TextView") && ALL) {
                    KLog.e("有2048事件被识别---->" + auto + " " + pubclassName);
                    getLastPacket(1);
                }
                if (auto && WXMAIN) {
                    getLastPacket();
                    auto = false;
                }
                break;
            case AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED://64
                KLog.e("发生事件，通知栏状态改变");
                List<CharSequence> texts = event.getText();
                if (!texts.isEmpty()) {
                    for (CharSequence text : texts) {
                        String content = text.toString();
                        KLog.e("youtongzhi" + content);
                        if (content.contains("微信红包")) {
                            if (event.getParcelableData() != null && event.getParcelableData() instanceof Notification) {
                                Notification notification = (Notification) event.getParcelableData();
                                PendingIntent pendingIntent = notification.contentIntent;
                                try {
                                    auto = true;
                                    wakeAndUnlock2(true);
                                    pendingIntent.send();
                                    KLog.e("进入微信" + auto + event.getClassName().toString());
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                }
                break;
            //当窗口的状态发生改变时
            case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED://32
                String className = event.getClassName().toString();
                KLog.e("发生事件，窗口状态改变---》" + className);
                switch (className) {
                    case "com.tencent.mm.ui.LauncherUI":
                        //点击最后一个红包
                        KLog.e("点击红包");
                        if (auto)
                            getLastPacket();
                        auto = false;
                        WXMAIN = true;
                        break;
                    case "com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyReceiveUI":
                        //开红包6.5.3 be_
                        // 6.3.32bdh
                        KLog.e("开红包--->" + className);//開
                        click("com.tencent.mm:id/bdh");//
                        auto = false;
                        WXMAIN = false;
                        break;
                    case "com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyDetailUI"://红包详情
                        //退出红包
                        KLog.e("退出红包");
                        click(HB_DETAIL_CLOSE);//gq---->hd
                        WXMAIN = false;
                        break;
                    case "com.tencent.mm.plugin.luckymoney.ui.En_fba4b94f":
                        KLog.e("开红包---->" + className);
                        click(HB_OPEN);//bdh--->bnr
                        auto = false;
                        WXMAIN = false;
                        break;
                    default:
                        KLog.e("未处理事件----->" + eventType);
                        WXMAIN = false;
                        break;
                }
                break;
            default:
                KLog.e("检测到处理事件----》" + eventType);
                break;
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void click(String clickId) {
        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
        if (nodeInfo != null) {
            List<AccessibilityNodeInfo> list = nodeInfo.findAccessibilityNodeInfosByViewId(clickId);
            KLog.e("控件未找到---》" + clickId + " " + list.size());
            for (AccessibilityNodeInfo item : list) {
                item.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            }
        }
    }

    private void clickByText(String text) {
        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
        if (nodeInfo != null) {
//            List<AccessibilityNodeInfo> list = nodeInfo.findAccessibilityNodeInfosByViewId(clickId);
            List<AccessibilityNodeInfo> list = nodeInfo.findAccessibilityNodeInfosByText(text);
            for (AccessibilityNodeInfo item : list) {
                item.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            }
        }
    }

    private void getLastPacket() {
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        recycle(rootNode);
        KLog.e("当前页面红包数老方法--->" + parents.size());
        if (parents.size() > 0) {
            parents.get(parents.size() - 1).performAction(AccessibilityNodeInfo.ACTION_CLICK);
            lastbagnum = parents.size();
            parents.clear();
        }
    }

    private void getLastPacket(int c) {
        KLog.e("新方法--->" + parents.size());
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        recycle(rootNode);
        KLog.e("last++--->" + lastbagnum + "，当前页面红包数--->" + parents.size());
        if (parents.size() > 0 && WXMAIN) {
            KLog.e("页面大于O且在微信界面");
            if (lastbagnum < parents.size())
                parents.get(parents.size() - 1).performAction(AccessibilityNodeInfo.ACTION_CLICK);
            lastbagnum = parents.size();
            parents.clear();
        }
    }

    /**
     * 遍历所有可点击元素
     *
     * @param info
     */
    public void recycle(AccessibilityNodeInfo info) {
        int childCount = info.getChildCount();
        if (childCount == 0) {
            if (info.getText() != null) {
                CharSequence className = info.getClassName();
                String string = info.getText().toString();
//                KLog.e(string);
                if ("领取红包".equals(string)) {
                    //这里有一个问题需要注意，就是需要找到一个可以点击的View
                    KLog.e("Click" + ",isClick:" + info.isClickable());
                    if (info.isClickable()) {
                        info.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    }
                    AccessibilityNodeInfo parent = info.getParent();
                    while (parent != null) {
                        KLog.e("parent isClick:" + parent.isClickable());
                        if (parent.isClickable()) {
                            parents.add(parent);
                            break;
                        }
                        parent = parent.getParent();
                    }
                }
            }
        } else {
            for (int i = 0; i < childCount; i++) {
                AccessibilityNodeInfo accessibilityNodeInfo = info.getChild(i);
                if (accessibilityNodeInfo != null) {
                    recycle(accessibilityNodeInfo);
                }
            }
        }
    }

    private void wakeAndUnlock2(boolean b) {
        if (b) {
            //获取电源管理器对象
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            //获取PowerManager.WakeLock对象，后面的参数|表示同时传入两个值，最后的是调试用的Tag
            wakeLock = pm.newWakeLock(PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "bright");
            //点亮屏幕
            wakeLock.acquire();
            //得到键盘锁管理器对象
            KeyguardManager km = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
            keyguardLock = km.newKeyguardLock("unLock");
            //解锁
            keyguardLock.disableKeyguard();
        } else {
            //锁屏
            keyguardLock.reenableKeyguard();
            //释放wakeLock，关灯
            wakeLock.release();
        }
    }

    @Override
    public void onInterrupt() {

    }
}
