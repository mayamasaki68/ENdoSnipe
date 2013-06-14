/*******************************************************************************
 * ENdoSnipe 5.0 - (https://github.com/endosnipe)
 * 
 * The MIT License (MIT)
 * 
 * Copyright (c) 2012 Acroquest Technology Co.,Ltd.
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 ******************************************************************************/
package jp.co.acroquest.endosnipe.javelin.converter.concurrent.monitor;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import jp.co.acroquest.endosnipe.common.config.JavelinConfig;
import jp.co.acroquest.endosnipe.common.event.EventConstants;
import jp.co.acroquest.endosnipe.common.logger.SystemLogger;
import jp.co.acroquest.endosnipe.javelin.CallTree;
import jp.co.acroquest.endosnipe.javelin.CallTreeRecorder;
import jp.co.acroquest.endosnipe.javelin.StatsJavelinRecorder;
import jp.co.acroquest.endosnipe.javelin.converter.leak.monitor.CollectionMonitor;
import jp.co.acroquest.endosnipe.javelin.event.CommonEvent;
import jp.co.acroquest.endosnipe.javelin.util.StatsUtil;
import jp.co.acroquest.endosnipe.javelin.util.ThreadUtil;

/**
 * 複数スレッドの同時アクセスを監視するクラス
 * 
 * @author fujii
 */
public class ConcurrentAccessMonitor
{
    /** ClassLoader.checkPackageAccess */
    private static final String CLASSLOADER_CHECKPACKAGEACCESS =
            "java.lang.ClassLoader.checkPackageAccess";

    /** スレッド情報取得用MXBean。 */
    private static ThreadMXBean threadMBean__ = ManagementFactory.getThreadMXBean();

    /** Finalizerスレッドの名称。 */
    private static final String FINALIZER_NAME = "Finalizer";

    /** 複数スレッドによるアクセスの判定中かどうかを表すフラグ */
    private static ThreadLocal<Boolean> isTracing__;

    /** アクセス中のHashMapを登録する。 */
    private static Map<String, Object> accessMap__;

    /** アクセス中のHashMapを登録する。 */
    private static Map<String, AtomicInteger> countMap__;

    /** Javelinの設定値 */
    private static JavelinConfig config__ = new JavelinConfig();

    /** java.lang.management.ThreadMXBean#getThreadInfo */
    private static Method threadInfoMethod__ = null;

    /** java.lang.management.ThreadInfo#getLockedSynchronizers */
    private static Method lockedSynchronizersMethod__ = null;

    /** java.lang.management.LockInfo#getClassName */
    private static Method monitorClassNameMethod__ = null;

    /** java.lang.management.LockInfo#getIdentityHashCode */
    private static Method monitorHashCodeMethod__ = null;

    /** 監視対象のオブジェクト管理クラス。 */
    private static ConcurrentAccessRegistry registry__ = new ConcurrentAccessRegistry();

    static
    {
        accessMap__ = Collections.synchronizedMap(new HashMap<String, Object>());
        countMap__ = Collections.synchronizedMap(new HashMap<String, AtomicInteger>());

        isTracing__ = new ThreadLocal<Boolean>() {
            protected Boolean initialValue()
            {
                return Boolean.TRUE;
            }
        };

        // Java6でのロック情報取得用のメソッドを初期化する。
        try
        {
            threadInfoMethod__ =
                    ThreadMXBean.class.getDeclaredMethod("getThreadInfo", long[].class,
                                                         boolean.class, boolean.class);
            lockedSynchronizersMethod__ =
                    ThreadInfo.class.getDeclaredMethod("getLockedSynchronizers");

            Class<?> monitorInfo = Class.forName("java.lang.management.LockInfo");
            monitorClassNameMethod__ = monitorInfo.getDeclaredMethod("getClassName");
            monitorHashCodeMethod__ = monitorInfo.getDeclaredMethod("getIdentityHashCode");
        }
        catch (Exception ex)
        {
            // 無視する。
            SystemLogger.getInstance().debug(ex);
        }
    }

    /**
     * プライベートコンストラクタ
     */
    private ConcurrentAccessMonitor()
    {
        // Do Nothing.
    }

    /**
     * メソッド呼び出し前の処理
     * @param obj 監視するオブジェクト
     */
    public static void postProcess(final Object obj)
    {
        if (threadInfoMethod__ == null)
        {
            return;
        }

        CallTreeRecorder recorder = CallTreeRecorder.getInstance();
        CallTree         tree     = recorder.getCallTree();
        
        if (tree.isConcurrentMonitorEnabled() == false)
        {
            return;
        }

        if (isTracing__.get().booleanValue() == false)
        {
            return;
        }

        setTracing(Boolean.FALSE);

        synchronized (obj)
        {
            String identifier = StatsUtil.createIdentifier(obj);
            AtomicInteger integer = countMap__.get(identifier);
            if (integer == null || integer.decrementAndGet() == 0)
            {
                accessMap__.remove(identifier);
                countMap__.remove(identifier);
            }
        }

        setTracing(Boolean.TRUE);
    }

