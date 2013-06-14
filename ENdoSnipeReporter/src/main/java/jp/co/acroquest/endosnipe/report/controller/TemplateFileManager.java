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
package jp.co.acroquest.endosnipe.report.controller;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;

import jp.co.acroquest.endosnipe.report.util.ReporterConfigAccessor;

/**
 * レポートの「種類」に対応したレポートテンプレートを管理する。
 * リクエストに応じて、レポートテンプレートリソースにアクセスするためのパスを返す。
 * 
 * @author M.Yoshida
 */
public class TemplateFileManager
{
    private static TemplateFileManager instance__ = null;

    /**
     * インスタンス防止のためのコンストラクタ
     */
    private TemplateFileManager()
    // CHECKSTYLE:OFF
    {
        // Do nothing.
    }
    // CHECKSTYLE:ON

    /**
     * インスタンスを取得する。
     * @return インスタンス。
     */
    public static TemplateFileManager getInstance()
    {
        if (instance__ == null)
        {
            instance__ = new TemplateFileManager();
        }
        return instance__;
    }

    /**
     * レポートの「種類」に対応するテンプレートファイルをリソースから取り出し
     * テンポラリ領域にコピー後、テンポラリファイルへの絶対パスを生成する。
     * 
     * @param type レポートの種類
     * @throws IOException 入出力エラー発生時
     * @return ファイルパス
     */
    public String getTemplateFile(ReportType type)
        throws IOException
    {
        String templateFileResourcePath =
                                          ReporterConfigAccessor.getReportTemplateResourcePath(type);

        if (templateFileResourcePath == null)
        {
            return null;
        }

        URL fileUrl = getClass().getResource(templateFileResourcePath);
        if (fileUrl == null)
        {
            return null;
        }

        File temporaryTemplate = null;
        BufferedInputStream templateFileStream = null;
        BufferedOutputStream bTempStream = null;
        try
        {
            templateFileStream = new BufferedInputStream(fileUrl.openStream());

            temporaryTemplate = File.createTempFile("tempTemplate", ".xls");

            FileOutputStream temporaryStream = new FileOutputStream(temporaryTemplate);
            bTempStream = new BufferedOutputStream(temporaryStream);

            while (true)
            {
                int data = templateFileStream.read();
                if (data == -1)
                {
                    break;
                }
                bTempStream.write(data);
            }

            bTempStream.flush();
        }
        catch (IOException ioex)
        {
            throw ioex;
        }
        finally
        {
            closeStream(templateFileStream);
            closeStream(bTempStream);
        }

        return temporaryTemplate.getAbsolutePath();
    }

    /**
     * ストリームをクローズします。<br />
     * 引数が <code>null</code> の場合は何も行いません。
     * 
     * @param stream ストリーム

     */
    private void closeStream(final Closeable stream)
    {
        if (stream != null)
        {
            try
            {
                stream.close();
            }
            catch (IOException ex)
            // CHECKSTYLE:OFF
            {
                // Do nothing.
            }
            // CHECKSTYLE:ON
        }
    }
}
