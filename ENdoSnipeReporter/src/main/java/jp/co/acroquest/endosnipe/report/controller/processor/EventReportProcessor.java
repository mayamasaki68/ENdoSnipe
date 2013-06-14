/*
 * Copyright (c) 2004-2010 SMG Co., Ltd. All Rights Reserved.
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
package jp.co.acroquest.endosnipe.report.controller.processor;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jp.co.acroquest.endosnipe.common.Constants;
import jp.co.acroquest.endosnipe.common.logger.ENdoSnipeLogger;
import jp.co.acroquest.endosnipe.report.LogIdConstants;
import jp.co.acroquest.endosnipe.report.controller.ReportProcessReturnContainer;
import jp.co.acroquest.endosnipe.report.controller.ReportSearchCondition;
import jp.co.acroquest.endosnipe.report.controller.ReportType;
import jp.co.acroquest.endosnipe.report.controller.TemplateFileManager;
import jp.co.acroquest.endosnipe.report.converter.compressor.CompressOperator;
import jp.co.acroquest.endosnipe.report.dao.util.GraphItemAccessUtil;
import jp.co.acroquest.endosnipe.report.entity.ApplicationRecord;
import jp.co.acroquest.endosnipe.report.entity.ItemData;
import jp.co.acroquest.endosnipe.report.output.RecordReporter;
import jp.co.acroquest.endosnipe.report.util.ReporterConfigAccessor;

/**
 * Javelinのイベント数のレポートを生成するレポートプロセッサです。
 * 
 * @author iida
 */
public class EventReportProcessor extends ReportPublishProcessorBase
{
    /** ロガー */
    private static final ENdoSnipeLogger LOGGER = ENdoSnipeLogger.getLogger(
            EventReportProcessor.class);

    /**
     * ReportProcessorを生成します。<br>
     * 
     * @param type レポート種別
     */
    public EventReportProcessor(ReportType type)
    {
        super(type);
    }

    /**
     * {@inheritDoc}
     * 
     * @param cond レポート出力設定
     * @param reportContainer レポート出力処理のコンテナ
     */
    @Override
    protected Object getReportPlotData(ReportSearchCondition cond,
            ReportProcessReturnContainer reportContainer)
    {
        // 検索条件を取得します。
        String database = cond.getDatabases().get(0);
        Timestamp startTime = cond.getStartDate();
        Timestamp endTime = cond.getEndDate();

        // DBからデータを検索します。
        List<ItemData> eventCountData;
        try
        {
            eventCountData = GraphItemAccessUtil.findItemData(
                    database, Constants.ITEMNAME_EVENT_COUNT,
                    CompressOperator.SIMPLE_AVERAGE, startTime, endTime);
        }
        catch (SQLException ex)
        {
            LOGGER.log(LogIdConstants.EXCEPTION_IN_READING, ex,
                    ReporterConfigAccessor.getReportName(getReportType()));
            return null;
        }

        // 取得したデータをMapにまとめてリターンします。
        Map<String, List<?>> data = new HashMap<String, List<?>>();
        data.put(Constants.ITEMNAME_EVENT_COUNT, eventCountData);

        return data;
    }
    
    /**
     * {@inheritDoc}
     * 
     * @param reportContainer
     */
    @Override
    protected Object convertPlotData(Object rawData,
            ReportSearchCondition cond,
            ReportProcessReturnContainer reportContainer)
    {
        // データ変換は特に行いません。
        return rawData;
    }

    /**
     * {@inheritDoc}
     * 
     * @param plotData グラフ出力データ
     * @param cond レポート出力設定
     * @param reportContainer レポート出力処理のコンテナ
     */
    @Override
    protected void outputReport(Object plotData, ReportSearchCondition cond,
            ReportProcessReturnContainer reportContainer)
    {
        if (plotData instanceof Map == false)
        {
            return;
        }
        
        // Mapからグラフのデータを取得します。
        Map<String, List<ItemData>> data = (Map<String, List<ItemData>>) plotData;

        List<ItemData> eventCountDataList = data.get(Constants.ITEMNAME_EVENT_COUNT);

        // 出力するレポートの種類に応じて、テンプレートのファイルパスを取得する。
        String templateFilePath;
        try
        {
            templateFilePath = TemplateFileManager.getInstance()
                    .getTemplateFile(ReportType.EVENT);
        }
        catch (IOException exception)
        {
            reportContainer.setHappendedError(exception);
            return;
        }

        // レポート出力の引数情報を取得する。
        String outputFolderPath = getOutputFolderName()
                + File.separator
                + ReporterConfigAccessor.getProperty(super.getReportType()
                        .getId()
                        + ".outputFile");
        Timestamp startTime = cond.getStartDate();
        Timestamp endTime = cond.getEndDate();

        // レポート出力を実行する。
        RecordReporter<ApplicationRecord> reporter = new RecordReporter<ApplicationRecord>(
                getReportType());
        reporter.outputReports(templateFilePath, outputFolderPath,
                eventCountDataList, startTime, endTime);
    }
}
