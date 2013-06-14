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
package jp.co.acroquest.endosnipe.javelin.converter.leak;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import jp.co.acroquest.endosnipe.common.logger.SystemLogger;
import jp.co.acroquest.endosnipe.javelin.common.JavassistUtil;
import jp.co.acroquest.endosnipe.javelin.conf.JavelinMessages;
import jp.co.acroquest.endosnipe.javelin.converter.AbstractConverter;
import jp.co.acroquest.endosnipe.javelin.converter.leak.monitor.CollectionMonitor;
import jp.co.smg.endosnipe.javassist.CannotCompileException;
import jp.co.smg.endosnipe.javassist.CtClass;
import jp.co.smg.endosnipe.javassist.CtMethod;
import jp.co.smg.endosnipe.javassist.NotFoundException;

/**
 * コレクションサイズ追跡用のコンバータ
 * コレクションクラス、マップクラスのコンストラクタに対し、
 * 監視マネージャに自らを登録するコードを埋め込む。
 * 
 * @author kimura
 */
public class CollectionMonitorConverter extends AbstractConverter
{
    /** 監視クラスの名称 */
    public static final String COLLECTIONTRACER_NAME = CollectionMonitor.class.getCanonicalName();

    /**
     * Mapを実装するクラスであれば、put,putAllメソッドに情報取得コードを埋め込む。
     * Queueを実装するクラスであれば、offerメソッドに情報取得コードを埋め込む。
     * Collectionを実装するクラスであれば、add,addAllメソッドに情報取得コードを埋め込む。
     * それ以外の場合は何もしない。
     * 
     * @throws CannotCompileException コードの埋め込みに失敗した場合。
     * @throws IOException 変換後のCtClassのバイトコードへの変換に失敗した場合。 
     */
    @Override
    public void convertImpl()
        throws CannotCompileException,
            IOException
    {
        CtClass targetClass = getCtClass();
        boolean excludeTarget = isExcludeTarget(targetClass);
        if (excludeTarget)
        {
            return;
        }

        boolean isInheritedMap =
                JavassistUtil.isInherited(targetClass, targetClass.getClassPool(),
                                          Map.class.getCanonicalName());

        boolean isInheritedCollection =
                JavassistUtil.isInherited(targetClass, targetClass.getClassPool(),
                                          Collection.class.getCanonicalName());
        boolean isInheritedQueue =
                JavassistUtil.isInherited(targetClass, targetClass.getClassPool(),
                                          Queue.class.getCanonicalName());

        if (isInheritedMap)
        {
            // putメソッドに関しては、引数が2であるもののみを取得する。
            List<CtMethod> targetMethods = getDeclaredPutMethods(targetClass);
            for (CtMethod targetMethod : targetMethods)
            {
                modifyMapAdd(targetMethod);
            }
            targetMethods = getDeclaredMethods(targetClass, "putAll");
            for (CtMethod targetMethod : targetMethods)
            {
                modifyMapAdd(targetMethod);
            }
        }
        else if (isInheritedQueue)
        {
            // Collection継承判定よりも、Queue継承判定の方を先に実行する。
            // 理由は、ArrayBlockingQueueなど、CollectionもQueueも両方実装しているクラスに対しては、
            // add()もaddAll()もoffer()も変換する必要があるため。
            List<CtMethod> targetMethods = getDeclaredMethods(targetClass,"add|addAll" +
            		"|addFirst|addLast|offer|offerFirst|offerLast|put|putFirst|putLast");
            for (CtMethod targetMethod : targetMethods)
            {
                modifyCollectionAdd(targetMethod);
            }
        }
        else if (isInheritedCollection)
        {
            List<CtMethod> targetMethods = getDeclaredMethods(targetClass, "add|addAll|addElement");
            for (CtMethod targetMethod : targetMethods)
            {
                modifyCollectionAdd(targetMethod);
            }
        }
        else
        {
            return;
        }

        setNewClassfileBuffer(targetClass.toBytecode());
    }

