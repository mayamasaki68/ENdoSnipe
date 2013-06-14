package jp.co.acroquest.endosnipe.report.controller.processor;

import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jp.co.acroquest.endosnipe.common.logger.ENdoSnipeLogger;
import jp.co.acroquest.endosnipe.report.LogIdConstants;
import jp.co.acroquest.endosnipe.report.controller.ReportProcessReturnContainer;
import jp.co.acroquest.endosnipe.report.controller.ReportSearchCondition;
import jp.co.acroquest.endosnipe.report.controller.ReportType;
import jp.co.acroquest.endosnipe.report.controller.TemplateFileManager;
import jp.co.acroquest.endosnipe.report.controller.dispatcher.ReportPublishProcessor;
import jp.co.acroquest.endosnipe.report.util.ReporterConfigAccessor;

import org.bbreak.excella.reports.exporter.ExcelExporter;
import org.bbreak.excella.reports.model.ReportBook;
import org.bbreak.excella.reports.model.ReportSheet;
import org.bbreak.excella.reports.processor.ReportProcessor;
import org.bbreak.excella.reports.tag.RowRepeatParamParser;
import org.bbreak.excella.reports.tag.SingleParamParser;

/**
 * サマリレポートを出力する、プロセッサクラス。<br>
 * 
 * @author iida
 */
public class SummaryReportProcessor implements ReportPublishProcessor
{
    /** ロガー */
    private static final ENdoSnipeLogger LOGGER = ENdoSnipeLogger.getLogger(
            SummaryReportProcessor.class);

	/** 出力されるレポートの種類のリスト */
	private static final ThreadLocal<ReportType[]> OUTPUT_FILE_TYPE_LIST
	        = new ThreadLocal<ReportType[]>();

	/**
	 * 出力されるレポートの種類のリストをセットします。<br>
	 * 
	 * @param outputFileTypeList 出力されるレポートの種類のリスト
	 */
	public static void setOutputFileTypeList(ReportType[] outputFileTypeList)
	{
		OUTPUT_FILE_TYPE_LIST.set(outputFileTypeList);
	}

	/** レポートプロセッサの工程の数 */
	public static final int PROCESS_PHASE_NUM = 3;

	/** データを取得する工程 */
	private static final String GET_DATA_PHASE_KEY
	        = "reporter.report.progress.detail.getData";

	/** 取得したデータを変換する工程 */
	private static final String CONVERT_DATA_PHASE_KEY
	        = "reporter.report.progress.detail.convData";

	/** ファイルを出力する工程 */
	private static final String OUTPUT_DATA_PHASE_KEY
	        = "reporter.report.progress.detail.output";
	
	/** レポート出力期間を示すタグ */
	private static final String DATE_RANGE_TAG = "dataRange";
	
	/** データベース名を示すタグ */
	private static final String DATABASE_NAME_TAG = "dataBaseName";
	
	/** 番号のリストを示すタグ */
	private static final String NUMBERS_TAG = "numbers";
	
	/** 出力ファイル名のリストを示すタグ */
	private static final String FILE_NAMES_TAG = "fileNames";
	
	/** 出力ファイルの説明を示すタグ */
	private static final String FILE_EXPLANATIONS_TAG = "fileExplanations";
	
	/** レポート種別 */
	private ReportType rType_;

	/** 出力先ディレクトリ名 */
	private String outputDir_;

	/**
	 * コンストラクタ。
	 * @param rType レポート種別
	 */
	public SummaryReportProcessor(ReportType rType)
	{
		rType_ = rType;
	}

	/**
	 * 自分が処理するレポート種別を取得する。
	 * @return レポート種別
	 */
	protected ReportType getReportType()
	{
		return rType_;
	}

	/**
	 * 出力先ファイルのファイル名を取得する（拡張子無し）
	 * @return 出力先ファイルのファイル名
	 */
	protected String getOutputFileName()
	{
		File outputFile = new File(outputDir_, ReporterConfigAccessor
				.getOutputFileName(rType_));
		return outputFile.getAbsolutePath();
	}
	
	/**
	 * 出力先フォルダのパスを取得する
	 * @return 出力先フォルダのパス
	 */
	protected String getOutputFolderName()
	{
		File outputFolder = new File(outputDir_);
		return outputFolder.getAbsolutePath();
	}

	/**
	 * レポート出力処理を行う。
	 * @param cond レポート出力の条件
	 * @return ReportProcessReturnContainer コンテナ
	 * @throws InterruptedException あるスレッドがこのスレッドを中断させた時に発生する。
	 */
	public ReportProcessReturnContainer publish(ReportSearchCondition cond)
			throws InterruptedException
	{
		// TODO 他のプロセッサと同様のPhaseを使用しているのを修正する。
		
		outputDir_ = cond.getOutputFilePath();

		ReportProcessReturnContainer retContainer = new ReportProcessReturnContainer();

		outputReport(null, cond, retContainer);

		return retContainer;
	}

