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
package jp.co.acroquest.endosnipe.web.dashboard.util;

import jp.co.acroquest.endosnipe.common.entity.ItemType;
import jp.co.acroquest.endosnipe.communicator.entity.Body;
import jp.co.acroquest.endosnipe.communicator.entity.Header;
import jp.co.acroquest.endosnipe.communicator.entity.Telegram;
import jp.co.acroquest.endosnipe.communicator.entity.TelegramConstants;
import jp.co.acroquest.endosnipe.web.dashboard.constants.SignalConstants;
import jp.co.acroquest.endosnipe.web.dashboard.dto.SignalDefinitionDto;
import jp.co.acroquest.endosnipe.web.dashboard.dto.SignalTreeMenuDto;

/**
 * 閾値判定定義情報に関するUtilクラスです。
 * @author fujii
 *
 */
public class SignalUtil
{

    /** シグナルのオブジェトに設定する引数の数 */
    private static final int SIGNAL_ARGUMENT_COUNT = 6;

    /**
     * インスタンス化を阻止するprivateコンストラクタです。
     */
    private SignalUtil()
    {
        // Do Nothing.
    }

    /**
     * 閾値判定定義情報のツリーオブジェクトを生成する。
     * @param signalName シグナル名（ツリーのフルパスを設定する。)
     * @param signalValue シグナルの状態値
     * @param level 閾値判定定義のレベル
     * @return 閾値判定定義情報のツリーオブジェクト
     */
    public static SignalTreeMenuDto createSignalMenu(final String signalName,
            final int signalValue, final int level)
    {
        SignalTreeMenuDto signalTreeMenu = new SignalTreeMenuDto();
        signalTreeMenu.setId(signalName);
        signalTreeMenu.setTreeId(signalName);
        signalTreeMenu.setType("signal");
        signalTreeMenu.setSignalValue(Integer.valueOf(signalValue));

        String icon = "";
        if (level == SignalConstants.STATE_LEVEL_3)
        {
            if (0 <= signalValue && signalValue < SignalConstants.STATE_LEVEL_3)
            {
                icon = SignalConstants.SIGNA_ICON_PREFIX + 2 * signalValue;
            }
            else
            {
                icon = SignalConstants.SIGNAL_ICON_STOP;
            }
        }
        else if (level == SignalConstants.STATE_LEVEL_5)
        {
            if (0 <= signalValue && signalValue < SignalConstants.STATE_LEVEL_5)
            {
                icon = SignalConstants.SIGNA_ICON_PREFIX + signalValue;
            }
            else
            {
                icon = SignalConstants.SIGNAL_ICON_STOP;
            }
        }
        else
        {
            icon = SignalConstants.SIGNAL_ICON_STOP;
        }

        signalTreeMenu.setIcon(icon);
        return signalTreeMenu;
    }

    /**
     * 閾値判定定義情報の追加を通知する電文を作成する。
     * 
     * @param signalDefinitionDto シグナル定義情報のリスト
     * @param itemName 項目名
     * @return 全閾値情報を取得する電文
     */
    public static Telegram createAddSignalDefinition(final SignalDefinitionDto signalDefinitionDto,
            final String itemName)
    {
        Telegram telegram = new Telegram();

        Header requestHeader = new Header();
        requestHeader.setByteTelegramKind(TelegramConstants.BYTE_TELEGRAM_KIND_SIGNAL_DEFINITION);
        requestHeader.setByteRequestKind(TelegramConstants.BYTE_REQUEST_KIND_NOTIFY);

        // 閾値判定定義情報
        Body signalBody = new Body();

        signalBody.setStrObjName(TelegramConstants.OBJECTNAME_SIGNAL_CHANGE);
        signalBody.setStrItemName(itemName);
        signalBody.setByteItemMode(ItemType.ITEMTYPE_STRING);

        long signalId = signalDefinitionDto.getSignalId();
        String signalName = signalDefinitionDto.getSignalName();
        String matchingPattern = signalDefinitionDto.getMatchingPattern();
        int level = signalDefinitionDto.getLevel();
        double escalationPeriod = signalDefinitionDto.getEscalationPeriod();
        String patternValue = signalDefinitionDto.getPatternValue();

        int dtoCount = SIGNAL_ARGUMENT_COUNT;
        signalBody.setIntLoopCount(dtoCount);
        String[] signalDefObj =
                { String.valueOf(signalId), signalName, matchingPattern, String.valueOf(level),
                        String.valueOf(escalationPeriod), patternValue };
        signalBody.setObjItemValueArr(signalDefObj);

        Body[] requestBodys = { signalBody };

        telegram.setObjHeader(requestHeader);
        telegram.setObjBody(requestBodys);

        return telegram;
    }

}
