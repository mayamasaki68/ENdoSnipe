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
import jp.co.acroquest.endosnipe.report.entity.ObjectRecord;
import jp.co.acroquest.endosnipe.report.output.RecordReporter;
import jp.co.acroquest.endosnipe.report.util.ReporterConfigAccessor;

/**
 * ResponseTimeのSummaryを作成するプロセッサ
 * 
 * @author kimura
 * 
 */
public class ResponseTimeListReportProcessor extends ReportPublishProcessorBase
{
    /** ロガー */
    private static final ENdoSnipeLogger LOGGER = ENdoSnipeLogger.getLogger(
            ResponseTimeListReportProcessor.class);

	/**
	 * コンストラクタ
	 * 
	 * @param type
	 *            レポート種別
	 */
	public ResponseTimeListReportProcessor(ReportType type)
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
		// 検索条件の取得
		String database = cond.getDatabases().get(0);
		Timestamp startTime = cond.getStartDate();
		Timestamp endTime = cond.getEndDate();

		// DBから検索
		List<ItemData> tatData = null;
		List<ItemData> tatMinData = null;
		List<ItemData> tatMaxData = null;
		List<ItemData> countData = null;
		List<ItemData> exceptionCountData = null;
		List<ItemData> stallCountData = null;
		try
		{
    		tatData = GraphItemAccessUtil.findItemData(database,
    				Constants.ITEMNAME_PROCESS_RESPONSE_TIME_AVERAGE,
    				CompressOperator.SIMPLE_AVERAGE,
    				startTime, endTime);

    		tatMinData = GraphItemAccessUtil.findItemData(database,
    				Constants.ITEMNAME_PROCESS_RESPONSE_TIME_MIN,
    				CompressOperator.SIMPLE_AVERAGE,
    				 startTime, endTime);

    		tatMaxData = GraphItemAccessUtil.findItemData(database,
    				Constants.ITEMNAME_PROCESS_RESPONSE_TIME_MAX,
    				CompressOperator.SIMPLE_AVERAGE,
    				 startTime, endTime);

    		countData = GraphItemAccessUtil.findItemData(database,
    				Constants.ITEMNAME_PROCESS_RESPONSE_TOTAL_COUNT,
    				CompressOperator.TOTAL,
    				startTime, endTime);

    		exceptionCountData = GraphItemAccessUtil
    				.findExceptionData(database, CompressOperator.TOTAL, startTime,
    						endTime, tatData);

    		stallCountData = GraphItemAccessUtil
                    .findStallData(database, CompressOperator.TOTAL, startTime,
                            endTime, tatData);
		}
		catch (SQLException ex)
		{
		    LOGGER.log(LogIdConstants.EXCEPTION_IN_READING, ex,
		            ReporterConfigAccessor.getReportName(getReportType()));
		    return null;
		}
        		

		// 取得したデータをmapにまとめてリターンする
		Map<String, List<? extends Object>> data = new HashMap<String, List<? extends Object>>();
		data.put(Constants.ITEMNAME_PROCESS_RESPONSE_TIME_AVERAGE, tatData);
		data.put(Constants.ITEMNAME_PROCESS_RESPONSE_TIME_MIN, tatMinData);
		data.put(Constants.ITEMNAME_PROCESS_RESPONSE_TIME_MAX, tatMaxData);
		data.put(Constants.ITEMNAME_PROCESS_RESPONSE_TOTAL_COUNT, countData);
        data.put(Constants.ITEMNAME_JAVAPROCESS_EXCEPTION_OCCURENCE_COUNT, exceptionCountData);
        data.put(Constants.ITEMNAME_JAVAPROCESS_STALL_OCCURENCE_COUNT, stallCountData);

		return data;
	}



	/**
	 * {@inheritDoc}
	 */
	@Override
	protected Object convertPlotData(Object rawData,
			ReportSearchCondition cond,
			ReportProcessReturnContainer reportContainer)
	{
		// map のデータを6グラフの個別のデータに分ける
		return (Map<String, List<? extends Object>>) rawData;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void outputReport(Object convertedData,
			ReportSearchCondition cond,
			ReportProcessReturnContainer reportContainer)
	{
		// map のデータを6グラフの個別のデータに分ける
		Map<String, List<? extends Object>> data = (Map<String, List<? extends Object>>) convertedData;

		List<ItemData> tatDataList = (List<ItemData>) data
				.get(Constants.ITEMNAME_PROCESS_RESPONSE_TIME_AVERAGE);

		List<ItemData> tatMinDataList = (List<ItemData>) data
				.get(Constants.ITEMNAME_PROCESS_RESPONSE_TIME_MIN);

		List<ItemData> tatMaxDataList = (List<ItemData>) data
				.get(Constants.ITEMNAME_PROCESS_RESPONSE_TIME_MAX);

		List<ItemData> countDataList = (List<ItemData>) data
				.get(Constants.ITEMNAME_PROCESS_RESPONSE_TOTAL_COUNT);

        List<ItemData> exceptionCountDataList = (List<ItemData>) data
                .get(Constants.ITEMNAME_JAVAPROCESS_EXCEPTION_OCCURENCE_COUNT);

        List<ItemData> stallCountDataList = (List<ItemData>)data
                .get(Constants.ITEMNAME_JAVAPROCESS_STALL_OCCURENCE_COUNT);
		
		

		// 出力するレポートの種類にあわせてテンプレートのファイルパスを取得する
		String templateFilePath;
		try
		{
			templateFilePath = TemplateFileManager.getInstance()
					.getTemplateFile(ReportType.ITEM);
		}
		catch (IOException exception)
		{
			reportContainer.setHappendedError(exception);
			return;
		}

		// レポート出力の引数情報を取得する
		String outputFolderPath = getOutputFolderName()
				+ File.separator
				+ ReporterConfigAccessor.getProperty(super.getReportType()
						.getId()
						+ ".outputFile");
		Timestamp startTime = cond.getStartDate();
		Timestamp endTime = cond.getEndDate();

		// レポート出力を実行する
		RecordReporter<ObjectRecord> reporter = new RecordReporter<ObjectRecord>(
				getReportType());
		
		for(int index = 0; index < tatDataList.size(); index++)
		{
		    List<ItemData> list = new ArrayList<ItemData>();
            list.add(countDataList.get(index));
            list.add(tatDataList.get(index));
            list.add(tatMaxDataList.get(index));
            list.add(tatMinDataList.get(index));
            list.add(exceptionCountDataList.get(index));
            list.add(stallCountDataList.get(index));

            reporter.outputReport(templateFilePath, outputFolderPath, list, startTime, endTime);
		    
		}
	}

}
