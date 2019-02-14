package org.opentripplanner.updater.stoptime;

public class TripUpdateStats {

    private int appliedUpdates;
    private int totalUpdates;
    private int addedUpdates;
    private int addedSuccess;
    private int scheduledUpdates;
    private int scheduledSuccess;
    private int cancelledUpdates;
    private int cancelledSuccess;
    private String feedId;

    public int getAppliedUpdates() {
        return appliedUpdates;
    }

    public void setAppliedUpdates(int appliedUpdates) {
        this.appliedUpdates = appliedUpdates;
    }

    public int getTotalUpdates() {
        return totalUpdates;
    }

    public void setTotalUpdates(int totalUpdates) {
        this.totalUpdates = totalUpdates;
    }

    public int getAddedUpdates() {
        return addedUpdates;
    }

    public void setAddedUpdates(int addedUpdates) {
        this.addedUpdates = addedUpdates;
    }

    public int getAddedSuccess() {
        return addedSuccess;
    }

    public void setAddedSuccess(int addedSuccess) {
        this.addedSuccess = addedSuccess;
    }

    public int getScheduledUpdates() {
        return scheduledUpdates;
    }

    public void setScheduledUpdates(int scheduledUpdates) {
        this.scheduledUpdates = scheduledUpdates;
    }

    public int getScheduledSuccess() {
        return scheduledSuccess;
    }

    public void setScheduledSuccess(int scheduledSuccess) {
        this.scheduledSuccess = scheduledSuccess;
    }

    public int getCancelledUpdates() {
        return cancelledUpdates;
    }

    public void setCancelledUpdates(int cancelledUpdates) {
        this.cancelledUpdates = cancelledUpdates;
    }

    public int getCancelledSuccess() {
        return cancelledSuccess;
    }

    public void setCancelledSuccess(int cancelledSuccess) {
        this.cancelledSuccess = cancelledSuccess;
    }

    public String getFeedId() {
        return feedId;
    }

    public void setFeedId(String feedId) {
        this.feedId = feedId;
    }
}
