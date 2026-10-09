package com.frauddetect.service;

import com.frauddetect.dao.AlertDAO;
import com.frauddetect.exception.DatabaseException;
import com.frauddetect.model.Alert;
import com.frauddetect.model.RiskLevel;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service managing security alert monitoring, investigations, and administrative resolutions.
 */
public class AlertService {

    private final AlertDAO alertDAO;

    public AlertService(AlertDAO alertDAO) {
        this.alertDAO = alertDAO;
    }

    public List<Alert> getAllAlerts() throws DatabaseException {
        return alertDAO.findAll();
    }

    public List<Alert> getUnresolvedAlerts() throws DatabaseException {
        return alertDAO.findUnresolved();
    }

    public List<Alert> getUserAlerts(int userId) throws DatabaseException {
        return alertDAO.findByUser(userId);
    }

    public Optional<Alert> getAlertById(int alertId) throws DatabaseException {
        return alertDAO.findById(alertId);
    }

    /**
     * Resolves an active security alert by marking it reviewed, adding admin notes,
     * and labeling whether it was confirmed fraud or a false positive.
     */
    public boolean resolveAlert(int alertId, String adminNote, boolean confirmedFraud) throws DatabaseException {
        String note = (adminNote != null && !adminNote.isBlank()) ? adminNote.trim() : "Resolved by Administrator";
        return alertDAO.resolveWithFraudLabel(alertId, note, confirmedFraud);
    }

    /**
     * Resolves an active security alert by marking it reviewed and adding admin notes.
     *
     * @param alertId   the alert identifier
     * @param adminNote the administrator's review explanation
     * @return true if successfully resolved
     * @throws DatabaseException on database error
     */
    public boolean resolveAlert(int alertId, String adminNote) throws DatabaseException {
        return resolveAlert(alertId, adminNote, false);
    }

    /**
     * RUBRIC: 2 - Collections & Streams
     * Filters alerts by risk level using Java streams.
     */
    public List<Alert> getAlertsByRisk(RiskLevel level) throws DatabaseException {
        return alertDAO.findAll().stream()
                .filter(a -> a.getRiskLevel() == level)
                .collect(Collectors.toList());
    }

    public long getUnresolvedCount() throws DatabaseException {
        return alertDAO.findUnresolved().size();
    }
}