    /**
     * メソッド呼び出し後の処理
     * @param obj 監視するオブジェクト
     */
    public static void preProcess(final Object obj)
    {
        if (threadInfoMethod__ == null)
        {
            return;
        }

        CallTreeRecorder recorder = CallTreeRecorder.getInstance();
        CallTree         tree     = recorder.getCallTree();
        
        if (tree.isConcurrentMonitorEnabled() == false)
        {
            return;
        }
        
        if (isTracing__.get().booleanValue() == false)
        {
            return;
        }

        Boolean prev = CollectionMonitor.isTracing();
        CollectionMonitor.setTracing(Boolean.FALSE);
        setTracing(Boolean.FALSE);
        synchronized (obj)
        {
            String identifier = StatsUtil.createIdentifier(obj);
            checkConcurrentAccess(identifier, obj);
            accessMap__.put(identifier, obj);
            AtomicInteger integer = countMap__.get(identifier);
            if (integer == null)
            {
                countMap__.put(identifier, new AtomicInteger(1));
            }
            else
            {
                integer.incrementAndGet();
            }
        }
        setTracing(Boolean.TRUE);
        CollectionMonitor.setTracing(prev);
    }

    /**
     * モニタが処理中かどうかを返します。<br />
     *
     * @return 処理中の場合は <code>true</code>
     */
    public static Boolean isTracing()
    {
        return isTracing__.get();
    }

    /**
     * モニタを行うかどうかを設定する。
     * 
     * @param isTracing モニタを行うかどうか。
     */
    public static void setTracing(Boolean isTracing)
    {
        isTracing__.set(isTracing);
    }

    /**
     * 対象とするオブジェクトに対し、以下の条件を満たしているかどうかを判定し、
     * 満たしている場合にはEventとして通知する。
     * 
     * <ol>
     * <li>複数スレッドによるアクセスを行っている。</li>
     * <li>アクセス時にロックしているオブジェクトに共通部が無い。</li>
     * </ol>
     * 
     * @param identifier 識別子。
     * @param obj オブジェクト
     */
    private static void checkConcurrentAccess(String identifier, Object obj)
    {
        String threadName = Thread.currentThread().getName();
        if (FINALIZER_NAME.equals(threadName))
        {
            return;
        }

        // 現在アクセスしているスレッドの情報を取得する。
        long threadId = ThreadUtil.getThreadId();
        ConcurrentMonitorObject entry = registry__.get(obj);
        if (entry == null || entry.getRef() != obj)
        {
            ConcurrentMonitorObject newEntry = new ConcurrentMonitorObject(obj, identifier);
            registry__.add(newEntry);
            newEntry.setPrevThreadId(threadId);
            return;
        }

        // 現在のスレッドが既に検出済みのものであれば、何もしない。
        if (entry.containsThreadId(threadId) || entry.getPrevThreadId() == threadId)
        {
            return;
        }

        // 非同期複数スレッドアクセスを行っていない場合は、何もしない。
        boolean concurrentAccess = isConcurrentAccess(threadId, entry);
        if (concurrentAccess == false)
        {
            return;
        }

        boolean isError = false;
        if (accessMap__.containsKey(identifier))
        {
            Object oldObj = accessMap__.get(identifier);
            if (obj == oldObj)
            {
                isError = true;
            }
        }

        // トレースログに非同期複数スレッドアクセス検出を通知する。
        CommonEvent event = createDetectedEvent(entry, isError);

        if (event != null)
        {
            StatsJavelinRecorder.addEvent(event);
        }
        else
        {
            registry__.remove(obj);
        }
    }

    /**
     * 非同期複数スレッドアクセスを行っているかどうかを判定する。
     * 
     * @param threadId 
     * @param obj 対象のオブジェクト。
     * @return　非同期複数スレッドアクセスを行っているかどうか。
     */
    private static boolean isConcurrentAccess(long threadId, ConcurrentMonitorObject entry)
    {
        // ロック情報取得用のAPIが存在しなければ、非同期アクセスと見なさない。
        List<String> lockedObjectList;
        if (threadInfoMethod__ == null)
        {
            return false;
        }
        else
        {
            lockedObjectList = createLockObjectList();
        }

        // 初めての場合は非同期アクセスとみなさない。
        List<String> prevList = entry.getLockedObjectList();
        if (prevList == null)
        {
            entry.setLockedObjectList(lockedObjectList);
            entry.setPrevThreadId(threadId);
            addTrace(threadId, entry, lockedObjectList);
            return false;
        }

        // ロックされていない場合は、非同期アクセスと見なす。
        int lockCount = lockedObjectList.size();
        if (lockCount == 0 || prevList.size() == 0)
        {
            entry.setLockedObjectList(lockedObjectList);
            addTrace(threadId, entry, lockedObjectList);
            return true;
        }

        // ロックの共通部分のみを残す。
        prevList.retainAll(lockedObjectList);

        // ロックの共通部分が無い場合は、非同期アクセスと見なす。
        if (prevList.size() == 0)
        {
            addTrace(threadId, entry, lockedObjectList);
            return true;
        }

        return false;
    }

