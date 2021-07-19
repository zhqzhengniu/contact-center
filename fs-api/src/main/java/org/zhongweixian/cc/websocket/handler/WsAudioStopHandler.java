package org.zhongweixian.cc.websocket.handler;

import org.cti.cc.po.AgentInfo;
import org.cti.cc.po.CallInfo;
import org.springframework.stereotype.Component;
import org.zhongweixian.cc.configration.HandlerType;
import org.zhongweixian.cc.websocket.event.WsAudioStopEvent;
import org.zhongweixian.cc.websocket.handler.base.WsBaseHandler;

/**
 * Created by caoliang on 2021/7/16
 * <p>
 * 取消静音
 */
@Component
@HandlerType("WS_AUDIO_STOP")
public class WsAudioStopHandler extends WsBaseHandler<WsAudioStopEvent> {
    @Override
    public void handleEvent(WsAudioStopEvent event) {
        AgentInfo agentInfo = getAgent(event);
        if (agentInfo == null || agentInfo.getCallId() == null) {

            return;
        }
        String deviceId = agentInfo.getDeviceId();
        CallInfo callInfo = cacheService.getCallInfo(agentInfo.getCallId());
        if (deviceId == null || callInfo == null) {

            return;
        }
        this.audioStop(callInfo.getMedia(), callInfo.getCallId(), deviceId);
    }
}