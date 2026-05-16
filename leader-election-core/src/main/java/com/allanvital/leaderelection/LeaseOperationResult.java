package com.allanvital.leaderelection;


/**
 * @author Allan Vital (https://allanvital.com)
 */
public enum LeaseOperationResult {

    ACQUIRED,
    RENEWED,
    HELD_BY_OTHER,
    NOT_OWNER,
    FAILED

}
