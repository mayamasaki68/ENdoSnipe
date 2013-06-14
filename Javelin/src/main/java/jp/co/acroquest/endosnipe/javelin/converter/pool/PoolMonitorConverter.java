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
package jp.co.acroquest.endosnipe.javelin.converter.pool;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jp.co.acroquest.endosnipe.javelin.converter.AbstractConverter;
import jp.co.acroquest.endosnipe.javelin.converter.pool.monitor.MonitoredPool;
import jp.co.acroquest.endosnipe.javelin.converter.pool.monitor.PoolMonitor;
import jp.co.smg.endosnipe.javassist.CannotCompileException;
import jp.co.smg.endosnipe.javassist.CtBehavior;
import jp.co.smg.endosnipe.javassist.CtClass;
import jp.co.smg.endosnipe.javassist.CtField;
import jp.co.smg.endosnipe.javassist.CtMethod;
import jp.co.smg.endosnipe.javassist.NotFoundException;

/**
 * Pool監視コンバータ。
 * Poolの監視を行い、グラフで利用するためのサイズ情報を取得する。
 * 
 * @author eriguchi
 */
public class PoolMonitorConverter extends AbstractConverter
{
    private final Map<String, String> maxFieldMap_ = new HashMap<String, String>();

    private final Map<String, String> numFieldMap_ = new HashMap<String, String>();

    /**
     * {@inheritDoc}
     */
    public void init()
    {
        // GenericObjectPool
        maxFieldMap_.put("org.apache.commons.pool.impl.GenericObjectPool", "_maxActive");
        numFieldMap_.put("org.apache.commons.pool.impl.GenericObjectPool", "_numActive");
        // SoftReferenceObjectPool
        numFieldMap_.put("org.apache.commons.pool.impl.SoftReferenceObjectPool", "_numActive");
        // StackObjectPool
        maxFieldMap_.put("org.apache.commons.pool.impl.StackObjectPool", "_maxSleeping");
        numFieldMap_.put("org.apache.commons.pool.impl.StackObjectPool", "_numActive");
        // GenericKeyedObjectPool
        maxFieldMap_.put("org.apache.commons.pool.impl.GenericKeyedObjectPool", "_maxActive");
        numFieldMap_.put("org.apache.commons.pool.impl.GenericKeyedObjectPool", "_totalActive");
        // StackKeyedObjectPool
        maxFieldMap_.put("org.apache.commons.pool.impl.StackKeyedObjectPool", "_maxSleeping");
        numFieldMap_.put("org.apache.commons.pool.impl.StackKeyedObjectPool", "_totActive");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void convertImpl()
        throws CannotCompileException,
            NotFoundException,
            IOException
    {
        CtClass ctClass = getCtClass();
        String maxFieldName = maxFieldMap_.get(ctClass.getName());
        String numFieldName = numFieldMap_.get(ctClass.getName());
        if (maxFieldName == null && numFieldName == null)
        {
            return;
        }

        CtClass monitorPoolIf = getClassPool().get(MonitoredPool.class.getCanonicalName());
        ctClass.addInterface(monitorPoolIf);

        CtField objectIdField = CtField.make("private java.lang.String objectId_;", ctClass);
        ctClass.addField(objectIdField);

        CtMethod setObjectIdMethod =
                CtMethod.make("public void setObjectId(String objectId){ objectId_ = objectId; }",
                              ctClass);
        CtMethod getObjectIdMethod =
                CtMethod.make("public String getObjectId(){ return objectId_; }", ctClass);
        ctClass.addMethod(setObjectIdMethod);
        ctClass.addMethod(getObjectIdMethod);

        CtMethod getMaxActiveMethod;
        if (maxFieldName != null)
        {
            getMaxActiveMethod =
                CtMethod.make("public int getDirectMaxActive(){ return this." + maxFieldName
                              + "; }", ctClass);
        }
        else
        {
            getMaxActiveMethod =
                CtMethod.make("public int getDirectMaxActive(){ return -1; }", ctClass);
        }
        
        CtMethod getNumActiveMethod;
        if (numFieldName != null)
        {
            getNumActiveMethod =
                CtMethod.make("public int getDirectNumActive(){ return this." + numFieldName
                              + "; }", ctClass);
        }
        else
        {
            getNumActiveMethod =
                CtMethod.make("public int getDirectNumActive(){ return -1; }", ctClass);
        }
        ctClass.addMethod(getMaxActiveMethod);
        ctClass.addMethod(getNumActiveMethod);

        List<CtBehavior> behaviorList = getMatcheDeclaredBehavior();
        for (CtBehavior ctBehavior : behaviorList)
        {
            convertBehavior(ctBehavior);
        }

        setNewClassfileBuffer(getCtClass().toBytecode());
    }

    /**
     * メソッドの振る舞いを修正する。
     * 
     * 
     * @param ctBehavior CtBehavior
     * @throws CannotCompileException コンパイルできない場合
     * @throws NotFoundException クラスが見つからない場合
     */
    private void convertBehavior(final CtBehavior ctBehavior)
        throws CannotCompileException,
            NotFoundException
    {
        ctBehavior.insertAfter(PoolMonitor.class.getCanonicalName() + ".addPool(this);");

        logModifiedMethod("PoolMonitorConverter", ctBehavior);
    }
}
