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
package jp.co.acroquest.endosnipe.report.controller.processor;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import jp.co.acroquest.endosnipe.common.logger.ENdoSnipeLogger;
import jp.co.acroquest.endosnipe.report.LogIdConstants;
import jp.co.acroquest.endosnipe.report.controller.ReportProcessReturnContainer;
import jp.co.acroquest.endosnipe.report.controller.ReportSearchCondition;
import jp.co.acroquest.endosnipe.report.controller.ReportType;
import jp.co.acroquest.endosnipe.report.controller.TemplateFileManager;
import jp.co.acroquest.endosnipe.report.dao.ResponseTimeRecordAccessor;
import jp.co.acroquest.endosnipe.report.entity.ResponseTimeRecord;
import jp.co.acroquest.endosnipe.report.output.ResponseTimeReporter;
import jp.co.acroquest.endosnipe.report.util.ReporterConfigAccessor;

/**
 * レスポンスタイムレポートを出力するためのプロセッサ
 * 
 * @author M.Yoshida
 *
 */
public class ResponseTimeReportProcessor extends ReportPublishProcessorBase
{
    /** ロガー */
    private static final ENdoSnipeLogger LOGGER = ENdoSnipeLogger.getLogger(
            ResponseTimeReportProcessor.class);

    /**
     * コンストラクタ
     * @param type レポート種別
     */
    public ResponseTimeReportProcessor(ReportType type)
    {
        super(type);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Object getReportPlotData(ReportSearchCondition cond,
            ReportProcessReturnContainer reportContainer)
    {
        ResponseTimeRecordAccessor recordAccessor = new ResponseTimeRecordAccessor();

        List<ResponseTimeRecord> responseTimeRecord;
        try
        {
            responseTimeRecord = recordAccessor.findResponseStatisticsByTerm(
                    cond.getDatabases().get(0), cond.getStartDate(), cond.getEndDate());
        }
        catch (SQLException ex)
        {
            LOGGER.log(LogIdConstants.EXCEPTION_IN_READING, ex,
                    ReporterConfigAccessor.getReportName(getReportType()));
            return null;
        }

        ResponseTimeRecord[] records = responseTimeRecord.toArray(new ResponseTimeRecord[0]);
        return records;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void outputReport(Object plotData, ReportSearchCondition cond,
            ReportProcessReturnContainer reportContainer)
    {

        ResponseTimeReporter reporter = new ResponseTimeReporter();

        String templateFilePath;
        try
        {
            templateFilePath = TemplateFileManager.getInstance().getTemplateFile(getReportType());
        }
        catch (IOException e)
        {
            LOGGER.log(LogIdConstants.EXCEPTION_HAPPENED, e, new Object[0]);
            reportContainer.setHappendedError(e);
            return;
        }

        reporter.outputReport(templateFilePath, getOutputFileName(),
                              (ResponseTimeRecord[])plotData, cond.getStartDate(),
                              cond.getEndDate());

        return;
    }
}