	/**
	 * データ取得時刻の範囲を表示する文字列を成型する
	 * @param startDate データ取得開始日時
	 * @param endDate データ取得終了日時
	 * @return　表示用の文字列
	 */
	private String getDataRangeString(Date startDate, Date endDate)
	{
		Calendar calendar = Calendar.getInstance();

		// データ取得開始日時とデータ取得終了日時を成型する
		calendar.setTime(startDate);
		String startDateString = String.format(
				"%1$tY/%1$tm/%1$td(%1$ta) %1$tH:%1$tM", calendar);
		calendar.setTime(endDate);
		String endDateString = String.format(
				"%1$tY/%1$tm/%1$td(%1$ta) %1$tH:%1$tM", calendar);

		// 表示用文字列を成形する
		StringBuilder builder = new StringBuilder();
		builder.append(startDateString);
		builder.append(" ～ ");
		builder.append(endDateString);

		String returnValue = builder.toString();

		return returnValue;
	}

	/**
	 * レポート出力処理を行う。
	 * @param plotData 使用しない
	 * @param cond レポート出力の条件
	 * @param reportContainer コンテナ
	 */
	private void outputReport(Object plotData, ReportSearchCondition cond,
			ReportProcessReturnContainer reportContainer)
	{
		// ダイアログでチェックされた、レポート種類のリストを取得する。
		ReportType[] outputFileTypeList = OUTPUT_FILE_TYPE_LIST.get();
		// 「番号」の列に出力するデータ
		Integer[] numbers = new Integer[outputFileTypeList.length];
		// 「ファイル名」の列に出力するデータ
		String[] fileNames = new String[outputFileTypeList.length];
		// 「ファイル名」と「説明」の列に出力するデータの対応関係を表すマップ
		Map<String, String> explanationMap = new HashMap<String, String>();
		for (int index = 0; index < outputFileTypeList.length; index++)
		{
		    ReportType currentReportType = outputFileTypeList[index];
			String fileName = ReporterConfigAccessor
			        .getOutputFileName(currentReportType);
			String explanation = ReporterConfigAccessor
	                .getExplanation(currentReportType);
			numbers[index] = index + 1;
			// ApplicationReportとObjectReport、ResponseTimeReport、EventReportは
			// ディレクトリとして出力されるので、".xls"を付けない
			if (currentReportType.equals(ReportType.SERVER_POOL)
			    || currentReportType.equals(ReportType.POOL_SIZE)
			    || currentReportType.equals(ReportType.OBJECT)
                || currentReportType.equals(ReportType.RESPONSE_LIST)
                || currentReportType.equals(ReportType.EVENT))
			{
				fileNames[index] = fileName;
			}
			else
			{
				fileNames[index] = fileName + ".xls";
			}
			explanationMap.put(fileNames[index], explanation);
		}
		Arrays.sort(fileNames);
		// 「説明」の列に出力するデータ
		String[] explanations = new String[outputFileTypeList.length];
		for (int index = 0; index < outputFileTypeList.length; index++)
		{
			String fileName = fileNames[index];
			explanations[index] = explanationMap.get(fileName);
		}

		// 出力するレポートの種類にあわせてテンプレートのファイルパスを取得する
		String templateFilePath;
		try
		{
			templateFilePath = TemplateFileManager.getInstance()
					.getTemplateFile(getReportType());
		}
		catch (IOException exception)
		{
			reportContainer.setHappendedError(exception);
			return;
		}

		// レポート出力の引数情報を取得する
		String outputFilePath = this.getOutputFileName();
		Timestamp startDate = cond.getStartDate();
		Timestamp endDate = cond.getEndDate();
		List<String> databases = cond.getDatabases();

		// 出力するExcelのブックとシートを表すオブジェクトを生成する
		String id = this.getReportType().getId();
		String templateSheetName = ReporterConfigAccessor.getProperty(id
				+ ".templateSheetName");
		ReportBook outputBook = new ReportBook(templateFilePath,
				outputFilePath, ExcelExporter.FORMAT_TYPE);
		ReportSheet outputDataSheet = new ReportSheet(templateSheetName);
		outputBook.addReportSheet(outputDataSheet);

		// シートにデータを流し込む
		outputDataSheet.addParam(SingleParamParser.DEFAULT_TAG,
				DATE_RANGE_TAG, this.getDataRangeString(startDate, endDate));
		outputDataSheet.addParam(SingleParamParser.DEFAULT_TAG,
				DATABASE_NAME_TAG, databases.get(0));

		outputDataSheet.addParam(RowRepeatParamParser.DEFAULT_TAG,
				NUMBERS_TAG, numbers);
		outputDataSheet.addParam(RowRepeatParamParser.DEFAULT_TAG,
				FILE_NAMES_TAG, fileNames);
		outputDataSheet.addParam(RowRepeatParamParser.DEFAULT_TAG,
				FILE_EXPLANATIONS_TAG, explanations);

		// Excelファイルを出力する。
		ReportProcessor reportProcessor = new ReportProcessor();
		try
		{
			reportProcessor.process(outputBook);
		}
		catch (Exception ex)
		{
		    LOGGER.log(LogIdConstants.EXCEPTION_HAPPENED, ex,
		            ReporterConfigAccessor.getReportName(getReportType()));
		}
	}
}
