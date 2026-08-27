package com.pharmasync.kafka;

public final class KafkaTopics {

    public static final String PRESCRIPTION_CREATED = "prescription.created";
    public static final String PRESCRIPTION_VALIDATED = "prescription.validated";
    public static final String INVENTORY_RESERVED = "inventory.reserved";
    public static final String MEDICINE_DISPENSED = "medicine.dispensed";
    public static final String INVENTORY_LOW = "inventory.low";
    public static final String MEDICINE_EXPIRING = "medicine.expiring";
    public static final String PURCHASE_RECEIVED = "purchase.received";
    public static final String INVENTORY_TRANSFERRED = "inventory.transferred";

    private KafkaTopics() {
    }
}
