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
package jp.co.acroquest.endosnipe.javelin.converter.hadoop;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import jp.co.acroquest.endosnipe.common.config.JavelinConfig;
import jp.co.acroquest.endosnipe.common.event.EventConstants;
import jp.co.acroquest.endosnipe.common.logger.SystemLogger;
import jp.co.acroquest.endosnipe.javelin.CallTree;
import jp.co.acroquest.endosnipe.javelin.CallTreeNode;
import jp.co.acroquest.endosnipe.javelin.CallTreeRecorder;
import jp.co.acroquest.endosnipe.javelin.MBeanManager;
import jp.co.acroquest.endosnipe.javelin.RecordStrategy;
import jp.co.acroquest.endosnipe.javelin.RootInvocationManager;
import jp.co.acroquest.endosnipe.javelin.VMStatus;
import jp.co.acroquest.endosnipe.javelin.bean.Component;
import jp.co.acroquest.endosnipe.javelin.bean.ExcludeMonitor;
import jp.co.acroquest.endosnipe.javelin.bean.Invocation;
import jp.co.acroquest.endosnipe.javelin.bean.TripleState;
import jp.co.acroquest.endosnipe.javelin.communicate.AlarmListener;
import jp.co.acroquest.endosnipe.javelin.converter.hadoop.HadoopObjectAnalyzer.HadoopJobStatus;
import jp.co.acroquest.endosnipe.javelin.converter.leak.monitor.CallTreeNodeMonitor;
import jp.co.acroquest.endosnipe.javelin.converter.util.ConverterUtil;
import jp.co.acroquest.endosnipe.javelin.event.CommonEvent;
import jp.co.acroquest.endosnipe.javelin.event.EventRepository;
import jp.co.acroquest.endosnipe.javelin.event.InvocationFullEvent;
import jp.co.acroquest.endosnipe.javelin.event.JavelinEventCounter;
import jp.co.acroquest.endosnipe.javelin.helper.VMStatusHelper;
import jp.co.acroquest.endosnipe.javelin.log.JavelinFileGenerator;
import jp.co.acroquest.endosnipe.javelin.log.JavelinLogCallback;
import jp.co.acroquest.endosnipe.javelin.record.AllRecordStrategy;
import jp.co.acroquest.endosnipe.javelin.record.JvnFileNotifyCallback;
import jp.co.acroquest.endosnipe.javelin.util.ThreadUtil;

/**
 * Hadoop用ログを記録する
 * @author asazuma
 *
 */
// TODO JobTrackerクラスのgetJobStatus()、submitJob()、heartbeat()のみ監視することを想定
public class HadoopRecorder
{
    /** アラームリスナのリスト */
    private static final List<AlarmListener> ALARM_LISTENER_LIST = new ArrayList<AlarmListener>();

    /** 初期化判定フラグ */
    private static boolean                 initialized__;

    /** Javelinログ出力クラス */
    private static JavelinFileGenerator    generator__;

    /** 記録条件判定クラス */
    private static RecordStrategy          recordStrategy__;

    /** javelinの設定ファイル */
    private static JavelinConfig           config__
                                               = new JavelinConfig();

    /** クラス名の省略化フラグ */
    private static boolean                 isSimplification__ = false;

    /** VM状態の取得と記録クラス */
    private static VMStatusHelper          vmStatusHelper__
                                               = new VMStatusHelper();

    /** イベントの重複をチェックするためのリポジトリ */
    private static EventRepository         eventRepository__
                                               = new EventRepository();

    /** 実行中のジョブのリスト */
    private static List<String>            runningJobList__
                                               = Collections.synchronizedList(new ArrayList<String>());

    static
    {
        isSimplification__ = config__.isClassNameSimplify();
    }

    /**
     * コンストラクタを隠ぺいしてインスタンス化させない
     */
    private HadoopRecorder()
    {
        // 何もしない
    }

    /**
     * 初期化されているかを返す。
     *
     * @return true:初期化されている、false:初期化されていない.
     */
    public static boolean isInitialized()
    {
        return initialized__;
    }

    /**
     * 設定クラスを設定する。
     *
     * @param config コンフィグ
     */
    public static void setJavelinConfig(final JavelinConfig config)
    {
        config__ = config;
    }

    /**
     * スレッドのIDを設定する
     *
     * @param threadId スレッドID
     */
    public static void setThreadid(final String threadId)
    {
        CallTree callTree = CallTreeRecorder.getInstance().getCallTree();
        callTree.setThreadID(threadId);
    }

    /**
     * 初期化処理。 AlarmListenerの登録を行う。 RecordStrategyを初期化する。
     * MBeanServerへのContainerMBeanの登録を行う。
     * 公開用HTTPポートが指定されていた場合は、HttpAdaptorの生成と登録も行う。
     *
     * @param config パラメータの設定値を保存するオブジェクト
     */
    public static void javelinInit(final JavelinConfig config)
    {
        if (initialized__ == true) return ;

        try
        {
            // エラーロガー初期化
            SystemLogger.initSystemLog(config);

            // ログ出力クラスのインスタンス生成
            generator__ = new JavelinFileGenerator(config);

            // AlarmListenerを登録
            registerAlarmListeners(config);

            // 記録条件判定クラス初期化
            String strategyName = config.getRecordStrategy();
            try
            {   recordStrategy__ = (RecordStrategy)loadClass(strategyName).newInstance();
            }
            catch (ClassNotFoundException cfne)
            {
             // 指定されたクラスが無ければデフォルトのクラスを使用
                String defaultRecordstrategy = JavelinConfig.DEFAULT_RECORDSTRATEGY;
                SystemLogger.getInstance().info("Failed to load " + strategyName
                                                + ". Use default value "
                                                + defaultRecordstrategy
                                                + " as javelin.recordStrategy.");

                recordStrategy__ =
                    (RecordStrategy)loadClass(defaultRecordstrategy).newInstance();
            }

            // スレッドの監視を開始
            vmStatusHelper__.init();

            initialized__ = true;
        }
        catch (Exception ex)
        {
            SystemLogger.getInstance().warn(ex);
        }
    }

    /**
     * AlarmListenerのクラスをJavelin設定から読み込み、登録する。<br />
     * クラスのロードは、以下の順でクラスローダでのロードを試みる。
     *
     * <ol>
     * <li>HadoopRecorderをロードしたクラスローダ</li>
     * <li>コンテキストクラスローダ</li>
     * </ol>
     *
     * @param config パラメータの設定値を保存するオブジェクト
     */
    private static void registerAlarmListeners(final JavelinConfig config)
    {
        String[] alarmListeners = config.getAlarmListeners().split(".");
        for (String alarmListenerName : alarmListeners)
        {
            try
            {
                if ("".equals(alarmListenerName)) continue;

                // alarmListenerNameからインスタンスを取得
                Class<?> alarmListenerClass = loadClass(alarmListenerName);
                Object listener = alarmListenerClass.newInstance();
                if (listener instanceof AlarmListener)
                {
                    // 取得したインスタンスがAlarmListenerインターフェイスを実装していたら登録
                    AlarmListener alarmListener = (AlarmListener)listener;
                    alarmListener.init();
                    addListener(alarmListener);
                    String message = "Register " + alarmListenerName + "for AlarmListener.";
                    SystemLogger.getInstance().debug(message);
                }
                else
                {
                    String message = alarmListenerName +
                        " is not used for sending alarms because it doesn't implement AlarmListener.";
                    SystemLogger.getInstance().info(message);
                }
            }
            catch (Exception ex)
            {
                SystemLogger.getInstance().warn(
                    alarmListenerName +
                        " is not used for sending alarms because of failing to be registered.",
                    ex);
            }
        }
    }

