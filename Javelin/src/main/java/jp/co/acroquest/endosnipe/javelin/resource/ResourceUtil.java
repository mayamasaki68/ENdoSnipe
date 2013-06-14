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
package jp.co.acroquest.endosnipe.javelin.resource;

import java.util.ArrayList;
import java.util.List;

import jp.co.acroquest.endosnipe.common.entity.ResourceItem;
import jp.co.acroquest.endosnipe.javelin.converter.leak.monitor.CollectionMonitorEntry;

/**
 * resourceパッケージで利用するユーティリティ。
 * 
 * @author eriguchi
 */
public class ResourceUtil
{
    /**
     * プライベートコンストラクタです。
     */
    private ResourceUtil()
    {
        // Do Nothing.
    }

    /**
     * CollectionMonitorEntryからResourceEntryへの型変換を行う。
     * @param list CollectionMonitorEntryのリスト
     * @return ResourceEntryのリスト
     */
    static List<ResourceItem> convert(List<CollectionMonitorEntry> list)
    {
        List<ResourceItem> result = new ArrayList<ResourceItem>(list.size());
        for (CollectionMonitorEntry collectionMonitorEntry : list)
        {
            ResourceItem entry = new ResourceItem();
            entry.setName(collectionMonitorEntry.getEntryIdentifier());
            entry.setValue(String.valueOf(collectionMonitorEntry.getEntryNumber()));
            result.add(entry);
        }
        return result;
    }
}
