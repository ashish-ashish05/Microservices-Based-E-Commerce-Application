package com.ecommerce.order_service.command;

public interface OrderCommand {
    void execute();
    void undo();
}
