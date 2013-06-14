package jp.co.acroquest.endosnipe.report.controller.processor;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import jp.co.acroquest.endosnipe.common.logger.ENdoSnipeLogger;
import jp.co.acroquest.endosnipe.common.parser.JavelinLogInputStreamAccessor;
import jp.co.acroquest.endosnipe.data.dao.JavelinLogDao;
import jp.co.acroquest.endosnipe.data.entity.JavelinLog;
import jp.co.acroquest.endosnipe.javelin.parser.JavelinLogElement;
import jp.co.acroquest.endosnipe.javelin.parser.JavelinParser;
import jp.co.acroquest.endosnipe.javelin.parser.ParseException;
import jp.co.acroquest.endosnipe.perfdoctor.PerfDoctor;
import jp.co.acroquest.endosnipe.perfdoctor.WarningUnit;
import jp.co.acroquest.endosnipe.perfdoctor.classfier.PerformanceDoctorFilter;
import jp.co.acroquest.endosnipe.perfdoctor.classfier.UnifiedFilter;
import jp.co.acroquest.endosnipe.perfdoctor.exception.RuleCreateException;
import jp.co.acroquest.endosnipe.perfdoctor.exception.RuleNotFoundException;
import jp.co.acroquest.endosnipe.report.LogIdConstants;
import jp.co.acroquest.endosnipe.report.controller.ReportProcessReturnContainer;
import jp.co.acroquest.endosnipe.report.controller.ReportSearchCondition;
import jp.co.acroquest.endosnipe.report.controller.ReportType;
import jp.co.acroquest.endosnipe.report.controller.TemplateFileManager;
import jp.co.acroquest.endosnipe.report.entity.PerfDoctorRecord;
import jp.co.acroquest.endosnipe.report.output.RecordReporter;
import jp.co.acroquest.endosnipe.report.util.ReporterConfigAccessor;

/**
 * PerformanceDoctorの診断結果のレポートを生成する、プロセッサです。
 * 
 * @author iida
 */
public class PerfDoctorProcessor extends ReportPublishProcessorBase
{
    /** ロガー */
    private static final ENdoSnipeLogger LOGGER = ENdoSnipeLogger.getLogger(
            PerfDoctorProcessor.class);

    /** レポートに出力される結果の数の最大値 */
    private static final int MAX_RECORD = 65534;
    
    /**
     * コンストラクタです。
     * 指定された、処理するレポートの種別を、セットします。
     * 
     * @param type 処理するレポートの種別
     */
    public PerfDoctorProcessor(ReportType type)
    {
        super(type);
    }

