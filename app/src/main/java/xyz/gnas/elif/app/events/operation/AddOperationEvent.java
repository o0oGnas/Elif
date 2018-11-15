package xyz.gnas.elif.app.events.operation;

import xyz.gnas.elif.core.models.Operation;

public class AddOperationEvent extends OperationEvent {
    public AddOperationEvent(Operation operation) {
        super(operation);
    }
}
