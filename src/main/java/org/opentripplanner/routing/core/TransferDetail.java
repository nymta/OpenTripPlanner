package org.opentripplanner.routing.core;

import org.onebusaway.gtfs.model.Stop;

public class TransferDetail {

    public int transferTime;
    public Stop requiredStop;

    public int getTransferTime(){
        return transferTime;
    }

    public Stop getRequiredStop() {
        return requiredStop;
    }

    public void setTransferTime(int transferTime) {
        this.transferTime = transferTime;
    }

    public void setRequiredStop(Stop requiredStop) {
        this.requiredStop = requiredStop;
    }

}
