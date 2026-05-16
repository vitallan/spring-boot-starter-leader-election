package com.allanvital.leaderelection;


/**
 * @author Allan Vital (https://allanvital.com)
 */
public interface LeaderLeaseStore {

    LeaseOperationResult acquire(LeaseIdentity leaseIdentity);
    LeaseOperationResult renew(LeaseIdentity leaseIdentity);

}
