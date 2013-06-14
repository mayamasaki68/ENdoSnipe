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
package jp.co.acroquest.endosnipe.javelin.log;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jp.co.acroquest.endosnipe.common.config.JavelinConfig;
import jp.co.acroquest.endosnipe.common.parser.JavelinConstants;
import jp.co.acroquest.endosnipe.common.parser.JavelinLogConstants;
import jp.co.acroquest.endosnipe.javelin.CallTree;
import jp.co.acroquest.endosnipe.javelin.CallTreeNode;
import jp.co.acroquest.endosnipe.javelin.VMStatus;
import jp.co.acroquest.endosnipe.javelin.bean.Invocation;
import jp.co.acroquest.endosnipe.javelin.converter.hadoop.HadoopAction;
import jp.co.acroquest.endosnipe.javelin.converter.hadoop.HadoopInfo;
import jp.co.acroquest.endosnipe.javelin.converter.hadoop.HadoopTaskStatus;
import jp.co.acroquest.endosnipe.javelin.converter.hadoop.HadoopTaskStatus.State;
import jp.co.acroquest.endosnipe.javelin.event.CommonEvent;
import jp.co.acroquest.endosnipe.javelin.helper.VMStatusHelper;
import jp.co.acroquest.endosnipe.javelin.util.StatsUtil;
import jp.co.acroquest.endosnipe.javelin.util.ThreadUtil;

/**
 * Javelinログの一要素作成するクラス。
 *
 * @author eriguchi
 */
public class JavelinLogMaker implements JavelinConstants, JavelinLogConstants
{
    /** ダブルクォーテーションを表すパターン。 */
    private static final Pattern DOUBLE_QUOTATION_PATTERN = Pattern.compile("\"");

    /**
     * インスタンス化を阻止するプライベートコンストラクタ。
     */
    protected JavelinLogMaker()
    {
        // Do Nothing.
    }

    /** 単位変換定数(ナノ → ミリ) */
    private static final int NANO_TO_MILLI = 1000000;

    private static final String[] MESSAGE_TYPES =
            new String[]{MSG_CALL, MSG_RETURN, MSG_FIELD_READ, MSG_FIELD_WRITE, MSG_CATCH,
                    MSG_THROW, MSG_EVENT};

    private static final String NEW_LINE = "\r\n";

    /**
     * イベントログ文字列を作成します。
     *
     * @param event イベント。
     * @param tree コールツリー。
     * @param node ノード。
     * @return イベントログ文字列。
     */
    public static String createEventLog(final CommonEvent event, final CallTree tree,
            final CallTreeNode node)
    {
        if (event == null)
        {
            return null;
        }
        long time = event.getTime();
        if (time == 0L)
        {
            return null;
        }

        StringBuffer jvnBuffer = new StringBuffer();

        Invocation callee = node.getInvocation();
        if (callee == null)
        {
            return "";
        }

        jvnBuffer.append(MESSAGE_TYPES[ID_EVENT]);

        SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_PATTERN);
        jvnBuffer.append(",");
        jvnBuffer.append(dateFormat.format(time));

        // イベント名
        addToJvnBuffer(event.getName(), jvnBuffer);

        // メソッド名
        addToJvnBuffer(getValidMethodName(callee), jvnBuffer);

        // クラス名
        addToJvnBuffer(callee.getClassName(), jvnBuffer);

        String levelStr = createLevelStr(event);

        // 警告レベル
        addToJvnBuffer(levelStr, jvnBuffer);

        // スレッドID
        addToJvnBuffer(tree.getThreadID(), jvnBuffer);
        jvnBuffer.append(NEW_LINE);

        jvnBuffer.append(JAVELIN_EVENTINFO_START);
        jvnBuffer.append(NEW_LINE);

        // パラメータを出力する。
        for (Map.Entry<String, String> entry : event.getParamMap().entrySet())
        {
            String key = entry.getKey();
            Object value = entry.getValue();
            addParam(jvnBuffer, key, String.valueOf(value));
        }

        jvnBuffer.append(JAVELIN_EVENTINFO_END);
        jvnBuffer.append(NEW_LINE);

