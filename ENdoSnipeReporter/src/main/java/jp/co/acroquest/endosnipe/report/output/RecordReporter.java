package jp.co.acroquest.endosnipe.report.output;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import jp.co.acroquest.endosnipe.common.logger.ENdoSnipeLogger;
import jp.co.acroquest.endosnipe.common.util.CSVTokenizer;
import jp.co.acroquest.endosnipe.common.util.PathUtil;
import jp.co.acroquest.endosnipe.report.LogIdConstants;
import jp.co.acroquest.endosnipe.report.controller.ReportType;
import jp.co.acroquest.endosnipe.report.entity.ItemData;
import jp.co.acroquest.endosnipe.report.entity.ItemRecord;
import jp.co.acroquest.endosnipe.report.util.ReporterConfigAccessor;

import org.bbreak.excella.reports.exporter.ExcelExporter;
import org.bbreak.excella.reports.model.ReportBook;
import org.bbreak.excella.reports.model.ReportSheet;
import org.bbreak.excella.reports.processor.ReportProcessor;
import org.bbreak.excella.reports.tag.BlockRowRepeatParamParser;
import org.bbreak.excella.reports.tag.RowRepeatParamParser;
import org.bbreak.excella.reports.tag.SingleParamParser;

/**
 * レポートを生成するためのクラス
 * 
 * @author kimura
 * 
 * @param <E>
 */
public class RecordReporter<E> {
	/** ロガー */
	private static final ENdoSnipeLogger LOGGER = ENdoSnipeLogger.getLogger(
			RecordReporter.class);

	private static final String XLS_EXTENTION = ".xls";

	/** 参照するテンプレートのシート名のリスト */
	private String[] templateSheetNames_;

	/** 置換パラメータ名 */
	private String[] recordParameters_;

	/** 項番を表すパラメータ名 */
	public static final String NUMBERS = "numbers";

	/** 取得時刻範囲を表示するパラメータ名 */
	public static final String DATA_RANGE = "dataRange";

	/** 複数系列グラフのテンプレートシート名 */
	public static final String TEMPLATE_SHEET_NAME = "データ";

	/** 複数系列グラフのパラメータ名 */
	public static final String PARAMETER_NAME = "repeatValues";

	/** グラフ名のパラメータ名 */
	public static final String GRAPH_TITLE = "graphTitle";

	/** 出力した複数系列グラフのカウンタ */
	private int counter_;

	/**
	 * コンストラクタ
	 * 
	 * @param type
	 *            レポート種別
	 */
	public RecordReporter(ReportType type) {
		String id = type.getId();
		String parameterProperty = ReporterConfigAccessor.getProperty(id
				+ ".recordParameter");
		if (parameterProperty == null) {
			parameterProperty = PARAMETER_NAME;
		}
		this.recordParameters_ = parameterProperty.split(" *, *");

		// テンプレートファイルのシート名の一覧を取得する。
		String templateSheetNames = ReporterConfigAccessor.getProperty(id
				+ ".templateSheetNames");
		CSVTokenizer tokenizer = new CSVTokenizer(templateSheetNames);
		int tokenCount = tokenizer.countTokens();
		this.templateSheetNames_ = new String[tokenCount];
		int index = 0;
		while (tokenizer.hasMoreTokens()) {
			this.templateSheetNames_[index] = tokenizer.nextToken();
			index++;
		}
	}

