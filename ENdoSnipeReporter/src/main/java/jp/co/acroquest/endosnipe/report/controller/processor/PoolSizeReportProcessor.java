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
import jp.co.acroquest.endosnipe.report.entity.PoolSizeRecord;
import jp.co.acroquest.endosnipe.report.output.RecordReporter;
import jp.co.acroquest.endosnipe.report.util.ReporterConfigAccessor;

import org.apache.commons.lang.StringUtils;

/**
 * APサーバのワーカスレッド数のレポートを生成するレポートプロセッサ。
 * 
 * @author iida
 */
public class PoolSizeReportProcessor extends ReportPublishProcessorBase
{
    /** ロガー */
    private static final ENdoSnipeLogger LOGGER = ENdoSnipeLogger.getLogger(
            PoolSizeReportProcessor.class);

    /**
     * ReportProcessorを生成する。<br>
     * 
     * @param type
     *            レポート種別
     */
    public PoolSizeReportProcessor(ReportType type)
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
        List<ItemData> workerThreadNumData = null;
        try
        {
            workerThreadNumData = GraphItemAccessUtil.findItemData(
                    database, Constants.ITEMNAME_SERVER_POOL,
                    CompressOperator.SIMPLE_AVERAGE, startTime, endTime);
        }
        catch (SQLException ex)
        {
            LOGGER.log(LogIdConstants.EXCEPTION_IN_READING, ex,
                    ReporterConfigAccessor.getReportName(getReportType()));
            return null;
        }

        return workerThreadNumData;
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
        List<ItemData> workerThreadNumData = (List<ItemData>) plotData;
        
        // 項目名が"http-8080_max","http-8080_current","http-8080_wait"となっているので、
        // "_"の前が等しいものをまとめ、同一のグラフに出力する。
        Map<String, List<PoolSizeRecord>> poolSizeRecordMap = new HashMap<String, List<PoolSizeRecord>>();
        for (ItemData itemData : workerThreadNumData) {
            String[] itemName = StringUtils.split(itemData.getItemName(), "_");
            if (!poolSizeRecordMap.containsKey(itemName[0])) {
                List<PoolSizeRecord> poolSizeRecords = new ArrayList<PoolSizeRecord>(200);
                for (ItemRecord itemRecord : itemData.getRecords()) {
                    PoolSizeRecord poolSizeRecord = new PoolSizeRecord();
                    poolSizeRecord.setMeasurementTime(itemRecord.getMeasurementTime());
                    poolSizeRecords.add(poolSizeRecord);
                }
                poolSizeRecordMap.put(itemName[0], poolSizeRecords);
            }
            for (int index = 0; index < itemData.getRecords().size(); index++) {
                ItemRecord itemRecord = itemData.getRecords().get(index);
                PoolSizeRecord poolSizeRecord = poolSizeRecordMap.get(itemName[0]).get(index);
                if ("max".equals(itemName[1])) {
                    poolSizeRecord.setPoolSizeMax(itemRecord.getValue());
                } else if ("current".equals(itemName[1])) {
                    poolSizeRecord.setPoolSizeCurrent(itemRecord.getValue());
                } else if ("wait".equals(itemName[1])) {
                    poolSizeRecord.setPoolSizeWait(itemRecord.getValue());
                }
            }
        }

        // 出力するレポートの種類に応じて、テンプレートのファイルパスを取得する。
        String templateFilePath;
        try
        {
            templateFilePath = TemplateFileManager.getInstance()
                    .getTemplateFile(ReportType.POOL_SIZE);
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
        RecordReporter<PoolSizeRecord> reporter = new RecordReporter<PoolSizeRecord>(
                getReportType());
        
        for (Map.Entry<String, List<PoolSizeRecord>> poolSizeRecordEntry : poolSizeRecordMap.entrySet())
        {
            PoolSizeRecord[] records = poolSizeRecordEntry.getValue().toArray(
                    new PoolSizeRecord[] {});
            String itemName = poolSizeRecordEntry.getKey();
            String[] graphTitles =
            { "APサーバのワーカスレッド数（" + itemName + "）" };
            reporter.outputReport(templateFilePath, outputFolderPath, itemName,
                    graphTitles, records, startTime, endTime);
        }
    }
}