    /**
     * ロックオブジェクトを取得する。
     * Java 6.0でのみ実行可能である。
     * 
     * @return ロックオブジェクトの一覧。
     */
    private static List<String> createLockObjectList()
    {
        List<String> lockedObjectList = new CopyOnWriteArrayList<String>();
        ThreadInfo[] threadInfo;
        try
        {
            // TODO: 性能に大きな影響があるため、getLockedSynchronizersを呼ぶ処理は無効としている。
            //       Ver4.1では、getLockedSynchronizersを呼ぶコードを有効にし、設定によりON/OFF出来るようにする。
            threadInfo =
                    (ThreadInfo[])threadInfoMethod__.invoke(threadMBean__,
                                                            new long[]{ThreadUtil.getThreadId()},
                                                            true, false);

            Object[] lockedMonitors = (Object[])lockedSynchronizersMethod__.invoke(threadInfo[0]);
            addLockObject(lockedObjectList, lockedMonitors);

          // Object[] lockedSynchronizers = (Object[])lockedMonitorsMethod__.invoke(threadInfo[0]);
          // addLockObject(lockedObjectList, lockedSynchronizers);

        }
        catch (Exception ex)
        {
            SystemLogger.getInstance().warn(ex);
        }
        return lockedObjectList;
    }

    /**
     * ロックオブジェクトのリストにロックオブジェクトを追加する。
     * 
     * @param lockedObjectList ロックオブジェクトのリスト。
     * @param lockedMonitors ロックオブジェクト。
     * @throws IllegalAccessException　メソッド実行時のアクセスエラー。
     * @throws InvocationTargetException メソッド実行時の例外。
     */
    private static void addLockObject(List<String> lockedObjectList, Object[] lockedMonitors)
        throws IllegalAccessException,
            InvocationTargetException
    {
        for (Object monitorInfo : lockedMonitors)
        {
            Object className = monitorClassNameMethod__.invoke(monitorInfo);
            Integer hashCode = (Integer)monitorHashCodeMethod__.invoke(monitorInfo);
            lockedObjectList.add(className + "@" + Integer.toHexString(hashCode));
        }
    }

    /** 
     * トレースを追加する。
     * 
     * @param threadId スレッドID。 
     * @param entry エントリ。
     */
    private static void addTrace(long threadId, ConcurrentMonitorObject entry,
            List<String> lockedObjectList)
    {
        StackTraceElement[] stacktraces = ThreadUtil.getCurrentStackTrace();
        String stackTrace = ThreadUtil.getStackTrace(stacktraces, config__.getTraceDepth());

        entry.addTrace(threadId, stackTrace, lockedObjectList);
    }

    /**
     * 複数スレッド検出Eventを出力する。
     * 
     * @param entry オブジェクトへのアクセス情報。
     * @param isError 問題そのものを検出したかどうか。
     * 
     * @return 複数スレッド検出Event。
     */
    private static CommonEvent createDetectedEvent(ConcurrentMonitorObject entry, boolean isError)
    {
        String identifier = entry.getIdentifier();

        CommonEvent event = new CommonEvent();
        event.setName(EventConstants.NAME_CONCURRENT_ACCESS);
        event.addParam(EventConstants.PARAM_CONCURRENT_IDENTIFIER, identifier);

        if (isError)
        {
            event.setLevel(CommonEvent.LEVEL_ERROR);
        }
        else
        {
            event.setLevel(CommonEvent.LEVEL_INFO);
        }

        List<ConcurrentMonitorItem> itemList = entry.getItemMap();
        int count = 0;
        for (ConcurrentMonitorItem item : itemList)
        {
            String stackTrace = item.getStackTrace();
            // ClassLoader.checkPackageAccess内部のHashMapはJavaのsynchronizedとは別の仕組みで同期されるため、無視する。

            if (stackTrace.contains(CLASSLOADER_CHECKPACKAGEACCESS))
            {
                return null;
            }
            event.addParam(EventConstants.PARAM_CONCURRENT_THREAD + "." + count,
                           item.getThreadName());
            event.addParam(EventConstants.PARAM_CONCURRENT_TIME + "." + count, "" + item.getTime());
            event.addParam(EventConstants.PARAM_CONCURRENT_LOCK + "." + count, ""
                    + item.getLockedObjectList());
            event.addParam(EventConstants.PARAM_CONCURRENT_STACKTRACE + "." + count, stackTrace);
            count++;
        }

        return event;
    }
}
