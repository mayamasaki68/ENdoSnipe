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

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jp.co.acroquest.endosnipe.common.config.JavelinConfig;
import jp.co.acroquest.endosnipe.common.event.EventConstants;
import jp.co.acroquest.endosnipe.common.logger.SystemLogger;
import jp.co.acroquest.endosnipe.communicator.entity.TelegramConstants;
import jp.co.acroquest.endosnipe.javelin.JavelinTransformer;
import jp.co.acroquest.endosnipe.javelin.StatsJavelinRecorder;
import jp.co.acroquest.endosnipe.javelin.event.CommonEvent;
import jp.co.acroquest.endosnipe.javelin.resource.ResourceCollector;

/**
 * フルGCを監視するモニタクラスです。<br />
 * 
 * @author fujii
 *
 */
public class FullGCMonitor implements Runnable
{
    /** GC発生回数を保存するMap */
    private Map<String, Long> gcCountMap_ = new HashMap<String, Long>();

    /** GC実行時間を保存するMap */
    private Map<String, Long> gcTimeMap_ = new HashMap<String, Long>();

    /** フルGCを発生させるGarbageCollectorの名前のリスト */
    private static final List<String> FULLGC_NAME_LIST;

    /** Javelinの設定。 */
    private static JavelinConfig config__ = new JavelinConfig();

    /** リソース情報取得用オブジェクト */
    private static ResourceCollector collector__;

    /** Singletonインスタンス */
    private static FullGCMonitor instance__ = new FullGCMonitor();

    /** スリープ時間 */
    private static final int SLEEP_TIME = 5000;

    static
    {
        String fullGCNameList = config__.getFullGCList();
        FULLGC_NAME_LIST = Arrays.asList(fullGCNameList.split(","));
        collector__ = ResourceCollector.getInstance();
    }

    /**
     * オブジェクトのインスタンス化を阻止するプライベートコンストラクタ
     */
    private FullGCMonitor()
    {
        // Do Nothing.
    }

    /**
     * フルGC取得モニタを取得します。<br />
     * 
     * @return {@link FullGCMonitor}オブジェクト
     */
    public static FullGCMonitor getInstance()
    {
        return instance__;
    }

    /**
     * フルGCが発生しているか、5秒ごとにチェックします。<br />
     * フルGCが発生している場合、イベントを作成し、送信します。
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
            if (config__.isFullGCMonitor())
            {
                List<GarbageCollectorMXBean> gcBeanList =
                        ManagementFactory.getGarbageCollectorMXBeans();
                for (GarbageCollectorMXBean gcBean : gcBeanList)
                {
                    String gcName = gcBean.getName();
                    if (FULLGC_NAME_LIST.contains(gcName))
                    {
                        detect(gcBean, gcName);
                    }
                }
            }
            try
            {
                Thread.sleep(SLEEP_TIME);
            }
            catch (InterruptedException ex)
            {
                SystemLogger.getInstance().warn(ex);
            }
        }
    }

    /**
     * フルGCの検出を行います。<br />
     * [条件]<br />
     *　・GCの実行回数が前回時よりも増えているとき。<br />
     * ・GCの実行時間が閾値を越えているとき。
     * 
     * @param gcBean {@link GarbageCollectorMXBean}オブジェクト
     * @param gcName GarbageCollector名
     */
    private void detect(GarbageCollectorMXBean gcBean, String gcName)
    {
        Long lastGCCount = this.gcCountMap_.get(gcName);
        long gcCount = gcBean.getCollectionCount();

        // １回目は、GC回数が1回以上のときのみ、フルGCイベントを発生させる。
        // ２回目以降は、GC発生回数が増えていたときのみ、フルGCイベントを発生させる。
        if (lastGCCount == null)
        {
            this.gcCountMap_.put(gcName, gcCount);
            if (gcCount > 0)
            {
                long gcTime = gcBean.getCollectionTime();
                CommonEvent event = createFullGCEvent(gcCount, gcTime);
                StatsJavelinRecorder.addEvent(event);
                this.gcTimeMap_.put(gcName, gcTime);
            }
        }
        else
        {
            this.gcCountMap_.put(gcName, gcCount);
            if (lastGCCount.longValue() < gcCount)
            {
                Long lastGCTime = this.gcTimeMap_.get(gcName);
                if (lastGCTime == null)
                {
                    lastGCTime = Long.valueOf(0);
                }
                long gcTime = gcBean.getCollectionTime() - lastGCTime.longValue();
                if (gcTime >= config__.getFullGCThreshold())
                {
                    CommonEvent event = createFullGCEvent(gcCount, gcTime);
                    StatsJavelinRecorder.addEvent(event);
                }
                this.gcTimeMap_.put(gcName, gcTime);
            }
        }
    }

    /**
     * FullGC発生イベントを作成します。<br />
     * [イベント形式]<br />
     * ■イベント名<br />
     *  FullGCDetected<br />
     * ■パラメータ<br />
     *  javelin.fullgc.count=&lt;フルGC発生回数&gt;<br />
     *  javelin.fullgc.time=&lt;フルGC実行時間(5秒以内で発生したGC時間の合計値)&gt;<br />
     *  javelin.fullgc.heapMemory=&lt;現在のヒープメモリ&gt;<br />
     * 
     * @param count フルGCの発生回数
     * @param time フルGCの実行時間
     * @return FullGC発生イベント
     */
    public CommonEvent createFullGCEvent(final long count, final long time)
    {
        CommonEvent event = new CommonEvent();
        event.setName(EventConstants.NAME_FULLGC);
        event.setLevel(CommonEvent.LEVEL_INFO);
        event.addParam(EventConstants.PARAM_FULLGC_COUNT, String.valueOf(count));
        event.addParam(EventConstants.PARAM_FULLGC_TIME, String.valueOf(time));

        Number memoryResource =
                collector__.getResource(TelegramConstants.ITEMNAME_SYSTEM_MEMORY_VIRTUAL_USED);
        if (memoryResource != null && memoryResource instanceof Long)
        {
            event.addParam(EventConstants.PARAM_FULLGC_HEAPMEMORY, String.valueOf(memoryResource));
        }

        return event;

    }
}
