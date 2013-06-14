package jp.co.acroquest.endosnipe.report.controller.processor;

import java.io.IOException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

import jp.co.acroquest.endosnipe.common.logger.ENdoSnipeLogger;
import jp.co.acroquest.endosnipe.report.LogIdConstants;
import jp.co.acroquest.endosnipe.report.controller.ReportProcessReturnContainer;
import jp.co.acroquest.endosnipe.report.controller.ReportSearchCondition;
import jp.co.acroquest.endosnipe.report.controller.ReportType;
import jp.co.acroquest.endosnipe.report.controller.TemplateFileManager;
import jp.co.acroquest.endosnipe.report.dao.ResponseTimeSummaryRecordAccessor;
import jp.co.acroquest.endosnipe.report.entity.ResponseTimeSummaryRecord;
import jp.co.acroquest.endosnipe.report.output.RecordReporter;
import jp.co.acroquest.endosnipe.report.util.ReporterConfigAccessor;

/**
 * ResponseTimeのSummaryを作成するプロセッサ
 * 
 * @author kimura
 *
 */
public class ResponseTimeSummaryReportProcessor extends ReportPublishProcessorBase
{
    /** ロガー */
    private static final ENdoSnipeLogger LOGGER = ENdoSnipeLogger.getLogger(
            ResponseTimeSummaryReportProcessor.class);

    /**
     * コンストラクタ
     * @param type レポート種別
     */
    public ResponseTimeSummaryReportProcessor(ReportType type)
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
        ResponseTimeSummaryRecordAccessor accessor = new ResponseTimeSummaryRecordAccessor();

        List<ResponseTimeSummaryRecord> rawData;
        try
        {
            rawData = accessor.findResponseStatisticsByTerm(cond.getDatabases().get(0),
                    cond.getStartDate(), cond.getEndDate());
        }
        catch (SQLException ex)
        {
            LOGGER.log(LogIdConstants.EXCEPTION_IN_READING, ex,
                    ReporterConfigAccessor.getReportName(getReportType()));
            return null;
        }

        return rawData;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Object convertPlotData(Object rawData, ReportSearchCondition cond,
            ReportProcessReturnContainer reportContainer)
    {
        List<ResponseTimeSummaryRecord> dataList = (List<ResponseTimeSummaryRecord>) rawData;

		return (ResponseTimeSummaryRecord[]) dataList
				.toArray(new ResponseTimeSummaryRecord[dataList.size()]);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void outputReport(Object plotData, ReportSearchCondition cond,
            ReportProcessReturnContainer reportContainer)
    {
        if ((plotData instanceof ResponseTimeSummaryRecord[]) == false)
        {
            return;
        }

        // 出力するレポートの種類にあわせてテンプレートのファイルパスを取得する
        String templateFilePath;
        try
        {
            templateFilePath = TemplateFileManager.getInstance().getTemplateFile(getReportType());
        }
        catch (IOException exception)
        {
            reportContainer.setHappendedError(exception);
            return;
        }

        // レポート出力の引数情報を取得する
        ResponseTimeSummaryRecord[] records = (ResponseTimeSummaryRecord[])plotData;
        String outputFilePath = getOutputFileName();
        Timestamp startTime = cond.getStartDate();
        Timestamp endTime = cond.getEndDate();

        // レポート出力を実行する
        RecordReporter<ResponseTimeSummaryRecord> reporter =
                                                             new RecordReporter<ResponseTimeSummaryRecord>(
                                                                                                           getReportType());
        reporter.outputReport(templateFilePath, outputFilePath, records, startTime, endTime);
    }

}
