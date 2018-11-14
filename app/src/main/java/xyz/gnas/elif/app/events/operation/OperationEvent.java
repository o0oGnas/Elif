package xyz.gnas.elif.app.events.operation;

import xyz.gnas.elif.app.models.Operation;

public abstract class OperationEvent {
    private Operation operation;

    public Operation getOperation() {
        return operation;
    }

    public OperationEvent(Operation operation) {
        this.operation = operation;
    }
}
