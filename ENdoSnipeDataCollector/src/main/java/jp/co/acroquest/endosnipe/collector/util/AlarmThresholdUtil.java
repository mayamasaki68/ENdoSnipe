/*******************************************************************************
 * ENdoSnipe 5.0 - (https://github.com/endosnipe)
 * 
 * The MIT License (MIT)
 * 
 * Copyright (c) 2013 Acroquest Technology Co.,Ltd.
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
package jp.co.acroquest.endosnipe.collector.util;

import jp.co.acroquest.endosnipe.collector.LogMessageCodes;
import jp.co.acroquest.endosnipe.common.entity.MeasurementData;
import jp.co.acroquest.endosnipe.common.entity.MeasurementDetail;
import jp.co.acroquest.endosnipe.common.entity.ResourceData;
import jp.co.acroquest.endosnipe.common.logger.ENdoSnipeLogger;

/**
 * システムリソースの閾値判定用ユーティリティクラス
 * @author fujii
 *
 */
public class AlarmThresholdUtil implements LogMessageCodes
{
    private static final ENdoSnipeLogger LOGGER =
                                                  ENdoSnipeLogger.getLogger(AlarmThresholdUtil.class);

    private AlarmThresholdUtil()
    {
        //do nothing
    }

    /**
     * ResourceData から、指定したitemName の値を取得する
     * @param resourceData データの入っているresourceData
     * @param itemName 指定するkey
     * 
     * @return 指定したitemName の値がない場合、nullを返す
     */
    public static Number getNumberFromResourceData(final ResourceData resourceData,
            final String itemName)
    {
        MeasurementData measurementData = resourceData.getMeasurementMap().get(itemName);

        if (measurementData == null)
        {
            return null;
        }

        // 単一のデータ系列のときは、空文字をキーに、一つの値のみが入っている
        MeasurementDetail measurementDetail = measurementData.getMeasurementDetailMap().get("");

        if (measurementDetail == null)
        {
            return null;
        }
        String value = measurementDetail.value;
        Number number = null;

        try
        {
            number = Long.valueOf(value);
        }
        catch (NumberFormatException ex)
        {
            LOGGER.log(SYSTEM_UNKNOW_ERROR, ex.getMessage(), ex);
        }

        return number;
    }
}
