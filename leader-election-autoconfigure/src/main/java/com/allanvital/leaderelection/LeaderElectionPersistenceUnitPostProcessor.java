package com.allanvital.leaderelection;

import org.springframework.orm.jpa.persistenceunit.MutablePersistenceUnitInfo;
import org.springframework.orm.jpa.persistenceunit.PersistenceUnitPostProcessor;

/**
 * @author Allan Vital (https://allanvital.com)
 */
final class LeaderElectionPersistenceUnitPostProcessor implements PersistenceUnitPostProcessor {

    @Override
    public void postProcessPersistenceUnitInfo(MutablePersistenceUnitInfo pui) {
        pui.addManagedClassName(LeaderLockEntity.class.getName());
    }

}