    /**
     * クラスをロードする。 以下の順でクラスローダでのロードを試みる。
     * <ol>
     * <li>HadoopRecorderをロードしたクラスローダ</li>
     * <li>コンテキストクラスローダ</li>
     * </ol>
     *
     * @param className ロードするクラスの名前。
     * @return ロードしたクラス。
     * @throws ClassNotFoundException 全てのクラスローダでクラスが見つからない場合
     */
    private static Class<?> loadClass(final String className)
        throws ClassNotFoundException
    {
        Class<?> clazz;
        try
        {
            clazz = Class.forName(className);
        }
        catch (ClassNotFoundException cnfe)
        {
            SystemLogger.getInstance().info(
                "Load classes from context class loader because of failing to load "
                + className + ".");
            clazz = Thread.currentThread().getContextClassLoader().loadClass(className);
        }
        return clazz;
    }

    /**
     * Alarm通知に利用するAlarmListenerを登録する
     *
     * @param alarmListener Alarm通知に利用するAlarmListener
     */
    private static void addListener(final AlarmListener alarmListener)
    {
        synchronized (ALARM_LISTENER_LIST)
        {
            ALARM_LISTENER_LIST.add(alarmListener);
        }
    }

    /**
     * 前処理。
     *
     * @param className クラス名
     * @param methodName メソッド名
     * @param args 引数
     * @param thisObject thisオブジェクト
     */
    public static void preProcess(String className, String methodName, final Object[] args)
    {
        try
        {
            if (isSimplification__)
            {
                className = ConverterUtil.toSimpleName(className);
                methodName = ConverterUtil.toSimpleName(methodName);
            }

            preProcess(className, methodName, args, true);
        }
        catch (Throwable th)
        {
            SystemLogger.getInstance().warn(th);
        }
    }

    /**
     * JavelinRecorder, JDBCJavelinRecorderから呼び出したときの前処理。
     * @param className クラス名
     * @param methodName メソッド名
     * @param args 引数
     * @param doExcludeProcess 除外対象処理を行うかどうか
     * @param thisObject thisオブジェクト
     */
    private static void preProcess(final String className, final String methodName,
            final Object[] args, final boolean doExcludeProcess)
    {
        // スタックトレース
        StackTraceElement[] stacktrace = null;
        if (config__.isLogStacktrace())
        {
            stacktrace = ThreadUtil.getCurrentStackTrace();
        }
        preProcess(null, null, className, methodName, args, stacktrace, doExcludeProcess, false);
    }


    /**
     * 前処理。
     * @param component コンポーネント
     * @param invocation {@link Invocation}オブジェクト
     * @param className  クラス名
     * @param methodName メソッド名
     * @param args 引数
     * @param stacktrace スタックトレース
     * @param doExcludeProcess 除外対象処理を行うかどうか
     * @param isResponse デフォルトでレスポンスを記録するかどうか。
     * @param thisObject thisオブジェクト
     */
    private static void preProcess(Component component, Invocation invocation,
            final String className, final String methodName, final Object[] args,
            final StackTraceElement[] stacktrace, final boolean doExcludeProcess, final boolean isResponse)
    {
        synchronized (HadoopRecorder.class)
        {
            // 初期化処理
            if (initialized__ == false)
            {
                javelinInit(config__);
            }
        }

        CallTreeRecorder callTreeRecorder = CallTreeRecorder.getInstance();

        // Javelinのログ出力処理が呼び出されている場合、処理を行わない
        if (callTreeRecorder.isRecordMethodCalled())
        {
            return;
        }

        // Javelinのログ出力処理呼び出しステータスをセット
        callTreeRecorder.setRecordMethodCalled(true);

        try
        {
            boolean isRecorded = recordPreInvocation(component, invocation, className, methodName,
                                                     args, stacktrace, doExcludeProcess,
                                                     isResponse, callTreeRecorder);

            if (isRecorded)
            {
                // 有効だったnodeの深さを保存する。
                callTreeRecorder.getCallTree().addDepth(callTreeRecorder.getDepth());
            }
        }
        finally
        {
            // Javelinのログ出力処理呼び出しステータスを解除
            callTreeRecorder.setRecordMethodCalled(false);
            callTreeRecorder.setDepth(callTreeRecorder.getDepth() + 1);
        }
    }

    /**
     * 前処理。
     * @param component コンポーネント
     * @param invocation {@link Invocation}オブジェクト
     * @param className  クラス名
     * @param methodName メソッド名
     * @param args 引数
     * @param stacktrace スタックトレース
     * @param doExcludeProcess 除外対象処理を行うかどうか
     * @param isResponse デフォルトでレスポンスを記録するかどうか。
     * @param callTreeRecorder コールツリー
     * @param thisObject thisオブジェクト
     *
     * @return 記録したかどうか
     */
    private static boolean recordPreInvocation(Component component, Invocation invocation,
            final String className, final String methodName, final Object[] args,
            final StackTraceElement[] stacktrace, final boolean doExcludeProcess,
            final boolean isResponse, CallTreeRecorder callTreeRecorder)
    {

        // 引数のcomponentとinvocationは必ずNULL
        component = MBeanManager.getComponent(className);
        invocation = getInvocation(component, methodName);

        boolean isExclude = ExcludeMonitor.isExclude(invocation);
        if (doExcludeProcess == true && isExclude == true)
        {
            return false;
        }

        // パラメータの解析後、CallTreeNodeを登録して終了
        if (methodName.equals("heartbeat"))
        {
            return recordPreHeartbeat(callTreeRecorder, component,
                                      invocation, className, methodName,
                                      args, stacktrace, isResponse);
        }
        else if (methodName.equals("submitJob"))
        {
            return recordPreSubmitJob(callTreeRecorder, component,
                                      invocation, className, methodName,
                                      args, stacktrace, isResponse);
        }
        // 上記のメソッド以外はエラー
        return false;
    }

    /**
     * コールツリーを初期化する。
     *
     * @param callTree callTree
     * @param methodName メソッド名
     * @param callTreeRecorder {@link CallTreeRecorder}オブジェクト
     */
    private static void initCallTree(CallTree callTree,
                                     final String methodName,
                                     CallTreeRecorder callTreeRecorder)
    {
        // 初回呼び出し時はコールツリーを初期化する。
        callTree.clearDepth();

        callTree.setRootCallerName(config__.getRootCallerName());
        callTree.setEndCalleeName(config__.getEndCalleeName());

        String threadId = createThreadId(methodName, callTreeRecorder);
        if (threadId != null)
        {
            callTree.setThreadID(threadId);
        }
    }

    /**
     * スレッドIDを生成する。
     *
     * @param methodName メソッド名。
     *
     * @return スレッドID。
     */
    private static String createThreadId(final String methodName,
                                         final CallTreeRecorder callTreeRecorder)
    {
        String threadId = null;
        switch (config__.getThreadModel())
        {
        case JavelinConfig.TM_THREAD_ID:
            threadId = "" + callTreeRecorder.getThreadId();
            break;
        case JavelinConfig.TM_THREAD_NAME:
            threadId = Thread.currentThread().getName();
            break;
        case JavelinConfig.TM_CONTEXT_PATH:
            threadId = methodName;
            break;
        default:
            break;
        }
        return threadId;
    }

    /**
     * Invocationを取得する。
     *
     * @param component クラス名
     * @param methodName メソッド名
     *
     * @return {@link Invocation}オブジェクト
     */
    private static Invocation getInvocation(final Component component,
                                           final String methodName)
    {
        if (component == null)
        {
            return null;
        }

        Invocation invocation = component.getInvocation(methodName);
        return invocation;
    }

