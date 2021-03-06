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
package jp.co.acroquest.endosnipe.util;

/**
 * 入力結果
 * 
 * @author sakamoto
 */
public class InsertResult
{
    private int insertCount_;

    private int cacheMissCount_;

    private int cacheOverflowCount_;

    /**
     * InsertCountを返す
     * @return InsertCount
     */
    public int getInsertCount()
    {
        return insertCount_;
    }

    /**
     * InsertCountを設定する
     * @param insertCount InsertCount
     */
    public void setInsertCount(int insertCount)
    {
        insertCount_ = insertCount;
    }

    /**
     * CacheMissCountを返す
     * @return CacheMissCount
     */
    public int getCacheMissCount()
    {
        return cacheMissCount_;
    }

    /**
     * CacheMissCountを設定する
     * @param cacheMissCount CacheMissCount
     */
    public void setCacheMissCount(int cacheMissCount)
    {
        cacheMissCount_ = cacheMissCount;
    }

    /**
     * CacheOverflowCountを返す
     * @return CacheOverflowCount
     */
    public int getCacheOverflowCount()
    {
        return cacheOverflowCount_;
    }

    /**
     * CacheOverflowCountを設定する
     * 
     * @param cacheOverflowCount CacheOverflowCount
     */
    public void setCacheOverflowCount(int cacheOverflowCount)
    {
        cacheOverflowCount_ = cacheOverflowCount;
    }

}