	/**
	 * レポートのエクセルファイルを出力する
	 * 
	 * @param templateFilePath
	 *            テンプレートファイルのパス
	 * @param outputFolderPath
	 *            出力するファイルのパス
	 * @param outputFileName
	 *            出力するファイルの名前
	 * @param graphTitles
	 *            各グラフのタイトル
	 * @param records
	 *            出力するデータのリスト
	 * @param startDate
	 *            データ取得開始時刻
	 * @param endDate
	 *            データ取得終了時刻
	 */
	public void outputReport(String templateFilePath, String outputFolderPath,
			String outputFileName, String[] graphTitles, E[] records,
			Date startDate, Date endDate) {
		// 指定されたフォルダを作成する
		File outputDir = new File(outputFolderPath);

		if (outputDir.exists() == false) {
			boolean result = outputDir.mkdirs();
			if (result == false) {
				return;
			}
		}

		String outputFilePath = createFilePath(outputFolderPath, outputFileName);

		// �読み込むテンプレートファイルのパス(拡張子含)
		// �出力先のファイルパス(拡張子はExporterによって自動的に付与されるため、不要。)
		// �ファイルフォーマット(ConvertConfigurationの配列)
		// を指定し、ReportBookインスタンスを生成する。
		ReportBook outputBook = new ReportBook(templateFilePath,
				outputFilePath, ExcelExporter.FORMAT_TYPE);

		for (int sheetIndex = 0; sheetIndex < this.templateSheetNames_.length; sheetIndex++) {
			String templateSheetName = this.templateSheetNames_[sheetIndex];

			// テンプレートファイル内のシート名と出力シート名を指定し、
			// ReportSheetインスタンスを生成して、ReportBookに追加する。
			ReportSheet outputDataSheet = new ReportSheet(templateSheetName);
			outputBook.addReportSheet(outputDataSheet);

			// 置換パラメータをReportSheetオブジェクトに追加する。
			// (反復置換のパラメータには配列を渡す。)
			List<Integer> numberList = new ArrayList<Integer>();
			for (int index = 0; index < records.length; index++) {
				numberList.add(index + 1);
			}
			outputDataSheet.addParam(BlockRowRepeatParamParser.DEFAULT_TAG,
					this.recordParameters_[0], records);

			// 表の一番左端の列に項目番号を追加
			outputDataSheet.addParam(RowRepeatParamParser.DEFAULT_TAG,
					RecordReporter.NUMBERS, numberList.toArray());

			// ○月○日(○) ○○:○○ から ○月○日(○) ○○:○○ までのデータ取得結果です
			// という文字列を表示させる
			String dataRange = this.getDataRangeString(startDate, endDate);
			outputDataSheet.addParam(SingleParamParser.DEFAULT_TAG,
					RecordReporter.DATA_RANGE, dataRange);

			// グラフのタイトルを表示させる
			outputDataSheet.addParam(SingleParamParser.DEFAULT_TAG,
					RecordReporter.GRAPH_TITLE, graphTitles[sheetIndex]);
		}

		//
		// ReportProcessorインスタンスを生成し、
		// ReportBookを元にレポート処理を実行します。
		//
		ReportProcessor reportProcessor = new ReportProcessor();
		try {
			reportProcessor.process(outputBook);
		} catch (Exception ex) {
			LOGGER.log(LogIdConstants.REPORT_PUBLISH_STOPPED_WARN, ex,
					outputFilePath);
		}
	}

	/**
	 * レポートのエクセルファイルを出力する
	 * 
	 * @param templateFilePath
	 *            テンプレートファイルのパス
	 * @param outputFilePath
	 *            出力するファイルのパス
	 * @param records
	 *            出力するデータのリスト
	 * @param startDate
	 *            データ取得開始時刻
	 * @param endDate
	 *            データ取得終了時刻
	 */
	public void outputReport(String templateFilePath, String outputFilePath,
			E[] records, Date startDate, Date endDate) {
		// �読み込むテンプレートファイルのパス(拡張子含)
		// �出力先のファイルパス(拡張子はExporterによって自動的に付与されるため、不要。)
		// �ファイルフォーマット(ConvertConfigurationの配列)
		// を指定し、ReportBookインスタンスを生成する。
		ReportBook outputBook = new ReportBook(templateFilePath,
				outputFilePath, ExcelExporter.FORMAT_TYPE);

		for (String templateSheetName : this.templateSheetNames_) {

			// テンプレートファイル内のシート名と出力シート名を指定し、
			// ReportSheetインスタンスを生成して、ReportBookに追加する。
			ReportSheet outputDataSheet = new ReportSheet(templateSheetName);
			outputBook.addReportSheet(outputDataSheet);

			// 置換パラメータをReportSheetオブジェクトに追加する。
			// (反復置換のパラメータには配列を渡す。)
			List<Integer> numberList = new ArrayList<Integer>();
			for (int index = 0; index < records.length; index++) {
				numberList.add(index + 1);
			}
			outputDataSheet.addParam(BlockRowRepeatParamParser.DEFAULT_TAG,
					this.recordParameters_[0], records);

			// 表の一番左端の列に項目番号を追加
			outputDataSheet.addParam(RowRepeatParamParser.DEFAULT_TAG,
					RecordReporter.NUMBERS, numberList.toArray());

			// ○月○日(○) ○○:○○ から ○月○日(○) ○○:○○ までのデータ取得結果です
			// という文字列を表示させる
			String dataRange = this.getDataRangeString(startDate, endDate);
			outputDataSheet.addParam(SingleParamParser.DEFAULT_TAG,
					RecordReporter.DATA_RANGE, dataRange);
		}

		//
		// ReportProcessorインスタンスを生成し、
		// ReportBookを元にレポート処理を実行します。
		//
		ReportProcessor reportProcessor = new ReportProcessor();
		try {
			reportProcessor.process(outputBook);
		} catch (Exception ex) {
			LOGGER.log(LogIdConstants.REPORT_PUBLISH_STOPPED_WARN, ex,
					outputFilePath);
		}
	}

