/*******************************************************************************
 * ENdoSnipe 5.0 - (https://github.com/endosnipe)
 * 
 * The MIT License (MIT)
 * 
 * Copyright (c) 2012 Acroquest Technology Co.,Ltd.
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 ******************************************************************************/
package jp.co.acroquest.endosnipe.data.db;


/**
 * DBの接続情報を保存するクラスです。<br />
 * 
 * @author fujii
 *
 */
public class DBManager
{
    /** PostgreSQLデータベース用のドライバ名称 */
    private static final String POSTGRES_DIVERNAME = "org.postgresql.Driver";

    /** 初期DBを利用するかどうか。 */
    private static boolean      useDefault__       = true;

    /** ホスト名 */
    private static String       hostName__;

    /** ポート番号 */
    private static String       port__;

    /** ユーザ名 */
    private static String       userName__;

    /** パスワード */
    private static String       password__;

    /** データベース名 */
    private static String       dbName__;

    /** データベース基準フォルダ */
    private static String       dbDir__;

    /**
     * ホスト名を取得します。<br />
     * 
     * @return ホスト名
     */
    public static String getHostName()
    {
        return hostName__;
    }

    /**
     * ホスト名を設定します。<br />
     * 
     * @param hostName ホスト名
     */
    public static void setHostName(String hostName)
    {
        hostName__ = hostName;
    }

    /**
     * ポート番号を取得します。<br />
     * 
     * @return ポート番号
     */
    public static String getPort()
    {
        return port__;
    }

    /**
     * ポート番号を設定します。<br />
     * 
     * @param port ポート番号
     */
    public static void setPort(String port)
    {
        port__ = port;
    }

    /**
     * インスタンス化を阻止するプライベートコンストラクタ
     */
    private DBManager()
    {
        // Do Nothing.
    }

    /**
     * デフォルトのDBを利用するかどうか。<br />
     * 
     * @return デフォルトのDBを利用する場合、<code>true</code>
     */
    public static boolean isDefaultDb()
    {
        return useDefault__;
    }

    /**
     * JDBCドライバクラス名を取得します。<br />
     * 現状このメソッドを使用するのはPostgreSQLのみのため、返り値は固定とする。
     * 
     * @return JDBCドライバのクラス名
     */
    public static String getDriverClass()
    {
        return POSTGRES_DIVERNAME;
    }

    /**
     * ユーザ名を取得します。<br />
     * 
     * @return ユーザ名
     */
    public static String getUserName()
    {
        return userName__;
    }

    /**
     * パスワードを取得します。<br />
     * 
     * @return パスワード
     */
    public static String getPassword()
    {
        return password__;
    }

    /**
     * データベース名を取得します。<br />
     * 
     * @return データベース名
     */
    public static String getDbName()
    {
        return dbName__;
    }

    /**
     * データベース名を設定します。<br />
     * 
     * @param dbName データベース名
     */
    public static void setDbName(String dbName)
    {
        dbName__ = dbName;
    }

    /**
     * 設定情報を更新します。<br />
     * 
     * @param useDefault デフォルトのDBを利用するかどうか。
     * @param dbDir データベース基準ディレクトリ
     * @param host ホスト名
     * @param port ポート番号
     * @param dbName データベース名
     * @param userName ユーザ名
     * @param password パスワード
     */
    public static synchronized void updateSettings(boolean useDefault, String dbDir, String host,
        String port, String dbName, String userName, String password)
    {
        useDefault__ = useDefault;
        if (useDefault__ == true)
        {
            dbDir__ = dbDir;
        }
        else
        {
            hostName__ = host;
            port__ = port;
            dbName__ = dbName;
            userName__ = userName;
            password__ = password;
        }
    }

    /**
     * データベース基準ディレクトリを取得する。
     * 
     * @return dbDir
     */
    public static String getDbDir()
    {
        return dbDir__;
    }

    /**
     * データベース基準ディレクトリを設定する。
     * 
     * @param dbDir セットする dbDir
     */
    public static void setDbDir(String dbDir)
    {
        dbDir__ = dbDir;
    }

    /**
     * フィールドに変更があったか確認します。
     * @param useDefault 初期DBを利用するかどうか
     * @param dbDir DBディレクトリ名
     * @param host ホスト名
     * @param port ポート番号
     * @param userName ユーザ名
     * @param password パスワード
     * @return 変更があったときtrue/そうでないときfalse
     */
    public static boolean isDirty(boolean useDefault, String dbDir, String host,
        String port, String userName, String password)
    {
        if (useDefault__ != useDefault)
        {
            return true;
        }
        if (dbDir__.equals(dbDir) == false)
        {
            return true;
        }
        if (hostName__.equals(host) == false)
        {
            return true;
        }
        if (port__.equals(port) == false)
        {
            return true;
        }
        if (userName__.equals(userName) == false)
        {
            return true;
        }
        if (password__.equals(password) == false)
        {
            return true;
        }

        return false;
    }
}
