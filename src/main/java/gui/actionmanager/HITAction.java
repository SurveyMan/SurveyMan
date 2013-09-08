package gui.actionmanager;


import com.amazonaws.mturk.requester.HIT;
import gui.ExperimentActions;
import gui.display.Experiment;
import system.mturk.ResponseManager;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

public class HITAction implements ActionListener {
    private ExperimentActions action;
    public HITAction(ExperimentActions action) {
        this.action = action;
    }
    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        switch (action) {
            case HITS_EXPIRE:
                List<HIT> expiredHITs = ResponseManager.expireOldHITs();
                for (HIT hit : expiredHITs)
                    Experiment.updateStatusLabel("Expired HIT "+hit.getHITId());
                break;
            case HITS_DELETE:
                List<HIT> deletedHITs = ResponseManager.deleteExpiredHITs();
                for (HIT hit : deletedHITs)
                    Experiment.updateStatusLabel("Deleted HIT "+hit.getHITId());
                break;
            case HITS_LIST_LIVE:
                List<HIT> hits = ResponseManager.assignableHITs();
                Experiment.updateStatusLabel("Assignable (available) HITs:");
                for (HIT hit : hits)
                    Experiment.updateStatusLabel(hit.getHITId());
                Experiment.updateStatusLabel("Unassignable (either currently being worked on or expired) HITs:");
                hits = ResponseManager.unassignableHITs();
                for (HIT hit : hits)
                    Experiment.updateStatusLabel(hit.getHITId());
                break;
        }
    }
}