	/**
	 * レポートのエクセルファイルを出力する
	 * 
	 * @param templateFilePath
	 *            テンプレートファイルのパス
	 * @param outputFolderPath
	 *            出力するフォルダのパス
	 * @param itemData
	 *            出力するデータのリスト
	 * @param startDate
	 *            データ取得開始時刻
	 * @param endDate
	 *            データ取得終了時刻
	 */
	public void outputReport(String templateFilePath, String outputFolderPath,
			ItemData itemData, Date startDate, Date endDate) {
		List<ItemData> itemDataList = new ArrayList<ItemData>();
		itemDataList.add(itemData);
		this.outputReport(templateFilePath, outputFolderPath, itemDataList,
				startDate, endDate);
	}

	/**
	 * レポートのエクセルファイルを出力する
	 * 
	 * @param templateFilePath
	 *            テンプレートファイルのパス
	 * @param outputFolderPath
	 *            出力するフォルダのパス
	 * @param itemDataList
	 *            出力するデータのリスト
	 * @param startDate
	 *            データ取得開始時刻
	 * @param endDate
	 *            データ取得終了時刻
	 */
	public void outputReport(String templateFilePath, String outputFolderPath,
			List<ItemData> itemDataList, Date startDate, Date endDate) {
		// 指定されたフォルダを作成する
		File outputDir = new File(outputFolderPath);

		if (outputDir.exists() == false) {
			boolean result = outputDir.mkdirs();
			if (result == false) {
				return;
			}
		}

		if (itemDataList == null || itemDataList.size() == 0) {
			return;
		}

		String itemName = itemDataList.get(0).getItemName();
		String[] itemNameSplits = itemName.split("/");
		int splitLength = itemNameSplits.length;
		String fileName = itemNameSplits[splitLength - 1];

		String outputFilePath = createFilePath(outputFolderPath, fileName);

		// �読み込むテンプレートファイルのパス(拡張子含)
		// �出力先のファイルパス(拡張子はExporterによって自動的に付与されるため、不要。)
		// �ファイルフォーマット(ConvertConfigurationの配列)
		// を指定し、ReportBookインスタンスを生成する。
		ReportBook outputBook = new ReportBook(templateFilePath,
				outputFilePath, ExcelExporter.FORMAT_TYPE);

		for (String templateSheetName : this.templateSheetNames_) {
			// テンプレートファイル内のシート名と出力シート名を指定し、
			// ReportSheetインスタンスを生成して、ReportBookに追加する。
			ReportSheet outputDataSheet = new ReportSheet(templateSheetName);
			outputBook.addReportSheet(outputDataSheet);

			for (int itemIndex = 0; itemIndex < itemDataList.size(); itemIndex++) {
				ItemData itemData = itemDataList.get(itemIndex);
				List<ItemRecord> recordList = itemData.getRecords();
				ItemRecord[] records = (ItemRecord[]) recordList
						.toArray(new ItemRecord[recordList.size()]);

				// 置換パラメータをReportSheetオブジェクトに追加する。
				// (反復置換のパラメータには配列を渡す。)
				List<Integer> numberList = new ArrayList<Integer>();
				for (int index = 0; index < records.length; index++) {
					numberList.add(index + 1);
				}
				String parameterName = RecordReporter.PARAMETER_NAME;
				if (this.recordParameters_.length > itemIndex) {
					parameterName = this.recordParameters_[itemIndex];
				}
				outputDataSheet.addParam(BlockRowRepeatParamParser.DEFAULT_TAG,
						parameterName, records);

				// 表の一番左端の列に項目番号を追加
				outputDataSheet.addParam(RowRepeatParamParser.DEFAULT_TAG,
						RecordReporter.NUMBERS, numberList.toArray());

				// ○月○日(○) ○○:○○ から ○月○日(○) ○○:○○ までのデータ取得結果です
				// という文字列を表示させる
				String dataRange = this.getDataRangeString(startDate, endDate);
				outputDataSheet.addParam(SingleParamParser.DEFAULT_TAG,
						RecordReporter.DATA_RANGE, dataRange);

				// グラフのタイトルを表示
				outputDataSheet.addParam(SingleParamParser.DEFAULT_TAG,
						RecordReporter.GRAPH_TITLE, itemName);
			}
		}

		//
		// ReportProcessorインスタンスを生成し、
		// ReportBookを元にレポート処理を実行します。
		//
		ReportProcessor reportProcessor = new ReportProcessor();
		try {
			reportProcessor.process(outputBook);
		} catch (Exception ex) {
			LOGGER.log(LogIdConstants.REPORT_PUBLISH_STOPPED_WARN, ex,
					outputFilePath);
		}
	}

