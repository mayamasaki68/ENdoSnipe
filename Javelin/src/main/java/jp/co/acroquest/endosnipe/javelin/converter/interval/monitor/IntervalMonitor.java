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
package jp.co.acroquest.endosnipe.javelin.converter.interval.monitor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jp.co.acroquest.endosnipe.common.config.JavelinConfig;
import jp.co.acroquest.endosnipe.common.event.EventConstants;
import jp.co.acroquest.endosnipe.common.logger.SystemLogger;
import jp.co.acroquest.endosnipe.javelin.StatsJavelinRecorder;
import jp.co.acroquest.endosnipe.javelin.conf.JavelinMessages;
import jp.co.acroquest.endosnipe.javelin.event.CommonEvent;
import jp.co.acroquest.endosnipe.javelin.event.IntervalErrorEvent;
import jp.co.acroquest.endosnipe.javelin.util.ThreadUtil;

/**
 * メソッドの呼び出し間隔を計測し、閾値を超過すれば、アラームを発します。<br />
 * フレームワークの初期化等、システム起動時等に１回だけ実行すれば良いメソッドが、
 * 頻繁に呼び出されていないか確認することができます。<br />
 * 呼び出しを確認した場合は、アラームを発報するため、異常をBottleneckEyeで検出する事が可能です。<br />
 * 
 * @author yamasaki
 */
public class IntervalMonitor
{
    private static final String KEY_CONFIG_ERROR =
            "javelin.conf.AbstractConversionConfig.AnIllegalConfigurationLabel";

    private static final String TOKEN_THRESHOLD = ",";

    private static final String TOKEN_INTERVAL = "=";

    private static final String TOKEN_CLASS_AND_METHOD = "#";

    private static final int INDEX_CLASS_AND_METHOD = 0;

    private static final int INDEX_INTERVAL = 1;

    private static final int SUBINDEX_CLASS = 0;

    private static final int SUBINDEX_METHD = 1;

    private static Map<CallKey, Long> callIntervalMap__ = new ConcurrentHashMap<CallKey, Long>();

    private static Map<CallKey, Long> thresholdMap__ = new ConcurrentHashMap<CallKey, Long>();

    private static JavelinConfig config__;

    /** メソッド呼び出し間隔計測中を表すフラグ */
    private static ThreadLocal<Boolean> isTracing__;

    static
    {
        init();
        isTracing__ = new ThreadLocal<Boolean>() {
            protected Boolean initialValue()
            {
                return Boolean.FALSE;
            }
        };

    }

    /**
     * インスタンス化を防止するコンストラクタです。
     */
    private IntervalMonitor()
    {
        // Do Nothing
    }

    private static void init()
    {
        config__ = new JavelinConfig();
        String threshold = config__.getIntervalThreshold();

        parse(threshold);
    }

    /**
     * 
     * @param className クラス名
     * @param methodName メソッド名
     */
    public static void preProcess(String className, String methodName)
    {
        // メソッド呼び出し間隔計測中の場合、処理を行わない。
        if (isTracing__.get().booleanValue() == true)
        {
            return;
        }

        isTracing__.set(Boolean.TRUE);
        call(className, methodName);
    }

    /**
     * メソッド呼出し後に、メソッド計測中を表すフラグをOFFにします。<br />
     */
    public static void postProcess()
    {
        isTracing__.set(Boolean.FALSE);
    }

    /**
     * 例外発生時に、メソッド計測中を表すフラグをOFFにします。<br />
     */
    public static void postProcessException()
    {
        isTracing__.set(Boolean.FALSE);
    }

    // javelin.propertiesに記述された設定値をパースし、
    // チェック対象のクラス、メソッドと閾値を取得します。
    private static void parse(String threshold)
    {
        String[] thresholds = threshold.split(TOKEN_THRESHOLD);
        for (String config : thresholds)
        {
            String[] values = config.split(TOKEN_INTERVAL);

            if (values.length != 2)
            {
                // チェック対象クラス、メソッドおよび閾値が正しく指定されていない場合、
                // ログに設定エラーを記録します。
                String message = JavelinMessages.getMessage(KEY_CONFIG_ERROR, config);
                SystemLogger.getInstance().warn(message);

                // 次の設定の処理に移行します。
                continue;
            }

            String intervalConfig = values[INDEX_INTERVAL];

            long interval;
            try
            {
                interval = Long.valueOf(intervalConfig);
            }
            catch (NumberFormatException ex)
            {
                // 閾値の設定がlong値として解釈不能な場合、ログに設定エラーを記録します。
                String message = JavelinMessages.getMessage(KEY_CONFIG_ERROR, config);
                SystemLogger.getInstance().warn(message, ex);

                // 次の設定の処理に移行します。
                continue;
            }

            String[] names = values[INDEX_CLASS_AND_METHOD].split(TOKEN_CLASS_AND_METHOD);

            if (names.length == 2)
            {
                String className = names[SUBINDEX_CLASS];
                String methodName = names[SUBINDEX_METHD];

                CallKey key = new CallKey(className, methodName);

                thresholdMap__.put(key, interval);
            }
            else
            {
                // クラス、メソッドが正しく指定されていない場合はログに設定エラーを記録します。
                String message = JavelinMessages.getMessage(KEY_CONFIG_ERROR, config);
                SystemLogger.getInstance().warn(message);
            }
        }
    }

    /**
     * メソッドの呼び出し間隔の判定を行います。<br />
     * 前回呼び出し時からの経過時間をミリ秒単位で求め、
     * 設定ファイルに記述された閾値を下回っていれば、アラームの発報を行います。
     *
     * @param className クラス名
     * @param methodName メソッド名
     */
    private static void call(String className, String methodName)
    {
        try
        {
            CallKey key = new CallKey(className, methodName);
            long presentTime = System.currentTimeMillis();
            Long lastCallTime = callIntervalMap__.get(key);

            if (lastCallTime != null && config__.isIntervalMonitor())
            {
                long diff = presentTime - lastCallTime.longValue();
                Long thresholdLong = thresholdMap__.get(key);
                if (thresholdLong != null)
                {
                    long threshold = thresholdLong.longValue();

                    // 閾値が設定されており、実際の呼び出し間隔が閾値を下回っていれば、
                    // アラームを発報設定を行います。
                    if (threshold > 0 && threshold > diff)
                    {
                        CommonEvent event = new IntervalErrorEvent();
                        event.addParam(EventConstants.PARAM_INTERVALERROR_THRESHOLD,
                                       String.valueOf(threshold));
                        event.addParam(EventConstants.PARAM_INTERVALERROR_ACTUAL_INTERVAL,
                                       String.valueOf(diff));
                        event.addParam(EventConstants.PARAM_INTERVALERROR_CLASSNAME, className);
                        event.addParam(EventConstants.PARAM_INTERVALERROR_METHODNAME, methodName);
                        StackTraceElement[] elements = ThreadUtil.getCurrentStackTrace();
                        String stackTrace = ThreadUtil.getStackTrace(elements);
                        event.addParam(EventConstants.PARAM_INTERVALERROR_STACKTRACE, stackTrace);
                        StatsJavelinRecorder.addEvent(event);
                    }
                }
            }

            // 次回判定用に最新の呼び出し時刻を記録しておきます。
            callIntervalMap__.put(key, presentTime);
        }
        catch (Exception ex)
        {
            //例外発生時はログに記録しておきます。
            SystemLogger.getInstance().warn(ex);
        }
    }

}
