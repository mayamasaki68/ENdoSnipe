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
package jp.co.acroquest.endosnipe.javelin.converter.thread.monitor;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.LinkedHashMap;
import java.util.Map;

import jp.co.acroquest.endosnipe.common.config.JavelinConfig;
import jp.co.acroquest.endosnipe.common.event.EventConstants;
import jp.co.acroquest.endosnipe.common.logger.SystemLogger;
import jp.co.acroquest.endosnipe.communicator.entity.TelegramConstants;
import jp.co.acroquest.endosnipe.javelin.JavelinTransformer;
import jp.co.acroquest.endosnipe.javelin.StatsJavelinRecorder;
import jp.co.acroquest.endosnipe.javelin.event.CommonEvent;
import jp.co.acroquest.endosnipe.javelin.resource.ResourceCollector;
import jp.co.acroquest.endosnipe.javelin.util.ThreadUtil;

/**
 * スレッドダンプを取得するためのスレッドです。<br />
 * 
 * @author fujii
 *
 */
public class ThreadDumpMonitor implements Runnable
{
    /** Singletonオブジェクト */
    private static ThreadDumpMonitor instance__ = new ThreadDumpMonitor();

    /** Javelinの設定。 */
    private final JavelinConfig config_ = new JavelinConfig();

    /** 前回のCPU時間 */
    private long lastCpuTime_ = 0;

    /** 前回のJavaUp時間 */
    private long lastUpTime_ = 0;

    /** Javaアップタイムの差分 */
    private long upTimeDif_ = 0;

    /** プロセス数 */
    private int processorCount_;

    /** CPU使用率 */
    private double cpuUsage_;

    /** スレッド数 */
    private int threadNum_;

    /** 各スレッド毎のCPU使用率 */
    private Map<Long, Double> threadCpuRateMap_;

    /** 前回のスレッド毎のCPU使用時間 */
    private Map<Long, Long> lastThreadCpuMap_;

    /** リソース情報取得用オブジェクト */
    private static ResourceCollector collector__;

    /** CPU使用率に変換するための定数 */
    private static final int CONVERT_RATIO = 10000;

    static
    {
        collector__ = ResourceCollector.getInstance();
    }

    /**
     * インスタンス化を阻止するプライベートコンストラクタ。
     */
    private ThreadDumpMonitor()
    {
        // Do Nothing.
    }

    /**
     * {@link ThreadDumpMonitor}オブジェクトを取得します。<br />
     * 
     * @return {@link ThreadDumpMonitor}オブジェクト
     */
    public static ThreadDumpMonitor getInstance()
    {
        return instance__;
    }

    /**
     * "javelin.thread.dump.interval"の間隔ごとに、
     * スレッドダンプを出力するかどうか判定します。
     */
    public void run()
    {
        try
        {
            Thread.sleep(JavelinTransformer.WAIT_FOR_THREAD_START);
        }
        catch (Exception ex)
        {
            ;
        }
        
        while (true)
        {
            try
            {
                int sleepTime = this.config_.getThreadDumpInterval();
                Thread.sleep(sleepTime);
                // スレッド数が閾値を越えているとき、CPU使用率が閾値を越えているときに、
                // フルスレッドダンプを出力する。
                synchronized (this)
                {
                    if (isThreadDump())
                    {
                        CommonEvent event = createThreadDumpEvent();
                        StatsJavelinRecorder.addEvent(event);
                    }
                }
            }
            catch (Throwable ex)
            {
                SystemLogger.getInstance().warn(ex);
            }
        }
    }

    /**
     * スレッドダンプを出力するかどうかを返します。<br />
     * 条件は以下の2つ
     * <ol>
     * <li>スレッドダンプ取得フラグが<code>true</code></li>
     * <li>スレッド数が閾値を越えている、又はCPU使用率が閾値を越えている</li>
     * </ol>
     * 
     * @return スレッドダンプを出力する場合、<code>true</code>
     */
    private synchronized boolean isThreadDump()
    {
        // CPU使用率は、差分によって計算するため、異常値が出ないように毎回計算する。
        this.cpuUsage_ = getCpuUssage();
        this.threadCpuRateMap_ = getThreadCpuRateMap(this.upTimeDif_);

        // スレッドダンプ取得フラグがOFFのときには、スレッドダンプを取得しない。
        if (config_.isThreadDump() == false)
        {
            return false;
        }

        int threasholdCpu = config_.getThreadDumpCpu();
        if (this.cpuUsage_ > threasholdCpu)
        {
            return true;
        }

        // スレッド数が閾値を越えているときに、スレッドダンプを出力する。
        int threasholdThread = config_.getThreadDumpThreadNum();
        this.threadNum_ = getThreadNum();
        if (this.threadNum_ > threasholdThread)
        {
            return true;
        }
        return false;
    }

    /**
     * CPU使用率を取得します。<br />
     * CPU使用率=(CPU時間の差分)/(JavaのUP時間 * プロセッサ数)
     * 
     * @return CPU使用率
     */
    private synchronized double getCpuUssage()
    {
        Number cpuResource = collector__.getResource(TelegramConstants.ITEMNAME_PROCESS_CPU_TOTAL_TIME);
        Number uptimeResource = collector__.getResource(TelegramConstants.ITEMNAME_JAVAUPTIME);
        Number processorResource =
                collector__.getResource(TelegramConstants.ITEMNAME_SYSTEM_CPU_PROCESSOR_COUNT);

        if (cpuResource == null || uptimeResource == null || processorResource == null)
        {
            return 0;
        }

        long cpuTime = cpuResource.longValue();
        long upTime = uptimeResource.longValue();
        this.processorCount_ = processorResource.intValue();

        // CPU使用率が閾値を越えているときに、スレッドダンプを出力する。
        double cpuUsage = 0;
        if (this.lastUpTime_ != 0)
        {
            this.upTimeDif_ = upTime - this.lastUpTime_;
            cpuUsage =
                    (double)(cpuTime - this.lastCpuTime_)
                            / (this.upTimeDif_ * CONVERT_RATIO * this.processorCount_);
        }
        this.lastCpuTime_ = cpuTime;
        this.lastUpTime_ = upTime;

        return cpuUsage;
    }