        String jvnMessage = jvnBuffer.toString();
        return jvnMessage;
    }

    /**
     * メソッド名にダブルクォーテーションがある場合は、それを2つのダブルクォーテーションに置き換え、
     * 改行がある場合は改行を削除して出力する。
     *
     * @param invocation
     * @return
     */
    protected static String getValidMethodName(Invocation invocation)
    {
        String validMethodName = invocation.getMethodName();
        Matcher matcher = DOUBLE_QUOTATION_PATTERN.matcher(validMethodName);
        validMethodName = matcher.replaceAll("\"\"");
        return validMethodName.replaceAll("[\r\n]", " ");
    }

    protected static String createLevelStr(final CommonEvent event)
    {
        String levelStr;
        if (event.getLevel() == CommonEvent.LEVEL_ERROR)
        {
            levelStr = JavelinLogConstants.EVENT_ERROR;
        }
        else if (event.getLevel() == CommonEvent.LEVEL_WARN)
        {
            levelStr = JavelinLogConstants.EVENT_WARN;
        }
        else
        {
            levelStr = JavelinLogConstants.EVENT_INFO;
        }
        return levelStr;
    }

    /**
     *
     * @param messageType メッセージタイプ
     * @param time 時刻
     * @param tree {@link CallTree}オブジェクト
     * @param node {@link CallTreeNode}オブジェクト
     * @return Javelinログの内容
     */
    public static String createJavelinLog(final int messageType, final long time,
            final CallTree tree, final CallTreeNode node)
    {
        if (time == 0L)
        {
            return null;
        }

        CallTreeNode parent = node.getParent();
        JavelinConfig config = new JavelinConfig();
        boolean isReturnDetail = config.isReturnDetail();

        StringBuffer jvnBuffer = new StringBuffer();

        Invocation callee = node.getInvocation();
        Invocation caller;
        if (parent == null)
        {
            String processName = VMStatusHelper.getProcessName();
            caller =
                    new Invocation(processName, tree.getRootCallerName(),
                                   JavelinLogConstants.DEFAULT_LOGMETHOD, 0);
        }
        else
        {
            caller = parent.getInvocation();
        }

        if (callee == null)
        {
            return "";
        }

        jvnBuffer.append(MESSAGE_TYPES[messageType]);

        SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_PATTERN);
        jvnBuffer.append(",");
        jvnBuffer.append(dateFormat.format(time));

        Throwable throwable = node.getThrowable();
        if (messageType == ID_THROW)
        {
            // 例外クラス名
            addToJvnBuffer(throwable.getClass().getName(), jvnBuffer);

            // 例外オブジェクトID
            addToJvnBuffer(StatsUtil.getObjectID(throwable), jvnBuffer);
        }

        // 呼び出し先メソッド名
        addToJvnBuffer(getValidMethodName(callee), jvnBuffer);

        // 呼び出し先クラス名
        addToJvnBuffer(callee.getClassName(), jvnBuffer);

        // 呼び出し先オブジェクトID
        addToJvnBuffer("unknown", jvnBuffer);

        if (messageType == ID_FIELD_READ || messageType == ID_FIELD_WRITE)
        {
            // アクセス元メソッド名
            addToJvnBuffer("", jvnBuffer);

            // アクセス元クラス名
            addToJvnBuffer("", jvnBuffer);

            // アクセス元オブジェクトID
            addToJvnBuffer("", jvnBuffer);

            // アクセス先フィールドの型
            addToJvnBuffer("", jvnBuffer);
        }
        else if (messageType == ID_CALL || messageType == ID_RETURN)
        {
            // 呼び出し元メソッド名
            addToJvnBuffer(getValidMethodName(caller), jvnBuffer);

            // 呼び出し元クラス名
            addToJvnBuffer(caller.getClassName(), jvnBuffer);

            // 呼び出し元オブジェクトID
            addToJvnBuffer("unknown", jvnBuffer);

            // モディファイア
            addToJvnBuffer("", jvnBuffer);
        }

        // スレッドID
        addToJvnBuffer(tree.getThreadID(), jvnBuffer);
        jvnBuffer.append(NEW_LINE);

        int stringLimitLength = config.getStringLimitLength();
        String[] args = node.getArgs();
        if (messageType == ID_CALL && args != null && args.length > 0)
        {
            addArgs(jvnBuffer, stringLimitLength, args);
        }

        if (messageType == ID_RETURN)
        {
            String returnValue = node.getReturnValue();
            if (returnValue != null)
            {
                addReturn(jvnBuffer, returnValue, isReturnDetail, stringLimitLength);
            }
        }

        if (config.isLogMBeanInfo() || (config.isLogMBeanInfoRoot() && parent == null))
        {
            if (messageType == ID_CALL)
            {
                // VM実行情報
                VMStatus startStatus = node.getStartVmStatus();
                VMStatus endStatus = node.getEndVmStatus();

                StringBuffer vmStatusBuffer = new StringBuffer();
                addVMStatusDiff(vmStatusBuffer, startStatus, endStatus);

                if (vmStatusBuffer.length() > 0)
                {
                    jvnBuffer.append(JAVELIN_JMXINFO_START);
                    jvnBuffer.append(NEW_LINE);

                    jvnBuffer.append(vmStatusBuffer);

                    jvnBuffer.append(JAVELIN_JMXINFO_END);
                    jvnBuffer.append(NEW_LINE);
                }
            }
        }

        if (messageType == ID_CALL)
        {
            jvnBuffer.append(JAVELIN_EXTRAINFO_START);
            jvnBuffer.append(NEW_LINE);

            long duration = node.getAccumulatedTime();
            if (duration >= 0)
            {
                addParam(jvnBuffer, EXTRAPARAM_DURATION, duration);
            }

            if (node.getParent() == null)
            {
                for (String key : tree.getLoggingKeys())
                {
                    Object value = tree.getLoggingValue(key);
                    addParam(jvnBuffer, key, String.valueOf(value));
                }
            }
            for (String key : node.getLoggingKeys())
            {
                Object value = node.getLoggingValue(key);
                addParam(jvnBuffer, key, value.toString());
            }

            jvnBuffer.append(JAVELIN_EXTRAINFO_END);
            jvnBuffer.append(NEW_LINE);
        }

        StackTraceElement[] stacktrace = node.getStacktrace();
        if (messageType == ID_CALL && stacktrace != null)
        {
            addStackTrace(jvnBuffer, stacktrace);
        }

        if (messageType == ID_THROW)
        {
            addThrowable(jvnBuffer, throwable);
        }
        return jvnBuffer.toString();
    }

    protected static void addStackTrace(final StringBuffer jvnBuffer,
            final StackTraceElement[] stacktrace)
    {
        jvnBuffer.append(JAVELIN_STACKTRACE_START);
        jvnBuffer.append(NEW_LINE);
        jvnBuffer.append(ThreadUtil.getStackTrace(stacktrace));
        jvnBuffer.append(JAVELIN_STACKTRACE_END);
        jvnBuffer.append(NEW_LINE);
    }

    protected static void addThrowable(final StringBuffer jvnBuffer, final Throwable throwable)
    {
        jvnBuffer.append(JAVELIN_STACKTRACE_START);
        jvnBuffer.append(NEW_LINE);
        jvnBuffer.append(throwable.getClass().getName() + ":" + throwable.getMessage());
        jvnBuffer.append(NEW_LINE);
        StackTraceElement[] stacktrace = throwable.getStackTrace();
        jvnBuffer.append(ThreadUtil.getStackTrace(stacktrace));
        jvnBuffer.append(JAVELIN_STACKTRACE_END);
        jvnBuffer.append(NEW_LINE);
    }

    protected static void addReturn(final StringBuffer jvnBuffer, final String returnValue,
            final boolean isReturnDetail, final int stringLimitLength)
    {
        jvnBuffer.append(JAVELIN_RETURN_START);
        jvnBuffer.append(NEW_LINE);
        jvnBuffer.append(StatsUtil.toStr(returnValue, stringLimitLength));
        jvnBuffer.append(NEW_LINE);
        jvnBuffer.append(JAVELIN_RETURN_END);
        jvnBuffer.append(NEW_LINE);
    }

    protected static void addArgs(final StringBuffer jvnBuffer, final int stringLimitLength,
            final String[] args)
    {
        jvnBuffer.append(JAVELIN_ARGS_START);
        jvnBuffer.append(NEW_LINE);
        for (int i = 0; i < args.length; i++)
        {
            jvnBuffer.append("args[");
            jvnBuffer.append(i);
            jvnBuffer.append("] = ");

            // 実行計画（[PLAN] で始まる）は結果の文字列を短縮しない
            if (args[i] != null && args[i].startsWith("[PLAN]"))
            {
                jvnBuffer.append(args[i]);
            }
            else
            {
                jvnBuffer.append(StatsUtil.toStr(args[i], stringLimitLength));
            }

            jvnBuffer.append(NEW_LINE);
        }
        jvnBuffer.append(JAVELIN_ARGS_END);
        jvnBuffer.append(NEW_LINE);
    }
    protected static void addVMStatusDiff(final StringBuffer jvnBuffer, final VMStatus startStatus,
            final VMStatus endStatus)
    {
        if (startStatus == null || endStatus == null)
        {
            return;
        }

        double cpuTimeDelta =
                (double)(endStatus.getCpuTime() - startStatus.getCpuTime()) / NANO_TO_MILLI;
        double userTimeDelta =
                (double)(endStatus.getUserTime() - startStatus.getUserTime()) / NANO_TO_MILLI;
        addParamDelta(jvnBuffer, JMXPARAM_THREAD_CURRENT_THREAD_CPU_TIME_DELTA,
                      String.valueOf(cpuTimeDelta));
        addParamDelta(jvnBuffer, JMXPARAM_THREAD_CURRENT_THREAD_USER_TIME_DELTA,
                      String.valueOf(userTimeDelta));
        addParamDelta(jvnBuffer, JMXPARAM_THREAD_THREADINFO_BLOCKED_COUNT_DELTA,
                      String.valueOf(endStatus.getBlockedCount() - startStatus.getBlockedCount()));
        addParamDelta(jvnBuffer, JMXPARAM_THREAD_THREADINFO_BLOCKED_TIME_DELTA,
                      String.valueOf(endStatus.getBlockedTime() - startStatus.getBlockedTime()));
        addParamDelta(jvnBuffer, JMXPARAM_THREAD_THREADINFO_WAITED_COUNT_DELTA,
                      String.valueOf(endStatus.getWaitedCount() - startStatus.getWaitedCount()));
        addParamDelta(jvnBuffer, JMXPARAM_THREAD_THREADINFO_WAITED_TIME_DELTA,
                      String.valueOf(endStatus.getWaitedTime() - startStatus.getWaitedTime()));
        addParamDelta(jvnBuffer, JMXPARAM_GARBAGECOLLECTOR_COLLECTION_COUNT_DELTA,
                      String.valueOf(endStatus.getCollectionCount()
                              - startStatus.getCollectionCount()));
        addParamDelta(jvnBuffer, JMXPARAM_GARBAGECOLLECTOR_COLLECTION_TIME_DELTA,
                      String.valueOf(endStatus.getCollectionTime()
                              - startStatus.getCollectionTime()));
    }

    protected static void addParamDelta(final StringBuffer jvnBuffer, final String paramName,
            final String paramValue)
    {
        if ("0.0".equals(paramValue) || "0".equals(paramValue))
        {
            return;
        }
        jvnBuffer.append(paramName);
        jvnBuffer.append(" = ");
        jvnBuffer.append(paramValue);
        jvnBuffer.append(NEW_LINE);
    }

    protected static void addParam(final StringBuffer jvnBuffer, final String paramName,
            final String paramValue)
    {
        jvnBuffer.append(paramName);
        jvnBuffer.append(" = ");
        jvnBuffer.append(paramValue);
        jvnBuffer.append(NEW_LINE);
    }

    protected static void addParam(final StringBuffer jvnBuffer, final String paramName,
            final long paramValue)
    {
        jvnBuffer.append(paramName);
        jvnBuffer.append(" = ");
        jvnBuffer.append(paramValue);
        jvnBuffer.append(NEW_LINE);
    }

    protected static void addToJvnBuffer(final String element, final StringBuffer jvnBuffer)
    {
        jvnBuffer.append(",\"");
        jvnBuffer.append(element);
        jvnBuffer.append("\"");
    }
    

    /**
     * Hadoopのノード間パラメータに対応したメッセージを作成する。
     *
     * @param messageType メッセージタイプ
     * @param time 時刻
     * @param tree {@link CallTree}オブジェクト
     * @param node {@link CallTreeNode}オブジェクト
     *
     * @return Javelinログの内容
     */
    static private String createHadoopLog(final int messageType,
                                          final long time,
                                          final CallTree tree,
                                          final CallTreeNode node)
    {
        // TODO "Call"と"Return"以外は未対応
        if ( !(messageType == ID_CALL) && !(messageType == ID_RETURN) )
                return null;
        if (!(node.hasHadoopInfo()))
            return null;

        CallTreeNode parent = node.getParent();
        JavelinConfig config = new JavelinConfig();

        StringBuffer jvnBuffer = new StringBuffer();

        Invocation callee = node.getInvocation();

        // RPCなので呼び出し元は必ずNULLになる
        if (parent != null)
        {
            return null;
        }

        if (callee == null)
        {
            return null;
        }

        // ここからメッセージ作成開始
        if (node.getHadoopInfo().hasActions() || node.getHadoopInfo().hasStatuses())
        {
            // heartbeat()はCallとReturnを逆転させて表示する
            if (messageType == ID_CALL)
            {
                if (!node.getHadoopInfo().hasStatuses())
                    return null;

                jvnBuffer.append(MESSAGE_TYPES[ID_RETURN]);
            }
            else
            {
                if (!node.getHadoopInfo().hasActions())
                    return null;

                jvnBuffer.append(MESSAGE_TYPES[ID_CALL]);
            }

            SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_PATTERN);
            jvnBuffer.append(",");
            jvnBuffer.append(dateFormat.format(time));

            // TODO 例外の書き出しは考慮しない

            // Phase
            addToJvnBuffer(getType(node.getHadoopInfo(), messageType), jvnBuffer);

            // 呼び出し元ホスト名
            addToJvnBuffer(node.getHadoopInfo().getHost(), jvnBuffer);

            // 呼び出し先オブジェクトID
            addToJvnBuffer("", jvnBuffer);

            // 呼び出し元IP
            addToJvnBuffer("", jvnBuffer);

            // "JobTracker"
            addToJvnBuffer("JobTracker", jvnBuffer);

            // 呼び出し元オブジェクトID
            addToJvnBuffer("", jvnBuffer);

            // モディファイア
            addToJvnBuffer("", jvnBuffer);

            // TT->JTの場合
            if (messageType == ID_CALL)
            {
                // スレッドIDに最初に見つかったジョブIDを設定
                for (HadoopTaskStatus stat : node.getHadoopInfo().getTaskStatuses())
                {
                    addToJvnBuffer(stat.getJobID(), jvnBuffer);
                    jvnBuffer.append(NEW_LINE);
                    break;
                }

                // 戻り値出力が設定されている場合
                if (config.isLogReturn())
                {
                    String retVal = makeTaskStatus(node.getHadoopInfo().getTaskStatuses());
                    // 出力対象のメッセージが無い場合はnullを返す
                    if (null == retVal)
                        return null;
                    jvnBuffer.append(retVal);
                }
            }
            if (messageType == ID_RETURN)
            {
                // スレッドIDに最初に見つかったジョブIDを設定
                for (HadoopAction action : node.getHadoopInfo().getTaskTrackerActions())
                {
                    addToJvnBuffer(action.getJobID(), jvnBuffer);
                    jvnBuffer.append(NEW_LINE);
                    break;
                }

                // 引数出力が設定されている場合
                if (config.isLogArgs())
                {
                    String retVal = makeHeartbeatResponse(node.getHadoopInfo().getTaskTrackerActions());
                    // 出力対象のメッセージが無い場合はnullを返す
                    if (null == retVal)
                        return null;
                    jvnBuffer.append(retVal);
                }
            }
        }
        // ジョブ投入、完了、停止の場合
        else if (node.getHadoopInfo().hasSubmitInfo() ||
                 node.getHadoopInfo().hasCompleteInfo() ||
                 node.getHadoopInfo().hasKilledInfo())
        {
            String command = "";

            // submitJob()のリターンは出力しない
            if (node.getHadoopInfo().hasSubmitInfo())
            {
                if (messageType == ID_RETURN)
                    return null;
                else
                    command = "SubmitJob";
            }
            // completedInfoのコールは出力しない
            if (node.getHadoopInfo().hasCompleteInfo())
            {
                if (messageType == ID_CALL)
                    return null;
                else
                    command = "JobCompleted";
            }
            // killedInfoのリターンは出力しない
            if (node.getHadoopInfo().hasKilledInfo())
            {
                if (messageType == ID_RETURN)
                    return null;
                else
                    command = "KillJob";
            }

            jvnBuffer.append(MESSAGE_TYPES[messageType]);

            SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_PATTERN);
            jvnBuffer.append(",");
            jvnBuffer.append(dateFormat.format(time));

            // TODO 例外の書き出しは考慮しない

            // 呼び出し先
            addToJvnBuffer(command, jvnBuffer);

            // "JobTracker"
            addToJvnBuffer("JobTracker", jvnBuffer);

            // 呼び出し先オブジェクトID
            addToJvnBuffer("", jvnBuffer);

            // 呼び出し元IP?
            addToJvnBuffer("", jvnBuffer);

            // 呼び出し元ホスト名
            addToJvnBuffer("root", jvnBuffer);

            // 呼び出し元オブジェクトID
            addToJvnBuffer("", jvnBuffer);

            // モディファイア
            addToJvnBuffer("", jvnBuffer);

            // ジョブID
            String jobID;
            if (node.getHadoopInfo().hasSubmitInfo())
                jobID = node.getHadoopInfo().getSubmitJobID();
            else if (node.getHadoopInfo().hasCompleteInfo())
                jobID = node.getHadoopInfo().getCompleteJobID();
            else
                jobID = node.getHadoopInfo().getKilledJobID();
            addToJvnBuffer(jobID, jvnBuffer);

            jvnBuffer.append(NEW_LINE);

            // Hadoop固有情報を書き出し
            if (messageType == ID_CALL)
            {
                // 引数出力が設定されてる場合
                if (config.isLogArgs())
                {
                    // ジョブIDを出力
                    jvnBuffer.append(JAVELIN_ARGS_START);
                    jvnBuffer.append(NEW_LINE);
                    jvnBuffer.append("JobID : " + jobID);
                    jvnBuffer.append(NEW_LINE);
                    jvnBuffer.append(JAVELIN_ARGS_END);
                    jvnBuffer.append(NEW_LINE);
                }
            }
            else if (messageType == ID_RETURN)
            {
                // 戻り値出力が設定されてる場合
                if (config.isLogReturn())
                {
                    // ジョブIDを出力
                    jvnBuffer.append(JAVELIN_RETURN_START);
                    jvnBuffer.append(NEW_LINE);
                    jvnBuffer.append("JobID : " + jobID);
                    jvnBuffer.append(NEW_LINE);
                    jvnBuffer.append(JAVELIN_RETURN_END);
                    jvnBuffer.append(NEW_LINE);
                }
            }
        }

        if (config.isLogMBeanInfo() || config.isLogMBeanInfoRoot())
        {
            if (messageType == ID_CALL)
            {
                // VM実行情報
                VMStatus startStatus = node.getStartVmStatus();
                VMStatus endStatus = node.getEndVmStatus();

                StringBuffer vmStatusBuffer = new StringBuffer();
                addVMStatusDiff(vmStatusBuffer, startStatus, endStatus);

                if (vmStatusBuffer.length() > 0)
                {
                    jvnBuffer.append(JAVELIN_JMXINFO_START);
                    jvnBuffer.append(NEW_LINE);

                    jvnBuffer.append(vmStatusBuffer);

                    jvnBuffer.append(JAVELIN_JMXINFO_END);
                    jvnBuffer.append(NEW_LINE);
                }
            }
        }

        if (messageType == ID_CALL)
        {
            jvnBuffer.append(JAVELIN_EXTRAINFO_START);
            jvnBuffer.append(NEW_LINE);

            long duration = node.getAccumulatedTime();
            if (duration >= 0)
            {
                addParam(jvnBuffer, EXTRAPARAM_DURATION, duration);
            }

            if (node.getParent() == null)
            {
                for (String key : tree.getLoggingKeys())
                {
                    Object value = tree.getLoggingValue(key);
                    addParam(jvnBuffer, key, String.valueOf(value));
                }
            }
            for (String key : node.getLoggingKeys())
            {
                Object value = node.getLoggingValue(key);
                addParam(jvnBuffer, key, value.toString());
            }

            jvnBuffer.append(JAVELIN_EXTRAINFO_END);
            jvnBuffer.append(NEW_LINE);
        }

        return jvnBuffer.toString();
    }

    /**
     * HadoopのTaskTrackerのタスク状態のメッセージを作成します。
     *
     * @param taskStatusList タスク状態のリスト
     *
     * @return タスク状態のメッセージ／{@code null}は出力対象の情報なし
     */
    private static String makeTaskStatus(final ArrayList<HadoopTaskStatus> taskStatusList)
    {
        boolean result = false;
        String ret = null;
        StringBuffer buf = new StringBuffer();

        if (taskStatusList == null) return ret;

        buf.append(JAVELIN_RETURN_START);
        buf.append(NEW_LINE);

        // それぞれのタスク情報を書き出し
        int index = 1;
        for (HadoopTaskStatus taskStatus : taskStatusList)
        {
            // タスク実行中は出力しない
            if (taskStatus.getState() == State.RUNNING)
                continue;

            String prefix = "Task[" + String.valueOf(index) + "] ";
            String jobID = prefix + "JobID : " + taskStatus.getJobID();
            String taskID = prefix + "TaskID : " + taskStatus.getTaskID();
            String phase = prefix + "Phase : " + taskStatus.getPhase().toString();
            String state = prefix + "State : " + taskStatus.getState().toString();

            buf.append(jobID + NEW_LINE);
            buf.append(taskID + NEW_LINE);
            buf.append(phase + NEW_LINE);
            buf.append(state + NEW_LINE);

            ++index;
            result = true;
        }

        buf.append(JAVELIN_RETURN_END);
        buf.append(NEW_LINE);

        if (result)
        {
            ret = buf.toString();
        }

        return ret;
    }


    /**
     * Hadoopのハートビート返信のメッセージを作成します。
     *
     * @param taskTrackerActions アクション情報のリスト
     *
     * @return ハートビート返信のメッセージ／{@code null}は出力対象の情報なし
     */
    private static String makeHeartbeatResponse(final ArrayList<HadoopAction> taskTrackerActions)
    {
        boolean result = false;
        String ret = null;
        StringBuffer buf = new StringBuffer();

        if (taskTrackerActions == null) return ret;

        buf.append(JAVELIN_ARGS_START);
        buf.append(NEW_LINE);

        // アクションごとに情報を書き出し
        int index = 1;
        for(HadoopAction action : taskTrackerActions)
        {

            String prefix = "Task[" + String.valueOf(index) + "] ";
            String jobID = prefix + "JobID : " + action.getJobID();
            String jobName = prefix + "JobName : " + action.getJobName();
            String taskID = prefix + "TaskID : " + action.getTaskID();
            String input = prefix + "InputDIR : " + action.getInputData();
            String act = prefix + "Action : " + action.getActionType().toString();
            String type = prefix + "Type : " +
                (action.isMapTask() ? "MapTask" : "ReduceTask");

            buf.append(jobID + NEW_LINE);
            buf.append(jobName + NEW_LINE);
            buf.append(taskID + NEW_LINE);
            buf.append(input + NEW_LINE);
            buf.append(act + NEW_LINE);
            buf.append(type + NEW_LINE);

            ++index;
            result = true;
        }

        buf.append(JAVELIN_ARGS_END);
        buf.append(NEW_LINE);

        if (result)
        {
            ret = buf.toString();
        }

        return ret;
    }

    /**
     * Hadoop情報から現在のタスク種別を取得する。
     *
     * @param hadoopInfo Hadoop情報
     * @param messageType CallまたはReturn
     *
     * @return MapかReduceの文字列
     */
    private static String getType(HadoopInfo hadoopInfo, int messageType)
    {
        // messageTypeがCallの時はTaskStatus、
        // messageTypeがReturnの時はTaskTrackerActionから
        // タスク種別を取得する。
        ArrayList<HadoopTaskStatus> taskStatuses = hadoopInfo.getTaskStatuses();
        if (ID_CALL == messageType)
        {
            String stateStr = "";
            if (taskStatuses != null && taskStatuses.size() > 0 && taskStatuses.get(0) != null)
            {
                HadoopTaskStatus hadoopTaskStatus = taskStatuses.get(0);
                State state = hadoopTaskStatus.getState();
                if (state != null)
                {
                    stateStr = "(" + state.toString() + ")";
                }
            }
            
            for (HadoopTaskStatus status : taskStatuses)
            {
                return status.getPhase().toString() + stateStr;
            }
        }
        else if (ID_RETURN == messageType)
        {
            for (HadoopAction action : hadoopInfo.getTaskTrackerActions())
            {
                return action.isMapTask()? "MAP" : "REDUCE";
            }
        }

        return "";
    }
}
