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
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.notification.info.EntityActionNotificationInfo;
import org.thingsboard.server.common.data.notification.info.NotificationInfo;
import org.thingsboard.server.common.data.notification.rule.trigger.EntityActionNotificationRuleTriggerConfig;
import org.thingsboard.server.common.data.notification.rule.trigger.NotificationRuleTriggerType;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class EntityActionTriggerProcessor implements RuleEngineMsgNotificationRuleTriggerProcessor<EntityActionNotificationRuleTriggerConfig> {

    @Override
    public boolean matchesFilter(TbMsg ruleEngineMsg, EntityActionNotificationRuleTriggerConfig triggerConfig) {
        String msgType = ruleEngineMsg.getType();
        if (msgType.equals(DataConstants.ENTITY_CREATED)) {
            if (!triggerConfig.isCreated()) {
                return false;
            }
        } else if (msgType.equals(DataConstants.ENTITY_UPDATED)) {
            if (!triggerConfig.isUpdated()) {
                return false;
            }
        } else if (msgType.equals(DataConstants.ENTITY_DELETED)) {
            if (!triggerConfig.isDeleted()) {
                return false;
            }
        } else {
            return false;
        }
        return triggerConfig.getEntityType() == null || getEntityType(ruleEngineMsg) == triggerConfig.getEntityType();
    }

    @Override
    public NotificationInfo constructNotificationInfo(TbMsg ruleEngineMsg, EntityActionNotificationRuleTriggerConfig triggerConfig) {
        EntityId entityId = ruleEngineMsg.getOriginator();
        String msgType = ruleEngineMsg.getType();
        ActionType actionType = msgType.equals(DataConstants.ENTITY_CREATED) ? ActionType.ADDED :
                                msgType.equals(DataConstants.ENTITY_UPDATED) ? ActionType.UPDATED :
                                msgType.equals(DataConstants.ENTITY_DELETED) ? ActionType.DELETED : null;
        return EntityActionNotificationInfo.builder()
                .entityId(actionType != ActionType.DELETED ? entityId : null)
                .entityName(ruleEngineMsg.getMetaData().getValue("entityName"))
                .actionType(actionType)
                .originatorUserId(UUID.fromString(ruleEngineMsg.getMetaData().getValue("userId")))
                .originatorUserName(ruleEngineMsg.getMetaData().getValue("userName"))
                .entityCustomerId(ruleEngineMsg.getCustomerId())
                .build();
    }

    private static EntityType getEntityType(TbMsg ruleEngineMsg) {
        return Optional.ofNullable(ruleEngineMsg.getMetaData().getValue("entityType"))
                .map(EntityType::valueOf).orElse(null);
    }

    @Override
    public NotificationRuleTriggerType getTriggerType() {
        return NotificationRuleTriggerType.ENTITY_ACTION;
    }

    @Override
    public Set<String> getSupportedMsgTypes() {
        return Set.of(DataConstants.ENTITY_CREATED, DataConstants.ENTITY_UPDATED, DataConstants.ENTITY_DELETED);
    }

}