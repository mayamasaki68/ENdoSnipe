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
package jp.co.acroquest.endosnipe.javelin.bean;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jp.co.acroquest.endosnipe.javelin.MBeanManager;
import jp.co.acroquest.endosnipe.javelin.comparator.AverageComparator;
import jp.co.acroquest.endosnipe.javelin.comparator.MaximumComparator;
import jp.co.acroquest.endosnipe.javelin.comparator.MinimumComparator;
import jp.co.acroquest.endosnipe.javelin.comparator.ThrowableComparator;

/**
 * 統計処理用MBean。<br>
 * S2JmxJavelinで蓄積した情報に対して統計処理を行った結果を返す。<br>
 * 現状、以下の情報を取得することが可能。
 * <ol>
 * <li>平均値でソートしたメソッドコール情報。</li>
 * <li>最大値でソートしたメソッドコール情報。</li>
 * <li>最小値でソートしたメソッドコール情報。</li>
 * <li>例外の発生回数でソートしたメソッドコール情報。</li>
 * </ol>
 * 
 * @author acroquest
 */
public class Statistics implements StatisticsMBean
{
    /**
     * {@inheritDoc}
     */
    public synchronized List<InvocationMBean> getInvocationListOrderByAverage()
    {
        List<InvocationMBean> list = createInvocationList();
        Collections.sort(list, new AverageComparator());

        return list;
    }

    /**
     * {@inheritDoc}
     */
    public synchronized List<InvocationMBean> getInvocationListOrderByMaximum()
    {
        List<InvocationMBean> list = createInvocationList();
        Collections.sort(list, new MaximumComparator());

        return list;
    }

    /**
     * {@inheritDoc}
     */
    public synchronized List<InvocationMBean> getInvocationListOrderByMinimum()
    {
        List<InvocationMBean> list = createInvocationList();
        Collections.sort(list, new MinimumComparator());

        return list;
    }

    /**
     * {@inheritDoc}
     */
    public synchronized List<InvocationMBean> getInvocationListOrderByThrowableCount()
    {
        List<InvocationMBean> list = createInvocationList();
        Collections.sort(list, new ThrowableComparator());

        return list;
    }

    private synchronized List<InvocationMBean> createInvocationList()
    {
        List<InvocationMBean> list = new ArrayList<InvocationMBean>();

        for (ComponentMBean component : MBeanManager.getAllComponents())
        {
            for (InvocationMBean invocation : component.getAllInvocation())
            {
                list.add(invocation);
            }
        }

        return list;
    }
}
