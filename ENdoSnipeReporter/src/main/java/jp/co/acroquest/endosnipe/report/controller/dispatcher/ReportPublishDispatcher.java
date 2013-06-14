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
package jp.co.acroquest.endosnipe.report.controller.dispatcher;

import jp.co.acroquest.endosnipe.report.LogIdConstants;
import jp.co.acroquest.endosnipe.report.controller.ReportProcessReturnContainer;
import jp.co.acroquest.endosnipe.report.controller.ReportSearchCondition;
import jp.co.acroquest.endosnipe.report.controller.ReportType;
import jp.co.acroquest.endosnipe.report.util.ReporterConfigAccessor;
import jp.co.acroquest.endosnipe.common.logger.ENdoSnipeLogger;
import jp.co.acroquest.endosnipe.report.controller.dispatcher.ReportPublishDispatcher;
import jp.co.acroquest.endosnipe.report.controller.dispatcher.ReportPublishProcessor;

/**
 * 各レポートの処理クラスの実行の振り分けを行うディスパッチャ
 * 
 * @author M.Yoshida
 */
public class ReportPublishDispatcher
{
    /** ロガー */
    private static final ENdoSnipeLogger   LOGGER     =
                                                        ENdoSnipeLogger.getLogger(
                                                                                  ReportPublishDispatcher.class);

    /** インスタンス保持用フィールド */
    private static ReportPublishDispatcher instance__ = null;

    /** インスタンス化を防止するためのコンストラクタ */
    private ReportPublishDispatcher()
    {
    }

    /**
     * ディスパッチャのインスタンスを取得する。
     * 
     * @return インスタンス
     */
    public static ReportPublishDispatcher getInstance()
    {
        if (instance__ == null)
        {
            instance__ = new ReportPublishDispatcher();
        }
        return instance__;
    }

    /**
     * 指定したレポートタイプのレポートを出力するプロセッサを呼び出す。
     * 
     * @param rType レポートタイプ
     * @param cond  プロセッサに渡す絞込み条件
     * @return レポート出力処理の結果
     */
    public ReportProcessReturnContainer dispatch(ReportType rType, ReportSearchCondition cond)
    {
        String reportProcessorName = ReporterConfigAccessor.getReportProcessorName(rType);
        ReportProcessReturnContainer returnContainer = null;

        if (reportProcessorName == null)
        {
            return null;
        }

        ReportPublishProcessor processor = null;

        try
        {
            Class<?> reportProcessorClass = Class.forName(reportProcessorName);
            processor =
                        (ReportPublishProcessor)reportProcessorClass.getConstructor(
                                                                                    ReportType.class).newInstance(
                                                                                                                  rType);
        }
        catch (Exception e1)
        {
            returnContainer = new ReportProcessReturnContainer();
            returnContainer.setHappendedError(e1);
            LOGGER.log(LogIdConstants.EXCEPTION_HAPPENED, e1, new Object[0]);

            return returnContainer;
        }

        try
        {
            returnContainer = processor.publish(cond);
        }
        catch (Throwable e)
        {
            returnContainer = new ReportProcessReturnContainer();
            returnContainer.setHappendedError(e);
        }

        return returnContainer;
    }
}