    /**
     * Invocationを登録する。
     *
     * @param component コンポーネント
     * @param className クラス名
     * @param methodName メソッド名
     * @param isResponse レスポンス
     *
     * @return 登録した{@link Invocation}オブジェクト
     */
    private static Invocation registerInvocation(Component component,
                                                final String className,
                                                final String methodName,
                                                boolean isResponse)
    {
        if (component == null)
        {
            try
            {
                // コンポーネントをクラス名から生成
                component = new Component(className);

                MBeanManager.setComponent(className, component);
            }
            catch (NullPointerException ex)
            {
                SystemLogger.getInstance().warn(ex);
            }
        }

        Invocation invocation = component.getInvocation(methodName);

        if (invocation == null)
        {
            invocation = registerInvocation(component, methodName, isResponse);
        }
        return invocation;
    }


    /**
     * {@link Invocation}オブジェクトをコンポーネントに登録します。
     *
     * @param component 登録対象コンポーネント
     * @param methodName 登録するメソッド
     * @param isResponse レスポンスグラフに表示する場合は <code>true</code>
     *
     * @return 登録した Invocation
     */
    private static Invocation registerInvocation(final Component component,
                                                final String methodName,
                                                final boolean isResponse)
    {
        String className = component.getClassName();
        Invocation invocation;
        int recordedInvocationNum = component.getRecordedInvocationNum();

        String processName = VMStatusHelper.getProcessName();
        invocation =
                     new Invocation(processName, className, methodName,
                                    Invocation.THRESHOLD_NOT_SPECIFIED);

        // Invocationの数が最大値に達しており、かつInvocationFullEventを送信する設定の場合、
        // InvocationFullEvent送信の処理を行う。
        if (config__.getSendInvocationFullEvent() == true
                && recordedInvocationNum >= config__.getRecordInvocationMax())
        {
            Invocation removedInvoction = component.addAndDeleteOldestInvocation(invocation);
            sendInvocationFullEvent(component, className, recordedInvocationNum, invocation,
                                    removedInvoction);
        }
        else
        {
            component.addInvocation(invocation);
        }

        if (isResponse)
        {
            invocation.setResponseGraphOutput(TripleState.ON);
            RootInvocationManager.addRootInvocation(invocation);
        }
        return invocation;
    }

    /**
     * VM状態を取得する。
     *
     * @param parent 呼び出し元
     * @param newNode 呼び出し先
     * @param callTreeRecorder {@link CallTreeRecorder}オブジェクト
     *
     * @return
     */
    private static VMStatus createVMStatus(CallTreeNode parent,
                                           CallTreeNode newNode,
                                           CallTreeRecorder callTreeRecorder)
    {
        VMStatus vmStatus;

        if (parent == null && config__.isLogMBeanInfoRoot())
        {
            vmStatus = vmStatusHelper__.createVMStatus(callTreeRecorder);
        }
        else if (config__.isLogMBeanInfo())
        {
            vmStatus = vmStatusHelper__.createVMStatus(callTreeRecorder);
        }
        else
        {
            vmStatus = VMStatus.EMPTY_STATUS;
        }

        return vmStatus;
    }

    /**
     * InvocationFullEventを送信する。
     *
     * @param component コンポーネント。
     * @param className クラス名
     * @param invocationNum Invocationの数
     */
    private static void sendInvocationFullEvent(Component component, String className,
            int invocationNum, Invocation addInvocation, Invocation removedInvocation)
    {
        CommonEvent event = new InvocationFullEvent();
        event.addParam(EventConstants.PARAM_INVOCATION, String.valueOf(invocationNum));
        event.addParam(EventConstants.PARAM_INVOCATION_CLASS, className);
        boolean containsEvent = eventRepository__.containsEvent(event);
        if (containsEvent == false)
        {
            if (addInvocation != null)
            {
                event.addParam(EventConstants.PARAM_INVOCATION_METHOD_ADD,
                               addInvocation.getMethodName());
            }
            else
            {
                event.addParam(EventConstants.PARAM_INVOCATION_METHOD_ADD, "");
            }

            if (removedInvocation != null)
            {
                event.addParam(EventConstants.PARAM_INVOCATION_METHOD_REMOVE,
                               removedInvocation.getMethodName());
            }
            else
            {
                event.addParam(EventConstants.PARAM_INVOCATION_METHOD_REMOVE, "");
            }

            String stackTrace = ThreadUtil.getStackTrace(ThreadUtil.getCurrentStackTrace());
            event.addParam(EventConstants.PARAM_INVOCATION_STACKTRACE, stackTrace);
            addEvent(event);
        }
    }

    /**
     * トランザクションを記録する。
     *
     * @param node CallTreeNode
     */
    private static void recordTransaction(final CallTreeNode node)
    {
        if (node.isRecoreded() == false)
        {
            node.setRecoreded(true);

            Invocation invocation = node.getInvocation();
            if (invocation != null)
            {
                long elapsedTime     = node.getAccumulatedTime();
                long elapsedCpuTime  = node.getCpuTime();
                long elapsedUserTime = node.getUserTime();

                elapsedTime     = elapsedTime     - node.getChildrenTime();
                if (elapsedTime < 0)
                {
                    elapsedTime = 0;
                }
                elapsedCpuTime  = elapsedCpuTime  - node.getChildrenCpuTime();
                if (elapsedCpuTime < 0)
                {
                    elapsedCpuTime = 0;
                }
                elapsedUserTime = elapsedUserTime - node.getChildrenUserTime();
                if (elapsedUserTime < 0)
                {
                    elapsedUserTime = 0;
                }

                invocation.addInterval(node, elapsedTime, elapsedCpuTime, elapsedUserTime);

                if (node.getParent() != null)
                {
                    invocation.addCaller(node.getParent().getInvocation());
                }
            }

            List<CallTreeNode> children = node.getChildren();
            int size = children.size();
            for (int index = 0; index < size; index++)
            {
                CallTreeNode child = children.get(index);
                recordTransaction(child);
            }
        }
    }

    /**
     * nodeにイベントを追加します。<br />
     * CallTreeが無い場合は、新規作成します。
     * 必ず発報する。
     *
     * @param event イベント。
     *
     * @return 追加したCallTreeNode。
     */
    private static CallTreeNode addEvent(CommonEvent event)
    {
        return addEvent(event, false, null, 0);
    }

