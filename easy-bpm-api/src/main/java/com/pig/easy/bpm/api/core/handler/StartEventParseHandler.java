/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.pig.easy.bpm.api.core.handler;

import org.flowable.bpmn.model.*;
import org.flowable.common.engine.impl.util.CollectionUtil;
import org.flowable.engine.impl.bpmn.parser.BpmnParse;

/**
 * @author Joram Barrez
 * @author Tijs Rademakers
 */
public class StartEventParseHandler extends AbstractActivityBpmnParseHandler<StartEvent> {

    @Override
    public Class<? extends BaseElement> getHandledType() {
        return StartEvent.class;
    }

    @Override
    protected void executeParse(BpmnParse bpmnParse, StartEvent element) {
        if (element.getSubProcess() != null && element.getSubProcess() instanceof EventSubProcess) {
            if (CollectionUtil.isNotEmpty(element.getEventDefinitions())) {
                EventDefinition eventDefinition = element.getEventDefinitions().get(0);
                if (eventDefinition instanceof MessageEventDefinition) {
                    MessageEventDefinition messageDefinition = fillMessageRef(bpmnParse, eventDefinition);
                    element.setBehavior(bpmnParse.getActivityBehaviorFactory().createEventSubProcessMessageStartEventActivityBehavior(element, messageDefinition));

                } else if (eventDefinition instanceof SignalEventDefinition) {
                    SignalEventDefinition signalDefinition = (SignalEventDefinition) eventDefinition;
                    Signal signal = null;
                    if (bpmnParse.getBpmnModel().containsSignalId(signalDefinition.getSignalRef())) {
                        signal = bpmnParse.getBpmnModel().getSignal(signalDefinition.getSignalRef());
                    }

                    element.setBehavior(bpmnParse.getActivityBehaviorFactory().createEventSubProcessSignalStartEventActivityBehavior(
                            element, signalDefinition, signal));

                } else if (eventDefinition instanceof TimerEventDefinition) {
                    TimerEventDefinition timerEventDefinition = (TimerEventDefinition) eventDefinition;
                    element.setBehavior(bpmnParse.getActivityBehaviorFactory().createEventSubProcessTimerStartEventActivityBehavior(
                            element, timerEventDefinition));

                } else if (eventDefinition instanceof ErrorEventDefinition) {
                    element.setBehavior(bpmnParse.getActivityBehaviorFactory().createEventSubProcessErrorStartEventActivityBehavior(element));
                }
            }

        } else if (CollectionUtil.isEmpty(element.getEventDefinitions())) {
            element.setBehavior(bpmnParse.getActivityBehaviorFactory().createNoneStartEventActivityBehavior(element));
        
        } else if (CollectionUtil.isNotEmpty(element.getEventDefinitions())) {
            EventDefinition eventDefinition = element.getEventDefinitions().get(0);
            if (eventDefinition instanceof MessageEventDefinition) {
                fillMessageRef(bpmnParse, eventDefinition);
            }
        }

        if (element.getSubProcess() == null && (CollectionUtil.isEmpty(element.getEventDefinitions()) ||
                bpmnParse.getCurrentProcess().getInitialFlowElement() == null)) {
            
            bpmnParse.getCurrentProcess().setInitialFlowElement(element);
        }
    }
    
    protected MessageEventDefinition fillMessageRef(BpmnParse bpmnParse, EventDefinition eventDefinition) {
        MessageEventDefinition messageDefinition = (MessageEventDefinition) eventDefinition;
        BpmnModel bpmnModel = bpmnParse.getBpmnModel();
        String messageRef = messageDefinition.getMessageRef();
        if (bpmnModel.containsMessageId(messageRef)) {
            Message message = bpmnModel.getMessage(messageRef);
            messageDefinition.setMessageRef(message.getName());
            messageDefinition.setExtensionElements(message.getExtensionElements());
        }
        
        return messageDefinition;
    }

}
