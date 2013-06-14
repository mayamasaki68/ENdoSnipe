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

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;


/**
 * ConcurrentMonitorObjectの追加、参照、削除を行うクラスです。
 * 
 * @author eriguchi
 */
public class ConcurrentAccessRegistry
{
    /** キーがオブジェクト識別子、値がConcurrentMonitorObjectのマップ。 */
    private Map<Object, ConcurrentMonitorObject> entryMap_;

    /**
     * コンストラクタ。
     */
    public ConcurrentAccessRegistry()
    {
        WeakHashMap<Object, ConcurrentMonitorObject> map =
                new WeakHashMap<Object, ConcurrentMonitorObject>();
        entryMap_ = Collections.synchronizedMap(map);
    }

    /**
     * ConcurrentMonitorObjectを追加します。
     * 
     * @param newEntry ConcurrentMonitorObject。
     */
    public synchronized void add(ConcurrentMonitorObject newEntry)
    {
        Object ref = newEntry.getRef();
        if(ref != null)
        {
            entryMap_.put(ref, newEntry);
        }
    }

    /**
     * ConcurrentMonitorObjectを削除します。
     * 
     * @param obj 監視対象オブジェクト。
     */
    public synchronized void remove(Object obj)
    {
        entryMap_.remove(obj);
    }

    /**
     * ConcurrentMonitorObjectを取得します。
     * 
     * @param obj 監視対象オブジェクト。
     * @return ConcurrentMonitorObject。
     */
    public ConcurrentMonitorObject get(Object obj)
    {
        ConcurrentMonitorObject entry = entryMap_.get(obj);
        return entry;
    }
}