    /**
     * nodeにイベントを追加します。<br />
     * CallTreeが無い場合は、新規作成します。
     * 必ず発報する。
     *
     * @param event イベント。
     * @param clear 既にイベントがある場合も発報する。
     * @param config 設定。
     * @param telegramId 電文 ID
     *
     * @return 追加したNode。
     */
    private static CallTreeNode addEvent(CommonEvent event, boolean clear, JavelinConfig config,
            long telegramId)
    {
        JavelinEventCounter.getInstance().addEvent(event);

        boolean containsEvent = eventRepository__.containsEvent(event);
        if (containsEvent && clear == false)
        {
            return null;
        }

        if (config == null)
        {
            config = new JavelinConfig();
        }

        // イベントの出力設定レベルが、引数で指定したイベントのレベルよりも大きい場合は、
        // イベントを出力しない。
        int outputEventLevel = convertEventLevel(config.getEventLevel());
        if (outputEventLevel > event.getLevel())
        {
            return null;
        }

        eventRepository__.putEvent(event);

        CallTreeRecorder callTreeRecorder = CallTreeRecorder.getInstance();
        CallTreeNode callTreeNode = callTreeRecorder.getCallTreeNode();
        CallTree tree = callTreeRecorder.getCallTree();

        // イベントのレベルがエラーの場合、即座にアラームを上げる。
        if(event.getLevel() >= CommonEvent.LEVEL_ERROR)
        {
            Invocation invocation = null;
            if( callTreeNode != null)
            {
                invocation = callTreeNode.getInvocation();
            }
            sendEventImmediately(event, invocation, callTreeRecorder, telegramId);
            if (tree != null)
            {
                tree.addHighPriorityRecordStrategy("AllRecordStrategy", new AllRecordStrategy());
            }

            return null;
        }

        boolean isNewCallTree = false;
        if (tree == null)
        {
            callTreeRecorder.clearCallTree();
        }
        if (callTreeNode == null)
        {
            isNewCallTree = true;

            CallTreeNode node = createEventNode(event, callTreeRecorder, tree, null);

            tree.addEventNode(node);

            tree.clearDepth();
            tree.addDepth(0);
            callTreeRecorder.setDepth(1);
        }

        CallTree callTree = callTreeRecorder.getCallTree();
        CallTreeNode rootNode = callTree.getRootNode();

        CallTreeNode node = null;
        if (rootNode != null)
        {
            tree.addHighPriorityRecordStrategy("AllRecordStrategy", new AllRecordStrategy());

            node = callTreeNode;
            event.setTime(System.currentTimeMillis());
            node.addEvent(event);
        }
        if (isNewCallTree)
        {
            postProcessCommon(null, null, null, null, telegramId);
        }

        return node;
    }

    /**
     * イベントレベルを文字列から数値に変換します。<br />
     *
     * @param eventLevelStr イベントレベル(文字列)
     * @return イベントレベル(数値)
     */
    private static int convertEventLevel(final String eventLevelStr)
    {
        if ("ERROR".equals(eventLevelStr))
        {
            return CommonEvent.LEVEL_ERROR;
        }
        if ("WARN".equals(eventLevelStr))
        {
            return CommonEvent.LEVEL_WARN;
        }
        if ("INFO".equals(eventLevelStr))
        {
            return CommonEvent.LEVEL_INFO;
        }
        return CommonEvent.LEVEL_WARN;
    }

    /**
     * 即座にイベントを送信する。
     *
     * @param event イベント
     * @param invocation {@link Invocation}オブジェクト
     * @param callTreeRecorder {@link CallTreeRecorder}オブジェクト
     * @param telegramId 電文ID
     */
    private static void sendEventImmediately(CommonEvent event,
                                             Invocation invocation,
                                             CallTreeRecorder callTreeRecorder,
                                             long telegramId)
    {
        CallTree callTree = new CallTree();
        callTree.init();
        CallTreeNode node = createEventNode(event, callTreeRecorder, callTree, invocation);
        callTree.addEventNode(node);
        recordAndAlarmEvents(callTree, null, telegramId);
    }

    /**
     * イベント用のノードを作成する。
     *
     * @param event イベント
     * @param callTreeRecorder {@link CallTreeRecorder}オブジェクト
     * @param tree ツリー
     * @param invocation {@link Invocation}オブジェクト
     *
     * @return イベント用のノード。
     */
    private static CallTreeNode createEventNode(CommonEvent event,
                                                CallTreeRecorder callTreeRecorder,
                                                CallTree tree,
                                                Invocation invocation)
    {
        if (invocation == null)
        {
            String className = config__.getRootCallerName();
            String methodName = "";

            // CallTreeにスレッド名を設定する。
            String threadId = createThreadId(methodName, callTreeRecorder);
            if (threadId != null)
            {
                tree.setThreadID(threadId);
            }

            String processName = VMStatusHelper.getProcessName();
            invocation = new Invocation(processName, className, methodName, 0);
        }

        event.setTime(System.currentTimeMillis());

        CallTreeNode node = new CallTreeNode();
        node.setInvocation(invocation);
        node.addEvent(event);
        return node;
    }

    /**
     * イベントが発生している場合に、 Javelin ログへの出力とアラーム通知を行う。
     *
     * @param callTree コールツリー
     * @param callTreeRecorder {@link CallTreeRecorder}オブジェクト
     * @param telegramId 電文ID
     */
    private static void recordAndAlarmEvents(CallTree callTree,
                                             CallTreeRecorder callTreeRecorder,
                                             long telegramId)
    {
        // CallTreeに対してEventNodeが存在しない状況でEventが発生した場合のみ、
        //  getEventNodeListに要素が追加されている。
        List<CallTreeNode> eventList = callTree.getEventNodeList();
        int size = eventList.size();
        if (size != 0)
        {
            // イベント処理が存在する場合、ログ出力処理と アラーム通知処理を行う。
            for (int num = 0; num < size; num++)
            {
                // Javelinの初期化が済んでいない場合、ログファイル作成とアラーム通知の処理を飛ばす。
                if (generator__ != null)
                {
                    generator__.generateJaveinFile(callTree, eventList.get(num),
                                                   new JvnFileNotifyCallback(), null,
                                                   telegramId);
                    sendEventAlarm();
                }

                // CallTreeに保持されていたNode数を記録する
                int totalNodeCount = callTree.getTotalNodeCount();
                CallTreeNodeMonitor.add(totalNodeCount);

            }

            if (callTreeRecorder != null)
            {
                callTreeRecorder.clearCallTree();
            }
        }
    }

    /**
     * Alarm通知する。
     */
    private static void sendEventAlarm()
    {
        CallTreeNode eventNode = new CallTreeNode();
        String processName = VMStatusHelper.getProcessName();
        Invocation invocation =
                                new Invocation(processName, EventConstants.EVENT_CLASSNAME,
                                               EventConstants.EVENT_METHODNAME, 0);
        eventNode.setInvocation(invocation);
        sendAlarmImpl(eventNode);
    }

    /**
     * Alarm通知する。
     * @param node CallTreeNode
     * @param telegramId 電文 ID
     */
    private static void sendAlarmImpl(final CallTreeNode node)
    {
        synchronized (ALARM_LISTENER_LIST)
        {
            for (AlarmListener alarmListener : ALARM_LISTENER_LIST)
            {
                // ルートノードのみAlarmを送信するAlarmListenerは、
                // 親を持つノードを無視する。
                boolean sendingRootOnly = alarmListener.isSendingRootOnly();
                if (sendingRootOnly == true && node.getParent() != null)
                {
                    continue;
                }

                try
                {
                    // AlarmListenerにはCallTreeNodeをそのまま渡す
                    // →アラーム通知で累積時間を使用するものがある為
                    alarmListener.sendExceedThresholdAlarm(node);
                }
                catch (Throwable ex)
                {
                    SystemLogger.getInstance().warn(ex);
                }
            }
        }
    }

    /**
     * 後処理（本処理成功時）
     *
     * @param className クラス名
     * @param methodName メソッド名
     * @param retValue 戻り値。
     */
    public static void postProcessOK(String className, String methodName, final Object retValue, Object thisObject)
    {
        try
        {
            if (isSimplification__)
            {
                methodName = ConverterUtil.toSimpleName(methodName);
            }

            postProcessCommon(thisObject, methodName, retValue, null, 0);
        }
        catch (Throwable th)
        {
            SystemLogger.getInstance().warn(th);
        }
    }

    /**
     * 後処理（本処理失敗時）。
     *
     * @param className クラス名
     * @param methodName メソッド名
     * @param cause 例外の原因
     */
    public static void postProcessNG(String className,
                                     String methodName,
                                     final Throwable cause,
                                     Object thisObject)
    {
        try
        {
            if (isSimplification__)
            {
                methodName = ConverterUtil.toSimpleName(methodName);
            }
            HadoopRecorder.postProcessCommon(thisObject, methodName, null, cause, 0);
        }
        catch (Throwable th)
        {
            SystemLogger.getInstance().warn(th);
        }
    }