    /**
     * スレッド数を取得します。<br />
     * 
     * @return スレッド数
     */
    private synchronized int getThreadNum()
    {
        long[] threadIds = ThreadUtil.getAllThreadIds();
        return threadIds.length;
    }

    /**
     * スレッド毎のCPU時間を保存するMapを返します。<br />
     * 
     * @return スレッド毎のCPU使用時間を保存したMap
     */
    private synchronized Map<Long, Long> getThreadCpuMap()
    {
        ThreadMXBean bean = ManagementFactory.getThreadMXBean();
        long[] threadIds = ThreadUtil.getAllThreadIds();

        Map<Long, Long> threadCpuMap = new LinkedHashMap<Long, Long>();
        for (int num = 0; num < threadIds.length; num++)
        {
            long threadId = threadIds[num];
            threadCpuMap.put(threadId, bean.getThreadCpuTime(threadId));
        }

        return threadCpuMap;

    }

    /**
     * フルスレッドダンプ出力イベントを作成します。<br />
     * [イベント形式]<br />
     * javelin.thread.dump.threadNum=&lt;スレッド数&gt;<br />
     * javelin.thread.dump.cpu.total=&lt;CPU使用率の合計値&gt;<br />
     * javelin.thread.dump.cpu.&lt;スレッドID1&gt;=&lt;スレッド1のCPU使用率&gt;<br />
     * javelin.thread.dump.cpu.&lt;スレッドID2&gt;=&lt;スレッド2のCPU使用率&gt;<br />
     * javelin.thread.dump.cpu.&lt;スレッドID3&gt;=&lt;スレッド3のCPU使用率&gt;<br />
     * ・・・<br />
     * javelin.thread.dump=&lt;フルスレッドダンプ&gt;<br />
     * 
     * @return フルスレッドダンプ出力。
     */
    private synchronized CommonEvent createThreadDumpEvent()
    {
        CommonEvent event = new CommonEvent();

        String threadDump = ThreadUtil.getFullThreadDump();

        event.setName(EventConstants.NAME_THREAD_DUMP);
        event.addParam(EventConstants.PARAM_THREAD_DUMP_THREADNUM, String.valueOf(this.threadNum_));
        event.addParam(EventConstants.PARAM_THREAD_DUMP_CPU_TOTAL, String.valueOf(this.cpuUsage_));

        // スレッド毎のCPU使用率を出力する。
        for (Map.Entry<Long, Double> entry : this.threadCpuRateMap_.entrySet())
        {
            Long threadId = entry.getKey();
            Double cpuRate = entry.getValue();
            if (cpuRate != 0.0)
            {
                event.addParam(EventConstants.PARAM_THREAD_DUMP_CPU + '.' + threadId,
                               String.valueOf(cpuRate));
            }
        }

        event.addParam(EventConstants.PARAM_THREAD_DUMP, threadDump);

        return event;
    }

    /**
     * スレッド毎のCPU使用率を取得します。<br />
     * 
     * @return スレッド毎のCPU使用率
     */
    private synchronized Map<Long, Double> getThreadCpuRateMap(long upTimeDif)
    {
        Map<Long, Long> threadCpuMap = getThreadCpuMap();
        Map<Long, Double> threadCpuRateMap = new LinkedHashMap<Long, Double>();
        for (Map.Entry<Long, Long> entry : threadCpuMap.entrySet())
        {
            Long threadId = entry.getKey();
            if (this.lastThreadCpuMap_ == null)
            {
                threadCpuRateMap.put(threadId, 0.0);
                continue;
            }
            Long cpuTime = entry.getValue();
            Long lastCpuTime = this.lastThreadCpuMap_.get(threadId);
            if (upTimeDif == 0)
            {
                threadCpuRateMap.put(threadId, 0.0);
            }
            else if (lastCpuTime != null)
            {
                double threadCpuRate =
                        (double)(cpuTime - lastCpuTime)
                                / (upTimeDif * CONVERT_RATIO * this.processorCount_);
                threadCpuRateMap.put(threadId, threadCpuRate);
            }
            else
            {
                double threadCpuRate =
                        (double)(cpuTime) / (upTimeDif * CONVERT_RATIO * this.processorCount_);
                threadCpuRateMap.put(threadId, threadCpuRate);
            }
        }
        this.lastThreadCpuMap_ = threadCpuMap;
        return threadCpuRateMap;
    }

    /**
     * スレッドダンプ取得イベントを送信します。<br />
     *
     * @param telegramId 電文 ID
     */
    public synchronized void sendThreadDumpEvent(final long telegramId)
    {
        this.cpuUsage_ = getCpuUssage();
        this.threadNum_ = getThreadNum();
        this.threadCpuRateMap_ = getThreadCpuRateMap(this.upTimeDif_);
        CommonEvent event = createThreadDumpEvent();
        StatsJavelinRecorder.addEvent(event, telegramId);
    }
}
