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
package jp.co.acroquest.endosnipe.javelin.converter.linear.monitor;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import jp.co.acroquest.endosnipe.common.config.JavelinConfig;
import jp.co.acroquest.endosnipe.common.event.EventConstants;
import jp.co.acroquest.endosnipe.javelin.StatsJavelinRecorder;
import jp.co.acroquest.endosnipe.javelin.event.CommonEvent;
import jp.co.acroquest.endosnipe.javelin.event.LinearSearchEvent;
import jp.co.acroquest.endosnipe.javelin.util.StatsUtil;
import jp.co.acroquest.endosnipe.javelin.util.ThreadUtil;

/**
 * 線形検索を検出するモニタクラスです。<br />
 * 
 * @author fujii
 *
 */
public class LinearSearchMonitor
{
    /** オブジェクトが前回指定したインデックスを保存するマップ */
    private static ThreadLocal<Map<Object, Integer>> lastIndexMap__;

    /** 線形検索イベント検出を保存するマップ */
    private static Map<Object, AtomicInteger> searchCountMap__ =
            new WeakHashMap<Object, AtomicInteger>();

    /** LinkedList#indexOf() 呼び出し回数を保存するマップ */
    private static Map<Object, AtomicInteger> indexOfMap__ =
            new WeakHashMap<Object, AtomicInteger>();

    /** Javelinの設定値 */
    private static JavelinConfig config__ = new JavelinConfig();

    /** 線形検索検出の判定中かどうかを表すフラグ */
    private static ThreadLocal<Boolean> isTracing__;

    /** 線形検索検出の判定対象 */
    private static ThreadLocal<Object> target__;

    /** 線形検索検出を行うかどうかを表すフラグ */
    private static boolean isMonitor__;

    /** 線形検索対象となるリストのサイズの閾値 */
    private static int linearSearchlistSize__;

    /** 線形検索対象となる、リストに対する線形アクセス回数の割合の閾値 */
    private static double listRatio__;

    static
    {
        isTracing__ = new ThreadLocal<Boolean>() {
            protected Boolean initialValue()
            {
                return Boolean.TRUE;
            }
        };

        target__ = new ThreadLocal<Object>();

        lastIndexMap__ = new ThreadLocal<Map<Object, Integer>>() {
            protected synchronized Map<Object, Integer> initialValue()
            {
                return new WeakHashMap<Object, Integer>();
            }
        };

        isMonitor__ = config__.isLinearSearchMonitor();
        linearSearchlistSize__ = config__.getLinearSearchListSize();
        listRatio__ = config__.getLinearSearchListRatio();
    }

    /**
     * オブジェクトのインスタンス化を阻止するプライベートコンストラクタです。<br />
     */
    private LinearSearchMonitor()
    {
        // Do Nothing.
    }

    /**
     * 線形検索をしているかどうかを検出します。（ get() が実行されると呼ばれます。）<br />
     * @param obj オブジェクト
     * @param index インデックス
     */
    public static void postProcess(Object obj, int index)
    {
        // 線形検索検出フラグがOFFになっている場合は処理を行わない。
        if (isMonitor__ == false)
        {
            return;
        }
        if (isTracing__.get().booleanValue() == false)
        {
            return;
        }
        
        if (target__.get() == null)
        {
            return;
        }
        target__.set(null);
        
        isTracing__.set(Boolean.FALSE);

        try
        {
            detect(obj, index);
        }
        finally
        {
            isTracing__.set(Boolean.TRUE);
        }
    }

    /**
     * 線形検索の検出のメイン処理を行う。
     * 
     * @param obj 検出対象。
     * @param index インデックス。
     */
    private static void detect(Object obj, int index)
    {
        if (obj instanceof List<?>)
        {
            List<?> list = (List<?>)obj;
            int listSize = list.size();
            AtomicInteger countInteger = searchCountMap__.get(list);
            if (countInteger != null)
            {
                int searchCount = countInteger.intValue();
                if (searchCount >= (int)((listSize - 1) * listRatio__))
                {
                    return;
                }
            }
            if (listSize >= linearSearchlistSize__)
            {
                detect(list, index);
            }
        }
    }

    public static void preProcess(Object obj)
    {
        // 線形検索検出フラグがOFFになっている場合は処理を行わない。
        if (isMonitor__ == false)
        {
            return;
        }
        if (isTracing__.get().booleanValue() == false)
        {
            return;
        }
        target__.set(obj);
    }
    
    /**
     * 線形検索をしているかどうかを検出します。（ indexOf() が実行されると呼ばれます。）<br />
     *
     * @param obj オブジェクト
     * @param search 検索したオブジェクト
     * @param index indexOf() の戻り値
     */
    public static void postProcessIndexOf(Object obj, Object search, int index)
    {
        // 線形検索検出フラグがOFFになっている場合は処理を行わない。
        if (isMonitor__ == false)
        {
            return;
        }
        if (isTracing__.get().booleanValue() == false)
        {
            return;
        }
        
        if (target__.get() == null)
        {
            return;
        }
        target__.set(null);
        
        isTracing__.set(Boolean.FALSE);
        try
        {
            if (obj instanceof List<?>)
            {
                List<?> list = (List<?>)obj;
                int listSize = list.size();
                if (listSize >= linearSearchlistSize__)
                {
                    detectIndexOf(list, index);
                }
            }
        }
        finally
        {
            isTracing__.set(Boolean.TRUE);
        }
    }