    /**
     * DBなどのエンティティから、レポートに出力するデータを取得します。
     * 
     * @param cond              データの取得条件
     * @param reportContainer   返り値などを渡すためのコンテナ
     * 
     * @return レポートに出力するデータ
     */
    @Override
    protected Object getReportPlotData(ReportSearchCondition cond,
            ReportProcessReturnContainer reportContainer)
    {
        // レポートに出力するデータ（このメソッドの戻り値）
        PerfDoctorRecord[] records = null;
        
        try
        {
            String databaseName = cond.getDatabases().get(0);   // データベース名
            Timestamp start = cond.getStartDate();              // 開始時刻のタイムスタンプ
            Timestamp end = cond.getEndDate();                  // 終了時刻のタイムスタンプ
            
            // Step 1. DBから、JavelinLogインスタンスを生成する。
            List<JavelinLog> javelinLogList = JavelinLogDao.selectByTerm(databaseName, start, end);
            
            List<JavelinLogElement> javelinLogElementList = new ArrayList<JavelinLogElement>();
            
            // Step 2. JavelinLogインスタンスからJavelinLogDatabaseAccessorを生成する
            for (JavelinLog javelinLog : javelinLogList)    // 生成した各JavelinLogについて、処理を繰り返す
            {
            	InputStream javelinLogStream = null;
            	try
            	{
					javelinLogStream = JavelinLogDao
							.selectJavelinLogByLogId(databaseName, javelinLog.logId);
					JavelinLogInputStreamAccessor javelinLogMemoryAccessor = new JavelinLogInputStreamAccessor(
							javelinLog.logFileName, javelinLogStream);
	                
	                // Step 3. JavelinLogMemoryAccessorインスタンスからJavelinParserを生成する
	                JavelinParser javelinParser = new JavelinParser(javelinLogMemoryAccessor);
	                javelinParser.init();
	                
	                // Step 4. JavelinParserインスタンスからJavelinLogElementを生成する
	                JavelinLogElement javelinLogElement;
	                while ((javelinLogElement = javelinParser.nextElement()) != null)
	                {
	                    javelinLogElementList.add(javelinLogElement);
                    }
	                
	                JavelinParser.initDetailInfo(javelinLogElementList);
                }
                finally
                {
                    if (javelinLogStream != null)
                    {
                        javelinLogStream.close();
                    }
				}
            }
            
            // Step 5. 生成したJavelinLogElementを、PerformanceDoctorに診断させる
            PerfDoctor perfDoctor = new PerfDoctor();
            perfDoctor.init();
            List<WarningUnit> warningUnitList =
                    perfDoctor.judgeJavelinLog(javelinLogElementList);

            if (MAX_RECORD <= warningUnitList.size())
            {
                warningUnitList = warningUnitList.subList(0, MAX_RECORD);
            }
            
            // フィルタ処理を実行する。
            warningUnitList = this.doFilter(warningUnitList, cond);
            
            List<PerfDoctorRecord> recordList = new ArrayList<PerfDoctorRecord>();
            for (WarningUnit warningUnit : warningUnitList)
            {
                PerfDoctorRecord record = new PerfDoctorRecord(warningUnit);
                recordList.add(record);
            }
            
            // 結果を配列に直す
            records = new PerfDoctorRecord[recordList.size()];
            recordList.toArray(records);
        }
        catch (SQLException ex)
        {
            LOGGER.log(LogIdConstants.EXCEPTION_IN_READING, ex,
                    ReporterConfigAccessor.getReportName(getReportType()));
        }
        catch (IOException ex)
        {
            LOGGER.log(LogIdConstants.EXCEPTION_HAPPENED, ex,
                    ReporterConfigAccessor.getReportName(getReportType()));
        }
        catch (ParseException ex)
        {
            LOGGER.log(LogIdConstants.EXCEPTION_HAPPENED, ex,
                    ReporterConfigAccessor.getReportName(getReportType()));
        }
        catch (RuleNotFoundException ex)
        {
            LOGGER.log(LogIdConstants.EXCEPTION_HAPPENED, ex,
                    ReporterConfigAccessor.getReportName(getReportType()));
        }
        catch (RuleCreateException ex)
        {
            LOGGER.log(LogIdConstants.EXCEPTION_HAPPENED, ex,
                    ReporterConfigAccessor.getReportName(getReportType()));
        }
        
        return records;
    }

    /**
     * データをレポートに出力します。
     * 
     * @param plotData          レポートに出力するデータ
     * @param cond              データの出力条件
     * @param reportContainer   返り値などを渡すためのコンテナ
     */
    @Override
    protected void outputReport(Object plotData, ReportSearchCondition cond,
            ReportProcessReturnContainer reportContainer)
    {
        if (plotData instanceof PerfDoctorRecord[])
        {
            PerfDoctorRecord[] records = (PerfDoctorRecord[])plotData;
            
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
			
            RecordReporter<PerfDoctorRecord> reporter = 
                new RecordReporter<PerfDoctorRecord>(this.getReportType());
          
            String outputFilePath = getOutputFolderName() + File.separator;
            outputFilePath += ReporterConfigAccessor.getProperty(
                    this.getReportType().getId() + ".outputFile");
            
            reporter.outputReport(templateFilePath, outputFilePath, records,
                                  new Date(), new Date());  // この2つのDateは、特に使用しない。
        }
    }

    /**
     * リストにフィルターをかける。
     * 
     * @param resultList    測定結果のリスト
     * @param cond          データの出力条件
     * @return フィルターをかけた後の、測定結果のリスト
     */
    private List<WarningUnit> doFilter(final List<WarningUnit> resultList,
            final ReportSearchCondition cond)
    {
        PerformanceDoctorFilter sameFilter = new PerformanceDoctorFilter();
        UnifiedFilter unifiedFilter = new UnifiedFilter();
        List<WarningUnit> filteredList = resultList;

        // 同要因フィルタと同種警告フィルタを設定する。
        if (cond.getLimitSameCause())
        {
            filteredList = unifiedFilter.doFilter(filteredList);
        }
        if (cond.getLimitBySameRule())
        {
            filteredList = sameFilter.doFilter(filteredList);
        }

        return filteredList;
    }

}
