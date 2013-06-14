/*
 * Copyright (c) 2004-2009 SMG Co., Ltd. All Rights Reserved.
 * Please read the associated COPYRIGHTS file for more details.
 *
 * THE  SOFTWARE IS  PROVIDED BY  SMG Co., Ltd., WITHOUT  WARRANTY  OF
 * ANY KIND,  EXPRESS  OR IMPLIED,  INCLUDING BUT  NOT LIMITED  TO THE
 * WARRANTIES OF  MERCHANTABILITY,  FITNESS FOR A  PARTICULAR  PURPOSE
 * AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDER BE LIABLE FOR ANY
 * CLAIM, DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING
 * OR DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES.
 */
package jp.co.acroquest.endosnipe.report.dao;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import jp.co.acroquest.endosnipe.common.Constants;
import jp.co.acroquest.endosnipe.common.logger.ENdoSnipeLogger;
import jp.co.acroquest.endosnipe.report.entity.ApplicationRecord;
import jp.co.acroquest.endosnipe.report.entity.ReportItemValue;

/**
 * アプリケーション情報をDBから取得するアクセサクラス。
 * 
 * @author akiba
 */
public class ApplicationRecordAccessor
{
    /** ロガー */
    private static final ENdoSnipeLogger LOGGER     = ENdoSnipeLogger.getLogger(
                                                            ApplicationRecordAccessor.class);

    /** 最大件数 */
    public static final int              ITEM_COUNT = 200;

    /**
     * 期間を指定し、その期間内での<br/>
     * アプリケーションのレポートデータを取得する。
     * 
     * @param database データベース名。
     * @param startTime 検索条件(開始時刻)。
     * @param endTime 検索条件(終了時刻)。
     * @return アプリケーションのレポートデータ。
     * @throws SQLException データ取得時に例外が発生した場合
     */
    public List<ApplicationRecord> findApplicationStaticsByTerm(
            String database, Timestamp startTime, Timestamp endTime)
            throws SQLException
    {
        List<ApplicationRecord> result = new ArrayList<ApplicationRecord>();

        // データベースから値を取得する
        List<ReportItemValue> httpSessionInstanceNumValues;
        List<ReportItemValue> httpSessionObjectSizeValues;

        httpSessionInstanceNumValues = ReportDao.selectAverage(database,
                startTime, endTime, Constants.ITEMNAME_HTTPSESSION_NUM);
        httpSessionObjectSizeValues = ReportDao.selectAverage(database,
                startTime, endTime, Constants.ITEMNAME_HTTPSESSION_TOTALSIZE);

        int httpSessionObjectCnt = 0;
        for (int index = 0; index < httpSessionInstanceNumValues.size(); index++)
        {
            ApplicationRecord record = new ApplicationRecord();
            ReportItemValue httpSessionInstanceNum;
            ReportItemValue httpSessionObjectSize;

            httpSessionInstanceNum = httpSessionInstanceNumValues.get(index);

            httpSessionObjectSize = new ReportItemValue();
            httpSessionObjectSize.measurementTime = httpSessionInstanceNumValues.get(index).measurementTime;
            if (httpSessionObjectSizeValues.size() > httpSessionObjectCnt)
            {
                if (httpSessionObjectSizeValues.get(httpSessionObjectCnt).measurementTime.compareTo(httpSessionInstanceNumValues.get(index).measurementTime) <= 0)
                {
                    httpSessionObjectSize = httpSessionObjectSizeValues.get(httpSessionObjectCnt);
                    httpSessionObjectCnt++;
                }
            }

            record.setHttpInstanceMeasurementTime(httpSessionInstanceNum.measurementTime);
            record.setHttpSessionInstanceNum(httpSessionInstanceNum.summaryValue.longValue());
            record.setHttpSessionInstanceNumMax(httpSessionInstanceNum.maxValue.longValue());
            record.setHttpSessionInstanceNumMin(httpSessionInstanceNum.minValue.longValue());

            record.setHttpSessionMeasurementTime(httpSessionObjectSize.measurementTime);
            record.setHttpSessionObjectSize(httpSessionObjectSize.summaryValue.longValue());
            record.setHttpSessionObjectSizeMax(httpSessionObjectSize.maxValue.longValue());
            record.setHttpSessionObjectSizeMin(httpSessionObjectSize.minValue.longValue());

            result.add(record);
        }

        return result;
    }
}
