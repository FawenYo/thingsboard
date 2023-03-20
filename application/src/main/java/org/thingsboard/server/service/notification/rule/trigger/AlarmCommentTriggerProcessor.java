/**
 * Copyright © 2016-2023 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.service.notification.rule.trigger;

import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmComment;
import org.thingsboard.server.common.data.alarm.AlarmCommentType;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.alarm.AlarmStatusFilter;
import org.thingsboard.server.common.data.notification.info.AlarmCommentNotificationInfo;
import org.thingsboard.server.common.data.notification.info.NotificationInfo;
import org.thingsboard.server.common.data.notification.rule.trigger.AlarmCommentNotificationRuleTriggerConfig;
import org.thingsboard.server.common.data.notification.rule.trigger.NotificationRuleTriggerType;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.Set;

import static org.apache.commons.collections.CollectionUtils.isEmpty;

@Service
public class AlarmCommentTriggerProcessor implements RuleEngineMsgNotificationRuleTriggerProcessor<AlarmCommentNotificationRuleTriggerConfig> {

    @Override
    public boolean matchesFilter(TbMsg ruleEngineMsg, AlarmCommentNotificationRuleTriggerConfig triggerConfig) {
        if (ruleEngineMsg.getMetaData().getValue("comment") == null) {
            return false;
        }
        if (ruleEngineMsg.getType().equals(DataConstants.COMMENT_UPDATED) && !triggerConfig.isNotifyOnCommentUpdate()) {
            return false;
        }
        if (triggerConfig.isOnlyUserComments()) {
            AlarmComment comment = JacksonUtil.fromString(ruleEngineMsg.getMetaData().getValue("comment"), AlarmComment.class);
            if (comment.getType() == AlarmCommentType.SYSTEM) {
                return false;
            }
        }
        Alarm alarm = JacksonUtil.fromString(ruleEngineMsg.getData(), Alarm.class);
        return (isEmpty(triggerConfig.getAlarmTypes()) || triggerConfig.getAlarmTypes().contains(alarm.getType())) &&
                (isEmpty(triggerConfig.getAlarmSeverities()) || triggerConfig.getAlarmSeverities().contains(alarm.getSeverity())) &&
                (isEmpty(triggerConfig.getAlarmStatuses()) || AlarmStatusFilter.from(triggerConfig.getAlarmStatuses()).matches(alarm));
    }

    @Override
    public NotificationInfo constructNotificationInfo(TbMsg ruleEngineMsg, AlarmCommentNotificationRuleTriggerConfig triggerConfig) {
        AlarmComment comment = JacksonUtil.fromString(ruleEngineMsg.getMetaData().getValue("comment"), AlarmComment.class);
        AlarmInfo alarmInfo = JacksonUtil.fromString(ruleEngineMsg.getData(), AlarmInfo.class);
        return AlarmCommentNotificationInfo.builder()
                .comment(comment.getComment().get("text").asText())
                .action(ruleEngineMsg.getType().equals(DataConstants.COMMENT_CREATED) ? "added" : "updated")
                .userName(ruleEngineMsg.getMetaData().getValue("userName"))
                .alarmId(alarmInfo.getUuidId())
                .alarmType(alarmInfo.getType())
                .alarmOriginator(alarmInfo.getOriginator())
                .alarmOriginatorName(alarmInfo.getOriginatorName())
                .alarmSeverity(alarmInfo.getSeverity())
                .alarmStatus(alarmInfo.getStatus())
                .alarmCustomerId(alarmInfo.getCustomerId())
                .build();
    }

    @Override
    public NotificationRuleTriggerType getTriggerType() {
        return NotificationRuleTriggerType.ALARM_COMMENT;
    }

    @Override
    public Set<String> getSupportedMsgTypes() {
        return Set.of(DataConstants.COMMENT_CREATED, DataConstants.COMMENT_UPDATED);
    }

}