    /**
     * 後処理の共通処理<br />
     * <br />
     * CallTree に情報を格納します。
     * また、必要に応じて Javelin ログ出力とアラーム通知を行います。<br />
     *
     * @param methodName メソッド名
     * @param returnValue 戻り値（ <code>null</code> も可）
     * @param cause 例外発生オブジェクト（ <code>null</code> も可）
     * @param telegramId 電文 ID
     */
    private static void postProcessCommon(final Object thisObject,
                                          final String methodName,
                                          final Object returnValue,
                                          final Throwable cause,
                                          final long telegramId)
    {
        CallTreeRecorder callTreeRecorder = CallTreeRecorder.getInstance();

        // Javelinのログ出力処理が呼び出されている場合、処理を行わない
        if (callTreeRecorder.isRecordMethodCalled())
        {
            return;
        }

        // Javelinのログ出力処理呼び出しステータスをセット
        callTreeRecorder.setRecordMethodCalled(true);

        try
        {
            Integer depth = callTreeRecorder.getDepth() - 1;
            callTreeRecorder.setDepth(depth);
            CallTree callTree = callTreeRecorder.getCallTree();

            // 計測対象外の深度であれば何もしない
            if (callTree.containsDepth(depth) == false) return;

            callTree.removeDepth(depth);

            recordPostInvocation(thisObject, returnValue, methodName,
                                 cause, callTreeRecorder, telegramId);
        }
        finally
        {
            // Javelinのログ出力処理呼び出しステータスを解除
            callTreeRecorder.setRecordMethodCalled(false);
        }
    }

    /**
     * 後処理の共通処理<br />
     * <br />
     * CallTree に情報を格納します。<br />
     * また、必要に応じて Javelin ログ出力とアラーム通知を行います。<br />
     *
     * @param returnValue 戻り値（ <code>null</code> も可）
     * @param cause 例外発生オブジェクト（ <code>null</code> も可）
     * @param config パラメータの設定値を保存するオブジェクト
     * @param callTreeRecorder コールツリーレコーダ
     * @param telegramId 電文 ID
     */
    private static boolean recordPostInvocation(final Object thisObject,
                                                final Object returnValue,
                                                final String methodName,
                                                final Throwable cause,
                                                CallTreeRecorder callTreeRecorder,
                                                long telegramId)
    {
        boolean ret = false;
        // パラメータの解析後、CallTreeNodeを登録して終了
        if (methodName.equals("heartbeat"))
        {
            ret = recordPostHeartbeat(thisObject, returnValue, methodName,
                                       cause, callTreeRecorder, telegramId);
        }
        else if (methodName.equals("submitJob"))
        {
            ret = recordPostSubmitJob(thisObject, returnValue, methodName,
                                       cause, callTreeRecorder, telegramId);
        }
        // 上記のメソッド以外はエラー

        // 呼び元が残ってしまう場合があるのでクリアする。
        CallTreeRecorder.getInstance().clearCallerNode();

        return ret;
    }

    /**
     * ノード情報にVM状態を設定する。
     *
     * @param node ノード
     * @param parent 呼び出し元
     * @param callTreeRecorder {@link CallTreeRecorder}オブジェクト
     */
    private static void addEndVMStatus(CallTreeNode node,
                                       CallTreeNode parent,
                                       CallTreeRecorder callTreeRecorder)
    {
        long duration = node.getEndTime() - node.getStartTime();
        if (duration == 0)
        {
            node.setEndVmStatus(node.getStartVmStatus());
            return;
        }

        VMStatus vmStatus = createVMStatus(parent, node, callTreeRecorder);
        node.setEndVmStatus(vmStatus);

        setCpuTime(node);
        setUserTime(node);
    }

    /**
     * ルートノードの場合の後処理を行う。
     *
     * @param callTree コールツリー
     * @param node ノード
     * @param callTreeRecorder {@link CallTreeRecorder}オブジェクト
     */
    private static void postProcessOnRootNode(CallTree callTree, CallTreeNode node,
            CallTreeRecorder callTreeRecorder)
    {
        // Strategyインタフェースを利用した判定後の後処理を行う
        postJudge(callTree, node, recordStrategy__);

        callTree.executeCallback();
        callTreeRecorder.clearCallerNode();

        // CallTreeに保持されていたNode数を記録する
        int totalNodeCount = callTree.getTotalNodeCount();
        CallTreeNodeMonitor.add(totalNodeCount);
        callTreeRecorder.clearCallTree();
    }

    /**
     * ノード実行のCPU時間を設定する。
     *
     * @param node ノード
     */
    private static void setCpuTime(CallTreeNode node)
    {
        long endCpuTime = node.getEndVmStatus().getCpuTime();
        long startCpuTime = node.getStartVmStatus().getCpuTime();
        long cpuTime = endCpuTime - startCpuTime;
        if (cpuTime < 0)
        {
            cpuTime = 0;
        }
        node.setCpuTime(cpuTime);
    }

    /**
     * ノード実行のユーザ時間を設定する。
     *
     * @param node ノード
     */
    private static void setUserTime(CallTreeNode node)
    {
        long endUserTime = node.getEndVmStatus().getUserTime();
        long startUserTime = node.getStartVmStatus().getUserTime();
        long userTime = endUserTime - startUserTime;
        if (userTime < 0)
        {
            userTime = 0;
        }
        node.setUserTime(userTime);
    }

    /**
     * ConfigとCallTreeに設定された判定クラスに対して、
     * 判定後に後処理を行う。
     *
     * @param callTree コールツリー
     * @param node ノード
     * @param strategy Configに設定された判定クラス。
     */
    private static void postJudge(final CallTree callTree, final CallTreeNode node,
                                 final RecordStrategy strategy)
    {
        strategy.postJudge();

        RecordStrategy[] highStrategyList = callTree.getHighPriorityRecordStrategy();
        for (RecordStrategy str : highStrategyList)
        {
            str.postJudge();
        }

        RecordStrategy[] strategyList = callTree.getRecordStrategy();
        for (RecordStrategy str : strategyList)
        {
            str.postJudge();
        }
    }

    /**
     * 必要に応じて、 Javelin ログへの出力、アラーム通知処理を行います。<br />
     *
     * @param callTree CallTree
     * @param node CallTreeNode
     * @param callTreeRecorder CallTreeRecorder
     * @param telegramId 電文 ID
     */
    private static void recordAndAlarmProcedure(CallTree callTree,
                                                CallTreeNode node,
                                                CallTreeRecorder callTreeRecorder,
                                                final long telegramId)
    {
        // 終了したジョブを出力するときだけ呼ばれるものとする。
        generator__.generateJaveinFile(callTree, createCallback(
                callTree, node), node, telegramId);
        sendAlarm(node, callTreeRecorder);

    }

    /**
     * CallTreeNodeに設定された判定クラス(判定優先度：高)を利用して、<br />
     * Javelinログをファイルに出力するかどうか判定する。
     *
     * @param node ノード
     *
     * @return true:出力する、false:出力しない
     */
    private static JavelinLogCallback createCallback(final CallTree tree,
                                                     final CallTreeNode node)
    {
        // CallTreeNodeに設定されていた判定クラスでの判定結果が
        // 1つでもtrueであれば、それを戻り値とする
        RecordStrategy[] strategyList = tree.getHighPriorityRecordStrategy();
        for (RecordStrategy str : strategyList)
        {
            JavelinLogCallback callback = str.createCallback();
            if (callback != null)
            {
                return callback;
            }
        }
        // 判定がすべてfalseの場合
        return recordStrategy__.createCallback();
    }

