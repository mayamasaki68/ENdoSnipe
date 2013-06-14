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
import java.util.ArrayList;
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
import jp.co.acroquest.endosnipe.report.entity.ItemData;
import jp.co.acroquest.endosnipe.report.entity.ItemRecord;
import jp.co.acroquest.endosnipe.report.entity.ServerPoolRecord;
import jp.co.acroquest.endosnipe.report.output.RecordReporter;
import jp.co.acroquest.endosnipe.report.util.ReporterConfigAccessor;

import org.apache.commons.lang.StringUtils;

/**
 * Commons Poolのサイズのレポートを生成するレポートプロセッサ。
 * 
 * @author iida
 */
public class ServerPoolReportProcessor extends ReportPublishProcessorBase
{
    /** ロガー */
    private static final ENdoSnipeLogger LOGGER = ENdoSnipeLogger.getLogger(
            ServerPoolReportProcessor.class);

    /**
     * ReportProcessorを生成する。<br>
     * 
     * @param type
     *            レポート種別
     */
    public ServerPoolReportProcessor(ReportType type)
    {
        super(type);
    }

    /**
     * {@inheritDoc}
     * 
     * @param reportContainer
     */
    @Override
    protected Object getReportPlotData(ReportSearchCondition cond,
            ReportProcessReturnContainer reportContainer)
    {
        // 検索条件を取得する。
        String database = cond.getDatabases().get(0);
        Timestamp startTime = cond.getStartDate();
        Timestamp endTime = cond.getEndDate();

        // DBからデータを検索する。
        List<ItemData> commonsPoolSizeData = null;
        try
        {
            commonsPoolSizeData = GraphItemAccessUtil.findItemData(
                    database, Constants.ITEMNAME_POOL_SIZE,
                    CompressOperator.SIMPLE_AVERAGE, startTime, endTime);
        }
        catch (SQLException ex)
        {
            LOGGER.log(LogIdConstants.EXCEPTION_IN_READING, ex,
                    ReporterConfigAccessor.getReportName(getReportType()));
            return null;
        }

        return commonsPoolSizeData;
    }

    /**
     * {@inheritDoc}
     * 
     * @param reportContainer
     */
    @SuppressWarnings("unchecked")
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
     * @param cond
     * @param reportContainer
     */
    @SuppressWarnings("unchecked")
    @Override
    protected void outputReport(Object plotData, ReportSearchCondition cond,
            ReportProcessReturnContainer reportContainer)
    {
        List<ItemData> commonsPoolSizeData = (List<ItemData>) plotData;
        
        // 項目名が"Max:ConnectionPoolImpl@56ff3d","Num:ConnectionPoolImpl@56ff3d"となっているので、
        // ":"の後が等しいものをまとめ、同一のグラフに出力する。
        Map<String, List<ServerPoolRecord>> serverPoolRecordMap = new HashMap<String, List<ServerPoolRecord>>();
        for (ItemData itemData : commonsPoolSizeData) {
            String[] itemName = StringUtils.split(itemData.getItemName(), ":");
            if (!serverPoolRecordMap.containsKey(itemName[1])) {
                List<ServerPoolRecord> serverPoolRecords = new ArrayList<ServerPoolRecord>(200);
                for (ItemRecord itemRecord : itemData.getRecords()) {
                    ServerPoolRecord serverPoolRecord = new ServerPoolRecord();
                    serverPoolRecord.setMeasurementTime(itemRecord.getMeasurementTime());
                    serverPoolRecords.add(serverPoolRecord);
                }
                serverPoolRecordMap.put(itemName[1], serverPoolRecords);
            }
            for (int index = 0; index < itemData.getRecords().size(); index++) {
                ItemRecord itemRecord = itemData.getRecords().get(index);
                ServerPoolRecord serverPoolRecord = serverPoolRecordMap.get(itemName[1]).get(index);
                if ("Max".equals(itemName[0])) {
                    serverPoolRecord.setServerPoolMax(itemRecord.getValue());
                } else if ("Num".equals(itemName[0])) {
                    serverPoolRecord.setServerPoolNum(itemRecord.getValue());
                }
            }
        }

        // 出力するレポートの種類に応じて、テンプレートのファイルパスを取得する。
        String templateFilePath;
        try
        {
            templateFilePath = TemplateFileManager.getInstance()
                    .getTemplateFile(ReportType.SERVER_POOL);
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
        RecordReporter<ServerPoolRecord> reporter = new RecordReporter<ServerPoolRecord>(
                getReportType());
        
        for (Map.Entry<String, List<ServerPoolRecord>> serverPoolRecordEntry : serverPoolRecordMap.entrySet())
        {
            ServerPoolRecord[] records = serverPoolRecordEntry.getValue().toArray(
                    new ServerPoolRecord[] {});
            String itemName = serverPoolRecordEntry.getKey();
            String[] graphTitles =
            { "Commons Poolのサイズ（" + itemName + "）" };
            reporter.outputReport(templateFilePath, outputFolderPath, itemName,
                    graphTitles, records, startTime, endTime);
        }
    }
}
