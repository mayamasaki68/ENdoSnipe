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
 * ConcurrentMonitorObject�̒ǉ��A�Q�ƁA�폜���s���N���X�ł��B
 * 
 * @author eriguchi
 */
public class ConcurrentAccessRegistry
{
    /** �L�[���I�u�W�F�N�g���ʎq�A�l��ConcurrentMonitorObject�̃}�b�v�B */
    private Map<Object, ConcurrentMonitorObject> entryMap_;

    /**
     * �R���X�g���N�^�B
     */
    public ConcurrentAccessRegistry()
    {
        WeakHashMap<Object, ConcurrentMonitorObject> map =
                new WeakHashMap<Object, ConcurrentMonitorObject>();
        entryMap_ = Collections.synchronizedMap(map);
    }

    /**
     * ConcurrentMonitorObject��ǉ����܂��B
     * 
     * @param newEntry ConcurrentMonitorObject�B
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
     * ConcurrentMonitorObject���폜���܂��B
     * 
     * @param obj �Ď��ΏۃI�u�W�F�N�g�B
     */
    public synchronized void remove(Object obj)
    {
        entryMap_.remove(obj);
    }

    /**
     * ConcurrentMonitorObject���擾���܂��B
     * 
     * @param obj �Ď��ΏۃI�u�W�F�N�g�B
     * @return ConcurrentMonitorObject�B
     */
    public ConcurrentMonitorObject get(Object obj)
    {
        ConcurrentMonitorObject entry = entryMap_.get(obj);
        return entry;
    }
}