    /**
     * Alarm通知する。
     *
     * @param node ノード
     * @param callTreeRecorder {@link allTreeRecorder}オブジェクト
     */
    private static void sendAlarm(final CallTreeNode node,
                                 final CallTreeRecorder callTreeRecorder)
    {
        synchronized (ALARM_LISTENER_LIST)
        {
            for (AlarmListener alarmListener : ALARM_LISTENER_LIST)
            {
                // ルートノードのみAlarmを送信するAlarmListenerは、
                // 親を持つノードを無視する。
                boolean sendingRootOnly = alarmListener.isSendingRootOnly();
                if (sendingRootOnly == true && node.getParent() != null)
                {
                    continue;
                }

                try
                {
                    // AlarmListenerにはCallTreeNodeをそのまま渡す
                    // →アラーム通知で累積時間を使用するものがある為
                    alarmListener.sendExceedThresholdAlarm(node);
                }
                catch (Throwable ex)
                {
                    SystemLogger.getInstance().warn(ex);
                }
            }
        }
        CallTree tree = callTreeRecorder.getCallTree();
        List<CallTreeNode> eventList = tree.getEventNodeList();
        if (eventList.size() > 0)
        {
            sendEventAlarm();
        }
    }

    /**
     * Javelinログファイルを出力する。
     *
     * @return Javelinログファイル
     */
    public static String dumpJavelinLog()
    {
        String fileName = "";
        // Javelinログファイルを出力する。
        JavelinFileGenerator generator = new JavelinFileGenerator(config__);

        CallTree callTree = CallTreeRecorder.getInstance().getCallTree();
        if (callTree == null)
        {
            return fileName;
        }

        CallTreeNode root = callTree.getRootNode();
        if (root != null)
        {
            fileName = generator.generateJaveinFile(callTree,
                                                    recordStrategy__.createCallback(),
                                                    null,
                                                    0);
        }
        return fileName;
    }

    /**
     * Heartbeat()の前処理を行う。
     * 
     * @param callTreeRecorder
     * @param component
     * @param invocation
     * @param className
     * @param methodName
     * @param args
     * @param stacktrace
     * @param isResponse
     * 
     * @return {@code true}：成功／{@code false}：失敗
     */
    private static boolean recordPreHeartbeat(CallTreeRecorder callTreeRecorder,
        Component component, Invocation invocation, final String className,
        final String methodName, final Object[] args,
        final StackTraceElement[] stacktrace, final boolean isResponse)
    {
        String hostName;
        ArrayList<HadoopTaskStatus> taskStatusList;
        try
        {
            // TaskTrackerStatusからホスト名とTaskStatusを取得
            hostName = HadoopObjectAnalyzer.hostNamefromTaskTrackerStatus(args[0]);
            taskStatusList = HadoopObjectAnalyzer.transTaskStatus(args[0]);
        }
        catch (Exception ex)
        {
            SystemLogger.getInstance().warn(ex);
            return false;
        }

        // TaskTrackerStatusをJobID毎にまとめる
        HashMap<String, ArrayList<HadoopTaskStatus>> arrangedMap = new HashMap<String, ArrayList<HadoopTaskStatus>>();
        for (HadoopTaskStatus status : taskStatusList)
        {
            String jobID = status.getJobID();
            ArrayList<HadoopTaskStatus> temp;
            if (arrangedMap.containsKey(jobID))
            {
                temp = arrangedMap.get(jobID);
            }
            else
            {
                temp = new ArrayList<HadoopTaskStatus>(1);
            }
            temp.add(status);
            arrangedMap.put(jobID, temp);
        }

        // ルート呼び出し時に、例外発生フラグをクリアする
        callTreeRecorder.setExceptionOccured(false);
        if (invocation == null)
            invocation = registerInvocation(component, className, methodName, isResponse);

        // 一度でもルートから呼ばれたことのあるメソッドを保存する。
        ExcludeMonitor.addTargetPreferred(invocation);
        ExcludeMonitor.removeExcludePreferred(invocation);

        // HadoopCallTreeRecorderにInvocationとホスト名を退避
        HadoopCallTreeRecorder recorder = HadoopCallTreeRecorder.getInstance();
        recorder.setInvocation(invocation);
        recorder.putHostName(hostName);
        recorder.putStartTime(System.currentTimeMillis());

        // JobID毎にcallTreeを作成する。
        Set<String>jobIDSet = arrangedMap.keySet();
        for (String jobID : jobIDSet)
        {

            // 最初の呼び出しなので、CallTreeを初期化しておく。
            // TODO CallTreeを勝手に作っても大丈夫？
            CallTree callTree = new CallTree();
            initCallTree(callTree, methodName, callTreeRecorder);
            CallTreeNode newNode = CallTreeRecorder.createNode(invocation, args, stacktrace, config__);
            newNode.setDepth(0);
            newNode.setTree(callTree);
            callTree.setRootNode(newNode);
            callTreeRecorder.setDepth(0);

            try
            {
                VMStatus vmStatus = createVMStatus(null, newNode, callTreeRecorder);
                newNode.setStartTime(System.currentTimeMillis());
                newNode.setStartVmStatus(vmStatus);

                // CallTreeNodeにTaskStatusを設定
                HadoopInfo hadoopInfo = new HadoopInfo();
                hadoopInfo.setHost(hostName);
                hadoopInfo.setTaskStatuses(arrangedMap.get(jobID));
                newNode.setHadoopInfo(hadoopInfo);

                // HadoopCallTreeRecorerにCallTreeを記録
                recorder.putCallTree(jobID, callTree);
                recorder.putCallTreeNode(jobID, newNode);
            }
            catch (Exception ex)
            {
                SystemLogger.getInstance().warn(ex);
                return false;
            }
        }

        return true;
    }

