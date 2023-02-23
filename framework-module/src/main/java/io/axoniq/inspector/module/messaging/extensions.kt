package io.axoniq.inspector.module.messaging

import io.axoniq.inspector.api.HandlerInformation
import io.axoniq.inspector.api.HandlerType
import io.axoniq.inspector.api.MessageInformation
import org.axonframework.commandhandling.CommandMessage
import org.axonframework.deadline.DeadlineMessage
import org.axonframework.eventhandling.EventMessage
import org.axonframework.messaging.Message
import org.axonframework.messaging.unitofwork.UnitOfWork
import org.axonframework.modelling.command.AggregateLifecycle
import org.axonframework.queryhandling.QueryMessage
import org.axonframework.queryhandling.SubscriptionQueryUpdateMessage

fun Message<*>.toInformation() = MessageInformation(
    when (this) {
        is DeadlineMessage<*> -> DeadlineMessage::class.java.simpleName
        is CommandMessage -> CommandMessage::class.java.simpleName
        is EventMessage -> EventMessage::class.java.simpleName
        is QueryMessage<*, *> -> QueryMessage::class.java.simpleName
        is SubscriptionQueryUpdateMessage<*> -> SubscriptionQueryUpdateMessage::class.java.simpleName
        else -> this::class.java.simpleName
    },
    when (this) {
        is CommandMessage -> this.commandName
        is QueryMessage<*, *> -> this.queryName
        is DeadlineMessage<*> -> this.deadlineName
        else -> this.payloadType.name
    }
)

fun UnitOfWork<*>.extractHandler(): HandlerInformation {
    return resources().computeIfAbsent(INSPECTOR_HANDLER_INFORMATION) {
        val processingGroup = resources()[INSPECTOR_PROCESSING_GROUP] as? String?
        val isAggregate = message is CommandMessage<*> && isAggregateLifecycleActive()
        val isProcessor = processingGroup != null
        HandlerInformation(
            type = when {
                isAggregate -> HandlerType.Aggregate
                isProcessor -> HandlerType.EventProcessor
                else -> HandlerType.Message
            },
            component = processingGroup ?: resources()[INSPECTOR_DECLARING_CLASS] as String?,
            message = message.toInformation(),
        )
    } as HandlerInformation
}

fun isAggregateLifecycleActive(): Boolean {
    return try {
        AggregateLifecycle.describeCurrentScope()
        true
    } catch (e: Exception) {
        false
    }
}