    /**
     * 指定したクラスで宣言されているメソッドのリストを取得する。
     * 
     * @param ctClass クラス。
     * @param methodName メソッド名。
     * @return 指定したクラスで宣言されているメソッドのリスト。
     */
    public List<CtMethod> getDeclaredMethods(final CtClass ctClass, final String methodName)
    {
        List<CtMethod> result = new ArrayList<CtMethod>();

        for (CtMethod ctMethod : ctClass.getDeclaredMethods())
        {
            if (ctMethod.getName().matches(methodName) && ctMethod.isEmpty() == false)
            {
                result.add(ctMethod);
            }
        }

        return result;
    }
    
    /**
     * 指定した、Mapを実装したクラスで宣言されている、putメソッドのリストを取得する。<br />
     * ただし、取得するputメソッドは、引数が2（keyとvalue）であるもののみを対象とする。<br />
     * 
     * @param ctClass Mapを実装したクラス。
     * @return 指定したクラスで宣言されている、指定された名前を持つメソッドのリスト。
     */
    public List<CtMethod> getDeclaredPutMethods(final CtClass ctClass)
    {
        List<CtMethod> result = new ArrayList<CtMethod>();

        for (CtMethod ctMethod : ctClass.getDeclaredMethods())
        {
            if ("put".equals(ctMethod.getName()) && ctMethod.isEmpty() == false)
            {
                CtClass[] parameterTypes = new CtClass[0];
                try
                {
                    parameterTypes = ctMethod.getParameterTypes();
                }
                catch (NotFoundException ex)
                {
                    String key = "javelin.converter.CollectionMonitorConverter.ArgumentTypeCannotReferred";
                    String message = JavelinMessages.getMessage(key);
                    SystemLogger.getInstance().warn(message, ex);
                }
                if (parameterTypes.length == 2)
                {
                    result.add(ctMethod);
                }
            }
        }

        return result;
    }

    /**
     * {@inheritDoc}
     */
    public void init()
    {
        // Do Nothing.
    }

    /**
     * 監視マネージャへの登録コードを埋め込む
     * 
     * @param target
     * @throws CannotCompileException 
     */
    private void modifyMapAdd(final CtMethod target)
        throws CannotCompileException
    {
        // 対象のメソッドの引数の数を調べ、追加されたオブジェクトのインデックスを取得する。
        String element = "$";
        int argsLength = 0;
        try
        {
            argsLength = target.getParameterTypes().length;
            element += argsLength;
        }
        catch (NotFoundException ex)
        {
            SystemLogger.getInstance().warn("Parameter Types not Found.", ex);
            element = "null";
        }
        
        target.insertAfter(COLLECTIONTRACER_NAME
                            + ".preProcessMapPut(this, "+ element + ");", true);
        logModifiedMethod("CollectionMonitorConverter", target);
    }

    /**
     * 監視マネージャへの登録コードを埋め込む
     * 
     * @param target
     * @throws CannotCompileException 
     */
    private void modifyCollectionAdd(final CtMethod target)
        throws CannotCompileException
    {
        // 対象のメソッドの引数の数を調べ、追加されたオブジェクトのインデックスを取得する。
        String element = "$";
        int argsLength = 0;
        try
        {
            argsLength = target.getParameterTypes().length;
            element += argsLength;
        }
        catch (NotFoundException ex)
        {
            SystemLogger.getInstance().warn("Parameter Types not Found.", ex);
            element = "null";
        }
        
        target.insertAfter(COLLECTIONTRACER_NAME
                            + ".preProcessCollectionAdd(this, " + element + ");", true);
        logModifiedMethod("CollectionMonitorConverter", target);
    }

    /**
     * 除外対象となるクラスであるかどうかを判定します。(WAS7.0 対応)<br />
     * <br />
     * 除外対象条件:<br />
     * <li>ベンダーがIBMである。</li>
     * <li>仮想マシンがJava1.6に準拠している。</li>
     * <li>java.util.Hashtable か java.util.AbstractList である。</li>
     *
     * @return
     */
    private boolean isExcludeTarget(CtClass targetClass)
    {
        String vendor = System.getProperty("java.vendor");
        if (vendor.contains("IBM"))
        {
            String version = System.getProperty("java.specification.version");
            if ("1.6".equals(version))
            {
                String className = targetClass.getName();
                if ("java.util.Hashtable".equals(className)
                        || "java.util.AbstractList".equals(className))
                {
                    SystemLogger.getInstance().info("exclude class:" + className);
                    return true;
                }
            }
        }
        return false;
    }
}