	/**
	 * レポート出力ファイル名を生成する。
	 * 
	 * @param outputFolderPath
	 *            　出力先ディレクトリ
	 * @param itemName
	 *            itemName
	 * @return レポート出力ファイル名。
	 */
	private String createFilePath(String outputFolderPath, String itemName) {
		String outputFileName = PathUtil.getValidFileName(itemName);
		DecimalFormat format = new DecimalFormat("00000");
		String addtion = format.format(this.counter_);
		this.counter_++;
		String outputFilePath = PathUtil.getValidLengthPath(outputFolderPath
				+ File.separator + outputFileName + XLS_EXTENTION, addtion);

		outputFilePath = outputFilePath.substring(0, outputFilePath.length()
				- XLS_EXTENTION.length());
		return outputFilePath;
	}

	/**
	 * レポートのエクセルファイルを出力する
	 * 
	 * @param templateFilePath
	 *            テンプレートファイルのパス
	 * @param outputFolderPath
	 *            出力するフォルダのパス
	 * @param dataList
	 *            出力するデータのリスト
	 * @param startDate
	 *            データ取得開始時刻
	 * @param endDate
	 *            データ取得終了時刻
	 */
	public void outputReports(String templateFilePath, String outputFolderPath,
			List<ItemData> dataList, Date startDate, Date endDate) {
		// ディレクトリに使えない禁止文字を変換する
		outputFolderPath = replaceForbidedDirName(outputFolderPath);

		// 指定されたフォルダを作成する
		File outputDir = new File(outputFolderPath);

		if (outputDir.exists() == false) {
			boolean result = outputDir.mkdirs();
			if (result == false) {
				return;
			}
		}

		for (ItemData itemData : dataList) {
			outputReport(templateFilePath, outputFolderPath, itemData,
					startDate, endDate);
		}
	}

	/**
	 * データ取得時刻の範囲を表示する文字列を成型する
	 * 
	 * @param startDate
	 *            データ取得開始日時
	 * @param endDate
	 *            データ取得終了日時
	 * @return　表示用の文字列
	 */
	private String getDataRangeString(Date startDate, Date endDate) {
		Calendar calendar = Calendar.getInstance();

		// データ取得開始日時とデータ取得終了日時を成型する
		calendar.setTime(startDate);
		
		Locale.setDefault(Locale.ENGLISH);
		String startDateString = String.format(
				"%1$tY/%1$tm/%1$td(%1$ta) %1$tH:%1$tM:%1$tS", calendar);
		calendar.setTime(endDate);
		String endDateString = String.format(
				"%1$tY/%1$tm/%1$td(%1$ta) %1$tH:%1$tM:%1$tS", calendar);

		// 表示用文字列を成型する
		StringBuilder builder = new StringBuilder();
		builder.append("Result of data acquisition from ");
		builder.append(startDateString);
		builder.append(" to ");
		builder.append(endDateString);

		String returnValue = builder.toString();

		return returnValue;
	}

	/**
	 * ディレクトリに禁止されている文字を"_"に置換する
	 * 
	 * @param dirPath
	 * @return
	 */
	private String replaceForbidedDirName(String dirPath) {
		String[] dirPathSplitList = dirPath.split("/");
		int dirPathLength = dirPathSplitList.length;

		StringBuilder builder = new StringBuilder();
		for (int index = 0; index < dirPathLength; index++) {
			String dirName = dirPathSplitList[index];

			if (index != 0) {
				dirName = dirName.replace(":", "_");
				dirName = dirName.replace("?", "_");
				dirName = dirName.replace("\"", "_");
				dirName = dirName.replace("<", "_");
				dirName = dirName.replace(">", "_");
				dirName = dirName.replace("|", "_");
				
				builder.append("/");
			}
			
			builder.append(dirName);
		}

		return builder.toString();
	}
}