    /**
     * Heartbeat()の後処理を行う。
     * 
     * @param thisObject
     * @param returnValue
     * @param methodName
     * @param cause
     * @param callTreeRecorder
     * @param telegramId
     * 
     * @return {@code true}：成功／{@code false}：失敗
     */
    private static boolean recordPostHeartbeat(final Object thisObject,
                                               final Object returnValue,
                                               final String methodName,
                                               final Throwable cause,
                                               CallTreeRecorder callTreeRecorder,
                                               long telegramId)
    {
        // パラメータ解析
        ArrayList<HadoopAction> ttActionList;
        try
        {
            ttActionList
                = HadoopObjectAnalyzer.taskTrackerActionListFromHearbeatResponse(returnValue);
        }
        catch (Exception ex)
        {
            SystemLogger.getInstance().warn(ex);
            return false;
        }
        
        // HadoopActionをJobID毎にまとめる
        HashMap<String, ArrayList<HadoopAction>> arrangedMap = new HashMap<String, ArrayList<HadoopAction>>();
        for (HadoopAction action : ttActionList)
        {
            String jobID = action.getJobID();
            ArrayList<HadoopAction> temp;
            if (arrangedMap.containsKey(jobID))
            {
                temp = arrangedMap.get(jobID);
            }
            else
            {
                temp = new ArrayList<HadoopAction>(1);
            }
            temp.add(action);
            arrangedMap.put(jobID, temp);
        }

        HadoopCallTreeRecorder recorder = HadoopCallTreeRecorder.getInstance();
        Invocation invocation = recorder.getInvocation();
        String hostName = recorder.takeHostname();
        long startTime = recorder.takeStartTime();

        // TaskTrackerStatusのJobID毎にcallTreeを登録する。
        HashMap<String, CallTree> callTreeMap = recorder.takeAllCallTree();
        HashMap<String, CallTreeNode> callTreeNodeMap = recorder.takeAllCallTreeNode();

        // CallTreeとCallTreeNodeのどちらか片方だけしか取得できない場合はエラー
        if (callTreeMap == null ^ callTreeNodeMap == null)
            return false;

        // 基準はCallTree
        if (callTreeMap != null)
        {
            // CallTree数とCallTreeNode数が異なる場合はエラー
            if (callTreeMap.size() != callTreeNodeMap.size())
                return false;

            // 例外を保存したかどうかのフラグ
            boolean saveException = false;

            Set<String> jobIDSet = callTreeMap.keySet();
            for (String jobID : jobIDSet)
            {
                CallTree callTree = callTreeMap.get(jobID);
                CallTreeNode node = callTreeNodeMap.get(jobID);

                // CallTreeNodeが取得できない場合は処理を中断する。
                if (node == null)
                    return false;

                try
                {
                    // アラーム通知処理、イベント出力処理を行う。
                    recordAndAlarmEvents(callTree, callTreeRecorder, telegramId);

                    // 呼び出し元情報が取得できない場合は処理をキャンセルする。
                    // (下位レイヤで例外が発生した場合のため。)
                    if (node == null)
                        continue;

                    node.setEndTime(System.currentTimeMillis());

                    addEndVMStatus(node, null, callTreeRecorder);

                    if (cause != null && callTree.getCause() != cause)
                    {
                        callTreeRecorder.setExceptionOccured(true);

                        if (!saveException)
                        {
                            invocation.addThrowable(cause);
                            saveException = true;
                        }

                        callTree.setCause(cause);

                        if (config__.isAlarmException())
                        {
                            // 発生した例外を記録しておく
                            node.setThrowable(cause);
                            node.setThrowTime(System.currentTimeMillis());
                        }
                    }

                    // JobIDに対応するTaskTrackerActionがあればCallTreeNodeに設定
                    if (arrangedMap.containsKey(jobID))
                    {
                        node.getHadoopInfo().setTaskTrackerActions(arrangedMap.get(jobID));
                        // 後処理のためにMapから削除
                        arrangedMap.remove(jobID);
                    }

                    recorder.addCallTree(jobID, callTree);

                    if (invocation.getAlarmThreshold()    != Invocation.THRESHOLD_NOT_SPECIFIED ||
                        invocation.getAlarmCpuThreshold() != Invocation.THRESHOLD_NOT_SPECIFIED)
                    {
                        // 以下、CallTreeNodeがrootの場合、または閾値が個別に指定されている場合の処理。
                        // CallTreeNodeがrootで、統計値記録の閾値を超えていた場合に、トランザクションを記録する。
                        if (node.getAccumulatedTime() >= config__.getStatisticsThreshold())
                            recordTransaction(node);

                        // ルートノードの場合の処理
                        postProcessOnRootNode(callTree, node, callTreeRecorder);
                    }
                }
                catch (Throwable ex)
                {
                    SystemLogger.getInstance().warn(ex);
                    return false;
                }
            }
        }

        // TaskTrackerStatusのジョブIDに対応するTaskTrackerActionが
        // 見つからなかった場合の後処理
        Set<String> jobIDSet = arrangedMap.keySet();
        for (String jobID : jobIDSet)
        {
            // 新規作成したCallTreeNodeにTaskTrackerActionを設定して次へ
            CallTree callTree = new CallTree();
            initCallTree(callTree, methodName, callTreeRecorder);
            CallTreeNode newNode = CallTreeRecorder.createNode(invocation, null, null, config__);
            newNode.setDepth(0);
            newNode.setTree(callTree);
            callTree.setRootNode(newNode);
            callTreeRecorder.setDepth(0);

            // 開始時のVM状態を記録
            VMStatus vmStatus = createVMStatus(null, newNode, callTreeRecorder);
            newNode.setStartTime(startTime);
            newNode.setStartVmStatus(vmStatus);

            HadoopInfo hadoopInfo = new HadoopInfo();
            hadoopInfo.setHost(hostName);
            hadoopInfo.setTaskTrackerActions(arrangedMap.get(jobID));
            newNode.setHadoopInfo(hadoopInfo);

            // 終了時のVM状態を記録してcallTreeNodeを保存
            newNode.setEndTime(System.currentTimeMillis());
            addEndVMStatus(newNode, null, callTreeRecorder);
            recorder.addCallTree(jobID, callTree);
        }

        // 終了したジョブの確認
        ArrayList<String> succeededList = new ArrayList<String>(0);
        ArrayList<String> killedList = new ArrayList<String>(0);
        synchronized(runningJobList__)
        {
            Iterator<String> itr = runningJobList__.iterator();
            while (itr.hasNext())
            {
                try
                {
                    String jobID = itr.next();
                    HadoopJobStatus status = HadoopObjectAnalyzer.checkJobStatus(jobID, thisObject);
                    if (status == HadoopJobStatus.SUCCEEDED)
                    {
                        succeededList.add(jobID);
                        itr.remove();
                    }
                    else if (status == HadoopJobStatus.KILLED)
                    {
                        killedList.add(jobID);
                        itr.remove();
                    }
                }
                catch (Exception e)
                {
                    SystemLogger.getInstance().warn(e);
                }
            }
        }

        // 終了したジョブをログに書き出す
        makeCallHistory(invocation, hostName, telegramId, succeededList, killedList);

        return true;
    }

    /**
     * SubmitJob()の前処理を行う。
     * 
     * @param callTreeRecorder
     * @param component
     * @param invocation
     * @param className
     * @param methodName
     * @param args
     * @param stacktrace
     * @param isResponse
     * 
     * @return {@code true}：成功／{@code false}：失敗
     */
    private static boolean recordPreSubmitJob(CallTreeRecorder callTreeRecorder,
        Component component, Invocation invocation, final String className,
        final String methodName, final Object[] args, final StackTraceElement[] stacktrace,
        final boolean isResponse)
    {
        CallTree callTree = callTreeRecorder.getCallTree();
        CallTreeNode newNode;
        CallTreeNode parent = callTreeRecorder.getCallTreeNode();

        // JobIDを取得
        String submitJobID;
        try
        {
            submitJobID = (String)(args[0].getClass().getMethod("toString", new Class[]{}).invoke(args[0], new Object[]{}));
        }
        catch (Exception ex)
        {
            SystemLogger.getInstance().warn(ex);
            return false;
        }
        
        // 呼び出し時に、例外発生フラグをクリアする
        callTreeRecorder.setExceptionOccured(false);
        if (invocation == null)
            invocation = registerInvocation(component, className, methodName, isResponse);

        // 一度でもルートから呼ばれたことのあるメソッドを保存する。
        ExcludeMonitor.addTargetPreferred(invocation);
        ExcludeMonitor.removeExcludePreferred(invocation);

        // 最初の呼び出しなので、CallTreeを初期化しておく。
        initCallTree(callTree, "submitJob", callTreeRecorder);
        newNode = CallTreeRecorder.createNode(invocation, args, stacktrace, config__);
        newNode.setDepth(0);
        newNode.setTree(callTree);
        callTree.setRootNode(newNode);
        callTreeRecorder.setDepth(0);

        try
        {
            if (newNode == null)
            {
                newNode = CallTreeRecorder.createNode(invocation, args, stacktrace, config__);
                newNode.setDepth(callTreeRecorder.getDepth());
            }

            // CallTreeNodeを追加
            CallTreeRecorder.addCallTreeNode(parent, callTree, newNode, config__);
            // VM状態取得
            VMStatus vmStatus = createVMStatus(parent, newNode, callTreeRecorder);

            newNode.setStartTime(System.currentTimeMillis());
            newNode.setStartVmStatus(vmStatus);

            // 投入されたJobIDをCallTreeNodeに登録
            HadoopInfo hadoopInfo = new HadoopInfo();
            hadoopInfo.setSubmitJobID(submitJobID);
            newNode.setHadoopInfo(hadoopInfo);

            callTreeRecorder.setCallerNode(newNode);
        }
        catch (Exception ex)
        {
            SystemLogger.getInstance().warn(ex);
            return false;
        }

        return true;
    }