    /**
     * 線形検索を検出します。（ get() 版）<br />
     * 以下の条件を満たす場合、線形検索として検出します。
     * <li>インデックスの値がリストサイズに対して一定以上の割合(javelin.propertiesにより設定)より１大きい(小さい)。</li>
     * <li>リストに指定したインデックスが、前回指定したインデックスより１大きい(小さい)。</li>
     * <li>オブジェクトに対する線形検索が未検出である。</li>
     * 
     * @param list 線形検索検出対象となるリスト
     * @param index インデックス
     */
    public static void detect(List<?> list, int index)
    {
        Map<Object, Integer> lastIndexMap = lastIndexMap__.get();
        Integer lastIndex = lastIndexMap.get(list);

        if (lastIndex != null)
        {
            int listSize = list.size();
            int searchCount = 0;

            if (detectLinearSearch(listSize, index, lastIndex.intValue()))
            {
                AtomicInteger searchCountObj;
                synchronized (searchCountMap__)
                {
                    searchCountObj = searchCountMap__.get(list);
                    if (searchCountObj == null)
                    {
                        searchCountMap__.put(list, new AtomicInteger(1));
                        searchCount = 1;
                    }
                    else
                    {
                        searchCount = searchCountObj.incrementAndGet();
                    }
                }
                if (searchCount == (int)((listSize - 1) * listRatio__))
                {
                    // 非同期複数スレッドアクセスイベントを発生させる
                    CommonEvent event = createDetectedEvent(list, searchCountObj);
                    StatsJavelinRecorder.addEvent(event);
                }
            }
        }
        // 検出位置を保存する。
        lastIndexMap.put(list, Integer.valueOf(index));
    }

    /**
     * 線形検索であるかどうかを判定します。<br />
     * 
     * @param index 検索中のリストのインデックス
     * @param lastIndex 前回アクセスしたリストのインデックス
     * @return 線形検索である場合、<code>true</code>
     */
    private static boolean detectLinearSearch(int listSize, int index, int lastIndex)
    {
        if (index == lastIndex + 1)
        {
            return true;
        }
        if (index == lastIndex - 1)
        {
            return true;
        }
        return false;
    }

    /**
     * 線形検索を検出します。（ indexOf() 版）<br />
     *
     * @param list 線形検索検出対象となるリスト
     * @param index インデックス
     */
    private static void detectIndexOf(List<?> list, int index)
    {
        int searchCount = 0;

        synchronized (indexOfMap__)
        {
            AtomicInteger searchCountObj = indexOfMap__.get(list);
            if (searchCountObj == null)
            {
                indexOfMap__.put(list, new AtomicInteger(1));
                searchCount = 1;
            }
            else
            {
                searchCount = searchCountObj.incrementAndGet();
            }
        }
        if (searchCount == (int)listRatio__)
        {
            // 非同期複数スレッドアクセスイベントを発生させる
            CommonEvent event = createDetectedEvent(list, indexOfMap__.get(list));
            StatsJavelinRecorder.addEvent(event);
        }
    }

    /**
     * 線形検索検出イベントを作成します。<br />
     * [出力内容]<br />
     * ■イベント名<br />
     * LinearSearchDetected : 線形検索検時のイベント名<br />
     * <br />
     * ■パラメータ<br />
     * linearsearch.size : 線形検索検出したリストのサイズ<br />
     * linearsearch.count : 線形アクセス回数<br />
     * linearsearch.objectID : 線形検索検出したリストのオブジェクトID<br />
     * linearsearch.stackTrace : 線形検索検出時のスタックトレース<br />
     * 
     * @param list 線形検索を検出したリスト
     * @param countObj 線形検索を行った回数
     * @return 線形検索検出イベント
     */
    public static CommonEvent createDetectedEvent(final List<?> list, final AtomicInteger countObj)
    {
        CommonEvent event = new LinearSearchEvent();
        event.addParam(EventConstants.PARAM_LINEARSEARCH_SIZE, String.valueOf(list.size()));
        if (countObj != null)
        {
            String searchCountStr = String.valueOf(countObj);
            event.addParam(EventConstants.PARAM_LINEARSEARCH_COUNT, searchCountStr);
        }
        else
        {
            event.addParam(EventConstants.PARAM_LINEARSEARCH_COUNT, String.valueOf(0));
        }
        String objectID = StatsUtil.createIdentifier(list);
        event.addParam(EventConstants.PARAM_LINEARSEARCH_OBJECTID, objectID);

        StackTraceElement[] stacktraces = ThreadUtil.getCurrentStackTrace();
        String stackTrace = ThreadUtil.getStackTrace(stacktraces, config__.getTraceDepth());

        event.addParam(EventConstants.PARAM_LINEARSEARCH_STACKTRACE, stackTrace);
        return event;
    }

    /**
     * 線形検索検出を行うかどうかを設定します。<br />
     * 
     * @param isMonitor 線形検索検出を行う場合は、<code>true</code>
     */
    public static void setMonitor(boolean isMonitor)
    {
        isMonitor__ = isMonitor;
    }

    /**
     * 線形検索検出を行うリストの閾値を設定します。<br />
     * 
     * @param listSize 線形検索検出を行うリストの閾値
     */
    public static void setLinearSearchListSize(int listSize)
    {
        linearSearchlistSize__ = listSize;
    }

    /**
     * 線形検索対象となる、リストに対する線形アクセス回数の割合の閾値を設定します。
     * 
     * @param listRatio 線形検索対象となる、リストに対する線形アクセス回数の割合の閾値
     */
    public static void setListRatio(double listRatio)
    {
        listRatio__ = listRatio;
    }

}