    /**
     * submitJob()の後処理を行う。
     * 
     * @param thisObject
     * @param returnValue
     * @param methodName
     * @param cause
     * @param callTreeRecorder
     * @param telegramId
     * 
     * @return {@code true}：成功／{@code false}：失敗
     */
    private static boolean recordPostSubmitJob(final Object thisObject,
                                               final Object returnValue,
                                               final String methodName,
                                               final Throwable cause,
                                               CallTreeRecorder callTreeRecorder,
                                               long telegramId)
    {
        CallTree callTree = callTreeRecorder.getCallTree();

        try
        {
            // アラーム通知処理、イベント出力処理を行う。
            recordAndAlarmEvents(callTree, callTreeRecorder, telegramId);

            // 呼び出し元情報取得。
            CallTreeNode node = callTreeRecorder.getCallTreeNode();
            if (node == null)
            {
                // 呼び出し元情報が取得できない場合は処理をキャンセルする。
                // (下位レイヤで例外が発生した場合のため。)
                return false;
            }

            node.setEndTime(System.currentTimeMillis());

            CallTreeNode parent = node.getParent();
            addEndVMStatus(node, parent, callTreeRecorder);

            Invocation invocation = node.getInvocation();

            if (cause != null && callTree.getCause() != cause)
            {
                callTreeRecorder.setExceptionOccured(true);

                invocation.addThrowable(cause);
                callTree.setCause(cause);

                if (config__.isAlarmException())
                {
                    // 発生した例外を記録しておく
                    node.setThrowable(cause);
                    node.setThrowTime(System.currentTimeMillis());
                }
            }

            // 登録されたジョブを実行中リストに登録
            String jobID = HadoopObjectAnalyzer.getJobIDfromJobStatus(returnValue);
            synchronized(runningJobList__)
            {
                runningJobList__.add(jobID);
            }

            if (node.hasHadoopInfo())
                HadoopCallTreeRecorder.getInstance().addCallTree(node.getHadoopInfo().getSubmitJobID(), callTree);

            if (invocation.getAlarmThreshold()    != Invocation.THRESHOLD_NOT_SPECIFIED ||
                invocation.getAlarmCpuThreshold() != Invocation.THRESHOLD_NOT_SPECIFIED)
            {
                // 以下、CallTreeNodeがrootの場合、または閾値が個別に指定されている場合の処理。
                // CallTreeNodeがrootで、統計値記録の閾値を超えていた場合に、トランザクションを記録する。
                if (node.getAccumulatedTime() >= config__.getStatisticsThreshold())
                {
                    recordTransaction(node);
                }

                // ルートノードの場合の処理
                postProcessOnRootNode(callTree, node, callTreeRecorder);
            }

        }
        catch (Throwable ex)
        {
            SystemLogger.getInstance().warn(ex);
            return false;
        }
        return true;
    }

    /**
     * ジョブ終了のCallTreeを作成後、ログを書き出します。
     * 
     * @param invocation {@link Invocation}
     * @param hostName ホスト名
     * @param telegramID 電文ID
     * @param succeededList 完了したジョブのリスト
     * @param killedList 停止されたジョブのリスト
     */
    private static void makeCallHistory(Invocation invocation,
                                        String hostName,
                                        long telegramID,
                                        List<String> succeededList,
                                        List<String> killedList)
    {
        HadoopCallTreeRecorder recorder = HadoopCallTreeRecorder.getInstance();

        // 完了リスト中のJobIDごとに、終了時の情報を設定し、ログ書き出し
        for (String jobID : succeededList)
        {
            CallTree tree = makeSucceededHistory(invocation, hostName, jobID);
            recorder.addCallTree(jobID, tree);

            recordAndAlarmProcedure(recorder.getCallTree(jobID), null, CallTreeRecorder.getInstance(), telegramID);
        }
        // 停止リスト中のJobIDごとに、終了時の情報を設定し、ログ書き出し
        for (String jobID : killedList)
        {
            CallTree tree = makeKilledHistory(invocation, hostName, jobID);
            recorder.addCallTree(jobID, tree);

            recordAndAlarmProcedure(recorder.getCallTree(jobID), null, CallTreeRecorder.getInstance(), telegramID);
        }
    }

    /**
     * ジョブ完了情報を作成します。
     * 
     * @param invocation {@link Invocation}
     * @param hostName ホスト名
     * @param jobID 完了したジョブID
     * 
     * @return {@link CallTree}
     */
    private static CallTree makeSucceededHistory(Invocation invocation,
                                                 String hostName,
                                                 String jobID)
    {
        // CallTreeRecorderの取得
        CallTreeRecorder recorder = CallTreeRecorder.getInstance();

        // CallTreeの作成
        CallTree tree = new CallTree();
        initCallTree(tree, "getJobStatus", recorder); // 便宜上、メソッド名を設定

        // CallTreeNodeの作成
        CallTreeNode node = CallTreeRecorder.createNode(invocation, null, null, config__);
        node.setDepth(0);
        node.setTree(tree);

        tree.setRootNode(node);
        recorder.setDepth(0);

        // 開始状態のVM状態を作成
        VMStatus vmStatus = createVMStatus(null, node, recorder);
        node.setStartTime(System.currentTimeMillis());
        node.setStartVmStatus(vmStatus);

        // ジョブ完了情報を設定
        HadoopInfo info = new HadoopInfo();
        info.setHost(hostName);
        info.setCompleteJobID(jobID);
        node.setHadoopInfo(info);

        // 終了時のVM状態を作成
        node.setEndTime(System.currentTimeMillis());
        addEndVMStatus(node, null, recorder);

        return tree;
    }

    /**
     * ジョブ停止情報を作成します。
     * 
     * @param invocation {@link Invocation}
     * @param hostName ホスト名
     * @param jobID 停止されたジョブID
     * 
     * @return {@link CallTree}
     */
    private static CallTree makeKilledHistory(Invocation invocation,
                                                 String hostName,
                                                 String jobID)
    {
        // CallTreeRecorderの取得
        CallTreeRecorder recorder = CallTreeRecorder.getInstance();

        // CallTreeの作成
        CallTree tree = new CallTree();
        initCallTree(tree, "killJob", recorder); // 便宜上、メソッド名を設定

        // CallTreeNodeの作成
        CallTreeNode node = CallTreeRecorder.createNode(invocation, null, null, config__);
        node.setDepth(0);
        node.setTree(tree);

        tree.setRootNode(node);
        recorder.setDepth(0);

        // 開始状態のVM状態を作成
        VMStatus vmStatus = createVMStatus(null, node, recorder);
        node.setStartTime(System.currentTimeMillis());
        node.setStartVmStatus(vmStatus);

        // ジョブ停止情報を設定
        HadoopInfo info = new HadoopInfo();
        info.setHost(hostName);
        info.setKilledJobID(jobID);
        node.setHadoopInfo(info);

        // 終了時のVM状態を作成
        node.setEndTime(System.currentTimeMillis());
        addEndVMStatus(node, null, recorder);

        return tree;
    }
}
